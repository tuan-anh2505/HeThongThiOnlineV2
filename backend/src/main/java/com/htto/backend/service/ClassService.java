package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.request.AddStudentToClassRequest;
import com.htto.backend.dto.request.ClassCreateRequest;
import com.htto.backend.dto.request.ClassUpdateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.ClassResponse;
import com.htto.backend.dto.response.ClassStudentResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AccountRepository accountRepository;
    private final MongoTemplate mongoTemplate;

    public ClassService(
            SchoolClassRepository schoolClassRepository,
            ClassStudentRepository classStudentRepository,
            StudentProfileRepository studentProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            AccountRepository accountRepository,
            MongoTemplate mongoTemplate
    ) {
        this.schoolClassRepository = schoolClassRepository;
        this.classStudentRepository = classStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.accountRepository = accountRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<ClassResponse> searchClasses(
            String classCode,
            String className,
            String teacherId,
            ClassStatus status
    ) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addRegexCriteria(criteria, "classCode", classCode);
        addRegexCriteria(criteria, "className", className);
        if (StringUtils.hasText(teacherId)) {
            criteria.add(Criteria.where("teacherId").is(teacherId.trim()));
        }
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return mongoTemplate.find(query, SchoolClass.class)
                .stream()
                .map(ClassResponse::from)
                .toList();
    }

    public ClassResponse createClass(ClassCreateRequest request) {
        ensureClassCodeAvailable(request.classCode(), null);
        validateTeacherIfPresent(request.teacherId());

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassCode(request.classCode().trim());
        schoolClass.setClassName(request.className().trim());
        schoolClass.setTeacherId(trimToNull(request.teacherId()));
        schoolClass.setStatus(request.status() == null ? ClassStatus.ACTIVE : request.status());
        schoolClass.setStudentCount(0);

        return ClassResponse.from(schoolClassRepository.save(schoolClass));
    }

    public ClassResponse getClass(String id) {
        return ClassResponse.from(getClassOrThrow(id));
    }

    public ClassResponse updateClass(String id, ClassUpdateRequest request) {
        SchoolClass schoolClass = getClassOrThrow(id);

        if (StringUtils.hasText(request.classCode()) && !request.classCode().equals(schoolClass.getClassCode())) {
            ensureClassCodeAvailable(request.classCode(), id);
            schoolClass.setClassCode(request.classCode().trim());
        }
        if (StringUtils.hasText(request.className())) {
            schoolClass.setClassName(request.className().trim());
        }
        if (request.teacherId() != null) {
            validateTeacherIfPresent(request.teacherId());
            schoolClass.setTeacherId(trimToNull(request.teacherId()));
        }
        if (request.status() != null) {
            schoolClass.setStatus(request.status());
        }

        return ClassResponse.from(schoolClassRepository.save(schoolClass));
    }

    public void deleteClass(String id) {
        SchoolClass schoolClass = getClassOrThrow(id);
        schoolClass.setStatus(ClassStatus.INACTIVE);
        schoolClassRepository.save(schoolClass);
    }

    public List<ClassStudentResponse> getStudents(String classId, String username) {
        SchoolClass schoolClass = getClassOrThrow(classId);
        Account account = getCurrentAccount(username);
        ensureCanManageClass(account, schoolClass);

        return toClassStudentResponses(classStudentRepository.findByClassIdAndStatus(
                classId,
                EnrollmentStatus.ACTIVE
        ));
    }

    public ClassStudentResponse addStudent(
            String classId,
            AddStudentToClassRequest request,
            String username
    ) {
        SchoolClass schoolClass = getActiveClassOrThrow(classId);
        Account account = getCurrentAccount(username);
        ensureCanManageClass(account, schoolClass);

        StudentProfile student = studentProfileRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));

        classStudentRepository.findByClassIdAndStudentIdAndStatus(
                        classId,
                        student.getId(),
                        EnrollmentStatus.ACTIVE
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already exists in class");
                });

        ClassStudent classStudent = classStudentRepository.findByClassIdAndStudentId(classId, student.getId())
                .orElse(null);
        boolean wasActive = classStudent != null && classStudent.getStatus() == EnrollmentStatus.ACTIVE;
        if (classStudent == null) {
            classStudent = new ClassStudent();
        }
        classStudent.setClassId(classId);
        classStudent.setStudentId(student.getId());
        classStudent.setJoinedAt(Instant.now());
        classStudent.setStatus(EnrollmentStatus.ACTIVE);
        ClassStudent saved = classStudentRepository.save(classStudent);

        if (!student.getClassIds().contains(classId)) {
            student.getClassIds().add(classId);
        }
        studentProfileRepository.save(student);

        if (!wasActive) {
            updateStudentCount(schoolClass);
        }

        return toClassStudentResponse(saved);
    }

    public void removeStudent(String classId, String studentId, String username) {
        SchoolClass schoolClass = getClassOrThrow(classId);
        Account account = getCurrentAccount(username);
        ensureCanManageClass(account, schoolClass);

        ClassStudent classStudent = classStudentRepository.findByClassIdAndStudentIdAndStatus(
                        classId,
                        studentId,
                        EnrollmentStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active class student not found"));

        classStudent.setStatus(EnrollmentStatus.REMOVED);
        classStudentRepository.save(classStudent);

        studentProfileRepository.findById(studentId).ifPresent(student -> {
            student.getClassIds().remove(classId);
            if (classId.equals(student.getMainClassId())) {
                student.setMainClassId(null);
            }
            studentProfileRepository.save(student);
        });

        updateStudentCount(schoolClass);
    }

    public List<ClassResponse> getTeacherClasses(String username) {
        Account account = getCurrentAccount(username);
        if (account.getRole() == Role.ADMIN) {
            return searchClasses(null, null, null, ClassStatus.ACTIVE);
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role is required");
        }

        TeacherProfile teacher = getTeacherProfile(account);
        Map<String, SchoolClass> classes = new LinkedHashMap<>();
        schoolClassRepository.findByTeacherIdAndStatus(teacher.getId(), ClassStatus.ACTIVE)
                .forEach(schoolClass -> classes.put(schoolClass.getId(), schoolClass));
        schoolClassRepository.findAllById(teacher.getClassIds())
                .stream()
                .filter(schoolClass -> schoolClass.getStatus() == ClassStatus.ACTIVE)
                .forEach(schoolClass -> classes.put(schoolClass.getId(), schoolClass));

        return classes.values().stream()
                .map(ClassResponse::from)
                .toList();
    }

    public List<ClassResponse> getStudentClasses(String username) {
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

        return schoolClassRepository.findAllById(classIds)
                .stream()
                .filter(schoolClass -> schoolClass.getStatus() == ClassStatus.ACTIVE)
                .map(ClassResponse::from)
                .toList();
    }

    private void updateStudentCount(SchoolClass schoolClass) {
        int count = classStudentRepository.findByClassIdAndStatus(
                schoolClass.getId(),
                EnrollmentStatus.ACTIVE
        ).size();
        schoolClass.setStudentCount(count);
        schoolClassRepository.save(schoolClass);
    }

    private List<ClassStudentResponse> toClassStudentResponses(List<ClassStudent> classStudents) {
        return classStudents.stream()
                .map(this::toClassStudentResponse)
                .toList();
    }

    private ClassStudentResponse toClassStudentResponse(ClassStudent classStudent) {
        StudentProfile student = studentProfileRepository.findById(classStudent.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        Account account = accountRepository.findById(student.getAccountId())
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        return ClassStudentResponse.from(classStudent, student.getStudentCode(), AccountResponse.from(account));
    }

    private void ensureCanManageClass(Account account, SchoolClass schoolClass) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin or assigned teacher can manage class");
        }

        TeacherProfile teacher = getTeacherProfile(account);
        boolean assignedByClass = Objects.equals(schoolClass.getTeacherId(), teacher.getId());
        boolean assignedByProfile = teacher.getClassIds().contains(schoolClass.getId());
        if (!assignedByClass && !assignedByProfile) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher is not assigned to this class");
        }
    }

    private TeacherProfile getTeacherProfile(Account account) {
        return teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private SchoolClass getClassOrThrow(String id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    private SchoolClass getActiveClassOrThrow(String id) {
        SchoolClass schoolClass = getClassOrThrow(id);
        if (schoolClass.getStatus() != ClassStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class is inactive");
        }
        return schoolClass;
    }

    private void ensureClassCodeAvailable(String classCode, String currentClassId) {
        schoolClassRepository.findByClassCode(classCode.trim())
                .filter(schoolClass -> currentClassId == null || !schoolClass.getId().equals(currentClassId))
                .ifPresent(schoolClass -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Class code already exists");
                });
    }

    private void validateTeacherIfPresent(String teacherId) {
        if (!StringUtils.hasText(teacherId)) {
            return;
        }
        teacherProfileRepository.findById(teacherId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
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
