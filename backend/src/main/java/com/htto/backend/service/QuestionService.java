package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.FillBlankAnswer;
import com.htto.backend.domain.embedded.MatchingPair;
import com.htto.backend.dto.request.AnswerDefinitionRequest;
import com.htto.backend.dto.request.AnswerOptionRequest;
import com.htto.backend.dto.request.FillBlankAnswerRequest;
import com.htto.backend.dto.request.MatchingPairRequest;
import com.htto.backend.dto.request.QuestionCreateRequest;
import com.htto.backend.dto.request.QuestionUpdateRequest;
import com.htto.backend.dto.response.QuestionResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.QuestionBankRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final AccountRepository accountRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final MongoTemplate mongoTemplate;
    private final SystemLogService systemLogService;

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            AccountRepository accountRepository,
            TeacherProfileRepository teacherProfileRepository,
            MongoTemplate mongoTemplate,
            SystemLogService systemLogService
    ) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.accountRepository = accountRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.mongoTemplate = mongoTemplate;
        this.systemLogService = systemLogService;
    }

    public List<QuestionResponse> searchQuestions(
            String questionBankId,
            QuestionType type,
            String content,
            Difficulty difficulty,
            String topic,
            QuestionStatus status,
            String username
    ) {
        QuestionBank questionBank = getQuestionBankOrThrow(questionBankId);
        ensureCanAccessBank(getCurrentAccount(username), questionBank);

        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("questionBankId").is(questionBankId));
        if (type != null) {
            criteria.add(Criteria.where("type").is(type));
        }
        addRegexCriteria(criteria, "content", content);
        if (difficulty != null) {
            criteria.add(Criteria.where("difficulty").is(difficulty));
        }
        addRegexCriteria(criteria, "topic", topic);
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));

        return mongoTemplate.find(query, Question.class)
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse createQuestion(String questionBankId, QuestionCreateRequest request, String username) {
        QuestionBank questionBank = getActiveQuestionBankOrThrow(questionBankId);
        ensureCanAccessBank(getCurrentAccount(username), questionBank);

        Question saved = questionRepository.save(buildQuestion(questionBank.getId(), request));
        systemLogService.logCurrentUser("CREATE_QUESTION", "QUESTION", saved.getId(), "Created question");
        return QuestionResponse.from(saved);
    }

    public void validateQuestionBankImportAccess(String questionBankId, String username) {
        QuestionBank questionBank = getActiveQuestionBankOrThrow(questionBankId);
        ensureCanAccessBank(getCurrentAccount(username), questionBank);
    }

    public void validateQuestionCreateRequest(QuestionCreateRequest request) {
        buildQuestion("validation", request);
    }

    public List<QuestionResponse> importQuestions(
            String questionBankId,
            List<QuestionCreateRequest> requests,
            String username
    ) {
        QuestionBank questionBank = getActiveQuestionBankOrThrow(questionBankId);
        ensureCanAccessBank(getCurrentAccount(username), questionBank);

        List<Question> questions = requests.stream()
                .map(request -> buildQuestion(questionBank.getId(), request))
                .toList();

        List<Question> savedQuestions = questionRepository.saveAll(questions);
        savedQuestions.forEach(question -> systemLogService.logCurrentUser(
                "CREATE_QUESTION",
                "QUESTION",
                question.getId(),
                "Imported question"
        ));
        return savedQuestions
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse getQuestion(String id, String username) {
        Question question = getQuestionOrThrow(id);
        QuestionBank questionBank = getQuestionBankOrThrow(question.getQuestionBankId());
        ensureCanAccessBank(getCurrentAccount(username), questionBank);
        return QuestionResponse.from(question);
    }

    public QuestionResponse updateQuestion(String id, QuestionUpdateRequest request, String username) {
        Question question = getQuestionOrThrow(id);
        QuestionBank questionBank = getQuestionBankOrThrow(question.getQuestionBankId());
        ensureCanAccessBank(getCurrentAccount(username), questionBank);

        QuestionType nextType = request.type() == null ? question.getType() : request.type();
        AnswerDefinition nextAnswer = request.answer() == null
                ? question.getAnswerDefinition()
                : buildAndValidateAnswer(nextType, request.answer());

        if (request.type() != null) {
            question.setType(request.type());
        }
        if (StringUtils.hasText(request.content())) {
            question.setContent(request.content().trim());
        }
        if (request.score() != null) {
            question.setScore(request.score());
        }
        if (request.difficulty() != null) {
            question.setDifficulty(request.difficulty());
        }
        if (request.topic() != null) {
            question.setTopic(trimToNull(request.topic()));
        }
        if (request.status() != null) {
            question.setStatus(request.status());
        }
        question.setAnswerDefinition(nextAnswer);

        validateQuestion(question);
        Question saved = questionRepository.save(question);
        systemLogService.logCurrentUser("UPDATE_QUESTION", "QUESTION", saved.getId(), "Updated question");
        return QuestionResponse.from(saved);
    }

    public QuestionResponse deactivateQuestion(String id, String username) {
        Question question = getQuestionOrThrow(id);
        QuestionBank questionBank = getQuestionBankOrThrow(question.getQuestionBankId());
        ensureCanAccessBank(getCurrentAccount(username), questionBank);
        question.setStatus(QuestionStatus.INACTIVE);
        Question saved = questionRepository.save(question);
        systemLogService.logCurrentUser("DEACTIVATE_QUESTION", "QUESTION", saved.getId(), "Deactivated question");
        return QuestionResponse.from(saved);
    }

    public QuestionResponse activateQuestion(String id, String username) {
        Question question = getQuestionOrThrow(id);
        QuestionBank questionBank = getActiveQuestionBankOrThrow(question.getQuestionBankId());
        ensureCanAccessBank(getCurrentAccount(username), questionBank);
        question.setStatus(QuestionStatus.ACTIVE);
        Question saved = questionRepository.save(question);
        systemLogService.logCurrentUser("ACTIVATE_QUESTION", "QUESTION", saved.getId(), "Activated question");
        return QuestionResponse.from(saved);
    }

    private Question buildQuestion(String questionBankId, QuestionCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question payload is required");
        }
        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question type is required");
        }
        if (!StringUtils.hasText(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question content is required");
        }
        if (request.score() == null || request.score().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question score must be greater than 0");
        }
        if (request.difficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question difficulty is required");
        }

        AnswerDefinition answerDefinition = buildAndValidateAnswer(request.type(), request.answer());

        Question question = new Question();
        question.setQuestionBankId(questionBankId);
        question.setType(request.type());
        question.setContent(request.content().trim());
        question.setScore(request.score());
        question.setDifficulty(request.difficulty());
        question.setTopic(trimToNull(request.topic()));
        question.setStatus(request.status() == null ? QuestionStatus.ACTIVE : request.status());
        question.setAnswerDefinition(answerDefinition);
        return question;
    }

    private AnswerDefinition buildAndValidateAnswer(QuestionType type, AnswerDefinitionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer definition is required");
        }

        return switch (type) {
            case TRUE_FALSE -> buildTrueFalseAnswer(request);
            case MULTIPLE_CHOICE -> buildMultipleChoiceAnswer(request);
            case FILL_BLANK -> buildFillBlankAnswer(request);
            case MATCHING -> buildMatchingAnswer(request);
        };
    }

    private AnswerDefinition buildTrueFalseAnswer(AnswerDefinitionRequest request) {
        if (request.correctAnswer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRUE_FALSE question requires correctAnswer");
        }

        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setTrueFalseAnswer(request.correctAnswer());
        return answerDefinition;
    }

    private AnswerDefinition buildMultipleChoiceAnswer(AnswerDefinitionRequest request) {
        List<AnswerOptionRequest> optionRequests = request.options();
        if (optionRequests == null || optionRequests.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MULTIPLE_CHOICE requires at least 2 options");
        }

        long correctCount = optionRequests.stream()
                .filter(option -> Boolean.TRUE.equals(option.isCorrect()))
                .count();
        if (correctCount != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MULTIPLE_CHOICE requires exactly 1 correct option"
            );
        }

        Set<String> optionIds = new HashSet<>();
        List<AnswerOption> options = new ArrayList<>();
        for (int i = 0; i < optionRequests.size(); i++) {
            AnswerOptionRequest optionRequest = optionRequests.get(i);
            if (!StringUtils.hasText(optionRequest.optionId()) || !StringUtils.hasText(optionRequest.content())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option id and content are required");
            }
            String optionId = optionRequest.optionId().trim();
            if (!optionIds.add(optionId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate optionId: " + optionId);
            }

            AnswerOption option = new AnswerOption();
            option.setOptionId(optionId);
            option.setContent(optionRequest.content().trim());
            option.setCorrect(Boolean.TRUE.equals(optionRequest.isCorrect()));
            option.setOrderIndex(optionRequest.orderIndex() == null ? i + 1 : optionRequest.orderIndex());
            options.add(option);
        }

        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setOptions(options);
        return answerDefinition;
    }

    private AnswerDefinition buildFillBlankAnswer(AnswerDefinitionRequest request) {
        FillBlankAnswerRequest fillBlankRequest = request.fillBlank();
        if (fillBlankRequest == null || fillBlankRequest.acceptedAnswers() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILL_BLANK requires acceptedAnswers");
        }

        List<String> acceptedAnswers = fillBlankRequest.acceptedAnswers()
                .stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (acceptedAnswers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILL_BLANK requires acceptedAnswers");
        }

        FillBlankAnswer fillBlankAnswer = new FillBlankAnswer();
        fillBlankAnswer.setAnswerId(trimToNull(fillBlankRequest.answerId()));
        fillBlankAnswer.setAcceptedAnswers(acceptedAnswers);
        fillBlankAnswer.setIgnoreCase(fillBlankRequest.ignoreCase() == null || fillBlankRequest.ignoreCase());
        fillBlankAnswer.setIgnoreAccent(fillBlankRequest.ignoreAccent() == null || fillBlankRequest.ignoreAccent());
        fillBlankAnswer.setTrimSpace(fillBlankRequest.trimSpace() == null || fillBlankRequest.trimSpace());

        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setFillBlankAnswer(fillBlankAnswer);
        return answerDefinition;
    }

    private AnswerDefinition buildMatchingAnswer(AnswerDefinitionRequest request) {
        List<MatchingPairRequest> pairRequests = request.matchingPairs();
        if (pairRequests == null || pairRequests.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATCHING requires at least 2 pairs");
        }

        Set<String> matchingIds = new HashSet<>();
        List<MatchingPair> matchingPairs = new ArrayList<>();
        for (int i = 0; i < pairRequests.size(); i++) {
            MatchingPairRequest pairRequest = pairRequests.get(i);
            if (!StringUtils.hasText(pairRequest.matchingId())
                    || !StringUtils.hasText(pairRequest.leftText())
                    || !StringUtils.hasText(pairRequest.rightText())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Matching id, leftText and rightText are required"
                );
            }

            String matchingId = pairRequest.matchingId().trim();
            if (!matchingIds.add(matchingId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate matchingId: " + matchingId);
            }

            MatchingPair pair = new MatchingPair();
            pair.setMatchingId(matchingId);
            pair.setLeftText(pairRequest.leftText().trim());
            pair.setRightText(pairRequest.rightText().trim());
            pair.setOrderIndex(pairRequest.orderIndex() == null ? i + 1 : pairRequest.orderIndex());
            matchingPairs.add(pair);
        }

        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setMatchingPairs(matchingPairs);
        return answerDefinition;
    }

    private void validateQuestion(Question question) {
        if (!StringUtils.hasText(question.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question content is required");
        }
        if (question.getScore() == null || question.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question score must be greater than 0");
        }
        if (question.getType() == null || question.getDifficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question type and difficulty are required");
        }
        buildAndValidateAnswer(question.getType(), toRequest(question.getAnswerDefinition()));
    }

    private AnswerDefinitionRequest toRequest(AnswerDefinition answerDefinition) {
        if (answerDefinition == null) {
            return null;
        }
        return new AnswerDefinitionRequest(
                answerDefinition.getTrueFalseAnswer(),
                answerDefinition.getOptions().stream()
                        .map(option -> new AnswerOptionRequest(
                                option.getOptionId(),
                                option.getContent(),
                                option.isCorrect(),
                                option.getOrderIndex()
                        ))
                        .toList(),
                answerDefinition.getFillBlankAnswer() == null
                        ? null
                        : new FillBlankAnswerRequest(
                                answerDefinition.getFillBlankAnswer().getAnswerId(),
                                answerDefinition.getFillBlankAnswer().getAcceptedAnswers(),
                                answerDefinition.getFillBlankAnswer().isIgnoreCase(),
                                answerDefinition.getFillBlankAnswer().isIgnoreAccent(),
                                answerDefinition.getFillBlankAnswer().isTrimSpace()
                        ),
                answerDefinition.getMatchingPairs().stream()
                        .map(pair -> new MatchingPairRequest(
                                pair.getMatchingId(),
                                pair.getLeftText(),
                                pair.getRightText(),
                                pair.getOrderIndex()
                        ))
                        .toList()
        );
    }

    private void ensureCanAccessBank(Account account, QuestionBank questionBank) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access question answers");
        }

        TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        if (!teacher.getId().equals(questionBank.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only manage own question bank");
        }
    }

    private QuestionBank getActiveQuestionBankOrThrow(String questionBankId) {
        QuestionBank questionBank = getQuestionBankOrThrow(questionBankId);
        if (questionBank.getStatus() != QuestionBankStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question bank is inactive");
        }
        return questionBank;
    }

    private QuestionBank getQuestionBankOrThrow(String questionBankId) {
        return questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question bank not found"));
    }

    private Question getQuestionOrThrow(String id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
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
