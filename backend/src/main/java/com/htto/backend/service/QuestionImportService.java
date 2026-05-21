package com.htto.backend.service;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.dto.request.AnswerDefinitionRequest;
import com.htto.backend.dto.request.AnswerOptionRequest;
import com.htto.backend.dto.request.FillBlankAnswerRequest;
import com.htto.backend.dto.request.MatchingPairRequest;
import com.htto.backend.dto.request.QuestionCreateRequest;
import com.htto.backend.dto.response.QuestionImportErrorResponse;
import com.htto.backend.dto.response.QuestionImportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionImportService {

    private static final String QUESTION_MARKER = "[QUESTION]";

    private final QuestionService questionService;

    public QuestionImportService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public QuestionImportResponse importTxt(String questionBankId, MultipartFile file, String username) {
        validateImportAccess(questionBankId, username);
        validateFile(file);

        return importParsedText(questionBankId, readFile(file), username);
    }

    public QuestionImportResponse importTextContent(String questionBankId, String content, String username) {
        validateImportAccess(questionBankId, username);
        return importParsedText(questionBankId, content, username);
    }

    public void validateImportAccess(String questionBankId, String username) {
        questionService.validateQuestionBankImportAccess(questionBankId, username);
    }

    private QuestionImportResponse importParsedText(String questionBankId, String content, String username) {
        ParseResult parseResult = parse(content);
        List<QuestionImportErrorResponse> errors = new ArrayList<>(parseResult.errors());

        List<ParsedQuestion> parsedQuestions = new ArrayList<>();
        for (RawQuestion rawQuestion : parseResult.questions()) {
            ParsedQuestion parsedQuestion = toQuestionRequest(rawQuestion, errors);
            if (parsedQuestion != null) {
                parsedQuestions.add(parsedQuestion);
            }
        }

        if (errors.isEmpty()) {
            for (ParsedQuestion parsedQuestion : parsedQuestions) {
                try {
                    questionService.validateQuestionCreateRequest(parsedQuestion.request());
                } catch (ResponseStatusException ex) {
                    errors.add(new QuestionImportErrorResponse(
                            parsedQuestion.questionIndex(),
                            parsedQuestion.lineNumber(),
                            ex.getReason() == null ? "Invalid question" : ex.getReason()
                    ));
                }
            }
        }

        int totalQuestions = parseResult.questions().size();
        if (!errors.isEmpty()) {
            return QuestionImportResponse.failed(totalQuestions, errors);
        }

        return QuestionImportResponse.success(questionService.importQuestions(
                questionBankId,
                parsedQuestions.stream().map(ParsedQuestion::request).toList(),
                username
        ));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import file is required");
        }

        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only .txt import is supported. DOCX/PDF can be added later as an extension."
            );
        }
    }

    private String readFile(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read import file");
        }
    }

    private ParseResult parse(String content) {
        List<RawQuestion> questions = new ArrayList<>();
        List<QuestionImportErrorResponse> errors = new ArrayList<>();
        RawQuestion current = null;

        String[] lines = (content == null ? "" : content).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }

            if (QUESTION_MARKER.equalsIgnoreCase(line)) {
                if (current != null) {
                    questions.add(current);
                }
                current = new RawQuestion(questions.size() + 1, lineNumber, new LinkedHashMap<>());
                continue;
            }

            if (current == null) {
                errors.add(new QuestionImportErrorResponse(0, lineNumber, "Line must be inside a [QUESTION] block"));
                continue;
            }

            int separatorIndex = line.indexOf('=');
            if (separatorIndex <= 0) {
                errors.add(new QuestionImportErrorResponse(
                        current.questionIndex(),
                        lineNumber,
                        "Line must use KEY=VALUE format"
                ));
                continue;
            }

            String key = line.substring(0, separatorIndex).trim().toUpperCase(Locale.ROOT);
            String value = line.substring(separatorIndex + 1).trim();
            current.values().computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new RawValue(lineNumber, value));
        }

        if (current != null) {
            questions.add(current);
        }
        if (questions.isEmpty()) {
            errors.add(new QuestionImportErrorResponse(0, 1, "No [QUESTION] block found"));
        }

        return new ParseResult(questions, errors);
    }

    private ParsedQuestion toQuestionRequest(
            RawQuestion rawQuestion,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue typeValue = required(rawQuestion, "TYPE", errors);
        RawValue contentValue = required(rawQuestion, "CONTENT", errors);
        RawValue scoreValue = required(rawQuestion, "SCORE", errors);
        RawValue difficultyValue = required(rawQuestion, "DIFFICULTY", errors);

        QuestionType type = parseEnum(rawQuestion, typeValue, QuestionType.class, "Invalid TYPE", errors);
        Difficulty difficulty = parseEnum(rawQuestion, difficultyValue, Difficulty.class, "Invalid DIFFICULTY", errors);
        BigDecimal score = parseScore(rawQuestion, scoreValue, errors);
        QuestionStatus status = parseEnum(rawQuestion, optional(rawQuestion, "STATUS"), QuestionStatus.class, "Invalid STATUS", errors);
        String topic = optionalValue(rawQuestion, "TOPIC");

        if (type == null || difficulty == null || score == null || contentValue == null) {
            return null;
        }

        AnswerDefinitionRequest answer = switch (type) {
            case TRUE_FALSE -> toTrueFalseAnswer(rawQuestion, errors);
            case MULTIPLE_CHOICE -> toMultipleChoiceAnswer(rawQuestion, errors);
            case FILL_BLANK -> toFillBlankAnswer(rawQuestion, errors);
            case MATCHING -> toMatchingAnswer(rawQuestion, errors);
        };

        if (answer == null) {
            return null;
        }

        return new ParsedQuestion(
                rawQuestion.questionIndex(),
                rawQuestion.startLine(),
                new QuestionCreateRequest(
                        type,
                        contentValue.value(),
                        score,
                        difficulty,
                        topic,
                        status,
                        answer
                )
        );
    }

    private AnswerDefinitionRequest toTrueFalseAnswer(
            RawQuestion rawQuestion,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue correctValue = required(rawQuestion, "CORRECT", errors);
        if (correctValue == null) {
            return null;
        }
        String normalized = correctValue.value().toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    correctValue.lineNumber(),
                    "TRUE_FALSE CORRECT must be true or false"
            ));
            return null;
        }
        return new AnswerDefinitionRequest(Boolean.parseBoolean(normalized), List.of(), null, List.of());
    }

    private AnswerDefinitionRequest toMultipleChoiceAnswer(
            RawQuestion rawQuestion,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue correctValue = required(rawQuestion, "CORRECT", errors);
        if (correctValue == null) {
            return null;
        }

        String correctOptionId = correctValue.value().trim().toUpperCase(Locale.ROOT);
        List<AnswerOptionRequest> options = new ArrayList<>();
        int orderIndex = 1;
        for (Map.Entry<String, List<RawValue>> entry : rawQuestion.values().entrySet()) {
            String key = entry.getKey();
            if (key.length() == 1 && key.charAt(0) >= 'A' && key.charAt(0) <= 'Z') {
                RawValue optionValue = entry.getValue().getFirst();
                options.add(new AnswerOptionRequest(
                        key,
                        optionValue.value(),
                        key.equals(correctOptionId),
                        orderIndex++
                ));
            }
        }

        if (options.stream().noneMatch(option -> option.optionId().equals(correctOptionId))) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    correctValue.lineNumber(),
                    "CORRECT must match one option key"
            ));
            return null;
        }

        return new AnswerDefinitionRequest(null, options, null, List.of());
    }

    private AnswerDefinitionRequest toFillBlankAnswer(
            RawQuestion rawQuestion,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue acceptedValue = required(rawQuestion, "ACCEPTED", errors);
        if (acceptedValue == null) {
            return null;
        }

        List<String> acceptedAnswers = List.of(acceptedValue.value().split("\\|"))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        FillBlankAnswerRequest fillBlank = new FillBlankAnswerRequest(
                optionalValue(rawQuestion, "ANSWER_ID"),
                acceptedAnswers,
                parseBooleanOrDefault(rawQuestion, "IGNORE_CASE", true, errors),
                parseBooleanOrDefault(rawQuestion, "IGNORE_ACCENT", true, errors),
                parseBooleanOrDefault(rawQuestion, "TRIM_SPACE", true, errors)
        );
        return new AnswerDefinitionRequest(null, List.of(), fillBlank, List.of());
    }

    private AnswerDefinitionRequest toMatchingAnswer(
            RawQuestion rawQuestion,
            List<QuestionImportErrorResponse> errors
    ) {
        List<RawValue> pairValues = rawQuestion.values().getOrDefault("PAIR", List.of());
        if (pairValues.isEmpty()) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    rawQuestion.startLine(),
                    "MATCHING requires at least one PAIR line"
            ));
            return null;
        }

        List<MatchingPairRequest> pairs = new ArrayList<>();
        for (int i = 0; i < pairValues.size(); i++) {
            RawValue pairValue = pairValues.get(i);
            String[] parts = pairValue.value().split("=>", 2);
            if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
                errors.add(new QuestionImportErrorResponse(
                        rawQuestion.questionIndex(),
                        pairValue.lineNumber(),
                        "PAIR must use left=>right format"
                ));
                continue;
            }
            pairs.add(new MatchingPairRequest(
                    "M" + (i + 1),
                    parts[0].trim(),
                    parts[1].trim(),
                    i + 1
            ));
        }

        return new AnswerDefinitionRequest(null, List.of(), null, pairs);
    }

    private RawValue required(
            RawQuestion rawQuestion,
            String key,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue value = optional(rawQuestion, key);
        if (value == null || !StringUtils.hasText(value.value())) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    rawQuestion.startLine(),
                    key + " is required"
            ));
            return null;
        }
        return value;
    }

    private RawValue optional(RawQuestion rawQuestion, String key) {
        List<RawValue> values = rawQuestion.values().get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    private String optionalValue(RawQuestion rawQuestion, String key) {
        RawValue value = optional(rawQuestion, key);
        return value == null || !StringUtils.hasText(value.value()) ? null : value.value().trim();
    }

    private BigDecimal parseScore(
            RawQuestion rawQuestion,
            RawValue value,
            List<QuestionImportErrorResponse> errors
    ) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.value());
        } catch (NumberFormatException ex) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    value.lineNumber(),
                    "SCORE must be a number"
            ));
            return null;
        }
    }

    private Boolean parseBooleanOrDefault(
            RawQuestion rawQuestion,
            String key,
            boolean defaultValue,
            List<QuestionImportErrorResponse> errors
    ) {
        RawValue value = optional(rawQuestion, key);
        if (value == null || !StringUtils.hasText(value.value())) {
            return defaultValue;
        }
        String normalized = value.value().trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            errors.add(new QuestionImportErrorResponse(
                    rawQuestion.questionIndex(),
                    value.lineNumber(),
                    key + " must be true or false"
            ));
            return defaultValue;
        }
        return Boolean.parseBoolean(normalized);
    }

    private <T extends Enum<T>> T parseEnum(
            RawQuestion rawQuestion,
            RawValue value,
            Class<T> enumType,
            String message,
            List<QuestionImportErrorResponse> errors
    ) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.value().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(new QuestionImportErrorResponse(rawQuestion.questionIndex(), value.lineNumber(), message));
            return null;
        }
    }

    private record RawValue(int lineNumber, String value) {
    }

    private record RawQuestion(
            int questionIndex,
            int startLine,
            Map<String, List<RawValue>> values
    ) {
    }

    private record ParsedQuestion(
            int questionIndex,
            int lineNumber,
            QuestionCreateRequest request
    ) {
    }

    private record ParseResult(
            List<RawQuestion> questions,
            List<QuestionImportErrorResponse> errors
    ) {
    }
}
