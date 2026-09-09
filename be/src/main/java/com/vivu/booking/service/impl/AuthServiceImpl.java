package com.vivu.booking.service.impl;

import com.vivu.booking.config.RedisConfig;
import com.vivu.booking.dao.OtpVerificationDao;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.LoginTwoFactorChallengeResponse;
import com.vivu.booking.dto.response.TwoFactorSetupResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.OtpVerification;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.OtpPurposeType;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.mapper.UserMapper;
import com.vivu.booking.service.AuthService;
import com.vivu.booking.service.EmailSender;
import com.vivu.booking.utils.AppProperties;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.OtpUtil;
import com.vivu.booking.utils.PasswordUntil;
import com.vivu.booking.utils.TotpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Role mặc định gán cho tài khoản tự đăng ký - phải tồn tại sẵn trong bảng roles (seed data). */
    private static final String DEFAULT_ROLE_CODE = "user";
    private static final int OTP_TTL_MINUTES = 5;
    private static final String REDIS_REFRESH_PREFIX = "auth:refresh:"; // key = auth:refresh:{userId}

    // ---------------------------------------------------------------- buoc OTP cua dang nhap
    /**
     * Sau khi username+password dung, BE KHONG cap token ma tra ve 1 phieu ngan han
     * (loginToken) luu Redis. Buoc 2 gui phieu + ma 6 so de doi lay access/refresh token.
     */
    private static final String REDIS_LOGIN_2FA_PREFIX = "auth:2fa:login:";      // key -> "{userId}|{1|0}" (1 = lan dau can quet QR)
    private static final String REDIS_LOGIN_2FA_ATTEMPTS = ":attempts";           // dem so lan nhap sai ma
    private static final int LOGIN_2FA_TTL_SECONDS = AppProperties.getInt("twofactor.login-token-ttl-seconds", 300);
    private static final int LOGIN_2FA_MAX_ATTEMPTS = AppProperties.getInt("twofactor.login-max-attempts", 5);

    private final UsersDao usersDao;
    private final RoleDao roleDao;
    private final OtpVerificationDao otpDao;
    private final EmailSender emailSender;

    public AuthServiceImpl(UsersDao usersDao, RoleDao roleDao, OtpVerificationDao otpDao, EmailSender emailSender) {
        this.usersDao = usersDao;
        this.roleDao = roleDao;
        this.otpDao = otpDao;
        this.emailSender = emailSender;
    }

    public AuthServiceImpl() {
        this(new UsersDao(), new RoleDao(), new OtpVerificationDao(OtpVerification.class), new ConsoleEmailSender());
    }

    // ---------------------------------------------------------------- 1. register

    @Override
    public UsersResponse register(RegisterRequest req) {
        if (usersDao.existsByCode(req.getEmail())) {
            throw new BusinessException(409, "Email đã được sử dụng");
        }
        if (usersDao.existsByUsername(req.getUsername())) {
            throw new BusinessException(409, "Username đã được sử dụng");
        }
        if (usersDao.existsByPhone(req.getPhone())) {
            throw new BusinessException(409, "Số điện thoại đã được sử dụng");
        }

        Role defaultRole = roleDao.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new BusinessException(500,
                        "Thiếu role mặc định '" + DEFAULT_ROLE_CODE + "' trong DB - cần seed bảng roles trước"));

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .username(req.getUsername())
                .password(PasswordUntil.hashedPassword(req.getPassword()))
                .gender(req.getGender())
                .status(UserStatus.ACTIVE)
                .active(true)
                .role(new HashSet<>(Set.of(defaultRole)))
                .build();

        usersDao.save(user);
        log.info("Đăng ký tài khoản mới id={} username={}", user.getId(), user.getUsername());
        return UserMapper.toResponse(user);
    }

    // ---------------------------------------------------------------- 2. otp/send + 6. forgot-password

    @Override
    public void sendOtp(SendOtpRequest req) {
        boolean userExists = usersDao.existsByCode(req.getEmail());
        if (!userExists) {
            // Áp dụng chung cho cả REGISTER lẫn FORGOT_PASSWORD: email phải đã tồn tại trong hệ thống.
            throw new BusinessException(404, "Email chưa đăng ký tài khoản nào");
        }

        otpDao.invalidateAllForEmail(req.getEmail(), req.getPurpose());

        String code = OtpUtil.generate6Digit();
        User linkedUser = usersDao.findByCode(req.getEmail()).orElse(null);

        OtpVerification otp = OtpVerification.builder()
                .user(linkedUser)
                .email(req.getEmail())
                .otpCode(code)
                .purpose(req.getPurpose())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .isUsed(false)
                .build();
        otpDao.save(otp);

        emailSender.sendOtpEmail(req.getEmail(), code, req.getPurpose());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest req) {
        sendOtp(SendOtpRequest.builder()
                .email(req.getEmail())
                .purpose(OtpPurposeType.FORGOT_PASSWORD)
                .build());
    }

    // ---------------------------------------------------------------- 3. otp/verify

    @Override
    public void verifyOtp(OtpVerifyRequest req) {
        checkOtpValid(req.getEmail(), req.getOtpCode(), req.getPurpose());
        // Chỉ xác nhận đúng, KHÔNG đánh dấu isUsed ở đây - để API dùng OTP thật
        // (VD: reset-password) tự tiêu (consume) mã sau khi hoàn tất hành động.
    }

    private OtpVerification checkOtpValid(String email, String otpCode, OtpPurposeType purpose) {
        OtpVerification otp = otpDao.findLatestValid(email, purpose)
                .orElseThrow(() -> new BusinessException(400, "Mã OTP không tồn tại hoặc đã được sử dụng"));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "Mã OTP đã hết hạn, vui lòng gửi lại mã mới");
        }
        if (!otp.getOtpCode().equals(otpCode)) {
            throw new BusinessException(400, "Mã OTP không đúng");
        }
        return otp;
    }

    // ---------------------------------------------------------------- 4. login (buoc 1: username + password)

    /**
     * Username+password dung VAN CHUA duoc dang nhap. Buoc nay LUON tra ve thu
     * buoc OTP de FE hien ngay tai man login:
     *   - Tai khoan CHUA dang ky app Authenticator -> sinh secret moi, tra QR de quet (setupRequired=true).
     *   - Da dang ky -> chi can nhap ma 6 so (setupRequired=false).
     */
    @Override
    public LoginTwoFactorChallengeResponse login(UsersLoginRequest req) {
        User user = usersDao.findByUsernameWithRoles(req.getUsername())
                .orElseThrow(() -> new BusinessException(401, "Sai username hoặc mật khẩu"));

        if (!PasswordUntil.checkPassword(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Sai username hoặc mật khẩu");
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new BusinessException(403, "Tài khoản đã bị khóa");
        }

        // Lan dau tien (hoac sau khi user tu tat 2FA): dang ky app Authenticator ngay tai day.
        boolean setupRequired = user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank();
        if (setupRequired) {
            user.setTwoFactorSecret(TotpUtil.generateSecret());
            // Chua bat 2FA - chi that su bat sau khi user nhap dung ma dau tien o buoc 2,
            // tranh khoa chet tai khoan neu ho bo giua chung.
            user.setTwoFactorEnabled(false);
            usersDao.update(user);
            log.info("Sinh khoa TOTP moi cho userId={} (lan dau dang nhap, cho user quet QR)", user.getId());
        }

        String loginToken = openTwoFactorChallenge(user.getId(), setupRequired);

        LoginTwoFactorChallengeResponse.LoginTwoFactorChallengeResponseBuilder res =
                LoginTwoFactorChallengeResponse.builder()
                        .requiresTwoFactor(true)
                        .setupRequired(setupRequired)
                        .loginToken(loginToken)
                        .expiresIn((long) LOGIN_2FA_TTL_SECONDS)
                        .issuer(TotpUtil.getIssuer())
                        .username(user.getUsername());

        if (setupRequired) {
            res.qrCodeDataUri(TotpUtil.generateQrDataUri(user.getUsername(), user.getTwoFactorSecret()))
                    .otpAuthUri(TotpUtil.buildOtpAuthUri(user.getUsername(), user.getTwoFactorSecret()))
                    .secret(user.getTwoFactorSecret());
        }
        return res.build();
    }

    // ---------------------------------------------------------------- 4b. login/2fa (buoc 2: ma OTP)

    @Override
    public AuthTokenResponse completeLogin(LoginTwoFactorRequest req) {
        String key = REDIS_LOGIN_2FA_PREFIX + req.getLoginToken();
        String attemptsKey = key + REDIS_LOGIN_2FA_ATTEMPTS;

        String payload;
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            payload = jedis.get(key);
        }
        if (payload == null) {
            throw new BusinessException(401, "Phiên xác thực 2 lớp đã hết hạn, vui lòng đăng nhập lại");
        }
        int sep = payload.indexOf('|');
        Long userId;
        try {
            userId = Long.valueOf(sep > 0 ? payload.substring(0, sep) : payload);
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "Phiên xác thực 2 lớp không hợp lệ, vui lòng đăng nhập lại");
        }
        boolean setupRequired = sep > 0 && "1".equals(payload.substring(sep + 1));

        User user = usersDao.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(401, "Tài khoản không còn tồn tại"));

        if (!TotpUtil.verify(user.getTwoFactorSecret(), req.getCode())) {
            throw registerFailedAttempt(key, attemptsKey);
        }

        // Ma dung -> tieu phieu (1 lan duy nhat) va bat 2FA neu day la lan xac nhan dau tien.
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.del(key, attemptsKey);
        }
        if (setupRequired || !Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            user.setTwoFactorEnabled(true);
            usersDao.update(user);
            log.info("Bật 2FA cho userId={} sau lần xác nhận OTP đầu tiên", userId);
        }
        log.info("Đăng nhập hoàn tất (đã qua OTP) userId={}", userId);
        return issueTokens(user);
    }

    private String openTwoFactorChallenge(Long userId, boolean setupRequired) {
        String token = UUID.randomUUID().toString();
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.setex(REDIS_LOGIN_2FA_PREFIX + token, LOGIN_2FA_TTL_SECONDS,
                    userId + "|" + (setupRequired ? "1" : "0"));
        }
        return token;
    }

    /** Dem so lan nhap sai; qua nguong thì huỷ phieu de chong doa ma 6 so. */
    private BusinessException registerFailedAttempt(String key, String attemptsKey) {
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            long attempts = jedis.incr(attemptsKey);
            if (attempts == 1) {
                jedis.expire(attemptsKey, LOGIN_2FA_TTL_SECONDS);
            }
            if (attempts >= LOGIN_2FA_MAX_ATTEMPTS) {
                jedis.del(key, attemptsKey);
                return new BusinessException(429,
                        "Nhập sai mã OTP quá " + LOGIN_2FA_MAX_ATTEMPTS + " lần, vui lòng đăng nhập lại");
            }
            long left = LOGIN_2FA_MAX_ATTEMPTS - attempts;
            return new BusinessException(401,
                    "Mã OTP không đúng, còn " + left + " lần thử trước khi phải đăng nhập lại");
        }
    }

    // ---------------------------------------------------------------- 5. refresh-token

    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest req) {
        JwtUtil.Claims claims = JwtUtil.parse(req.getRefreshToken());
        if (!"refresh".equals(claims.getType())) {
            throw new BusinessException(401, "Token không phải refresh token");
        }
        Long userId = claims.getUserId();

        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            String stored = jedis.get(REDIS_REFRESH_PREFIX + userId);
            if (stored == null || !stored.equals(req.getRefreshToken())) {
                throw new BusinessException(401, "Refresh token không hợp lệ hoặc đã đăng xuất trước đó");
            }
        }

        User user = usersDao.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(401, "Tài khoản không còn tồn tại"));

        Set<String> roles = roleCodesOf(user);
        String newAccessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(req.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(JwtUtil.accessTokenTtlSeconds())
                .user(toSummary(user, roles))
                .build();
    }

    // ---------------------------------------------------------------- 7. reset-password

    @Override
    public void resetPassword(ResetPasswordRequest req) {
        OtpVerification otp = checkOtpValid(req.getEmail(), req.getOtpCode(), OtpPurposeType.FORGOT_PASSWORD);

        User user = usersDao.findByCode(req.getEmail())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản"));

        user.setPassword(PasswordUntil.hashedPassword(req.getNewPassword()));
        usersDao.update(user);

        otpDao.markUsed(otp.getId());

        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.del(REDIS_REFRESH_PREFIX + user.getId());
        }

        log.info("Đặt lại mật khẩu thành công cho userId={}", user.getId());
    }

    // ---------------------------------------------------------------- 8. logout

    @Override
    public void logout(RefreshTokenRequest req) {
        if (req == null || req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            return;
        }
        Long userId;
        try {
            userId = JwtUtil.parse(req.getRefreshToken(), false).getUserId();
        } catch (BusinessException e) {
            return;
        }
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.del(REDIS_REFRESH_PREFIX + userId);
        }
    }

    // ---------------------------------------------------------------- helpers

    private AuthTokenResponse issueTokens(User user) {
        Set<String> roles = roleCodesOf(user);
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = JwtUtil.generateRefreshToken(user.getId());

        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.setex(REDIS_REFRESH_PREFIX + user.getId(),
                    (int) JwtUtil.refreshTokenTtlSeconds(), refreshToken);
        }

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(JwtUtil.accessTokenTtlSeconds())
                .user(toSummary(user, roles))
                .build();
    }

    private Set<String> roleCodesOf(User user) {
        return user.getRole().stream().map(Role::getCode).collect(Collectors.toSet());
    }

    private AuthTokenResponse.UserSummary toSummary(User user, Set<String> roles) {
        return AuthTokenResponse.UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .roles(roles)
                .twoFactorEnabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .build();
    }

    // ---------------------------------------------------------------- 2FA sau dang nhap (doi thiet bi / dang ky lai / tat)

    @Override
    public TwoFactorSetupResponse setupTwoFactor(Long userId) {
        User user = usersDao.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(404, "Tai khoản không tồn tại"));

        String secret = TotpUtil.generateSecret();
        user.setTwoFactorSecret(secret);
        // Chua bat 2FA - phai qua buoc confirm (nhap ma tu app) moi thuc su bat.
        user.setTwoFactorEnabled(false);
        usersDao.update(user);

        log.info("Tạo khóa TOTP mới cho userId={}", userId);
        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .otpAuthUri(TotpUtil.buildOtpAuthUri(user.getUsername(), secret))
                .qrCodeDataUri(TotpUtil.generateQrDataUri(user.getUsername(), secret))
                .enabled(false)
                .issuer(TotpUtil.getIssuer())
                .build();
    }

    @Override
    public void confirmTwoFactor(Long userId, String totpCode) {
        User user = usersDao.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(404, "Tai khoản không tồn tại"));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessException(400, "Tai khoan da bat 2FA tu truoc");
        }
        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank()) {
            throw new BusinessException(400, "Chua bat dau thiet lap 2FA - goi /api/auth/2fa/setup truoc");
        }
        if (!TotpUtil.verify(user.getTwoFactorSecret(), totpCode)) {
            throw new BusinessException(400, "Ma 2FA khong dung - kiem tra lai dong ho thiet bi hoac nhap ma moi");
        }

        user.setTwoFactorEnabled(true);
        usersDao.update(user);
        log.info("Bat 2FA thanh cong cho userId={}", userId);
    }

    @Override
    public void disableTwoFactor(Long userId, String totpCode) {
        User user = usersDao.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException(404, "Tai khoản không tồn tại"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessException(400, "Tai khoan chua bat 2FA");
        }
        if (!TotpUtil.verify(user.getTwoFactorSecret(), totpCode)) {
            throw new BusinessException(400, "Ma 2FA khong dung - khong the tat");
        }

        // Xoa luon khoa: lan dang nhap sau se tu dong quay lai buoc quet QR tu dau,
        // dam bao OTP luon la bat buoc va khong ai tat duoc vinh vien.
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        usersDao.update(user);
        log.info("Tat 2FA thanh cong cho userId={}", userId);
    }
}
