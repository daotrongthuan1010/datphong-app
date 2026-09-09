package com.vivu.booking.utils;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TOTP (RFC 6238) dung thu vien dev.samstevens.totp:totp - tuong thich
 * Google Authenticator, Microsoft Authenticator, Authy...
 *
 * - Secret:  Base32 do {@link DefaultSecretGenerator} sinh (32 ky tu = 160 bit).
 * - Verify:  {@link DefaultCodeVerifier} (HMAC-SHA1, 6 so, chu ky 30s), cho phep
 *            lech dong ho +/- 1 buoc thoi gian.
 * - QR:      {@link ZxingPngQrGenerator} sinh anh PNG, tra ve data URI
 *            "data:image/png;base64,..." de FE gan thang vao the <img>.
 *
 * Cau hinh (application.properties, co default neu thieu):
 *   twofactor.issuer=VIVU Booking
 *   twofactor.digits=6
 *   twofactor.period-seconds=30
 *   twofactor.window-steps=1
 *   twofactor.secret-length=32
 *   twofactor.qr-image-size=260
 */
public final class TotpUtil {

    private static final Logger log = LoggerFactory.getLogger(TotpUtil.class);

    private static final HashingAlgorithm ALGORITHM = HashingAlgorithm.SHA1;
    private static final int DIGITS = AppProperties.getInt("twofactor.digits", 6);
    private static final int PERIOD_SECONDS = AppProperties.getInt("twofactor.period-seconds", 30);
    private static final int WINDOW_STEPS = AppProperties.getInt("twofactor.window-steps", 1);
    private static final int SECRET_LENGTH = AppProperties.getInt("twofactor.secret-length", 32);
    private static final int QR_IMAGE_SIZE = AppProperties.getInt("twofactor.qr-image-size", 260);
    private static final String ISSUER = AppProperties.get("twofactor.issuer", "VIVU Booking");

    private static final DefaultSecretGenerator SECRET_GENERATOR = new DefaultSecretGenerator(SECRET_LENGTH);
    private static final ZxingPngQrGenerator QR_GENERATOR = new ZxingPngQrGenerator();
    private static final QrDataFactory QR_DATA_FACTORY = new QrDataFactory(ALGORITHM, DIGITS, PERIOD_SECONDS);
    private static final DefaultCodeVerifier CODE_VERIFIER =
            new DefaultCodeVerifier(new DefaultCodeGenerator(ALGORITHM, DIGITS), new SystemTimeProvider());

    static {
        QR_GENERATOR.setImageSize(QR_IMAGE_SIZE);
        CODE_VERIFIER.setTimePeriod(PERIOD_SECONDS);
        CODE_VERIFIER.setAllowedTimePeriodDiscrepancy(WINDOW_STEPS);
    }

    private TotpUtil() {
    }

    /** Sinh secret moi, dang Base32 - luu vao DB va dung de tao QR cho user quet. */
    public static String generateSecret() {
        return SECRET_GENERATOR.generate();
    }

    /** Xac thuc ma 6 so user nhap, cho phep lech +/- WINDOW_STEPS buoc thoi gian. */
    public static boolean verify(String base32Secret, String codeInput) {
        if (base32Secret == null || base32Secret.isBlank() || codeInput == null || codeInput.isBlank()) {
            return false;
        }
        return CODE_VERIFIER.isValidCode(base32Secret, codeInput.trim());
    }

    public static String getIssuer() {
        return ISSUER;
    }

    public static int getDigits() {
        return DIGITS;
    }

    /** Du lieu otpauth:// totp (label + secret + issuer + SHA1/6 so/30s) cho app Authenticator. */
    public static QrData qrData(String accountName, String base32Secret) {
        return QR_DATA_FACTORY.newBuilder()
                .label(ISSUER + ":" + accountName)
                .secret(base32Secret)
                .issuer(ISSUER)
                .algorithm(ALGORITHM)
                .digits(DIGITS)
                .period(PERIOD_SECONDS)
                .build();
    }

    /** Chuoi otpauth://totp/... - FE co the tu render QR neu muon, hoac cho phep copy. */
    public static String buildOtpAuthUri(String accountName, String base32Secret) {
        return qrData(accountName, base32Secret).getUri();
    }

    /**
     * Sinh QR code dang data URI (data:image/png;base64,...) - FE chi can
     * <img src={giaTriNay}/> la quet duoc bang Google/Microsoft Authenticator.
     */
    public static String generateQrDataUri(String accountName, String base32Secret) {
        try {
            byte[] png = QR_GENERATOR.generate(qrData(accountName, base32Secret));
            return Utils.getDataUriForImage(png, QR_GENERATOR.getImageMimeType());
        } catch (QrGenerationException e) {
            // Khong de lo secret ra log - chi bao loi chung.
            log.error("Khong the sinh QR code cho TOTP", e);
            throw new IllegalStateException("Khong the sinh ma QR xac thuc 2 lop", e);
        }
    }
}
