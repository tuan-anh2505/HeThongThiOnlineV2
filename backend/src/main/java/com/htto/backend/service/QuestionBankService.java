package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.request.QuestionBankCreateRequest;
import com.htto.backend.dto.request.QuestionBankUpdateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.QuestionBankResponse;
import com.htto.backend.dto.response.SubjectResponse;
import com.htto.backend.dto.response.TeacherProfileResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.QuestionBankRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AccountRepository accountRepository;
    private final ClassSubjectTeacherRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final MongoTemplate mongoTemplate;
    private final SystemLogService systemLogService;

    public QuestionBankService(
            QuestionBankRepository questionBankRepository,
            SubjectRepository subjectRepository,
            TeacherProfileRepository teacherProfileRepository,
            AccountRepository accountRepository,
            ClassSubjectTeacherRepository assignmentRepository,
            ExamRepository examRepository,
            MongoTemplate mongoTemplate,
            SystemLogService systemLogService
    ) {
        this.questionBankRepository = questionBankRepository;
        this.subjectRepository = subjectRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.accountRepository = accountRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.mongoTemplate = mongoTemplate;
        this.systemLogService = systemLogService;
    }

    public List<QuestionBankResponse> searchQuestionBanks(
            String name,
            String subjectId,
            String teacherId,
            QuestionBankStatus status,
            String username
    ) {
        Account account = getCurrentAccount(username);
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addRegexCriteria(criteria, "name", name);
        addExactCriteria(criteria, "subjectId", subjectId);
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }

        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            criteria.add(Criteria.where("teacherId").is(teacher.getId()));
        } else if (account.getRole() == Role.ADMIN) {
            addExactCriteria(criteria, "teacherId", teacherId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access question banks");
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return mongoTemplate.find(query, QuestionBank.class)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuestionBankResponse> getTeacherQuestionBanks(String username) {
        Account account = getCurrentAccount(username);
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role is required");
        }
        TeacherProfile teacher = getTeacherProfile(account);
        return questionBankRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuestionBankResponse> getAdminQuestionBanks() {
        return questionBankRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionBankResponse createQuestionBank(QuestionBankCreateRequest request, String username) {
        Account account = getCurrentAccount(username);
        Subject subject = getActiveSubject(request.subjectId());
        TeacherProfile teacher = resolveTeacherForCreate(account, request.teacherId(), subject.getId());

        if (account.getRole() == Role.TEACHER) {
            ensureTeacherAssignedToSubject(teacher.getId(), subject.getId());
        }

        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(request.name().trim());
        questionBank.setDescription(trimToNull(request.description()));
        questionBank.setSubjectId(subject.getId());
        questionBank.setTeacherId(teacher.getId());
        questionBank.setStatus(request.status() == null ? QuestionBankStatus.ACTIVE : request.status());

        QuestionBank saved = questionBankRepository.save(questionBank);
        systemLogService.logCurrentUser("CREATE_QUESTION_BANK", "QUESTION_BANK", saved.getId(), "Created question bank");
        return toResponse(saved);
    }

    public QuestionBankResponse getQuestionBank(String id, String username) {
        Account account = getCurrentAccount(username);
        QuestionBank questionBank = getQuestionBankOrThrow(id);
        ensureCanAccess(account, questionBank);
        return toResponse(questionBank);
    }

    public QuestionBankResponse updateQuestionBank(String id, QuestionBankUpdateRequest request, String username) {
        Account account = getCurrentAccount(username);
        QuestionBank questionBank = getQuestionBankOrThrow(id);
        ensureCanAccess(account, questionBank);

        if (StringUtils.hasText(request.name())) {
            questionBank.setName(request.name().trim());
        }
        if (request.description() != null) {
            questionBank.setDescription(trimToNull(request.description()));
        }

        String nextSubjectId = StringUtils.hasText(request.subjectId())
                ? request.subjectId().trim()
                : questionBank.getSubjectId();
        String nextTeacherId = resolveTeacherIdForUpdate(account, request.teacherId(), questionBank.getTeacherId());

        if (!nextSubjectId.equals(questionBank.getSubjectId())) {
            Subject subject = getActiveSubject(nextSubjectId);
            questionBank.setSubjectId(subject.getId());
        }
        if (!nextTeacherId.equals(questionBank.getTeacherId())) {
            validateTeacher(nextTeacherId);
            questionBank.setTeacherId(nextTeacherId);
        }
        if (account.getRole() == Role.TEACHER) {
            ensureTeacherAssignedToSubject(questionBank.getTeacherId(), questionBank.getSubjectId());
        }
        if (request.status() != null) {
            questionBank.setStatus(request.status());
        }

        QuestionBank saved = questionBankRepository.save(questionBank);
        systemLogService.logCurrentUser("UPDATE_QUESTION_BANK", "QUESTION_BANK", saved.getId(), "Updated question bank");
        return toResponse(saved);
    }

    public QuestionBankResponse deactivateQuestionBank(String id, String username) {
        Account account = getCurrentAccount(username);
        QuestionBank questionBank = getQuestionBankOrThrow(id);
        ensureCanAccess(account, questionBank);

        questionBank.setStatus(QuestionBankStatus.INACTIVE);
        QuestionBank saved = questionBankRepository.save(questionBank);
        systemLogService.logCurrentUser(
                "DEACTIVATE_QUESTION_BANK",
                "QUESTION_BANK",
                saved.getId(),
                examRepository.findByQuestionBankId(saved.getId()).isEmpty()
                        ? "Deactivated question bank"
                        : "Deactivated question bank used by exams"
        );
        return toResponse(saved);
    }

    public QuestionBankResponse activateQuestionBank(String id, String username) {
        Account account = getCurrentAccount(username);
        QuestionBank questionBank = getQuestionBankOrThrow(id);
        ensureCanAccess(account, questionBank);
        Subject subject = getActiveSubject(questionBank.getSubjectId());
        if (account.getRole() == Role.TEACHER) {
            ensureTeacherAssignedToSubject(questionBank.getTeacherId(), subject.getId());
        }

        questionBank.setStatus(QuestionBankStatus.ACTIVE);
        QuestionBank saved = questionBankRepository.save(questionBank);
        systemLogService.logCurrentUser("ACTIVATE_QUESTION_BANK", "QUESTION_BANK", saved.getId(), "Activated question bank");
        return toResponse(saved);
    }

    private TeacherProfile resolveTeacherForCreate(Account account, String requestedTeacherId, String subjectId) {
        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            if (StringUtils.hasText(requestedTeacherId) && !teacher.getId().equals(requestedTeacherId.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only create own question bank");
            }
            return teacher;
        }

        if (account.getRole() == Role.ADMIN) {
            if (!StringUtils.hasText(requestedTeacherId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teacherId is required for admin");
            }
            TeacherProfile teacher = validateTeacher(requestedTeacherId.trim());
            if (!isTeacherAssignedToSubject(teacher.getId(), subjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher is not assigned to this subject");
            }
            return teacher;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage question banks");
    }

    private String resolveTeacherIdForUpdate(Account account, String requestedTeacherId, String currentTeacherId) {
        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            if (StringUtils.hasText(requestedTeacherId) && !teacher.getId().equals(requestedTeacherId.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher cannot reassign question bank");
            }
            return teacher.getId();
        }

        if (account.getRole() == Role.ADMIN && StringUtils.hasText(requestedTeacherId)) {
            return requestedTeacherId.trim();
        }
        return currentTeacherId;
    }

    private void ensureCanAccess(Account account, QuestionBank questionBank) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access question banks");
        }

        TeacherProfile teacher = getTeacherProfile(account);
        if (!teacher.getId().equals(questionBank.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only manage own question bank");
        }
    }

    private Subject getActiveSubject(String subjectId) {
        Subject subject = subjectRepository.findById(subjectId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is inactive");
        }
        return subject;
    }

    private TeacherProfile validateTeacher(String teacherId) {
        return teacherProfileRepository.findById(teacherId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private void ensureTeacherAssignedToSubject(String teacherId, String subjectId) {
        if (!isTeacherAssignedToSubject(teacherId, subjectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher is not assigned to this subject");
        }
    }

    private boolean isTeacherAssignedToSubject(String teacherId, String subjectId) {
        return assignmentRepository.findByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE)
                .stream()
                .map(ClassSubjectTeacher::getSubjectId)
                .anyMatch(subjectId::equals);
    }

    private QuestionBank getQuestionBankOrThrow(String id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question bank not found"));
    }

    private TeacherProfile getTeacherProfile(Account account) {
        return teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private QuestionBankResponse toResponse(QuestionBank questionBank) {
        SubjectResponse subject = subjectRepository.findById(questionBank.getSubjectId())
                .map(SubjectResponse::from)
                .orElse(null);
        TeacherProfileResponse teacher = teacherProfileRepository.findById(questionBank.getTeacherId())
                .map(profile -> TeacherProfileResponse.from(profile, getTeacherAccount(profile)))
                .orElse(null);
        return QuestionBankResponse.from(questionBank, subject, teacher);
    }

    private AccountResponse getTeacherAccount(TeacherProfile profile) {
        return accountRepository.findById(profile.getAccountId())
                .filter(account -> !account.isDeleted())
                .map(AccountResponse::from)
                .orElse(null);
    }

    private void addExactCriteria(List<Criteria> criteria, String field, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        criteria.add(Criteria.where(field).is(value.trim()));
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
