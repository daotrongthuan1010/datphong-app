package com.vivu.booking.service;

import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.dto.response.PaymentResponse;

/**
 * Thanh toan cho booking dang giu cho.
 *
 * <p>Chuan enterprise (xem Javadoc trong {@code PaymentServiceImpl}): hoa don tu BE tinh
 * (khong nhan tu client), moi ma cong deu ghi lai, webhook phai duoc xac thuc bang
 * goi nguoc len cong, tien + phong duoc doi trong cung 1 transaction, intent mo côi
 * (gia tri ghi de khi bam 2 lan) van tim thay.
 */
public interface PaymentService {

    /**
     * Tao giao dich thanh toan cho mot booking dang HOLD/PENDING_PAYMENT.
     * Tra ve {@code paymentUrl} = checkout URL cua cong (fake-bank / VNPay) de FE redirect.
     *
     * <p>Idempotent: neu booking nay da co Payment PENDING thi tra lai chinh payment do,
     * ke ca khi phuong thuc khac — tranh tao 2 giao dich cho 1 phong. Da SUCCESS thi 409.
     */
    PaymentResponse createPayment(PaymentRequest req, Long userId, String clientId);

    /**
     * Webhook/return tu cong: {@code success=true} → payment SUCCESS + booking CONFIRMED
     * (idempotent); {@code success=false} → payment FAILED, booking giu nguyen cho toi het han.
     *
     * <h2>Luu y bao mat</h2>
     * <p>
     * Nhan {@code success} tu webhook ma khong xac thuc thi bat ky ai cung co the goi
     * {@code POST /api/payments/webhook {"event":"payment.succeeded"}} va co phong mien phi.
     * Phien ban an toan la {@link #handleGatewayResult(String, boolean, boolean)} voi
     * {@code signatureVerified=true} hoac de service tu goi nguoc len cong (server-to-server).
     * Phien ban nay giu lai de tuong thich VnPayReturnServlet cu — service tu re-query cong
     * neu cong ho tro {@code PaymentGateway.queryStatus}.
     *
     * @param gatewayRef ma giao dich cua cong (fake-bank {@code pay_*}, VNPay {@code vnp_TxnRef})
     * @return booking sau khi xu ly, hoac {@code null} neu khong tim thay giao dich
     */
    BookingResponse handleGatewayResult(String gatewayRef, boolean success);

    /**
     * Ban an toan cua webhook — caller da xac minh chu ky (HMAC) cua cong.
     *
     * @param gatewayRef        ma giao dich cua cong
     * @param claimedSuccess    trang thai cong <b>tuyen bo</b> (qua event / resultCode)
     * @param signatureVerified true neu caller da xac minh HMAC/chu ky dung la tu cong
     */
    BookingResponse handleGatewayResult(String gatewayRef, boolean claimedSuccess, boolean signatureVerified);

    /** Payment cua mot booking (owner check). Dung cho FE poll trang thai sau khi quay ve tu cong. */
    PaymentResponse getByBookingId(Long userId, Long bookingId);

    /**
     * Doi soat (reconcile) chu dong — FE goi ngay khi khach quay ve tu trang checkout cua cong.
     *
     * <h2>Vì sao can buoc nay ngoai webhook?</h2>
     * <p>
     * Webhook la kenh "day" (cong goi ve BE) nhung khong phai luc nao cung toi duoc:
     * <ul>
     *   <li>Dev local: fake-bank nam tren server con BE chay tren may ca nhan —
     *       {@code webhook_url} suy ra tu {@code returnUrl} thanh {@code localhost:8081}
     *       se trỏ về chính server chứ không phải máy dev → webhook không bao giờ tới.</li>
     *   <li>Mang/cong nghet, BE restart dung luc webhook ban → mat tin hieu.</li>
     * </ul>
     * Kenh "keo" nay bu vao cho hong do: FE quay ve voi {@code ?paid=1} → goi endpoint nay →
     * BE <b>chu dong hoi cong</b> ({@code queryStatus}) trang thai that cua ma giao dich gan nhat,
     * roi di qua dung mot luong {@link #handleGatewayResult} (ACID, idempotent) de doi tien + phong.
     *
     * <p>An toan: khong tin FE tuyen bo "da tra" — ket luan cuoi van dua tren {@code queryStatus}
     * + doi chieu so tien trong {@code handleGatewayResult}. Da SUCCESS/FAILED thi no-op.
     *
     * @return booking sau khi doi soat (co the van PENDING neu cong chua co ket qua cuoi)
     */
    BookingResponse reconcileByBookingId(Long userId, Long bookingId);

    /** Chi tiet 1 payment (owner check). */
    PaymentResponse getById(Long userId, Long paymentId);
}
