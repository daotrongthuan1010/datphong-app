package com.vivu.booking.scheduler;

import com.vivu.booking.service.BookingService;
import com.vivu.booking.service.impl.BookingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job tu dong het han HOLD — TAI LIEU TU HOC.
 *
 * <h2>Vai tro</h2>
 * <p>
 * Moi lan user bam "Giu cho 15 phut", hệ thống tạo {@code Booking(HOLD)} va danh dau
 * {@code RoomCalendar(BLOCKED)} cho tung dem. Neu user khong thanh toan, khoi BLOCKED phai
 * quay lai AVAILABLE — neu khong phong bi "khoa ma" den khi co xoa tay.
 *
 * <h2>Sao class nay mong den vay?</h2>
 * <p>
 * Toan bo logic nghiep vu (gianh Redis lock {@code job:hold-expire} de nhieu node khong quat
 * trung nhau, tim HOLD qua han, doi sang EXPIRED, tra lich <b>co bao ve</b> — chi tra dem khi
 * khong con booking ACTIVE khac phu len dem do) nam trong
 * {@link BookingService#expireHolds()}. Class nay chi lam dung mot viec: <b>go</b> no dung
 * chu ky. Vi tri dat logic la service — o day dat no thi moi luong (ke ca admin bam nut "quat
 * thu cong") di qua cung mot doan code, khong the lech nhau.
 *
 * <h2>Vì sao khong bat logic o day?</h2>
 * Khi viet tach ra thi sinh ra hai ban khong gio nhau: ban trong service dung (co job lock, co
 * bao ve dem), ban trong scheduler thieu job lock → hai Tomcat cung chay la tra nhat lich cua nhau.
 * Day la vi du dien hinh cua "duplicate code" — loi khong phai o viec goi sai, ma o cho co hai
 * noi quyet dinh cung mot viec.
 *
 * <h2>Vì sao dung ScheduledExecutorService chu khong {@code @Scheduled} hay Timer?</h2>
 * <ul>
 *   <li>Day la Servlet/Tomcat thuan (khong Spring) → khong co {@code @Scheduled}.</li>
 *   <li>{@code java.util.Timer} chay bang 1 thread va <b>chet im</b> neu task nem exception
 *       khong duoc bat → job dung chay sau vai ngay ma khong ai biet. Executor giu chu ky song.</li>
 *   <li>Thread daemon de context destroy khong bi ke.</li>
 * </ul>
 *
 * <h2>Vì sao delay 15 giay khi khoi dong?</h2>
 * Luc {@code contextInitialized} thi DB pool va Redis moi duoc mo; chay ngay de tranh
 * moi quan he chua san sang va de 2 node cung khoi dong khong va nhau.
 */
public class HoldExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpireScheduler.class);

    /** Cho DB/Redis khoi dong xong. */
    private static final long INITIAL_DELAY_SECONDS = 15;
    /** Chu ky quat — nho hon nhieu so voi TTL 15 phut cua HOLD nen do tre toi da la 1 phut. */
    private static final long PERIOD_SECONDS = 60;

    private final BookingService bookingService;
    private ScheduledExecutorService executor;

    public HoldExpireScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Servlet listener khoi dong no-arg — tu dung service. */
    public HoldExpireScheduler() {
        this(new BookingServiceImpl());
    }

    /** Bat dau chay dinh ky — goi tu {@code AppContextListener.contextInitialized}. */
    public void start() {
        if (executor != null && !executor.isShutdown()) {
            log.warn("HoldExpireScheduler dang chay — bo qua lan start tiep theo");
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hold-expire");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::runQuietly, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
        log.info("HoldExpireScheduler khoi dong (initialDelay={}s, period={}s)", INITIAL_DELAY_SECONDS, PERIOD_SECONDS);
    }

    /** Dung chay — goi tu {@code AppContextListener.contextDestroyed}. */
    public void stop() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("HoldExpireScheduler da dung");
    }

    /**
     * Vo bao ngoai lai moi exception.
     *
     * <p>{@code scheduleAtFixedRate} <b>huy hoan toan chu ky</b> neu task nem ra exception.
     * Khong co vong try nay thi chi can mot lan DB glitch la job chet vinh vien va moi HOLD
     * con lai khong bao gio duoc expire — khep kin loi trang man hinh.
     */
    private void runQuietly() {
        try {
            int expired = bookingService.expireHolds();
            if (expired > 0) {
                log.info("HoldExpireScheduler: {} HOLD qua han -> EXPIRED", expired);
            } else {
                log.debug("HoldExpireScheduler: khong co HOLD qua han");
            }
        } catch (Exception e) {
            log.error("HoldExpireScheduler lot so (giu nguyen chu ky): {}", e.toString(), e);
        }
    }
}
