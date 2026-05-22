package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.PasswordResetToken;
import com.htto.backend.dto.request.ChangePasswordRequest;
import com.htto.backend.dto.request.ForgotPasswordRequest;
import com.htto.backend.dto.request.ResetPasswordRequest;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.PasswordResetTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordService {

    private static final String FORGOT_PASSWORD_MESSAGE =
            "Nếu email tồn tại trong hệ thống, mã xác thực sẽ được gửi đến email đó";
    private static final String RESET_PASSWORD_SUCCESS_MESSAGE = "Đặt lại mật khẩu thành công";
    private static final String CHANGE_PASSWORD_SUCCESS_MESSAGE = "Đổi mật khẩu thành công";
    private static final String INVALID_OR_EXPIRED_OTP_MESSAGE = "Mã xác thực không đúng hoặc đã hết hạn";
    private static final int OTP_BOUND = 1_000_000;
    private static final long OTP_EXPIRATION_MINUTES = 10;

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SystemLogService systemLogService;
    private final MongoTemplate mongoTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordService(
            AccountRepository accountRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            SystemLogService systemLogService,
            MongoTemplate mongoTemplate
    ) {
        this.accountRepository = accountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.systemLogService = systemLogService;
        this.mongoTemplate = mongoTemplate;
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<Account> accountOptional = findActiveAccountByEmail(email);

        if (accountOptional.isEmpty()) {
            systemLogService.log(null, "FORGOT_PASSWORD_REQUEST", "ACCOUNT", null,
                    "Password reset OTP requested");
            return FORGOT_PASSWORD_MESSAGE;
        }

        Account account = accountOptional.get();
        markOpenTokensUsed(email);

        String otp = generateOtp();
        PasswordResetToken token = new PasswordResetToken();
        token.setAccountId(account.getId());
        token.setEmail(email);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiresAt(Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        mailService.sendPasswordResetOtp(account.getEmail(), otp);
        systemLogService.log(account.getId(), "FORGOT_PASSWORD_REQUEST", "ACCOUNT", account.getId(),
                "Password reset OTP requested");

        return FORGOT_PASSWORD_MESSAGE;
    }

    public String resetPassword(ResetPasswordRequest request) {
        validateNewPassword(request.newPassword(), request.confirmPassword());

        String email = normalizeEmail(request.email());
        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_OTP_MESSAGE));

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_OTP_MESSAGE);
        }

        if (!StringUtils.hasText(request.otp()) || !passwordEncoder.matches(request.otp().trim(), token.getOtpHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_OTP_MESSAGE);
        }

        Account account = findActiveAccountByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_OTP_MESSAGE));

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        systemLogService.log(account.getId(), "RESET_PASSWORD_SUCCESS", "ACCOUNT", account.getId(),
                "Password reset completed");
        return RESET_PASSWORD_SUCCESS_MESSAGE;
    }

    public String changePassword(String username, ChangePasswordRequest request) {
        validateNewPassword(request.newPassword(), request.confirmPassword());

        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (!StringUtils.hasText(request.oldPassword())
                || !passwordEncoder.matches(request.oldPassword(), account.getPassword())) {
            systemLogService.log(account.getId(), "CHANGE_PASSWORD_FAILED", "ACCOUNT", account.getId(),
                    "Current password did not match");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);

        systemLogService.log(account.getId(), "CHANGE_PASSWORD_SUCCESS", "ACCOUNT", account.getId(),
                "Password changed successfully");
        return CHANGE_PASSWORD_SUCCESS_MESSAGE;
    }

    private Optional<Account> findActiveAccountByEmail(String email) {
        Pattern emailPattern = Pattern.compile("^" + Pattern.quote(email) + "$", Pattern.CASE_INSENSITIVE);
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("email").regex(emailPattern),
                Criteria.where("deleted").ne(true),
                Criteria.where("status").is(AccountStatus.ACTIVE)
        ));
        return Optional.ofNullable(mongoTemplate.findOne(query, Account.class));
    }

    private void markOpenTokensUsed(String email) {
        List<PasswordResetToken> openTokens = passwordResetTokenRepository.findByEmailAndUsedFalse(email);
        if (openTokens.isEmpty()) {
            return;
        }
        openTokens.forEach(token -> token.setUsed(true));
        passwordResetTokenRepository.saveAll(openTokens);
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được để trống");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(OTP_BOUND));
    }
}
