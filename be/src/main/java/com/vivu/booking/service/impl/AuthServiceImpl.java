package com.vivu.booking.service.impl;

import com.vivu.booking.config.RedisConfig;
import com.vivu.booking.dao.OtpVerificationDao;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.ForgotPasswordRequest;
import com.vivu.booking.dto.request.OtpVerifyRequest;
import com.vivu.booking.dto.request.RefreshTokenRequest;
import com.vivu.booking.dto.request.RegisterRequest;
import com.vivu.booking.dto.request.ResetPasswordRequest;
import com.vivu.booking.dto.request.SendOtpRequest;
import com.vivu.booking.dto.request.UsersLoginRequest;
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

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * Role mặc định cho tài khoản tự đăng ký.
     * Role này phải tồn tại sẵn trong bảng roles.
     */
    private static final String DEFAULT_ROLE_CODE = "CUSTOMER";

    /**
     * OTP có hiệu lực trong 5 phút.
     */
    private static final int OTP_TTL_MINUTES = 5;

    /**
     * Redis key:
     * auth:refresh:{userId}
     */
    private static final String REDIS_REFRESH_PREFIX = "auth:refresh:";

    private final UsersDao usersDao;
    private final RoleDao roleDao;
    private final OtpVerificationDao otpDao;
    private final EmailSender emailSender;

    public AuthServiceImpl(
            UsersDao usersDao,
            RoleDao roleDao,
            OtpVerificationDao otpDao,
            EmailSender emailSender) {

        this.usersDao = usersDao;
        this.roleDao = roleDao;
        this.otpDao = otpDao;
        this.emailSender = emailSender;
    }

    public AuthServiceImpl() {
        this(
                new UsersDao(),
                new RoleDao(),
                new OtpVerificationDao(),
                new ConsoleEmailSender()
        );
    }

    // ================================================================
    // 1. REGISTER
    // ================================================================

    @Override
    public UsersResponse register(RegisterRequest req) {

        // 1. Kiểm tra email
        if (usersDao.existsByCode(req.getEmail())) {
            throw new BusinessException(
                    409,
                    "Email đã được sử dụng"
            );
        }

        // 2. Kiểm tra username
        if (usersDao.existsByUsername(req.getUsername())) {
            throw new BusinessException(
                    409,
                    "Username đã được sử dụng"
            );
        }

        // 3. Kiểm tra phone
        if (usersDao.existsByPhone(req.getPhone())) {
            throw new BusinessException(
                    409,
                    "Số điện thoại đã được sử dụng"
            );
        }

        // 4. Kiểm tra role mặc định tồn tại
        Role defaultRole = roleDao
                .findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() ->
                        new BusinessException(
                                500,
                                "Thiếu role mặc định '"
                                        + DEFAULT_ROLE_CODE
                                        + "' trong DB"
                        )
                );

        // 5. Tạo User
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .username(req.getUsername())
                .password(
                        PasswordUntil.hashedPassword(
                                req.getPassword()
                        )
                )
                .gender(req.getGender())
                .status(UserStatus.ACTIVE)
                .active(true)
                .role(new HashSet<>())
                .build();

        // 6. Lưu User
        usersDao.save(user);

        // 7. Gán role mặc định = user
        roleDao.addRole(
                user.getId(),
                defaultRole.getId()
        );

        log.info(
                "Đăng ký tài khoản mới id={} username={} role={}",
                user.getId(),
                user.getUsername(),
                DEFAULT_ROLE_CODE
        );

        // 8. Set role để response có role
        user.setRole(
                new HashSet<>(
                        Set.of(defaultRole)
                )
        );

        return UserMapper.toResponse(user);
    }

    // ================================================================
    // 2. SEND OTP
    // ================================================================

    @Override
    public void sendOtp(SendOtpRequest req) {

        String email = req.getEmail();

        OtpPurposeType purpose = req.getPurpose();

        // ------------------------------------------------------------
        // Kiểm tra email có tồn tại hay chưa
        // ------------------------------------------------------------

        boolean userExists =
                usersDao.existsByCode(email);

        // ------------------------------------------------------------
        // REGISTER
        //
        // Đăng ký:
        // email phải CHƯA tồn tại
        // ------------------------------------------------------------

        if (purpose == OtpPurposeType.REGISTER) {

            if (userExists) {
                throw new BusinessException(
                        409,
                        "Email đã được đăng ký"
                );
            }
        }

        // ------------------------------------------------------------
        // FORGOT_PASSWORD
        //
        // Quên mật khẩu:
        // email phải tồn tại
        // ------------------------------------------------------------

        if (purpose == OtpPurposeType.FORGOT_PASSWORD) {

            if (!userExists) {
                throw new BusinessException(
                        404,
                        "Email chưa đăng ký tài khoản nào"
                );
            }
        }

        // ------------------------------------------------------------
        // Vô hiệu hóa OTP cũ
        // ------------------------------------------------------------

        otpDao.invalidateAllForEmail(
                email,
                purpose
        );

        // ------------------------------------------------------------
        // Generate OTP 6 số
        // ------------------------------------------------------------

        String code =
                OtpUtil.generate6Digit();

        // ------------------------------------------------------------
        // Nếu là FORGOT_PASSWORD thì liên kết User.
        //
        // REGISTER chưa có User nên user = null.
        // ------------------------------------------------------------

        User linkedUser = null;

        if (userExists) {
            linkedUser =
                    usersDao.findByCode(email)
                            .orElse(null);
        }

        // ------------------------------------------------------------
        // Tạo OTP entity
        // ------------------------------------------------------------

        OtpVerification otp =
                OtpVerification.builder()
                        .user(linkedUser)
                        .email(email)
                        .otpCode(code)
                        .purpose(purpose)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(
                                                OTP_TTL_MINUTES
                                        )
                        )
                        .isUsed(false)
                        .build();

        // ------------------------------------------------------------
        // Lưu OTP
        // ------------------------------------------------------------

        otpDao.save(otp);

        // ------------------------------------------------------------
        // Gửi email
        // ------------------------------------------------------------

        emailSender.sendOtpEmail(
                email,
                code,
                purpose
        );

        log.info(
                "Đã gửi OTP email={} purpose={}",
                email,
                purpose
        );
    }

    // ================================================================
    // 3. FORGOT PASSWORD
    // ================================================================

    @Override
    public void forgotPassword(
            ForgotPasswordRequest req) {

        sendOtp(
                SendOtpRequest.builder()
                        .email(req.getEmail())
                        .purpose(
                                OtpPurposeType.FORGOT_PASSWORD
                        )
                        .build()
        );
    }

    // ================================================================
    // 4. VERIFY OTP
    // ================================================================

    @Override
    public void verifyOtp(
            OtpVerifyRequest req) {

        checkOtpValid(
                req.getEmail(),
                req.getOtpCode(),
                req.getPurpose()
        );

        /*
         * Không markUsed() ở đây.
         *
         * Vì OTP sau khi verify vẫn cần được sử dụng
         * trong bước reset password.
         */
    }

    // ================================================================
    // CHECK OTP
    // ================================================================

    private OtpVerification checkOtpValid(
            String email,
            String otpCode,
            OtpPurposeType purpose) {

        // ------------------------------------------------------------
        // Lấy OTP mới nhất còn hiệu lực
        // ------------------------------------------------------------

        OtpVerification otp =
                otpDao.findLatestValid(
                                email,
                                purpose
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        400,
                                        "Mã OTP không tồn tại hoặc đã được sử dụng"
                                )
                        );

        // ------------------------------------------------------------
        // Kiểm tra hết hạn
        // ------------------------------------------------------------

        if (otp.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new BusinessException(
                    400,
                    "Mã OTP đã hết hạn, vui lòng gửi lại mã mới"
            );
        }

        // ------------------------------------------------------------
        // Kiểm tra OTP
        // ------------------------------------------------------------

        if (!otp.getOtpCode().equals(otpCode)) {

            throw new BusinessException(
                    400,
                    "Mã OTP không đúng"
            );
        }

        return otp;
    }

    // ================================================================
    // 5. LOGIN
    // ================================================================

    @Override
    public AuthTokenResponse login(
            UsersLoginRequest req) {

        // ------------------------------------------------------------
        // Tìm User + Roles
        // ------------------------------------------------------------

        User user =
                usersDao.findByUsernameWithRoles(
                                req.getUsername()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        401,
                                        "Sai username hoặc mật khẩu"
                                )
                        );

        // ------------------------------------------------------------
        // Kiểm tra password
        // ------------------------------------------------------------

        if (!PasswordUntil.checkPassword(
                req.getPassword(),
                user.getPassword())) {

            throw new BusinessException(
                    401,
                    "Sai username hoặc mật khẩu"
            );
        }

        // ------------------------------------------------------------
        // Kiểm tra tài khoản bị khóa
        // ------------------------------------------------------------

        if (Boolean.FALSE.equals(
                user.getActive())) {

            throw new BusinessException(
                    403,
                    "Tài khoản đã bị khóa"
            );
        }

        // ------------------------------------------------------------
        // Generate token
        // ------------------------------------------------------------

        return issueTokens(user);
    }

    // ================================================================
    // 6. REFRESH TOKEN
    // ================================================================

    @Override
    public AuthTokenResponse refreshToken(
            RefreshTokenRequest req) {

        // ------------------------------------------------------------
        // Parse refresh token
        // ------------------------------------------------------------

        JwtUtil.Claims claims =
                JwtUtil.parse(
                        req.getRefreshToken()
                );

        // ------------------------------------------------------------
        // Kiểm tra token type
        // ------------------------------------------------------------

        if (!"refresh".equals(
                claims.getType())) {

            throw new BusinessException(
                    401,
                    "Token không phải refresh token"
            );
        }

        Long userId =
                claims.getUserId();

        // ------------------------------------------------------------
        // Kiểm tra refresh token trong Redis
        // ------------------------------------------------------------

        try (Jedis jedis =
                     RedisConfig.getPool()
                             .getResource()) {

            String stored =
                    jedis.get(
                            REDIS_REFRESH_PREFIX
                                    + userId
                    );

            if (stored == null
                    || !stored.equals(
                    req.getRefreshToken())) {

                throw new BusinessException(
                        401,
                        "Refresh token không hợp lệ hoặc đã đăng xuất trước đó"
                );
            }
        }

        // ------------------------------------------------------------
        // Lấy User + Roles
        // ------------------------------------------------------------

        User user =
                usersDao.findByIdWithRoles(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        401,
                                        "Tài khoản không còn tồn tại"
                                )
                        );

        // ------------------------------------------------------------
        // Lấy roles
        // ------------------------------------------------------------

        Set<String> roles =
                roleCodesOf(user);

        // ------------------------------------------------------------
        // Chỉ tạo access token mới
        // ------------------------------------------------------------

        String newAccessToken =
                JwtUtil.generateAccessToken(
                        user.getId(),
                        user.getUsername(),
                        roles
                );

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(req.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(
                        JwtUtil.accessTokenTtlSeconds()
                )
                .user(
                        toSummary(
                                user,
                                roles
                        )
                )
                .build();
    }

    // ================================================================
    // 7. RESET PASSWORD
    // ================================================================

    @Override
    public void resetPassword(
            ResetPasswordRequest req) {

        // ------------------------------------------------------------
        // Kiểm tra OTP
        // ------------------------------------------------------------

        OtpVerification otp =
                checkOtpValid(
                        req.getEmail(),
                        req.getOtpCode(),
                        OtpPurposeType.FORGOT_PASSWORD
                );

        // ------------------------------------------------------------
        // Tìm User
        // ------------------------------------------------------------

        User user =
                usersDao.findByCode(
                                req.getEmail()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        404,
                                        "Không tìm thấy tài khoản"
                                )
                        );

        // ------------------------------------------------------------
        // Hash password mới
        // ------------------------------------------------------------

        String hashedPassword =
                PasswordUntil.hashedPassword(
                        req.getNewPassword()
                );

        user.setPassword(
                hashedPassword
        );

        // ------------------------------------------------------------
        // Update User
        // ------------------------------------------------------------

        usersDao.update(user);

        // ------------------------------------------------------------
        // OTP đã được sử dụng
        // ------------------------------------------------------------

        otpDao.markUsed(
                otp.getId()
        );

        // ------------------------------------------------------------
        // Xóa refresh token cũ
        //
        // Bắt buộc đăng nhập lại sau khi đổi password.
        // ------------------------------------------------------------

        try (Jedis jedis =
                     RedisConfig.getPool()
                             .getResource()) {

            jedis.del(
                    REDIS_REFRESH_PREFIX
                            + user.getId()
            );
        }

        log.info(
                "Đặt lại mật khẩu thành công cho userId={}",
                user.getId()
        );
    }

    // ================================================================
    // 8. LOGOUT
    // ================================================================

    @Override
    public void logout(
            RefreshTokenRequest req) {

        if (req == null
                || req.getRefreshToken() == null
                || req.getRefreshToken().isBlank()) {

            return;
        }

        Long userId;

        try {

            userId =
                    JwtUtil.parse(
                            req.getRefreshToken(),
                            false
                    ).getUserId();

        } catch (BusinessException e) {

            return;
        }

        // ------------------------------------------------------------
        // Xóa refresh token khỏi Redis
        // ------------------------------------------------------------

        try (Jedis jedis =
                     RedisConfig.getPool()
                             .getResource()) {

            jedis.del(
                    REDIS_REFRESH_PREFIX
                            + userId
            );
        }

        log.info(
                "Logout thành công userId={}",
                userId
        );
    }

    // ================================================================
    // HELPER - ISSUE TOKENS
    // ================================================================

    private AuthTokenResponse issueTokens(
            User user) {

        // ------------------------------------------------------------
        // Roles
        // ------------------------------------------------------------

        Set<String> roles =
                roleCodesOf(user);

        // ------------------------------------------------------------
        // Access token
        // ------------------------------------------------------------

        String accessToken =
                JwtUtil.generateAccessToken(
                        user.getId(),
                        user.getUsername(),
                        roles
                );

        // ------------------------------------------------------------
        // Refresh token
        // ------------------------------------------------------------

        String refreshToken =
                JwtUtil.generateRefreshToken(
                        user.getId()
                );

        // ------------------------------------------------------------
        // Lưu refresh token Redis
        // ------------------------------------------------------------

        try (Jedis jedis =
                     RedisConfig.getPool()
                             .getResource()) {

            jedis.setex(
                    REDIS_REFRESH_PREFIX
                            + user.getId(),

                    (int) JwtUtil
                            .refreshTokenTtlSeconds(),

                    refreshToken
            );
        }

        // ------------------------------------------------------------
        // Response
        // ------------------------------------------------------------

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(
                        JwtUtil.accessTokenTtlSeconds()
                )
                .user(
                        toSummary(
                                user,
                                roles
                        )
                )
                .build();
    }

    // ================================================================
    // HELPER - GET ROLE CODES
    // ================================================================

    private Set<String> roleCodesOf(
            User user) {

        if (user.getRole() == null
                || user.getRole().isEmpty()) {

            return new HashSet<>();
        }

        return user.getRole()
                .stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }

    // ================================================================
    // HELPER - USER SUMMARY
    // ================================================================

    private AuthTokenResponse.UserSummary toSummary(
            User user,
            Set<String> roles) {

        return AuthTokenResponse.UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }
}