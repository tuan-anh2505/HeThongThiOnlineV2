package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.NotificationStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.Notification;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.dto.response.NotificationReadAllResponse;
import com.htto.backend.dto.response.NotificationResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.NotificationRepository;
import com.htto.backend.repository.StudentProfileRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
    }

    public List<NotificationResponse> getMyNotifications(String username, Boolean isRead) {
        Account account = getCurrentAccount(username);
        List<Notification> notifications;
        if (isRead == null) {
            notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(account.getId());
        } else {
            notifications = notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                    account.getId(),
                    Boolean.TRUE.equals(isRead) ? NotificationStatus.READ : NotificationStatus.UNREAD
            );
        }
        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse markRead(String notificationId, String username) {
        Account account = getCurrentAccount(username);
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setStatus(NotificationStatus.READ);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public NotificationReadAllResponse markAllRead(String username) {
        Account account = getCurrentAccount(username);
        List<Notification> unreadNotifications = notificationRepository.findByRecipientUserIdAndStatus(
                account.getId(),
                NotificationStatus.UNREAD
        );
        unreadNotifications.forEach(notification -> notification.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unreadNotifications);
        return new NotificationReadAllResponse(unreadNotifications.size());
    }

    public void notifyExamPublished(Exam exam) {
        if (exam == null) {
            return;
        }
        List<Account> receivers = getAssignedStudentAccounts(exam.getClassIds());
        createForAccounts(
                receivers,
                "Bài thi mới được công bố",
                "Bài thi \"" + exam.getTitle() + "\" đã được công bố cho lớp của bạn.",
                "EXAM",
                exam.getId()
        );
    }

    public void notifyExamResultsPublished(Exam exam) {
        if (exam == null) {
            return;
        }
        List<Account> receivers = getAssignedStudentAccounts(exam.getClassIds());
        createForAccounts(
                receivers,
                "Kết quả bài thi đã được công bố",
                "Kết quả bài thi \"" + exam.getTitle() + "\" đã được công bố.",
                "EXAM",
                exam.getId()
        );
    }

    public void notifyAccountLocked(Account account) {
        if (account == null) {
            return;
        }
        createForAccount(
                account,
                "Tài khoản đã bị khóa",
                "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên nếu cần hỗ trợ.",
                "ACCOUNT",
                account.getId()
        );
    }

    public void notifyAccountUnlocked(Account account) {
        if (account == null) {
            return;
        }
        createForAccount(
                account,
                "Tài khoản đã được mở khóa",
                "Tài khoản của bạn đã được mở khóa.",
                "ACCOUNT",
                account.getId()
        );
    }

    private void createForAccounts(
            Collection<Account> accounts,
            String title,
            String content,
            String targetType,
            String targetId
    ) {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(accounts.stream()
                .filter(Objects::nonNull)
                .filter(account -> !account.isDeleted())
                .map(account -> buildNotification(account, title, content, targetType, targetId))
                .toList());
    }

    private void createForAccount(Account account, String title, String content, String targetType, String targetId) {
        if (account == null || account.isDeleted()) {
            return;
        }
        notificationRepository.save(buildNotification(account, title, content, targetType, targetId));
    }

    private Notification buildNotification(
            Account account,
            String title,
            String content,
            String targetType,
            String targetId
    ) {
        Notification notification = new Notification();
        notification.setTitle(sanitize(title));
        notification.setContent(sanitize(content));
        notification.setReceiverId(account.getId());
        notification.setReceiverRole(account.getRole());
        notification.setRecipientGroupId(targetType + ":" + targetId);
        notification.setCreatedAtForUser(Instant.now());
        notification.setStatus(NotificationStatus.UNREAD);
        return notification;
    }

    private List<Account> getAssignedStudentAccounts(List<String> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        Set<String> studentIds = classIds.stream()
                .filter(StringUtils::hasText)
                .flatMap(classId -> classStudentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE)
                        .stream())
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toSet());
        if (studentIds.isEmpty()) {
            return List.of();
        }

        Map<String, StudentProfile> studentsByAccountId = studentProfileRepository.findAllById(studentIds)
                .stream()
                .filter(student -> StringUtils.hasText(student.getAccountId()))
                .collect(Collectors.toMap(
                        StudentProfile::getAccountId,
                        student -> student,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (studentsByAccountId.isEmpty()) {
            return List.of();
        }

        return accountRepository.findAllById(studentsByAccountId.keySet())
                .stream()
                .filter(account -> account.getRole() == Role.STUDENT)
                .filter(account -> !account.isDeleted())
                .toList();
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
    }
}
