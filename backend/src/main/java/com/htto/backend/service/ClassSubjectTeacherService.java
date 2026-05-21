package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.request.ClassSubjectTeacherCreateRequest;
import com.htto.backend.dto.request.ClassSubjectTeacherUpdateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.ClassResponse;
import com.htto.backend.dto.response.ClassSubjectTeacherResponse;
import com.htto.backend.dto.response.SubjectResponse;
import com.htto.backend.dto.response.TeacherProfileResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class ClassSubjectTeacherService {

    private final ClassSubjectTeacherRepository assignmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;
    private final AccountRepository accountRepository;
    private final MongoTemplate mongoTemplate;
    private final SystemLogService systemLogService;

    public ClassSubjectTeacherService(
            ClassSubjectTeacherRepository assignmentRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherProfileRepository teacherProfileRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository,
            AccountRepository accountRepository,
            MongoTemplate mongoTemplate,
            SystemLogService systemLogService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
        this.accountRepository = accountRepository;
        this.mongoTemplate = mongoTemplate;
        this.systemLogService = systemLogService;
    }

    public List<ClassSubjectTeacherResponse> searchAssignments(
            String classId,
            String subjectId,
            String teacherId,
            String semester,
            String schoolYear,
            AssignmentStatus status
    ) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addExactCriteria(criteria, "classId", classId);
        addExactCriteria(criteria, "subjectId", subjectId);
        addExactCriteria(criteria, "teacherId", teacherId);
        addRegexCriteria(criteria, "semester", semester);
        addRegexCriteria(criteria, "schoolYear", schoolYear);
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return toResponses(mongoTemplate.find(query, ClassSubjectTeacher.class));
    }

    public ClassSubjectTeacherResponse createAssignment(ClassSubjectTeacherCreateRequest request) {
        AssignmentStatus status = request.status() == null ? AssignmentStatus.ACTIVE : request.status();
        validateAssignmentTargets(request.classId(), request.subjectId(), request.teacherId());
        if (status == AssignmentStatus.ACTIVE) {
            ensureNoActiveDuplicate(request.classId(), request.subjectId(), request.teacherId(), null);
        }

        ClassSubjectTeacher assignment = new ClassSubjectTeacher();
        assignment.setClassId(request.classId().trim());
        assignment.setSubjectId(request.subjectId().trim());
        assignment.setTeacherId(request.teacherId().trim());
        assignment.setSemester(trimToNull(request.semester()));
        assignment.setSchoolYear(trimToNull(request.schoolYear()));
        assignment.setStatus(status);

        ClassSubjectTeacher saved = assignmentRepository.save(assignment);
        syncTeacherScope(saved.getTeacherId());
        systemLogService.logCurrentUser("CREATE_CLASS_SUBJECT_TEACHER", "CLASS_SUBJECT_TEACHER", saved.getId(), "Created class-subject-teacher assignment");
        return toResponse(saved);
    }

    public ClassSubjectTeacherResponse updateAssignment(String id, ClassSubjectTeacherUpdateRequest request) {
        ClassSubjectTeacher assignment = getAssignmentOrThrow(id);

        String nextClassId = StringUtils.hasText(request.classId()) ? request.classId().trim() : assignment.getClassId();
        String nextSubjectId = StringUtils.hasText(request.subjectId()) ? request.subjectId().trim() : assignment.getSubjectId();
        String nextTeacherId = StringUtils.hasText(request.teacherId()) ? request.teacherId().trim() : assignment.getTeacherId();
        AssignmentStatus nextStatus = request.status() == null ? assignment.getStatus() : request.status();
        String previousTeacherId = assignment.getTeacherId();

        validateAssignmentTargets(nextClassId, nextSubjectId, nextTeacherId);
        if (nextStatus == AssignmentStatus.ACTIVE) {
            ensureNoActiveDuplicate(nextClassId, nextSubjectId, nextTeacherId, id);
        }

        assignment.setClassId(nextClassId);
        assignment.setSubjectId(nextSubjectId);
        assignment.setTeacherId(nextTeacherId);
        if (request.semester() != null) {
            assignment.setSemester(trimToNull(request.semester()));
        }
        if (request.schoolYear() != null) {
            assignment.setSchoolYear(trimToNull(request.schoolYear()));
        }
        assignment.setStatus(nextStatus);

        ClassSubjectTeacher saved = assignmentRepository.save(assignment);
        syncTeacherScope(previousTeacherId);
        syncTeacherScope(saved.getTeacherId());
        systemLogService.logCurrentUser("UPDATE_CLASS_SUBJECT_TEACHER", "CLASS_SUBJECT_TEACHER", saved.getId(), "Updated class-subject-teacher assignment");
        return toResponse(saved);
    }

    public void deleteAssignment(String id) {
        ClassSubjectTeacher assignment = getAssignmentOrThrow(id);
        assignment.setStatus(AssignmentStatus.INACTIVE);
        assignmentRepository.save(assignment);
        syncTeacherScope(assignment.getTeacherId());
        systemLogService.logCurrentUser("DELETE_CLASS_SUBJECT_TEACHER", "CLASS_SUBJECT_TEACHER", assignment.getId(), "Set class-subject-teacher assignment inactive");
    }

    public List<ClassSubjectTeacherResponse> getTeacherSubjects(String username) {
        Account account = getCurrentAccount(username);
        if (account.getRole() == Role.ADMIN) {
            return searchAssignments(null, null, null, null, null, AssignmentStatus.ACTIVE);
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role is required");
        }

        TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        return toResponses(assignmentRepository.findByTeacherIdAndStatus(teacher.getId(), AssignmentStatus.ACTIVE));
    }

    public List<ClassSubjectTeacherResponse> getStudentSubjects(String username) {
        Account account = getCurrentAccount(username);
        if (account.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role is required");
        }

        StudentProfile student = studentProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        List<String> classIds = classStudentRepository.findByStudentIdAndStatus(
                        student.getId(),
                        EnrollmentStatus.ACTIVE
                )
                .stream()
                .map(ClassStudent::getClassId)
                .toList();

        if (classIds.isEmpty()) {
            return List.of();
        }

        return toResponses(assignmentRepository.findByClassIdInAndStatus(classIds, AssignmentStatus.ACTIVE));
    }

    private void validateAssignmentTargets(String classId, String subjectId, String teacherId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        if (schoolClass.getStatus() != ClassStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class is inactive");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is inactive");
        }

        teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private void ensureNoActiveDuplicate(String classId, String subjectId, String teacherId, String currentId) {
        assignmentRepository.findByClassIdAndSubjectIdAndTeacherIdAndStatus(
                        classId,
                        subjectId,
                        teacherId,
                        AssignmentStatus.ACTIVE
                )
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Active class-subject-teacher assignment already exists"
                    );
                });
    }

    private void syncTeacherScope(String teacherId) {
        if (!StringUtils.hasText(teacherId)) {
            return;
        }

        teacherProfileRepository.findById(teacherId).ifPresent(teacher -> {
            List<ClassSubjectTeacher> activeAssignments = assignmentRepository.findByTeacherIdAndStatus(
                    teacherId,
                    AssignmentStatus.ACTIVE
            );

            Set<String> classIds = activeAssignments.stream()
                    .map(ClassSubjectTeacher::getClassId)
                    .collect(Collectors.toSet());
            schoolClassRepository.findByTeacherIdAndStatus(teacherId, ClassStatus.ACTIVE)
                    .forEach(schoolClass -> classIds.add(schoolClass.getId()));

            Set<String> subjectIds = activeAssignments.stream()
                    .map(ClassSubjectTeacher::getSubjectId)
                    .collect(Collectors.toSet());

            teacher.setClassIds(new ArrayList<>(classIds));
            teacher.setSubjectIds(new ArrayList<>(subjectIds));
            teacherProfileRepository.save(teacher);
        });
    }

    private List<ClassSubjectTeacherResponse> toResponses(List<ClassSubjectTeacher> assignments) {
        return assignments.stream()
                .map(this::toResponse)
                .toList();
    }

    private ClassSubjectTeacherResponse toResponse(ClassSubjectTeacher assignment) {
        SchoolClass schoolClass = schoolClassRepository.findById(assignment.getClassId()).orElse(null);
        Subject subject = subjectRepository.findById(assignment.getSubjectId()).orElse(null);
        TeacherProfile teacher = teacherProfileRepository.findById(assignment.getTeacherId()).orElse(null);

        return ClassSubjectTeacherResponse.from(
                assignment,
                schoolClass == null ? null : ClassResponse.from(schoolClass),
                subject == null ? null : SubjectResponse.from(subject),
                teacher == null ? null : TeacherProfileResponse.from(teacher, getTeacherAccountResponse(teacher))
        );
    }

    private AccountResponse getTeacherAccountResponse(TeacherProfile teacher) {
        return accountRepository.findById(teacher.getAccountId())
                .filter(account -> !account.isDeleted())
                .map(AccountResponse::from)
                .orElse(null);
    }

    private ClassSubjectTeacher getAssignmentOrThrow(String id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
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
