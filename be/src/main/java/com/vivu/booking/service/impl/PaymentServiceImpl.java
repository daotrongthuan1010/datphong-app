package com.vivu.booking.service.impl;

import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.PaymentDao;
import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.dto.response.PaymentResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.enums.BookingStatusType;
import com.vivu.booking.enums.PaymentMethodType;
import com.vivu.booking.enums.PaymentStatusType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.PaymentMapper;
import com.vivu.booking.payment.gateway.PaymentGateway;
import com.vivu.booking.payment.gateway.PaymentGatewayFactory;
import com.vivu.booking.service.BookingService;
import com.vivu.booking.service.PaymentService;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * THANH TOAN — CAC TINH CHAT DOANH NGHIEP (simplified): NGAN GON
 *
 * <pre>
 * ACID:  Payment SUCCESS va Booking CONFIRMED phai cung transaction (ca hai cung commit,
 *        hoac ca hai cung rollback). Tach ra hai transaction = "tien da nhan ma phong
 *        chua xac nhan" khi process chet giua.
 *
 * Idempotency / webhook co the ban lai nhieu lan cho cung ma. Da SUCCESS/CONFIRMED thi no-op,
 *         khong nem loi (nem loi khien cong retry mai).
 *
 * Tieng tin: webhook chi la tin hieu — nhan webhook xong phai goi nguoc len cong
 *         (server-to-server) de xac nhan so tien that, hoac kiem tra chu ky. Tin webhook
 *         vo dieu kien = co phong mien phi.
 *
 * Khong mat intent: 2 lan bam "Thanh toan" -> cong cap 2 ma pay_aaa/pay_bbb. Chi luu cot
 *         gateway_transaction_ref thi ma thu nhat bi ghi de. Khach tra tien o tab pay_aaa ->
 *         webhook pay_aaa khong tim thay -> tien tru, booking khong xac nhan. Dung
 *         PaymentAttempt luu moi ma tung phat hanh.
 *
 * Khong double intent: 1 booking = 1 Payment (unique booking_id). Han che so giao dich.
 *
 * Khong ro ri ton kho: PENDING_PAYMENT phai co job het han nhu HOLD, khong thi check-out do
 *         bi BLOCKED vinh vien.
 *
 * Tu chua: HOLD qua han nhung tien da ve (webhook tre / BE restart) -> job expire tu xac nhan
 *         thay vi tra phong va de webhook sau gap EXPIRED -> hoan tien.
 * </pre>
 *
 * <h2>Thu tu khoa (ky luat tranh deadlock)</h2>
 * <pre>Room -> RoomCalendar -> Booking (FOR UPDATE) -> Payment (FOR UPDATE)</pre>
 * Moi luong theo dung thu tu nay — kho do nguoc nhau la deadlock co dien.
 */
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentDao paymentDao;
    private final BookingDao bookingDao;
    private final BookingService bookingService;
    private final PaymentGateway gateway;

    public PaymentServiceImpl(PaymentDao paymentDao, BookingDao bookingDao,
                              BookingService bookingService, PaymentGateway paymentGateway) {
        this.paymentDao = paymentDao;
        this.bookingDao = bookingDao;
        this.bookingService = bookingService;
        this.gateway = paymentGateway;
    }

    /** Servlet khoi tao no-arg — tu dung DAO + chon cong theo config. */
    public PaymentServiceImpl() {
        this(new PaymentDao(), new BookingDao(), new BookingServiceImpl(), PaymentGatewayFactory.resolve());
    }

    /**
     * Tao giao dich thanh toan cho booking dang HOLD hoac PENDING_PAYMENT.
     *
     * <h2>Idempotent va chong lap don</h2>
     * <ul>
     *   <li>Booking chi co <b>1 Payment</b> (unique tren {@code booking_id}). Lan 2 khong tao dong
     *       Payment moi ma tai su dung dong cu — viec ghi {@code gateway_transaction_ref} khong lam
     *       tang so tien phai doi thu.</li>
     *   <li>Moi ma cong tung phat hanh deu luu vao {@link com.vivu.booking.entity.PaymentAttempt} — khach
     *       tra tien o tab mo tu ma cu van duoc webhook map dung. Neu chi ghi de 1 cot, ma cu mat
     *       khi bam 2 lan → "tien da tru ma phong khong duoc xac nhan".</li>
     *   <li>{@code amount} lay tu {@code booking.totalPrice} do BE tu tinh — khong bao gio nhan tu
     *       request de khach tu ha gia trong devtools.</li>
     *   <li>Chuyen {@code HOLD → PENDING_PAYMENT} bao hieu khach da vao cong (van dem nguoc
     *       {@code holdExpiresAt}); PENDING_PAYMENT cung bi job expire quet nhu HOLD.</li>
     * </ul>
     *
     * @throws BusinessException 403 (khong phai chu booking), 400 (trang thai/ phuong thuc sai),
     *                           409 (da SUCCESS), 410 (hold het han)
     */
    @Override
    public PaymentResponse createPayment(PaymentRequest req, Long userId, String clientIp) {
        // (1) Kiem so huu + trang thai TRUOC khi cham giao dich (khong khoa gi o day — van re)
        Booking booking = bookingDao.findByIdWithRoom(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking " + req.getBookingId()));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Ban khong co quyen thanh toan booking nay");
        }
        if (booking.getStatus() != BookingStatusType.HOLD && booking.getStatus() != BookingStatusType.PENDING_PAYMENT) {
            throw new BusinessException(400, "Booking dang o trang thai " + booking.getStatus() + ", khong the thanh toan");
        }
        if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(410, "Da het thoi gian giu cho — vui long giu cho lai");
        }
        if (req.getMethod() != PaymentMethodType.VNPAY && req.getMethod() != PaymentMethodType.CREDIT_CARD) {
            throw new BusinessException(400, "Phuong thuc thanh toan chua duoc ho tro: " + req.getMethod());
        }

        // (2) Sang che giao dich cho cong — KHONG nam trong lock DB, vi cong co the cham (network).
        // Dung ban sao nhe cua Payment/Booking de truyen cho gateway; amount luon tu booking.
        Payment probe = Payment.builder().amount(booking.getTotalPrice()).build();
        PaymentGateway.GatewayRef ref = gateway.createRef(probe, booking, clientIp, req.getReturnUrl());

        // (3) Ghi Payment + PaymentAttempt + chuyen trang thai TRONG MOT TRANSACTION, theo ky luat khoa.
        Payment saved = paymentDao.inTransaction(s -> {
            // Khoa booking truoc (diem neo` — theo dung thu tu Room→RoomCalendar→Booking→Payment
            // ma BookingServiceImpl da dinh nghia; PaymentDao.lockBooking kho Booking FOR UPDATE).
            com.vivu.booking.entity.Booking lockedBooking =
                    PaymentDao.lockBooking(s, booking.getId());

            // Kiem lai sau khi khoa — tranh truong hop webhook da xac nhan giua (1) va (3).
            if (lockedBooking.getStatus() == BookingStatusType.CONFIRMED
                    || lockedBooking.getStatus() == BookingStatusType.COMPLETED) {
                throw new BusinessException(409, "Booking nay da thanh toan thanh cong");
            }
            if (lockedBooking.getStatus() != BookingStatusType.HOLD
                    && lockedBooking.getStatus() != BookingStatusType.PENDING_PAYMENT) {
                throw new BusinessException(400,
                        "Booking dang o trang thai " + lockedBooking.getStatus() + ", khong the thanh toan");
            }
            if (lockedBooking.getHoldExpiresAt() == null
                    || !lockedBooking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
                throw new BusinessException(410, "Da het thoi gian giu cho — vui long giu cho lai");
            }

            Payment payment = paymentDao.findByBookingIdForUpdate(s, booking.getId()).orElse(null);
            if (payment != null && payment.getStatus() == PaymentStatusType.SUCCESS) {
                throw new BusinessException(409, "Booking nay da thanh toan thanh cong");
            }
            if (payment == null) {
                // 1 booking = 1 payment — unique tren booking_id; tang dan so giao dich moi la sai.
                payment = Payment.builder()
                        .booking(lockedBooking)
                        .method(req.getMethod())
                        .amount(lockedBooking.getTotalPrice()) // KHONG nhan amount tu client
                        .currency("VND")
                        .status(PaymentStatusType.PENDING)
                        .gatewayTransactionRef(ref.gatewayRef())
                        .build();
                s.persist(payment);
                s.flush(); // co id de PaymentAttempt tham chieu
            } else {
                // Tai su dung payment cu — khong tang dong giao dich, chi doi phuong thuc neu can.
                if (payment.getStatus() != PaymentStatusType.PENDING
                        && payment.getStatus() != PaymentStatusType.FAILED) {
                    throw new BusinessException(400,
                            "Giao dich dang o trang thai " + payment.getStatus() + ", khong the thu lai");
                }
                payment.setMethod(req.getMethod());
                payment.setGatewayTransactionRef(ref.gatewayRef());
                payment.setFailureReason(null);
            }

            // Moi ma cong tung phat hanh deu phai tra nguoc duoc — neu chi luu cot duy nhat
            // gateway_transaction_ref thi lan bam thu 2 ghi de ma thu nhat, khach tra tien
            // o tab mo tu ma thu nhat -> webhook ma cu khong tim thay -> tien tru, booking treo.
            BigDecimal amt = lockedBooking.getTotalPrice();
            paymentDao.recordAttempt(s, payment, ref.gatewayRef(), amt, "VND");

            if (lockedBooking.getStatus() == BookingStatusType.HOLD) {
                // Con dem nguoc holdExpiresAt — PENDING_PAYMENT cung duoc job expire quet nhu HOLD
                lockedBooking.setStatus(BookingStatusType.PENDING_PAYMENT);
            }
            return payment;
        });

        PaymentResponse resp = PaymentMapper.toResponse(saved);
        resp.setPaymentUrl(ref.checkoutUrl());
        log.info("Tao giao dich booking={} code={} method={} gateway={} ref={} (attempt da ghi)",
                booking.getId(), booking.getBookingCode(), req.getMethod(), gateway.name(), ref.gatewayRef());
        return resp;
    }

    @Override
    public BookingResponse handleGatewayResult(String gatewayRef, boolean claimedSuccess) {
        return handleGatewayResult(gatewayRef, claimedSuccess, false);
    }

    /**
     * Goi tu webhook/return cua cong.
     *
     * <h2>Idempotent — webhook co the ban lai nhieu lan</h2>
     * Da SUCCESS/CONFIRMED thi tra thang, khong nem loi (nem loi khien cong retry mai).
     *
     * <h2>Tin cay — khong tin webhook tuyen bo</h2>
     * <ul>
     *   <li>Neu cong ho tro {@code queryStatus} (fake-bank): webhook chi la tin hieu; service
     *       goi nguoc len cong lay trang thai that + so tien that roi moi quyet dinh. Kẻ gia
     *       POST webhook mo phong su kien khong qua duoc buoc nay.</li>
     *   <li>Neu cong khong ho tro (VNPay sandbox cu): caller <b>phải</b> kiem tra chu ky truoc
     *       roi moi truyen {@code signatureVerified=true}. Chu ky that bai = reject.</li>
     *   <li>So tien phai trung voi {@code payment.amount} do BE tu tinh tu booking — khach
     *       ha gia bang devtools hoac gia webhook deu bi lo.</li>
     * </ul>
     *
     * <h2>ACID — tien va phong cung mot transaction</h2>
     * {@code Payment = SUCCESS} va {@code Booking = CONFIRMED} phai commit cung nhau. Tach ra
     * hai transaction = process chet giua ("tien da nhan ma phong chua xac nhan").
     */
    @Override
    public BookingResponse handleGatewayResult(String gatewayRef, boolean claimedSuccess,
                                               boolean signatureVerified) {
        if (gatewayRef == null || gatewayRef.isBlank()) {
            throw new BusinessException(400, "Thieu ma giao dich cua cong");
        }

        // (A) Xac minh nguon goc + so tien truoc khi cham DB (re-query cong — khong trong transaction).
        PaymentGateway.GatewayStatus verified = null;
        try {
            verified = gateway.queryStatus(gatewayRef).orElse(null);
        } catch (Exception e) {
            log.warn("Khong lien lac duoc cong de xac minh ref={}: {}", gatewayRef, e.getMessage());
        }

        final boolean gatewayConfirmsSuccess;
        final PaymentGateway.GatewayStatus verifiedFinal;
        if (verified != null) {
            if (verified.pending()) {
                // Cong noi "chua co ket qua cuoi" -> coi nhu that bai tam (khong CONFIRMED).
                // Khong nem loi: webhook phai tra 200 de cong khong retry mai, chi ghi FAILED.
                log.info("Webhook ref={} nhung cong bao {} -> cho doi (khong xac nhan booking)",
                        gatewayRef, verified.status());
                return handleReconciled(gatewayRef, false, verified, signatureVerified, claimedSuccess, "Gateway bao pending");
            }
            gatewayConfirmsSuccess = verified.succeeded();
            verifiedFinal = verified;
        } else {
            // Cong khong ho tro queryStatus (VD VNPay cu): bat buoc co chu ky
            if (claimedSuccess && !signatureVerified) {
                log.warn("Tu choi gateway success REF={}: cong khong re-query duoc va chu ky CHUA duoc xac minh", gatewayRef);
                throw new BusinessException(400,
                        "Khong the xac minh thanh toan: thieu chu ky cong thanh toan");
            }
            gatewayConfirmsSuccess = claimedSuccess;
            verifiedFinal = null;
        }

        return handleReconciled(gatewayRef, gatewayConfirmsSuccess, verifiedFinal,
                signatureVerified, claimedSuccess, null);
    }

    /**
     * Ghi DB sau khi da xac minh — Payment + Booking trong cung 1 transaction, kho đúng thứ tự.
     *
     * @param reconciledSuccess ket luan cuoi sau khi doi chieu voi cong / chu ky
     * @param verified          trang thai cong tra ve (co the null neu cong khong ho tro queryStatus)
     * @param claimedSuccess    cong tuyen bo ban dau (de log sai lech)
     */
    private BookingResponse handleReconciled(String gatewayRef, boolean reconciledSuccess,
                                              PaymentGateway.GatewayStatus verified,
                                              boolean signatureVerified,
                                              boolean claimedSuccess, String pendingFailureReason) {
        // Ghi mot transaction duy nhat: Payment (FOR UPDATE qua PaymentAttempt) + Booking (FOR UPDATE)
        // cung commit. Het transaction moi xoa Redis hold key (forgetHold = sau commit, khong so rollback).
        Booking[] booked = new Booking[1];
        Payment p = paymentDao.inTransaction(s -> {
            Payment payment = paymentDao.findByGatewayRefForUpdate(s, gatewayRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay giao dich " + gatewayRef));

            if (verified != null && verified.amount() != null
                    && payment.getAmount() != null
                    && verified.amount().compareTo(payment.getAmount()) != 0) {
                String msg = "So tien cong bao ve (" + verified.amount()
                        + ") khong khop so tien BE ghi (" + payment.getAmount() + ")";
                payment.setStatus(PaymentStatusType.FAILED);
                payment.setFailureReason(msg);
                log.warn("Tu choi gateway REF={}: {}", gatewayRef, msg);
                return payment;
            }
            if (verified != null && claimedSuccess != reconciledSuccess) {
                log.warn("Lech webhook vs cong REF={}: webhook claimedSuccess={} nhung cong bao {} -> dung ket luan cua cong",
                        gatewayRef, claimedSuccess, verified.status());
            }

            booked[0] = PaymentDao.lockBooking(s, payment.getBooking().getId());
            Booking b = booked[0];

            if (!reconciledSuccess) {
                // That bai: ghi FAILED + ly do that bai chi tiet (khong im lang).
                if (payment.getStatus() != PaymentStatusType.FAILED) {
                    payment.setStatus(PaymentStatusType.FAILED);
                }
                String reason;
                if (pendingFailureReason != null) reason = pendingFailureReason;
                else if (verified != null) reason = "Gateway bao " + verified.status();
                else if (!signatureVerified) reason = "Chu ky chua duoc xac minh";
                else reason = "Cong bao thanh toan that bai / huy";
                payment.setFailureReason(reason);
                return payment;
            }

            // Thanh cong: idempotent — da SUCCESS/CONFIRMED thi no-op.
            if (payment.getStatus() == PaymentStatusType.SUCCESS
                    && (b.getStatus() == BookingStatusType.CONFIRMED
                    || b.getStatus() == BookingStatusType.COMPLETED)) {
                return payment;
            }
            if (b.getStatus() == BookingStatusType.EXPIRED) {
                // EXPIRED = phong da tra cho nguoi khac (co bao ve: khong ngay nao con BLOCKED cua rieng b).
                // Im lang CONFIRMED lai se ghi de phong cua nguoi khac -> 409 de tang tren hoan tien.
                payment.setStatus(PaymentStatusType.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                payment.setFailureReason(null);
                throw new BusinessException(409,
                        "Booking da het han giu cho, khong the xac nhan — lien he ho tro de hoan tien. REF=" + gatewayRef);
            }

            payment.setStatus(PaymentStatusType.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setFailureReason(null);

            // ACID: booking CONFIRMED ngay trong cung Session — khong goi BookingService.confirm
            // transaction rieng vi khi do "tien da nhan" va "phong da xac nhan" nam o hai commit khac nhau.
            bookingService.confirmInSession(s, b.getId());
            return payment;
        });

        Booking target = booked[0];
        if (target != null) {
            if (!reconciledSuccess) {
                log.info("Thanh toan THAT BAI ref={} booking={} ly do={}", gatewayRef, target.getId(), p.getFailureReason());
                return com.vivu.booking.mapper.BookingMapper.toResponse(
                        bookingDao.findByIdWithRoom(target.getId()).orElse(target));
            }
            if (target.getStatus() == BookingStatusType.EXPIRED) {
                log.warn("Webhook tre REF={} gap booking EXPIRED — payment da SUCCESS, cho hoan: booking={}", gatewayRef, target.getId());
            } else {
                bookingService.forgetHoldKey(target.getId());
                log.info("Thanh toan THANH CONG ref={} booking={} -> CONFIRMED (xac minh: {}, pending={})",
                        gatewayRef, target.getId(),
                        verified != null ? "cong xac nhan" : (signatureVerified ? "chu ky dung" : "TIN WEBHOOK"),
                        verified != null && verified.pending());
            }
            return com.vivu.booking.mapper.BookingMapper.toResponse(
                    bookingDao.findByIdWithRoom(target.getId()).orElse(target));
        }
        // Nhanh cu — khong co context booking (gatewar ref khong tim thay Booking): tra null
        return null;
    }

    @Override
    public PaymentResponse getByBookingId(Long userId, Long bookingId) {
        Payment payment = paymentDao.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking chua co giao dich thanh toan"));
        requireOwner(payment, userId);
        return PaymentMapper.toResponse(payment);
    }

    /**
     * Doi soat chu dong — xem {@link PaymentService#reconcileByBookingId}.
     *
     * <p>Chi "keo" khi payment con PENDING; da SUCCESS/FAILED/REFUNDED thi no-op (idempotent —
     * FE co the goi nhieu lan khi poll). Ket luan cuoi van nam trong {@link #handleGatewayResult}
     * (re-query cong + doi chieu so tien + ghi tien/phong cung transaction), endpoint nay chi la
     * cai "co" de kich hoat luong do khi webhook khong toi duoc.
     */
    @Override
    public BookingResponse reconcileByBookingId(Long userId, Long bookingId) {
        Payment payment = paymentDao.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking chua co giao dich thanh toan"));
        requireOwner(payment, userId);

        // Da co ket qua cuoi -> khong can hoi cong nua.
        if (payment.getStatus() == PaymentStatusType.SUCCESS
                || payment.getStatus() == PaymentStatusType.FAILED
                || payment.getStatus() == PaymentStatusType.REFUNDED) {
            return respondBooking(payment.getBooking().getId());
        }

        String ref = payment.getGatewayTransactionRef();
        if (ref == null || ref.isBlank()) {
            throw new BusinessException(400, "Giao dich chua co ma cong — chua the doi soat");
        }

        // Hoi cong trang thai that cua ma gan nhat. Loi mang tam thoi -> nem 5xx de FE thu lai.
        PaymentGateway.GatewayStatus verified;
        try {
            verified = gateway.queryStatus(ref).orElse(null);
        } catch (Exception e) {
            log.warn("Reconcile booking={} khong lien lac duoc cong ref={}: {}", bookingId, ref, e.getMessage());
            throw new BusinessException(503, "Khong lien lac duoc cong thanh toan — thu lai sau");
        }
        if (verified == null) {
            // Cong khong ho tro queryStatus (VD VNPay cu) -> doi chu ky qua return URL, khong keo duoc.
            log.info("Reconcile booking={}: cong {} khong ho tro queryStatus — doi return/webhook", bookingId, gateway.name());
            return respondBooking(payment.getBooking().getId());
        }
        if (verified.pending()) {
            // Khach chua hoan tat tren trang checkout, hoac cong dang xu ly -> giu PENDING, FE tiep tuc poll.
            log.info("Reconcile booking={}: cong bao {} (chua co ket qua cuoi)", bookingId, verified.status());
            return respondBooking(payment.getBooking().getId());
        }

        // Cong da co ket qua cuoi -> di qua dung mot luong ACID/idempotent cua webhook.
        boolean success = verified.succeeded();
        log.info("Reconcile booking={}: cong bao {} -> goi handleGatewayResult ref={}", bookingId, verified.status(), ref);
        BookingResponse result = handleGatewayResult(ref, success, false);
        return result != null ? result : respondBooking(payment.getBooking().getId());
    }

    /** Doc lai booking day du de map response (session cua findByBookingId da dong). */
    private BookingResponse respondBooking(Long bookingId) {
        return com.vivu.booking.mapper.BookingMapper.toResponse(
                bookingDao.findByIdWithRoom(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId)));
    }

    @Override
    public PaymentResponse getById(Long userId, Long paymentId) {
        Payment payment = paymentDao.findWithBookingById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay giao dich " + paymentId));
        requireOwner(payment, userId);
        return PaymentMapper.toResponse(payment);
    }

    private static void requireOwner(Payment payment, Long userId) {
        Booking b = payment.getBooking();
        if (b == null || b.getUser() == null || !b.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Ban khong co quyen xem giao dich nay");
        }
    }
}
