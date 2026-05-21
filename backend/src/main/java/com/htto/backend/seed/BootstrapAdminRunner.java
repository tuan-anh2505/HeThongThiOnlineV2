package com.htto.backend.seed;

import com.htto.backend.config.BootstrapAdminProperties;
import com.htto.backend.domain.Account;
import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.AdminProfile;
import com.htto.backend.domain.Role;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AdminProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final AccountRepository accountRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(
            BootstrapAdminProperties properties,
            AccountRepository accountRepository,
            AdminProfileRepository adminProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.accountRepository = accountRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        requireText(properties.username(), "BOOTSTRAP_ADMIN_USERNAME");
        requireText(properties.email(), "BOOTSTRAP_ADMIN_EMAIL");
        requireText(properties.password(), "BOOTSTRAP_ADMIN_PASSWORD");
        requireText(properties.fullName(), "BOOTSTRAP_ADMIN_FULL_NAME");
        requireText(properties.adminCode(), "BOOTSTRAP_ADMIN_CODE");

        Account account = accountRepository.findByUsernameOrEmail(properties.username(), properties.email())
                .map(this::updateExistingAdmin)
                .orElseGet(this::createAdminAccount);

        createAdminProfileIfMissing(account);
        log.info("Bootstrap admin account is ready: username={}, email={}", account.getUsername(), account.getEmail());
    }

    private Account createAdminAccount() {
        Account account = new Account();
        account.setUsername(properties.username().trim());
        account.setEmail(properties.email().trim());
        account.setPassword(passwordEncoder.encode(properties.password()));
        account.setFullName(properties.fullName().trim());
        account.setRole(Role.ADMIN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setDeleted(false);
        Account savedAccount = accountRepository.save(account);
        log.info("Created bootstrap admin account: username={}", savedAccount.getUsername());
        return savedAccount;
    }

    private Account updateExistingAdmin(Account account) {
        account.setRole(Role.ADMIN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setDeleted(false);
        account.setDeletedAt(null);
        account.setPassword(passwordEncoder.encode(properties.password()));
        account.setFullName(properties.fullName().trim());
        account.setEmail(properties.email().trim());
        Account savedAccount = accountRepository.save(account);
        log.info("Updated bootstrap admin account: username={}", savedAccount.getUsername());
        return savedAccount;
    }

    private void createAdminProfileIfMissing(Account account) {
        adminProfileRepository.findByAccountId(account.getId())
                .or(() -> adminProfileRepository.findByAdminCode(properties.adminCode().trim()))
                .orElseGet(() -> {
                    AdminProfile profile = new AdminProfile();
                    profile.setAccountId(account.getId());
                    profile.setAdminCode(properties.adminCode().trim());
                    log.info("Created bootstrap admin profile: adminCode={}", profile.getAdminCode());
                    return adminProfileRepository.save(profile);
                });
    }

    private void requireText(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(envName + " is required when BOOTSTRAP_ADMIN_ENABLED=true");
        }
    }
}
