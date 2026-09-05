package com.vivu.booking.service.impl;

import com.vivu.booking.config.RedisConfig;
import com.vivu.booking.dao.OtpVerificationDao;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
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
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.OtpUtil;
import com.vivu.booking.utils.PasswordUntil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Role mặc định gán cho tài khoản tự đăng ký - phải tồn tại sẵn trong bảng roles (seed data). */
    private static final String DEFAULT_ROLE_CODE = "user";
    private static final int OTP_TTL_MINUTES = 5;
    private static final String REDIS_REFRESH_PREFIX = "auth:refresh:"; // key = auth:refresh:{userId}

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

    // ---------------------------------------------------------------- 4. login

    @Override
    public AuthTokenResponse login(UsersLoginRequest req) {
        User user = usersDao.findByUsernameWithRoles(req.getUsername())
                .orElseThrow(() -> new BusinessException(401, "Sai username hoặc mật khẩu"));

        if (!PasswordUntil.checkPassword(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Sai username hoặc mật khẩu");
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new BusinessException(403, "Tài khoản đã bị khóa");
        }

        return issueTokens(user);
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
                .roles(roles)
                .build();
    }
}
