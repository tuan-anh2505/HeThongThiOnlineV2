package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.Role;
import com.htto.backend.dto.request.AccountCreateRequest;
import com.htto.backend.dto.request.AccountUpdateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.repository.AccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public class AccountService {

    private final AccountRepository accountRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            AccountRepository accountRepository,
            MongoTemplate mongoTemplate,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse create(AccountCreateRequest request) {
        ensureUsernameAvailable(request.username(), null);
        ensureEmailAvailable(request.email(), null);

        Account account = new Account();
        account.setUsername(request.username().trim());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName().trim());
        account.setDateOfBirth(request.dateOfBirth());
        account.setEmail(request.email().trim());
        account.setPhone(trimToNull(request.phone()));
        account.setRole(request.role());
        account.setStatus(request.status() == null ? AccountStatus.ACTIVE : request.status());

        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse update(String id, AccountUpdateRequest request) {
        Account account = getAccountOrThrow(id);

        if (StringUtils.hasText(request.username()) && !request.username().equals(account.getUsername())) {
            ensureUsernameAvailable(request.username(), id);
            account.setUsername(request.username().trim());
        }
        if (StringUtils.hasText(request.password())) {
            account.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.fullName() != null) {
            account.setFullName(request.fullName().trim());
        }
        if (request.dateOfBirth() != null) {
            account.setDateOfBirth(request.dateOfBirth());
        }
        if (StringUtils.hasText(request.email()) && !request.email().equals(account.getEmail())) {
            ensureEmailAvailable(request.email(), id);
            account.setEmail(request.email().trim());
        }
        if (request.phone() != null) {
            account.setPhone(trimToNull(request.phone()));
        }
        if (request.role() != null) {
            account.setRole(request.role());
        }
        if (request.status() != null) {
            account.setStatus(request.status());
        }

        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse lock(String id) {
        Account account = getAccountOrThrow(id);
        account.setStatus(AccountStatus.LOCKED);
        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse unlock(String id) {
        Account account = getAccountOrThrow(id);
        account.setStatus(AccountStatus.ACTIVE);
        return AccountResponse.from(accountRepository.save(account));
    }

    public void softDelete(String id) {
        Account account = getAccountOrThrow(id);
        account.setDeleted(true);
        account.setDeletedAt(Instant.now());
        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);
    }

    public AccountResponse getByUsername(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        return AccountResponse.from(account);
    }

    public Account getActiveAccountByUsername(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked");
        }
        return account;
    }

    public List<AccountResponse> search(
            String username,
            String fullName,
            String email,
            String phone,
            Role role,
            AccountStatus status) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("deleted").ne(true));

        addRegexCriteria(criteria, "username", username);
        addRegexCriteria(criteria, "fullName", fullName);
        addRegexCriteria(criteria, "email", email);
        addRegexCriteria(criteria, "phone", phone);

        if (role != null) {
            criteria.add(Criteria.where("role").is(role));
        }
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }

        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        return mongoTemplate.find(query, Account.class)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    private Account getAccountOrThrow(String id) {
        return accountRepository.findById(id)
                .filter(account -> !account.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void ensureUsernameAvailable(String username, String currentAccountId) {
        accountRepository.findByUsername(username.trim())
                .filter(account -> currentAccountId == null || !account.getId().equals(currentAccountId))
                .ifPresent(account -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
                });
    }

    private void ensureEmailAvailable(String email, String currentAccountId) {
        accountRepository.findByEmail(email.trim())
                .filter(account -> currentAccountId == null || !account.getId().equals(currentAccountId))
                .ifPresent(account -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });
    }

    private void addRegexCriteria(List<Criteria> criteria, String field, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(value.trim()), Pattern.CASE_INSENSITIVE);
        criteria.add(Criteria.where(field).regex(pattern));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
