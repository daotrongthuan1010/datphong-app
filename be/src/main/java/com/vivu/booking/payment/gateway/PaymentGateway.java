package com.vivu.booking.payment.gateway;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Mot cong thanh toan. Tra ve ca {@code checkoutUrl} (de FE redirect) lan
 * {@code gatewayRef} (ma giao dich cua cong — luu vao {@code payments.gateway_transaction_ref}
 * de doi chieu webhook/return ve sau).
 */
public interface PaymentGateway {

    /** Ten cong, dung de chon theo cau hinh {@code payment.gateway}. */
    String name();

    GatewayRef createRef(Payment payment, Booking booking, String clientIp, String returnUrl);

    /**
     * Hoi cong ve trang thai <b>that</b> cua mot ma giao dich.
     *
     * <h2>Vì sao bat buoc phai co buoc nay?</h2>
     * <p>
     * {@code POST /api/payments/webhook} la endpoint <b>public</b> — bat ky ai biet URL deu co the
     * ban len {@code {"event":"payment.succeeded","payment_id":"pay_xxx"}} va neu BE tin ngay thi
     * ho co mot phong da xac nhan ma khong tra dong nao. Day la loi bao mat pho bien nhat khi
     * tich hop cong thanh toan.
     *
     * <p>Nguyen tac enterprise: <b>webhook chi la tin hieu, khong phai nguon chan ly</b>. Nhan
     * webhook xong phai goi nguoc len cong (server-to-server) de xac nhan status + so tien, roi
     * moi duoc doi trang thai booking. Cong khong ho tro truy van (VD VNPay sandbox) thi phai
     * co chu ky — xem {@code signatureVerified} trong
     * {@link com.vivu.booking.service.PaymentService#handleGatewayResult}.
     *
     * @return {@code empty} khi cong khong ho tro truy van trang thai
     */
    default Optional<GatewayStatus> queryStatus(String gatewayRef) {
        return Optional.empty();
    }

    /**
     * @param gatewayRef  ma giao dich do cong cap (fake-bank: {@code pay_*}; VNPay: {@code vnp_TxnRef})
     * @param checkoutUrl URL de FE redirect nguoi dung sang
     */
    record GatewayRef(String gatewayRef, String checkoutUrl) {}

    /**
     * Trang thai cong bao ve mot ma giao dich, da chuan hoa ve 3 gia tri
     * {@code succeeded / failed / pending} de service khong phai biet tu vung cua tung cong.
     */
    record GatewayStatus(String status, BigDecimal amount, String currency) {

        public boolean succeeded() {
            return "succeeded".equalsIgnoreCase(status)
                    || "success".equalsIgnoreCase(status)
                    || "paid".equalsIgnoreCase(status)
                    || "completed".equalsIgnoreCase(status);
        }

        public boolean failed() {
            return "failed".equalsIgnoreCase(status)
                    || "failure".equalsIgnoreCase(status)
                    || "declined".equalsIgnoreCase(status)
                    || "canceled".equalsIgnoreCase(status)
                    || "cancelled".equalsIgnoreCase(status)
                    || "expired".equalsIgnoreCase(status);
        }

        /** Chua ket luan duoc (khach chua bam xac nhan tren trang checkout, hoac cong chua xu ly xong). */
        public boolean pending() {
            return !succeeded() && !failed();
        }
    }
}
