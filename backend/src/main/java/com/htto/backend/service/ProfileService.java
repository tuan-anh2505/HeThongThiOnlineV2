package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AdminProfile;
import com.htto.backend.domain.DomainEnums.AcademicStatus;
import com.htto.backend.domain.DomainEnums.ProfileStatus;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.request.AccountCreateRequest;
import com.htto.backend.dto.request.AdminProfileCreateRequest;
import com.htto.backend.dto.request.StudentProfileCreateRequest;
import com.htto.backend.dto.request.TeacherProfileCreateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.AdminProfileResponse;
import com.htto.backend.dto.response.ProfileMeResponse;
import com.htto.backend.dto.response.StudentProfileResponse;
import com.htto.backend.dto.response.TeacherProfileResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AdminProfileRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final MongoTemplate mongoTemplate;

    public ProfileService(
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            AdminProfileRepository adminProfileRepository,
            MongoTemplate mongoTemplate
    ) {
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void validateProfilePayload(AccountCreateRequest request) {
        validatePayloadMatchesRole(request);

        if (request.studentProfile() != null) {
            ensureStudentCodeAvailable(request.studentProfile().studentCode());
        }
        if (request.teacherProfile() != null) {
            ensureTeacherCodeAvailable(request.teacherProfile().teacherCode());
        }
        if (request.adminProfile() != null && StringUtils.hasText(request.adminProfile().adminCode())) {
            ensureAdminCodeAvailable(request.adminProfile().adminCode());
        }
    }

    public void createProfileForAccount(Account account, AccountCreateRequest request) {
        if (account.getRole() == Role.STUDENT && request.studentProfile() != null) {
            createStudentProfile(account, request.studentProfile());
            return;
        }
        if (account.getRole() == Role.TEACHER && request.teacherProfile() != null) {
            createTeacherProfile(account, request.teacherProfile());
            return;
        }
        if (account.getRole() == Role.ADMIN && request.adminProfile() != null) {
            createAdminProfile(account, request.adminProfile());
        }
    }

    public List<StudentProfileResponse> searchStudents(String studentCode, String fullName, String email) {
        Set<String> accountIds = findAccountIdsByNameOrEmail(fullName, email);
        if (accountIds != null && accountIds.isEmpty()) {
            return List.of();
        }

        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        addRegexCriteria(criteria, "studentCode", studentCode);
        if (accountIds != null) {
            criteria.add(Criteria.where("accountId").in(accountIds));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return toStudentResponses(mongoTemplate.find(query, StudentProfile.class));
    }

    public List<TeacherProfileResponse> searchTeachers(String teacherCode, String fullName, String email) {
        Set<String> accountIds = findAccountIdsByNameOrEmail(fullName, email);
        if (accountIds != null && accountIds.isEmpty()) {
            return List.of();
        }

        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        addRegexCriteria(criteria, "teacherCode", teacherCode);
        if (accountIds != null) {
            criteria.add(Criteria.where("accountId").in(accountIds));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return toTeacherResponses(mongoTemplate.find(query, TeacherProfile.class));
    }

    public StudentProfileResponse getStudentById(String id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        return StudentProfileResponse.from(profile, AccountResponse.from(getAccount(profile.getAccountId())));
    }

    public TeacherProfileResponse getTeacherById(String id) {
        TeacherProfile profile = teacherProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        return TeacherProfileResponse.from(profile, AccountResponse.from(getAccount(profile.getAccountId())));
    }

    public ProfileMeResponse getCurrentProfile(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        AccountResponse accountResponse = AccountResponse.from(account);

        return switch (account.getRole()) {
            case STUDENT -> {
                StudentProfile student = studentProfileRepository.findByAccountId(account.getId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found"
                        ));
                yield new ProfileMeResponse(
                        account.getRole(),
                        accountResponse,
                        StudentProfileResponse.from(student, accountResponse),
                        null,
                        null
                );
            }
            case TEACHER -> {
                TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Teacher profile not found"
                        ));
                yield new ProfileMeResponse(
                        account.getRole(),
                        accountResponse,
                        null,
                        TeacherProfileResponse.from(teacher, accountResponse),
                        null
                );
            }
            case ADMIN -> {
                AdminProfileResponse adminProfile = adminProfileRepository.findByAccountId(account.getId())
                        .map(profile -> AdminProfileResponse.from(profile, accountResponse))
                        .orElse(null);
                yield new ProfileMeResponse(account.getRole(), accountResponse, null, null, adminProfile);
            }
        };
    }

    private void createStudentProfile(Account account, StudentProfileCreateRequest request) {
        ensureNoStudentProfile(account.getId());

        StudentProfile profile = new StudentProfile();
        profile.setAccountId(account.getId());
        profile.setStudentCode(request.studentCode().trim());
        profile.setMainClassId(trimToNull(request.mainClassId()));
        profile.setStatus(request.status() == null ? AcademicStatus.ACTIVE : request.status());
        studentProfileRepository.save(profile);
    }

    private void createTeacherProfile(Account account, TeacherProfileCreateRequest request) {
        ensureNoTeacherProfile(account.getId());

        TeacherProfile profile = new TeacherProfile();
        profile.setAccountId(account.getId());
        profile.setTeacherCode(request.teacherCode().trim());
        profile.setStatus(request.status() == null ? ProfileStatus.ACTIVE : request.status());
        teacherProfileRepository.save(profile);
    }

    private void createAdminProfile(Account account, AdminProfileCreateRequest request) {
        ensureNoAdminProfile(account.getId());

        AdminProfile profile = new AdminProfile();
        profile.setAccountId(account.getId());
        profile.setAdminCode(resolveAdminCode(account, request));
        adminProfileRepository.save(profile);
    }

    private String resolveAdminCode(Account account, AdminProfileCreateRequest request) {
        if (request != null && StringUtils.hasText(request.adminCode())) {
            return request.adminCode().trim();
        }
        return "ADM-" + account.getId();
    }

    private void validatePayloadMatchesRole(AccountCreateRequest request) {
        boolean hasStudentProfile = request.studentProfile() != null;
        boolean hasTeacherProfile = request.teacherProfile() != null;
        boolean hasAdminProfile = request.adminProfile() != null;

        if (request.role() != Role.STUDENT && hasStudentProfile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentProfile is only valid for STUDENT");
        }
        if (request.role() != Role.TEACHER && hasTeacherProfile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teacherProfile is only valid for TEACHER");
        }
        if (request.role() != Role.ADMIN && hasAdminProfile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "adminProfile is only valid for ADMIN");
        }
    }

    private void ensureStudentCodeAvailable(String studentCode) {
        studentProfileRepository.findByStudentCode(studentCode.trim())
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Student code already exists");
                });
    }

    private void ensureTeacherCodeAvailable(String teacherCode) {
        teacherProfileRepository.findByTeacherCode(teacherCode.trim())
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher code already exists");
                });
    }

    private void ensureAdminCodeAvailable(String adminCode) {
        adminProfileRepository.findByAdminCode(adminCode.trim())
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin code already exists");
                });
    }

    private void ensureNoStudentProfile(String accountId) {
        studentProfileRepository.findByAccountId(accountId)
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Student profile already exists");
                });
    }

    private void ensureNoTeacherProfile(String accountId) {
        teacherProfileRepository.findByAccountId(accountId)
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher profile already exists");
                });
    }

    private void ensureNoAdminProfile(String accountId) {
        adminProfileRepository.findByAccountId(accountId)
                .ifPresent(profile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin profile already exists");
                });
    }

    private List<StudentProfileResponse> toStudentResponses(List<StudentProfile> profiles) {
        Map<String, Account> accounts = loadAccountMap(profiles.stream()
                .map(StudentProfile::getAccountId)
                .collect(Collectors.toSet()));

        return profiles.stream()
                .filter(profile -> accounts.containsKey(profile.getAccountId()))
                .map(profile -> StudentProfileResponse.from(
                        profile,
                        AccountResponse.from(accounts.get(profile.getAccountId()))
                ))
                .toList();
    }

    private List<TeacherProfileResponse> toTeacherResponses(List<TeacherProfile> profiles) {
        Map<String, Account> accounts = loadAccountMap(profiles.stream()
                .map(TeacherProfile::getAccountId)
                .collect(Collectors.toSet()));

        return profiles.stream()
                .filter(profile -> accounts.containsKey(profile.getAccountId()))
                .map(profile -> TeacherProfileResponse.from(
                        profile,
                        AccountResponse.from(accounts.get(profile.getAccountId()))
                ))
                .toList();
    }

    private Map<String, Account> loadAccountMap(Set<String> accountIds) {
        return accountRepository.findAllById(accountIds)
                .stream()
                .filter(account -> !account.isDeleted())
                .collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private Set<String> findAccountIdsByNameOrEmail(String fullName, String email) {
        if (!StringUtils.hasText(fullName) && !StringUtils.hasText(email)) {
            return null;
        }

        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("deleted").ne(true));
        addRegexCriteria(criteria, "fullName", fullName);
        addRegexCriteria(criteria, "email", email);
        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));

        return mongoTemplate.find(query, Account.class)
                .stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .filter(account -> !account.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
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
