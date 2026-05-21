package com.htto.backend.service;

import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.domain.Subject;
import com.htto.backend.dto.request.SubjectCreateRequest;
import com.htto.backend.dto.request.SubjectUpdateRequest;
import com.htto.backend.dto.response.SubjectResponse;
import com.htto.backend.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final MongoTemplate mongoTemplate;
    private final SystemLogService systemLogService;

    public SubjectService(
            SubjectRepository subjectRepository,
            MongoTemplate mongoTemplate,
            SystemLogService systemLogService
    ) {
        this.subjectRepository = subjectRepository;
        this.mongoTemplate = mongoTemplate;
        this.systemLogService = systemLogService;
    }

    public List<SubjectResponse> searchSubjects(String subjectCode, String subjectName, SubjectStatus status) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addRegexCriteria(criteria, "subjectCode", subjectCode);
        addRegexCriteria(criteria, "subjectName", subjectName);
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return mongoTemplate.find(query, Subject.class)
                .stream()
                .map(SubjectResponse::from)
                .toList();
    }

    public SubjectResponse createSubject(SubjectCreateRequest request) {
        ensureSubjectCodeAvailable(request.subjectCode(), null);

        Subject subject = new Subject();
        subject.setSubjectCode(request.subjectCode().trim());
        subject.setSubjectName(request.subjectName().trim());
        subject.setDescription(trimToNull(request.description()));
        subject.setStatus(request.status() == null ? SubjectStatus.ACTIVE : request.status());

        Subject saved = subjectRepository.save(subject);
        systemLogService.logCurrentUser("CREATE_SUBJECT", "SUBJECT", saved.getId(), "Created subject");
        return SubjectResponse.from(saved);
    }

    public SubjectResponse getSubject(String id) {
        return SubjectResponse.from(getSubjectOrThrow(id));
    }

    public SubjectResponse updateSubject(String id, SubjectUpdateRequest request) {
        Subject subject = getSubjectOrThrow(id);

        if (StringUtils.hasText(request.subjectCode()) && !request.subjectCode().equals(subject.getSubjectCode())) {
            ensureSubjectCodeAvailable(request.subjectCode(), id);
            subject.setSubjectCode(request.subjectCode().trim());
        }
        if (StringUtils.hasText(request.subjectName())) {
            subject.setSubjectName(request.subjectName().trim());
        }
        if (request.description() != null) {
            subject.setDescription(trimToNull(request.description()));
        }
        if (request.status() != null) {
            subject.setStatus(request.status());
        }

        Subject saved = subjectRepository.save(subject);
        systemLogService.logCurrentUser("UPDATE_SUBJECT", "SUBJECT", saved.getId(), "Updated subject");
        return SubjectResponse.from(saved);
    }

    public void deleteSubject(String id) {
        Subject subject = getSubjectOrThrow(id);
        subject.setStatus(SubjectStatus.INACTIVE);
        subjectRepository.save(subject);
        systemLogService.logCurrentUser("DELETE_SUBJECT", "SUBJECT", subject.getId(), "Set subject inactive");
    }

    private Subject getSubjectOrThrow(String id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
    }

    private void ensureSubjectCodeAvailable(String subjectCode, String currentSubjectId) {
        subjectRepository.findBySubjectCode(subjectCode.trim())
                .filter(subject -> currentSubjectId == null || !subject.getId().equals(currentSubjectId))
                .ifPresent(subject -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists");
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
