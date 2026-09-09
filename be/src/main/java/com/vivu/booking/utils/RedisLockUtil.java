package com.vivu.booking.utils;

import com.vivu.booking.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Khoa phan tan (distributed lock) tren Redis — TAI LIEU TU HOC.
 *
 * <h2>Vai tro: lop phong ve thu 1, KHONG phai nguon chan ly</h2>
 * <p>
 * Co 2 lop chong double-booking trong he nay:
 * <ol>
 *   <li><b>Redis lock (lop nay)</b> — chan tu ngo: 100 request cung bam "Dat ngay" thi 99 request
 *       bi tu choi ngay tai day, <i>khong cham DB</i>. Re, nhanh, giam lock contention tren Postgres.</li>
 *   <li><b>{@code SELECT ... FOR UPDATE} tren dong Room + RoomCalendar (trong Hibernate transaction)</b>
 *       — day moi la nguon chan ly quyet dinh co duoc dat hay khong.</li>
 * </ol>
 *
 * <h2>Vai sao Redis lock KHONG du de dam bao dung?</h2>
 * <ul>
 *   <li>Redis co the <b>mat dien / failover</b>: khoa da cap phat bien mat, 2 client tuong minh doc quyen.</li>
 *   <li>Khoa co <b>TTL</b>: neu tx cua ta chay lau hon TTL, khoa tu het han va request khac lot vao.</li>
 *   <li>Redis va Postgres la <b>2 he thong rieng</b> — khong co transaction chung (khong 2PC).</li>
 * </ul>
 * Vi vay nguyen tac: <i>Redis lock chi de giam tai, DB lock moi de dung</i>. Bo Redis lock di thi
 * he van dung (cham hon); bo DB lock di thi he SAI (ban trung phong).
 *
 * <h2>Vai sao phai dung token + Lua script khi tra khoa?</h2>
 * <p>
 * Gia su khong dung token:
 * <pre>
 *   T0: Request A lay khoa, TTL 8s
 *   T8: A van dang chay tx -> khoa TU HET HAN
 *   T9: Request B lay khoa (hop le, A het han roi)
 *   T10: A chay xong, goi DEL -> <b>XOA MAT KHOA CUA B</b>
 *   T11: Request C lay khoa -> B va C cung chay = race condition
 * </pre>
 * Cach sua: moi lan lay khoa sinh 1 {@code token} ngau nhien (UUID), khi tra phai kiem tra
 * "khoa nay con la cua ta khong". Viec <b>doc + so sanh + xoa</b> phai la 1 lenh Lua de Redis
 * thuc hien atomic — neu tach thanh GET roi DEL thi giua 2 buoc khoa van co the het han.
 *
 * <h2>Vai sao {@code SET NX EX} chu khong phai {@code SETNX} + {@code EXPIRE}?</h2>
 * <p>
 * {@code SETNX} roi {@code EXPIRE} la <b>2 lenh</b>. Neu tien trinh chet dung giua 2 lenh do,
 * khoa ton tai <i>vinh vien</i> (khong TTL) → phong bi khoa chet, khong ai dat duoc nua.
 * {@code SET key val NX EX ttl} gop ca hai thanh 1 lenh atomic.
 */
public final class RedisLockUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisLockUtil.class);

    /** Buoc ngu giua 2 lan thu lai khoa — 100ms du nhay de khong tre user, du thua de khong bao Redis. */
    private static final long RETRY_BACKOFF_MILLIS = 100;

    /**
     * Tra khoa co dieu kien: chi xoa khi token dung bang gia tri dang luu.
     * Redis thuc hien toan bo script nhu 1 lenh duy nhat (atomic).
     */
    private static final String UNLOCK_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

    private RedisLockUtil() {
    }

    /**
     * Ket qua cua 1 lan xin khoa. Tach 3 trang thai de caller xu ly dung:
     * <ul>
     *   <li>{@code ACQUIRED}  — da giu khoa, {@link #token()} dung de tra khoa.</li>
     *   <li>{@code BUSY}      — <b>co nguoi khac dang giu</b> → day la race that, nen tra 409.</li>
     *   <li>{@code UNAVAILABLE} — <b>Redis khong lien lac duoc</b>. KHONG the ket luan gi ve race,
     *       nen dung lam co so tu choi. Cach xu ly dung: <i>fail-open</i> — di tiep xuong DB,
     *       de {@code SELECT ... FOR UPDATE} quyet dinh. Tu choi o day = Redis chet thi ca he
     *       khong ai dat duoc phong, tuc la bien Redis thanh single point of failure.</li>
     * </ul>
     * Loi cu cua ban truoc: gop BUSY va UNAVAILABLE thanh cung 1 gia tri {@code null},
     * nen khong phan biet duoc "dang bi giu" voi "Redis chet".
     */
    /**
     * Nhà máy tạo LockResult. Ten phuong thuc duoc dat khac cac phuong thuc kiem tra
     * ({@code acquired()}/{@code busy()}) vi trong {@code record}, ham tinh va ham instance
     * cung ten + cung so tham so la loi bien dich — va loi bien dich o pha doc source
     * se kham luon ca lombok annotation processing, khien hang chuc file khac bao
     * "cannot find symbol" cho nhung getter chua bao gio bi sua.
     */
    public record LockResult(Status status, String token) {
        public enum Status { ACQUIRED, BUSY, UNAVAILABLE }

        public boolean acquired() {
            return status == Status.ACQUIRED;
        }

        public boolean busy() {
            return status == Status.BUSY;
        }

        static LockResult granted(String token) {
            return new LockResult(Status.ACQUIRED, token);
        }

        static LockResult heldByOther() {
            return new LockResult(Status.BUSY, null);
        }

        static LockResult redisDown() {
            return new LockResult(Status.UNAVAILABLE, null);
        }
    }

    /**
     * Xin khoa, khong cho: duoc thi {@code ACQUIRED}, co nguoi giu thi {@code BUSY}.
     *
     * @param key        ten khoa, vd {@code booking:lock:room:42}
     * @param ttlSeconds khoa tu het han sau bao nhieu giay — <b>phai lon hon thoi gian tx chay</b>,
     *                   nhung cung khong qua dai vi neu tien trinh chet ma khong kip tra khoa
     *                   thi moi nguoi phai cho het TTL
     * @return ket qua 3 trang thai, xem {@link LockResult}
     */
    public static LockResult tryAcquire(String key, int ttlSeconds) {
        String token = UUID.randomUUID().toString();
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            // SET key token NX EX ttl — 1 lenh atomic: chi dat neu chua ton tai, kem TTL.
            String r = jedis.set(key, token, SetParams.setParams().nx().ex(ttlSeconds));
            return "OK".equals(r) ? LockResult.granted(token) : LockResult.heldByOther();
        } catch (Exception e) {
            // Redis chet: log de biet, nhung KHONG tu choi nghiep vu (fail-open xuong DB lock).
            log.warn("Redis lock UNAVAILABLE key={} — bo qua lop Redis, dua vao DB lock: {}", key, e.toString());
            return LockResult.redisDown();
        }
    }

    /**
     * Tra khoa. Chi thuc hien khi {@code result.acquired()} — 2 trang thai con lai khong co gi de tra.
     * An toan de goi trong {@code finally}: khong bao gio nem exception ra ngoai.
     */
    public static void release(String key, LockResult result) {
        if (result == null || !result.acquired()) {
            return;
        }
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.eval(UNLOCK_SCRIPT, List.of(key), List.of(result.token()));
        } catch (Exception e) {
            // Khong tra duoc thi khoa van tu bien mat sau TTL — he tu phuc hoi, chi la cham hon.
            log.warn("Khong tra duoc Redis lock key={} (se tu het han theo TTL): {}", key, e.toString());
        }
    }

    /**
     * Chay {@code work} trong khoa; neu <b>co nguoi khac dang giu</b> thi chay {@code onBusy}.
     *
     * <p>Diem quan trong nhat cua helper nay la cai {@code finally}: du {@code work} thanh cong,
     * nem 409, hay nem SQLException thi khoa <b>luon luon</b> duoc tra. Thieu {@code finally}
     * la loi pho bien nhat khi dung lock — 1 request loi se khoa phong den het TTL.
     *
     * @param key        ten khoa
     * @param ttlSeconds TTL cua khoa
     * @param work       nghiep vu chay khi giu duoc khoa (hoac khi Redis khong san dung)
     * @param onBusy     xu ly khi khoa dang bi nguoi khac giu (thuong la tra 409)
     */
    public static <T> T withLock(String key, int ttlSeconds, Supplier<T> work, Supplier<T> onBusy) {
        LockResult lock = tryAcquire(key, ttlSeconds);
        if (lock.busy()) {
            return onBusy.get();
        }
        try {
            return work.get();
        } finally {
            release(key, lock);
        }
    }

    /**
     * Nhu {@link #withLock(String, int, Supplier, Supplier)} nhung <b>cho toi da {@code maxWaitMillis}</b>
     * truoc khi ket luan BUSY.
     *
     * <p>Vai sao can ban nay: khoa o day duoc giu rat ngan (mot transaction insert, vai chuc ms).
     * Truong hop gap nhieu nhat khong phai "100 nguoi cung dat" ma la <b>1 nguoi bam nut 2 lan</b>
     * (double-click) hoac FE retry sau timeout. Voi ban khong cho, lan bam thu 2 nhan 409 ngay
     * trong khi lan 1 sap xong — user thay loi du khong co loi gi. Cho ~1.5s thi lan 2 thuong
     * lay duoc khoa va bi chan dung o lop DB (phong het) — thong bao chinh xac hon.
     *
     * <p>Khong nen cho lau: cho dai = giu connection HTTP + thread pool, va neu tat ca request
     * deu cho thi mot phong "hot" se dung hang ca he. Do do co {@code maxWaitMillis} nho.
     *
     * @param maxWaitMillis thoi gian cho toi da; {@code <= 0} thi khong cho (giong ban 4 tham so)
     */
    public static <T> T withLock(String key, int ttlSeconds, long maxWaitMillis,
                                 Supplier<T> work, Supplier<T> onBusy) {
        if (maxWaitMillis <= 0) {
            return withLock(key, ttlSeconds, work, onBusy);
        }
        long deadline = System.currentTimeMillis() + maxWaitMillis;
        LockResult lock = tryAcquire(key, ttlSeconds);
        // Chi thu lai khi BUSY. UNAVAILABLE khong can cho: Redis dang chet, thu lai cung vay,
        // cu di tiep (fail-open) de DB lock quyet dinh.
        while (lock.busy() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(RETRY_BACKOFF_MILLIS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            lock = tryAcquire(key, ttlSeconds);
        }
        if (lock.busy()) {
            return onBusy.get();
        }
        try {
            return work.get();
        } finally {
            release(key, lock);
        }
    }
}
