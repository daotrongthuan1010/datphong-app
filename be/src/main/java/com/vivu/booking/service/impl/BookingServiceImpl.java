package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.config.RedisConfig;
import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.RoomCalendarDao;
import com.vivu.booking.dao.RoomDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Room;
import com.vivu.booking.entity.RoomCalendar;
import com.vivu.booking.entity.User;
import com.vivu.booking.entity.Voucher;
import com.vivu.booking.entity.VoucherUsage;
import com.vivu.booking.enums.BookingStatusType;
import com.vivu.booking.enums.CalendarStatusType;
import com.vivu.booking.enums.DiscountTypeEnum;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.BookingMapper;
import com.vivu.booking.service.BookingService;
import com.vivu.booking.utils.AppProperties;
import com.vivu.booking.utils.RedisLockUtil;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * NGHIEP VU DAT PHONG — TRANSACTIONAL &amp; CHONG DOUBLE-BOOKING.
 *
 * <h2>Ba ao giac phai tranh</h2>
 * <ol>
 *   <li><b>"check existsOverlap roi create la du":</b> hai request song song cung doc thay
 *   "chua ai dat", roi ca hai cung ghi → 1 phong 2 booking cung dem (over-booking).</li>
 *   <li><b>"RoomCalendar vo ich, co Booking la du":</b> FE can bang lich thang to mau tung ngay.
 *   Khong co RoomCalendar thi moi request phai chong lan toan bo don — dat va khong phan anh
 *   duoc gia theo ngay (priceOverride).</li>
 *   <li><b>"cancel xong lich tu ranh":</b> neu khong tra RoomCalendar ve AVAILABLE thi phong bi
 *   BLOCKED vinh vien o cac ngay do → mat doanh thu.</li>
 * </ol>
 *
 * <h2>Hai lop khoa</h2>
 * <table border="1">
 *   <tr><th>Lop</th><th>Vai tro</th><th>Khi Redis chet</th></tr>
 *   <tr><td>{@link RedisLockUtil} (ngoai)</td><td>Barrier: chan som, request thua tra 409 ngay, khong cham DB</td>
 *       <td><b>Fail-open</b> — bo qua, di thang xuong DB</td></tr>
 *   <tr><td>{@code SELECT ... FOR UPDATE} (trong)</td><td>Chan ly cuoi cung, quyet dinh ai thang</td>
 *       <td>Khong phu thuoc Redis, luon hoat dong</td></tr>
 * </table>
 *
 * <p>Lock trong DB theo thu tu co dinh <b>Room → RoomCalendar → Booking</b> o moi luong
 * (create/cancel/confirm/expire) de khong tao chu trinh cho = khong deadlock.
 *
 * <p>Moi thay doi trang thai nam trong <b>mot transaction Hibernate duy nhat</b> tren mot Session,
 * mo qua {@link BookingDao#inTransaction}. Tach thanh 2-3 transaction nho la sai: giua hai
 * transaction do mot request khac co the chen vao va commit truoc.
 */
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    /** Thoi gian giu cho mac dinh, dem nguoc tren FE. */
    private static final int HOLD_MINUTES = AppProperties.getInt("booking.hold-minutes", 15);
    /** TTL lock Redis — chi can song lau hon mot transaction tao booking. */
    private static final int LOCK_TTL_SECONDS = AppProperties.getInt("booking.lock-ttl-seconds", 10);
    /** Cho gianh lock toi da — user bam dup nut thi cho ~1.5s la du. */
    private static final long LOCK_WAIT_MILLIS = AppProperties.getLong("booking.lock-wait-millis", 1500);
    /** So HOLD toi da moi luot quat cua scheduler. */
    private static final int EXPIRE_BATCH = AppProperties.getInt("booking.expire-batch-size", 100);

    /** Lock chong 2 Tomcat chay song song cung quat HOLD. */
    private static final String JOB_LOCK_KEY = "job:hold-expire";
    private static final String ROOM_LOCK_PREFIX = "booking:lock:room:";
    private static final String HOLD_KEY_PREFIX = "booking:hold:";

    /** Trang thai cuoi — booking khong the quay lai vong doi dat phong. */
    private static final Set<BookingStatusType> TERMINAL =
            Set.of(BookingStatusType.CANCELLED, BookingStatusType.COMPLETED,
                    BookingStatusType.REFUNDED, BookingStatusType.EXPIRED);

    private final BookingDao bookingDao;
    private final RoomDao roomDao;
    private final RoomCalendarDao roomCalendarDao;
    private final UsersDao usersDao;
    /**
     * Chi dung o {@link #markExpired}: truoc khi tra phong ve AVAILABLE phai biet booking nay
     * da co giao dich SUCCESS chua. Thieu buoc kiem tra nay thi mot webhook den tre (cong retry)
     * se gap phong da bi ban cho nguoi khac — tien da thu ma khong con phong.
     */
    private final com.vivu.booking.dao.PaymentDao paymentDao;

    public BookingServiceImpl(BookingDao bookingDao, RoomDao roomDao, RoomCalendarDao roomCalendarDao,
                              UsersDao usersDao, com.vivu.booking.dao.PaymentDao paymentDao) {
        this.bookingDao = bookingDao;
        this.roomDao = roomDao;
        this.roomCalendarDao = roomCalendarDao;
        this.usersDao = usersDao;
        this.paymentDao = paymentDao;
    }

    public BookingServiceImpl(BookingDao bookingDao, RoomDao roomDao, RoomCalendarDao roomCalendarDao,
                              UsersDao usersDao) {
        this(bookingDao, roomDao, roomCalendarDao, usersDao, new com.vivu.booking.dao.PaymentDao());
    }

    /** Servlet khoi tao no-arg — tu dung cac DAO. */
    public BookingServiceImpl() {
        this(new BookingDao(), new RoomDao(), new RoomCalendarDao(), new UsersDao(),
                new com.vivu.booking.dao.PaymentDao());
    }

    /** @deprecated chi de code cu con bien dich; dung constructor 4 tham so hoac no-arg. */
    @Deprecated
    public BookingServiceImpl(BookingDao bookingDao, RoomDao roomDao, UsersDao usersDao) {
        this(bookingDao, roomDao, new RoomCalendarDao(), usersDao);
    }

    // ==================================================================== create (HOLD)

    /**
     * Giu cho — tao {@code Booking(HOLD)} va BLOCK cac dem {@code [checkin, checkout)}.
     *
     * <p>Trinh tu:
     * <ol>
     *   <li>Validate re tien (ngay, suc chua) truoc khi ton tai nguyen lock.</li>
     *   <li>Gianh Redis lock theo <b>roomId</b> (khong ghep ngay — giai thich ben duoi).</li>
     *   <li>Mot transaction: {@code FOR UPDATE} tren Room → load/tao cac dong RoomCalendar
     *   cung FOR UPDATE → ngay nao != AVAILABLE thi 409 → dem booking chong lan (bao hiem cho
     *   du lieu cu chua co dong lich) → ap voucher → persist HOLD → BLOCK lich.</li>
     *   <li>Ghi key {@code booking:hold:{id}} TTL = HOLD_MINUTES de admin ra nhanh tren Redis.</li>
     * </ol>
     *
     * <p><b>Vì sao lock theo roomId chu khong theo {@code roomId:checkin_checkout}?</b>
     * Hai yeu cau [1/9 → 5/9] va [3/9 → 7/9] <b>chong lan nhau</b> nhung chuoi ngay khac nhau,
     * neu ghep ngay vao key thi chung gianh duoc hai lock khac nhau va cung lao vao DB.
     * Redis lock luc do vo dung. Khoa theo roomId moi dung la mutex cua tai nguyen "cai phong".
     *
     * @throws BusinessException 400 (ngay/suc chua/voucher), 403 (tai khoan khoa), 404 (voucher),
     *                           409 (trung lich hoac phong dang co giao dich khac)
     */
    @Override
    public BookingResponse create(Long userId, BookingCreateRequest req) {
        validateRange(req);

        Room peek = roomDao.findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + req.getRoomId()));
        if (Boolean.FALSE.equals(peek.getActive())) {
            throw new BusinessException(400, "Phong da ngung phuc vu");
        }
        if (req.getGuestsCount() > peek.getCapacity()) {
            throw new BusinessException(400, "So khach (" + req.getGuestsCount()
                    + ") vuot suc chua phong (" + peek.getCapacity() + ")");
        }

        String lockKey = ROOM_LOCK_PREFIX + req.getRoomId();
        Booking saved = RedisLockUtil.withLock(lockKey, LOCK_TTL_SECONDS, LOCK_WAIT_MILLIS,
                () -> createInTransaction(userId, req),
                () -> {
                    throw new BusinessException(409,
                            "Phong nay dang co mot yeu cau dat khac xu ly, vui long thu lai sau vai giay");
                });

        rememberHold(saved);
        log.info("Giu cho thanh cong id={} code={} user={} room={} {} -> {} (het han {})",
                saved.getId(), saved.getBookingCode(), userId, saved.getRoom().getId(),
                saved.getCheckinDate(), saved.getCheckoutDate(), saved.getHoldExpiresAt());
        return respond(saved);
    }

    /** Toan bo phan ghi DB cua create — chay trong MOT transaction, MOT session. */
    private Booking createInTransaction(Long userId, BookingCreateRequest req) {
        return bookingDao.inTransaction(s -> {
            User user = s.find(User.class, userId);
            if (user == null) throw new ResourceNotFoundException("User not found: " + userId);
            if (Boolean.FALSE.equals(user.getActive())) {
                throw new BusinessException(403, "Tai khoan da bi khoa");
            }

            // (1) Khoa phong — mutex that su. Moi luong khac muon cham phong nay phai cho ta commit.
            Room room = s.find(Room.class, req.getRoomId(), LockModeType.PESSIMISTIC_WRITE);
            if (room == null) throw new ResourceNotFoundException("Room not found: " + req.getRoomId());
            if (Boolean.FALSE.equals(room.getActive())) {
                throw new BusinessException(400, "Phong da ngung phuc vu");
            }
            if (req.getGuestsCount() > room.getCapacity()) {
                throw new BusinessException(400, "So khach (" + req.getGuestsCount()
                        + ") vuot suc chua phong (" + room.getCapacity() + ")");
            }

            LocalDate checkin = req.getCheckinDate();
            LocalDate checkout = req.getCheckoutDate();

            // (2) Load + tao cac dong lich cua khoang ngay, tat ca da FOR UPDATE.
            Map<LocalDate, RoomCalendar> days =
                    roomCalendarDao.findOrCreateForUpdate(s, room.getId(), checkin, checkout);

            for (Map.Entry<LocalDate, RoomCalendar> e : days.entrySet()) {
                if (e.getValue().getStatus() != CalendarStatusType.AVAILABLE) {
                    throw new BusinessException(409, "Phong da co nguoi giu ngay " + e.getKey()
                            + " — vui long chon khoang ngay khac");
                }
            }

            // (3) Bao hiem cho du lieu cu (booking tao truoc khi co RoomCalendar).
            long overlap = bookingDao.countOverlapInSession(s, room.getId(), checkin, checkout);
            if (overlap > 0) {
                throw new BusinessException(409,
                        "Phong da duoc dat trong khoang thoi gian nay, vui long chon ngay khac");
            }

            long nights = ChronoUnit.DAYS.between(checkin, checkout);
            BigDecimal gross = totalForNights(room, days, checkin, checkout);

            // (4) Voucher — kiem trong cung session de luoi su dung gioi han khong doc gia tri cu.
            Voucher voucher = resolveVoucher(s, user, req.getVoucherCode(), nights, gross);
            BigDecimal total = applyDiscount(gross, voucher);

            Booking booking = Booking.builder()
                    .bookingCode("BV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .user(user)
                    .room(room)
                    .checkinDate(checkin)
                    .checkoutDate(checkout)
                    .guestsCount(req.getGuestsCount())
                    .status(BookingStatusType.HOLD)
                    .holdExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES))
                    .totalPrice(total)
                    .currency("VND")
                    .voucher(voucher)
                    .loyaltyDiscountPercent(BigDecimal.ZERO)
                    .build();
            s.persist(booking);

            // (5) BLOCK tung dem. Entity dang managed nen chi can setStatus — dirty checking tu flush.
            days.values().forEach(c -> c.setStatus(CalendarStatusType.BLOCKED));

            // flush de co id (dung cho VoucherUsage + log + Redis hold key) va loi unique no ngay trong tx.
            s.flush();
            if (voucher != null) {
                s.persist(VoucherUsage.builder().voucher(voucher).user(user).booking(booking).build());
            }
            log.debug("HOLD da ghi id={} nights={} gross={} total={} voucher={}",
                    booking.getId(), nights, gross, total, voucher == null ? "-" : voucher.getCode());
            return booking;
        });
    }

    /**
     * Tim va kiem tra voucher — tra {@code null} neu khong dung.
     *
     * <p>Nhung gi phai chan o day, khong phai o FE:
     * <ul>
     *   <li><b>Thoi han</b>: {@code validFrom <= now <= validTo}.</li>
     *   <li><b>Dieu kien toi thieu</b>: so dem va gia tri don phai dat {@code minNights} /
     *       {@code minOrderValue}. Kiem tra tren <b>gia gross</b>, truoc khi tru — neu de sau
     *       thi voucher nay mo duong cho voucher khac.</li>
     *   <li><b>Han su dung</b>: toan cuc va theo tung user. Dem truc tiep trong session dang mo,
     *       va Room da bi khoa o buoc (1) nen request thu hai phai cho request thu nhat commit —
     *       hai nguoi cung dung mot ma khong the cung thang.</li>
     * </ul>
     */
    private Voucher resolveVoucher(Session s, User user, String rawCode, long nights, BigDecimal gross) {
        if (rawCode == null || rawCode.isBlank()) return null;

        String code = rawCode.trim().toUpperCase();
        Voucher v = s.createQuery("from Voucher where code = :code", Voucher.class)
                .setParameter("code", code)
                .uniqueResultOptional()
                .orElseThrow(() -> new BusinessException(404, "Voucher khong ton tai: " + code));

        LocalDateTime now = LocalDateTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom())) {
            throw new BusinessException(400, "Voucher chua den hieu luc");
        }
        if (v.getValidTo() != null && now.isAfter(v.getValidTo())) {
            throw new BusinessException(400, "Voucher da het hieu luc");
        }
        if (v.getMinNights() != null && nights < v.getMinNights()) {
            throw new BusinessException(400, "Voucher can it nhat " + v.getMinNights() + " dem");
        }
        if (v.getMinOrderValue() != null && gross.compareTo(v.getMinOrderValue()) < 0) {
            throw new BusinessException(400, "Gia tri don phai tu " + v.getMinOrderValue()
                    + " de dung voucher nay");
        }

        if (v.getUsageLimitTotal() != null) {
            long used = s.createQuery("select count(u) from VoucherUsage u where u.voucher.id = :vid", Long.class)
                    .setParameter("vid", v.getId()).getSingleResult();
            if (used >= v.getUsageLimitTotal()) {
                throw new BusinessException(400, "Voucher da het luot su dung");
            }
        }
        if (v.getUsageLimitPerUser() != null) {
            long mine = s.createQuery("""
                            select count(u) from VoucherUsage u
                            where u.voucher.id = :vid and u.user.id = :uid
                            """, Long.class)
                    .setParameter("vid", v.getId())
                    .setParameter("uid", user.getId()).getSingleResult();
            if (mine >= v.getUsageLimitPerUser()) {
                throw new BusinessException(400, "Ban da dung het luot cho voucher nay");
            }
        }
        return v;
    }

    /**
     * Tru gia tri voucher vao gross, khong cho am.
     *
     * <p>{@code PERCENT} dung {@code divide} voi scale 2 + {@code HALF_UP}: BigDecimal khong
     * tu lam tron, chia khong het se nem ArithmeticException — phai neu ro scale.
     */
    private static BigDecimal applyDiscount(BigDecimal gross, Voucher v) {
        if (v == null || v.getDiscountValue() == null) return gross;
        BigDecimal off = v.getDiscountType() == DiscountTypeEnum.PERCENT
                ? gross.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : v.getDiscountValue();
        BigDecimal net = gross.subtract(off);
        return net.signum() < 0 ? BigDecimal.ZERO.setScale(2) : net;
    }

    /**
     * Tong tien = tong gia tung dem, dem nao co {@code priceOverride} thi dung gia do.
     * Day la ly do ton tai RoomCalendar ngoai viec chong trung lich: gia cuoi tuan/le khac gia thuong.
     */
    private static BigDecimal totalForNights(Room room, Map<LocalDate, RoomCalendar> days,
                                             LocalDate checkin, LocalDate checkout) {
        BigDecimal base = BigDecimal.valueOf(room.getPricePerNight());
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate d = checkin; d.isBefore(checkout); d = d.plusDays(1)) {
            RoomCalendar c = days.get(d);
            BigDecimal night = (c != null && c.getPriceOverride() != null) ? c.getPriceOverride() : base;
            total = total.add(night);
        }
        return total;
    }

    private static void validateRange(BookingCreateRequest req) {
        if (req.getCheckinDate() == null || req.getCheckoutDate() == null) {
            throw new BusinessException(400, "Thieu ngay nhan/tra phong");
        }
        long nights = ChronoUnit.DAYS.between(req.getCheckinDate(), req.getCheckoutDate());
        if (nights <= 0) throw new BusinessException(400, "Ngay tra phong phai sau ngay nhan phong");
        if (nights > 90) throw new BusinessException(400, "Khong the dat qua 90 dem mot lan");
        if (req.getCheckinDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "Ngay nhan phong khong duoc trong qua khu");
        }
    }

    // ==================================================================== doc

    @Override
    public PageResponse<BookingResponse> listByUser(Long userId, int page, int size) {
        long total = bookingDao.countByUserId(userId);
        List<BookingResponse> content = bookingDao.findByUserId(userId, page, size)
                .stream().map(BookingMapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BookingResponse getById(Long userId, Long id) {
        Booking b = bookingDao.findByIdWithRoom(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!b.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Ban khong co quyen xem dat phong nay");
        }
        return BookingMapper.toResponse(b);
    }

    @Override
    public boolean isAvailable(Long roomId, LocalDate checkin, LocalDate checkout) {
        return !bookingDao.existsOverlap(roomId, checkin, checkout);
    }

    // ==================================================================== cancel

    /**
     * Huy dat phong cua chinh minh — tra cac dem ve AVAILABLE <b>co bao ve</b>.
     *
     * <p>"Co bao ve" = chi tra ngay d ve AVAILABLE khi khong con booking ACTIVE nao khac phu ngay d.
     * Khong kiem tra dieu nay thi mot lan cancel co the "mo khoa" ngay ma booking khac van dang giu.
     *
     * @throws BusinessException 403 (khong phai chu booking), 400 (da o trang thai cuoi)
     */
    @Override
    public BookingResponse cancel(Long userId, Long id) {
        Booking saved = bookingDao.inTransaction(s -> {
            Booking b = lockBooking(s, id);
            if (!b.getUser().getId().equals(userId)) {
                throw new BusinessException(403, "Ban khong co quyen huy dat phong nay");
            }
            if (TERMINAL.contains(b.getStatus())) {
                throw new BusinessException(400, "Dat phong da o trang thai khong the huy: " + b.getStatus());
            }
            b.setStatus(BookingStatusType.CANCELLED);
            releaseDays(s, b);
            return b;
        });

        forgetHold(saved.getId());
        log.info("Huy booking id={} code={} user={}", saved.getId(), saved.getBookingCode(), userId);
        return respond(saved);
    }

    // ==================================================================== confirm

    /**
     * Thanh toan thanh cong — {@code HOLD/PENDING_PAYMENT → CONFIRMED} va {@code BLOCKED → BOOKED}.
     *
     * <p><b>Idempotent</b>: webhook cua cong thanh toan co the ban lai nhieu lan cho cung mot
     * giao dich. Neu da CONFIRMED thi coi nhu thanh cong, khong nem loi — nem loi o day khien cong
     * thanh toan retry mai va day day log loi gia.
     *
     * <p>Canh tranh voi job expire: neu {@link #markExpired} da chay truoc thi booking la EXPIRED
     * (trang thai cuoi) → nem 409 de tang thanh toan biet ma hoan tien, thay vi am tham CONFIRMED
     * mot phong da bi tra lai cho nguoi khac.
     */
    @Override
    public BookingResponse confirm(Long bookingId) {
        Booking saved = bookingDao.inTransaction(s -> confirmInSession(s, bookingId));
        forgetHold(saved.getId());
        log.info("Xac nhan booking id={} code={}", saved.getId(), saved.getBookingCode());
        return respond(saved);
    }

    /**
     * Ban "trong Session" — xem {@link BookingService#confirmInSession}.
     *
     * <p>Idempotent: webhook co the ban lai nhieu lan; da CONFIRMED thi tra nguyen hien
     * tai, khong nem loi (nem loi khien cong retry mai va day log loi gia).
     */
    @Override
    public Booking confirmInSession(Session s, Long bookingId) {
        Booking b = lockBooking(s, bookingId);
        if (b.getStatus() == BookingStatusType.CONFIRMED
                || b.getStatus() == BookingStatusType.COMPLETED) {
            return b; // webhook/return goi lai
        }
        if (b.getStatus() != BookingStatusType.HOLD && b.getStatus() != BookingStatusType.PENDING_PAYMENT) {
            throw new BusinessException(409,
                    "Khong the xac nhan booking dang o trang thai " + b.getStatus());
        }
        b.setStatus(BookingStatusType.CONFIRMED);
        b.setHoldExpiresAt(null); // het y nghia dem nguoc
        markDays(s, b, CalendarStatusType.BOOKED);
        return b;
    }

    /** Xoa key Redis booking:hold:{id} — goi sau khi commit (xem BookingService.forgetHoldKey). */
    @Override
    public void forgetHoldKey(Long bookingId) {
        forgetHold(bookingId);
    }

    // ==================================================================== expire HOLD

    /**
     * Danh dau MOT giu cho qua han thanh EXPIRED va tra lich.
     *
     * <h2>Phai quét cả {@code PENDING_PAYMENT}, không chỉ {@code HOLD}</h2>
     * <p>
     * Sau khi khách bấm "Thanh toán", booking chuyển {@code HOLD → PENDING_PAYMENT}. Bản cũ
     * chỉ lọc {@code status = HOLD} nên một booking PENDING_PAYMENT bị bỏ rơi trong trang
     * checkout <b>không bao giờ hết hạn</b>: các đêm vẫn BLOCKED mãi → phòng vẫn đang trống
     * nhưng không ai đặt được, doanh thu chết âm thầm và không có lỗi nào được log.
     *
     * <h2>Tự chữa trước khi trả phòng</h2>
     * <p>
     * Thứ tự sự kiện sau là hợp lệ và không được phép làm mất tiền khách:
     * <pre>
     *   T0        hold hết hạn, webhook chưa về          (mạng cổng chậm / BE vừa restart)
     *   T0+ε      job expire chạy
     *   T0+2min   webhook "succeeded" về
     * </pre>
     * Nếu job cứ việc expire thì webhook sau đó gặp booking EXPIRED → phải hoàn tiền, khách
     * mất công chờ tiền về dù đã trả đủ. Vì vậy trước khi expire, kiểm tra Payment: nếu cổng
     * đã báo SUCCESS thì <b>xác nhận booking luôn</b> (confirm lại idempotent), chỉ expire
     * khi tiền thực sự chưa vào.
     *
     * @return true nếu đã đổi trạng thái (EXPIRED hoặc tự chữa thành CONFIRMED)
     */
    @Override
    public boolean markExpired(Long bookingId) {
        ExpireOutcome outcome = bookingDao.inTransaction(s -> {
            Booking b = lockBooking(s, bookingId);
            boolean holding = b.getStatus() == BookingStatusType.HOLD
                    || b.getStatus() == BookingStatusType.PENDING_PAYMENT;
            if (!holding) return new ExpireOutcome(b.getStatus(), false); // job khac/user da xu ly
            if (b.getHoldExpiresAt() == null || b.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
                return new ExpireOutcome(b.getStatus(), false); // chua toi han
            }

            // Tien da ve roi? -> xac nhan thay vi tra phong
            if (paymentDao.existsSuccessForBookingInSession(s, b.getId())) {
                Booking confirmed = confirmInSession(s, bookingId);
                return new ExpireOutcome(confirmed.getStatus(), true);
            }

            b.setStatus(BookingStatusType.EXPIRED);
            releaseDays(s, b);
            return new ExpireOutcome(b.getStatus(), true);
        });

        if (outcome.changed()) {
            forgetHold(bookingId);
            if (outcome.status() == BookingStatusType.CONFIRMED) {
                log.warn("Booking id={} qua han giu cho nhung tien DA thanh cong -> tu chua sang CONFIRMED",
                        bookingId);
            } else {
                log.info("HOLD/PENDING_PAYMENT qua han -> EXPIRED id={}", bookingId);
            }
        }
        return outcome.changed();
    }

    /** Kết quả của một lượt expire — tách khỏi Session đã đóng để log/đếm bên ngoài transaction. */
    private record ExpireOutcome(BookingStatusType status, boolean changed) {
    }

    /**
     * Quet toan bo HOLD qua han — scheduler goi moi phut.
     *
     * <p>Redis lock {@code job:hold-expire} de nhieu node Tomcat khong quat trung nhau.
     * Khong gianh duoc lock thi <b>im lang bo qua</b> (khong nem loi): node khac dang lam roi.
     *
     * @return so booking da chuyen sang EXPIRED
     */
    @Override
    public int expireHolds() {
        return RedisLockUtil.withLock(JOB_LOCK_KEY, 55, 0L,
                () -> {
                    List<Long> ids = bookingDao.findExpiredHoldIds(LocalDateTime.now(), EXPIRE_BATCH);
                    if (ids.isEmpty()) return 0;
                    int count = 0;
                    for (Long id : ids) {
                        // Moi booking mot transaction rieng: mot booking loi (FK, du lieu ban)
                        // khong keo ca batch rollback, va lock DB duoc giu rat ngan.
                        try {
                            if (markExpired(id)) count++;
                        } catch (Exception e) {
                            log.error("Khong expire duoc booking id={}: {}", id, e.getMessage(), e);
                        }
                    }
                    return count;
                },
                () -> 0); // node khac dang quat
    }

    /** Admin keo dai thoi gian giu cho (vi du khach goi dien xin them 10 phut). */
    @Override
    public BookingResponse extendHold(Long bookingId, int minutes) {
        if (minutes <= 0 || minutes > 180) {
            throw new BusinessException(400, "So phut gia han phai trong khoang 1..180");
        }
        Booking saved = bookingDao.inTransaction(s -> {
            Booking b = lockBooking(s, bookingId);
            if (b.getStatus() != BookingStatusType.HOLD) {
                throw new BusinessException(400, "Chi co the gia han booking dang HOLD, hien la " + b.getStatus());
            }
            LocalDateTime base = b.getHoldExpiresAt() == null ? LocalDateTime.now() : b.getHoldExpiresAt();
            LocalDateTime until = base.isBefore(LocalDateTime.now())
                    ? LocalDateTime.now().plusMinutes(minutes) // da qua han thi dem tu bay gio
                    : base.plusMinutes(minutes);
            b.setHoldExpiresAt(until);
            return b;
        });

        // Dong bo TTL key Redis de ra Redis thay dung thoi gian con lai.
        try (Jedis j = RedisConfig.getPool().getResource()) {
            long ttl = Math.max(1, ChronoUnit.SECONDS.between(LocalDateTime.now(), saved.getHoldExpiresAt()));
            j.setex(HOLD_KEY_PREFIX + saved.getId(), ttl, holdValue(saved));
        } catch (Exception e) {
            log.debug("Khong cap nhat duoc TTL hold key id={}: {}", saved.getId(), e.getMessage());
        }
        log.info("Gia han HOLD id={} them {} phut (den {})", saved.getId(), minutes, saved.getHoldExpiresAt());
        return respond(saved);
    }

    // ==================================================================== helper trong transaction

    /** Khoa booking bang {@code FOR UPDATE} + fetch room/user de dung sau khi session dong. */
    private static Booking lockBooking(Session s, Long bookingId) {
        Booking b = s.createQuery("""
                        select distinct b from Booking b
                        left join fetch b.room
                        left join fetch b.user
                        where b.id = :id
                        """, Booking.class)
                .setParameter("id", bookingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .uniqueResult();
        if (b == null) throw new ResourceNotFoundException("Booking not found: " + bookingId);
        return b;
    }

    /** {@code BLOCKED → BOOKED} cho cac dem cua booking (khi confirm). */
    private void markDays(Session s, Booking b, CalendarStatusType to) {
        Map<LocalDate, RoomCalendar> days = roomCalendarDao.findForUpdate(
                s, b.getRoom().getId(), b.getCheckinDate(), b.getCheckoutDate());
        for (RoomCalendar c : days.values()) {
            if (c.getStatus() == CalendarStatusType.BLOCKED) c.setStatus(to);
        }
    }

    /**
     * Tra cac dem ve AVAILABLE — chi khi khong con booking ACTIVE nao khac phu dem do.
     * Dung cho cancel va expire.
     */
    private void releaseDays(Session s, Booking b) {
        Map<LocalDate, RoomCalendar> days = roomCalendarDao.findForUpdate(
                s, b.getRoom().getId(), b.getCheckinDate(), b.getCheckoutDate());

        for (Map.Entry<LocalDate, RoomCalendar> e : days.entrySet()) {
            RoomCalendar c = e.getValue();
            if (c.getStatus() == CalendarStatusType.AVAILABLE) continue;
            // b da duoc doi trang thai trong session nay nen truy van dem duoi day
            // (chay cung transaction) da thay trang thai moi → khong can exclude id.
            long stillBusy = bookingDao.countActiveOnDateInSession(s, b.getRoom().getId(), e.getKey());
            if (stillBusy == 0) c.setStatus(CalendarStatusType.AVAILABLE);
        }
    }

    /** Doc lai booking day du quan he de map response (session cu da dong). */
    private BookingResponse respond(Booking b) {
        return BookingMapper.toResponse(bookingDao.findByIdWithRoom(b.getId()).orElse(b));
    }

    // ==================================================================== Redis hold key

    /**
     * Ghi {@code booking:hold:{id}} TTL = thoi gian con lai.
     *
     * <p>Key nay <b>khong phai</b> co che expire — nguon chan ly la {@code bookings.hold_expires_at}
     * vi Redis co the mat du lieu khi restart. No chi giup: (a) admin {@code KEYS booking:hold:*}
     * thay ngay danh sach giu cho dang song, (b) TTL tu bien mat = tin hieu truc quan.
     */
    private void rememberHold(Booking b) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            j.setex(HOLD_KEY_PREFIX + b.getId(), HOLD_MINUTES * 60L, holdValue(b));
        } catch (Exception e) {
            log.warn("Khong ghi duoc Redis hold key id={} (Redis down?): {}", b.getId(), e.getMessage());
        }
    }

    private void forgetHold(Long bookingId) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            j.del(HOLD_KEY_PREFIX + bookingId);
        } catch (Exception e) {
            log.debug("Khong xoa duoc Redis hold key id={}: {}", bookingId, e.getMessage());
        }
    }

    private static String holdValue(Booking b) {
        return b.getRoom().getId() + "|" + b.getCheckinDate() + "|" + b.getCheckoutDate() + "|" + b.getUser().getId();
    }

    /** Gia tri HOLD hien hanh — noi doc can gia tri nay thay vi hardcode 15. */
    public static int holdMinutes() {
        return HOLD_MINUTES;
    }
}
