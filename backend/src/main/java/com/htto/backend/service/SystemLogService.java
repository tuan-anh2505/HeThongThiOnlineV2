package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.SystemLog;
import com.htto.backend.dto.response.SystemLogResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.SystemLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final AccountRepository accountRepository;
    private final MongoTemplate mongoTemplate;

    public SystemLogService(
            SystemLogRepository systemLogRepository,
            AccountRepository accountRepository,
            MongoTemplate mongoTemplate
    ) {
        this.systemLogRepository = systemLogRepository;
        this.accountRepository = accountRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void logCurrentUser(String action, String targetType, String targetId, String description) {
        log(resolveCurrentAccountId(), action, targetType, targetId, description);
    }

    public void log(String accountId, String action, String targetType, String targetId, String description) {
        SystemLog log = new SystemLog();
        log.setAccountId(accountId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setIpAddress(resolveClientIp());
        log.setOccurredAt(Instant.now());
        systemLogRepository.save(log);
    }

    public List<SystemLogResponse> search(
            String accountId,
            String action,
            String targetType,
            String targetId,
            String ipAddress,
            Instant from,
            Instant to
    ) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addExact(criteria, "accountId", accountId);
        addExact(criteria, "action", action);
        addExact(criteria, "targetType", targetType);
        addExact(criteria, "targetId", targetId);
        addExact(criteria, "ipAddress", ipAddress);
        if (from != null || to != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null) {
                createdAt = createdAt.gte(from);
            }
            if (to != null) {
                createdAt = createdAt.lte(to);
            }
            criteria.add(createdAt);
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        return mongoTemplate.find(query, SystemLog.class)
                .stream()
                .sorted(Comparator.comparing(
                        (SystemLog log) -> log.getCreatedAt() == null ? log.getOccurredAt() : log.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(SystemLogResponse::from)
                .toList();
    }

    private void addExact(List<Criteria> criteria, String field, String value) {
        if (StringUtils.hasText(value)) {
            criteria.add(Criteria.where(field).is(value.trim()));
        }
    }

    private String resolveCurrentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !StringUtils.hasText(authentication.getName())) {
            return null;
        }
        return accountRepository.findByUsernameAndDeletedFalse(authentication.getName())
                .map(Account::getId)
                .orElse(null);
    }

    private String resolveClientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
