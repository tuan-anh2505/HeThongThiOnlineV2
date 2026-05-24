package com.htto.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.ImportSourceType;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.dto.request.AiQuestionImportCommitRequest;
import com.htto.backend.dto.request.AnswerDefinitionRequest;
import com.htto.backend.dto.request.AnswerOptionRequest;
import com.htto.backend.dto.request.FillBlankAnswerRequest;
import com.htto.backend.dto.request.ImportQuestionsFromUrlRequest;
import com.htto.backend.dto.request.MatchingPairRequest;
import com.htto.backend.dto.request.QuestionCreateRequest;
import com.htto.backend.dto.response.AiQuestionDraftResponse;
import com.htto.backend.dto.response.AiQuestionImportPreviewResponse;
import com.htto.backend.dto.response.QuestionImportErrorResponse;
import com.htto.backend.dto.response.QuestionImportResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiQuestionImportService {

    private static final int MAX_AI_TEXT_CHARS = 30_000;
    private static final String TARGET_QUESTION_BANK = "QUESTION_BANK";

    private final QuestionImportService questionImportService;
    private final QuestionTextExtractionService questionTextExtractionService;
    private final UrlContentFetchService urlContentFetchService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiEndpoint;
    private final SystemLogService systemLogService;

    public AiQuestionImportService(
            QuestionImportService questionImportService,
            QuestionTextExtractionService questionTextExtractionService,
            UrlContentFetchService urlContentFetchService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            SystemLogService systemLogService,
            @Value("${app.ai.openai.api-key:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${app.ai.openai.model:${OPENAI_MODEL:gpt-4o-mini}}") String openAiModel,
            @Value("${app.ai.openai.endpoint:${OPENAI_ENDPOINT:https://api.openai.com/v1/chat/completions}}")
            String openAiEndpoint
    ) {
        this.questionImportService = questionImportService;
        this.questionTextExtractionService = questionTextExtractionService;
        this.urlContentFetchService = urlContentFetchService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.systemLogService = systemLogService;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.openAiEndpoint = openAiEndpoint;
    }

    public AiQuestionImportPreviewResponse previewFromFile(
            String questionBankId,
            MultipartFile file,
            ImportSourceType sourceType,
            String username
    ) {
        questionImportService.validateImportAccess(questionBankId, username);
        ExtractedQuestionContent extractedContent = questionTextExtractionService.extractFromFile(file, sourceType);
        return previewText(extractedContent.text());
    }

    public AiQuestionImportPreviewResponse previewFromUrl(
            String questionBankId,
            ImportQuestionsFromUrlRequest request,
            String username
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }

        questionImportService.validateImportAccess(questionBankId, username);
        FetchedUrlContent fetchedContent = urlContentFetchService.fetch(request.url());
        ExtractedQuestionContent extractedContent = questionTextExtractionService.extractFromUrl(
                fetchedContent,
                request.sourceType()
        );
        return previewText(extractedContent.text());
    }

    public QuestionImportResponse commitPreview(
            String questionBankId,
            AiQuestionImportCommitRequest request,
            String username
    ) {
        if (request == null || request.questions() == null || request.questions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questions are required");
        }

        QuestionImportResponse response = questionImportService.importStructuredQuestions(
                questionBankId,
                request.questions(),
                username
        );
        if (response.success()) {
            systemLogService.logCurrentUser(
                    "IMPORT_QUESTIONS_AI",
                    TARGET_QUESTION_BANK,
                    questionBankId,
                    "Imported questions from AI preview, importedCount=" + response.importedCount()
            );
        }
        return response;
    }

    private AiQuestionImportPreviewResponse previewText(String text) {
        if (!StringUtils.hasText(text)) {
            return failedPreview("Không đọc được nội dung văn bản từ nguồn import");
        }

        List<QuestionCreateRequest> extractedQuestions = extractQuestionsWithAi(limitText(text));
        if (extractedQuestions.isEmpty()) {
            return failedPreview("AI không trích xuất được câu hỏi hợp lệ từ nội dung đã cung cấp");
        }

        List<AiQuestionDraftResponse> drafts = new ArrayList<>();
        for (int i = 0; i < extractedQuestions.size(); i++) {
            QuestionCreateRequest question = extractedQuestions.get(i);
            List<QuestionImportErrorResponse> errors = questionImportService.validateStructuredQuestions(List.of(question));
            String error = errors.isEmpty() ? null : errors.getFirst().message();
            drafts.add(new AiQuestionDraftResponse(i + 1, errors.isEmpty(), error, question));
        }

        int validCount = (int) drafts.stream().filter(AiQuestionDraftResponse::valid).count();
        int failedCount = drafts.size() - validCount;
        List<String> errors = drafts.stream()
                .filter(draft -> !draft.valid())
                .map(draft -> "Câu " + draft.questionIndex() + ": " + draft.error())
                .toList();

        return new AiQuestionImportPreviewResponse(
                failedCount == 0,
                drafts.size(),
                validCount,
                failedCount,
                drafts,
                errors
        );
    }

    private AiQuestionImportPreviewResponse failedPreview(String message) {
        return new AiQuestionImportPreviewResponse(false, 0, 0, 1, List.of(), List.of(message));
    }

    private List<QuestionCreateRequest> extractQuestionsWithAi(String text) {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chưa cấu hình OPENAI_API_KEY cho chức năng import bằng AI"
            );
        }

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", buildUserPrompt(text))
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(openAiEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null
                    ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("");
            return parseAiJsonContent(content);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service request failed with status " + ex.getStatusCode().value()
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cannot connect to AI service");
        }
    }

    private String buildSystemPrompt() {
        return """
                Bạn là bộ chuyển đổi dữ liệu câu hỏi cho hệ thống thi online.
                Nhiệm vụ duy nhất là đọc nội dung thô và trả về JSON hợp lệ.
                Không giải thích, không thêm markdown, không dùng emoji.
                Chỉ trả về object có key questions.
                Mỗi question phải có type, content, score, difficulty, topic nếu có.
                type chỉ được là TRUE_FALSE, MULTIPLE_CHOICE, FILL_BLANK, MATCHING.
                difficulty chỉ được là EASY, MEDIUM, HARD.
                MULTIPLE_CHOICE phải có options với optionId, content, isCorrect, orderIndex và đúng 1 đáp án đúng.
                TRUE_FALSE phải có correctAnswer true hoặc false.
                FILL_BLANK phải có acceptedAnswers.
                MATCHING phải có matchingPairs với leftText, rightText, orderIndex.
                Nếu không chắc đáp án đúng, không tạo câu hỏi đó.
                """;
    }

    private String buildUserPrompt(String text) {
        return """
                Hãy chuyển nội dung sau thành JSON theo schema:
                {
                  "questions": [
                    {
                      "type": "MULTIPLE_CHOICE",
                      "content": "Nội dung câu hỏi",
                      "score": 1,
                      "difficulty": "EASY",
                      "topic": "Chủ đề nếu có",
                      "correctAnswer": true,
                      "options": [
                        {"optionId": "A", "content": "Đáp án A", "isCorrect": true, "orderIndex": 1}
                      ],
                      "acceptedAnswers": ["Đáp án điền khuyết"],
                      "matchingPairs": [
                        {"matchingId": "M1", "leftText": "Vế trái", "rightText": "Vế phải", "orderIndex": 1}
                      ]
                    }
                  ]
                }

                Nội dung thô:
                """ + text;
    }

    private String limitText(String text) {
        if (text.length() <= MAX_AI_TEXT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_AI_TEXT_CHARS);
    }

    private List<QuestionCreateRequest> parseAiJsonContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response does not contain question data");
        }

        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(content));
            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response has invalid questions format");
            }

            List<QuestionCreateRequest> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                questions.add(toQuestionCreateRequest(node));
            }
            return questions;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response is not valid JSON");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }

    private String extractJsonObject(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("(?s)^```[a-zA-Z]*\\s*", "").replaceFirst("(?s)\\s*```$", "");
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response is not a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private QuestionCreateRequest toQuestionCreateRequest(JsonNode node) {
        QuestionType type = parseEnum(requiredText(node, "type"), QuestionType.class, "Loại câu hỏi không hợp lệ");
        Difficulty difficulty = parseEnum(
                requiredText(node, "difficulty"),
                Difficulty.class,
                "Mức độ câu hỏi không hợp lệ"
        );
        BigDecimal score = parseScore(node.path("score"));
        AnswerDefinitionRequest answer = toAnswerDefinition(type, node);

        return new QuestionCreateRequest(
                type,
                requiredText(node, "content"),
                score,
                difficulty,
                optionalText(node, "topic"),
                QuestionStatus.ACTIVE,
                answer
        );
    }

    private AnswerDefinitionRequest toAnswerDefinition(QuestionType type, JsonNode node) {
        return switch (type) {
            case TRUE_FALSE -> new AnswerDefinitionRequest(parseBoolean(node, "correctAnswer"), List.of(), null, List.of());
            case MULTIPLE_CHOICE -> new AnswerDefinitionRequest(null, toOptions(node.path("options")), null, List.of());
            case FILL_BLANK -> new AnswerDefinitionRequest(
                    null,
                    List.of(),
                    new FillBlankAnswerRequest(
                            optionalText(node, "answerId"),
                            toStringList(node.path("acceptedAnswers")),
                            booleanOrDefault(node, "ignoreCase", true),
                            booleanOrDefault(node, "ignoreAccent", true),
                            booleanOrDefault(node, "trimSpace", true)
                    ),
                    List.of()
            );
            case MATCHING -> new AnswerDefinitionRequest(null, List.of(), null, toMatchingPairs(node.path("matchingPairs")));
        };
    }

    private List<AnswerOptionRequest> toOptions(JsonNode optionsNode) {
        if (!optionsNode.isArray()) {
            throw new IllegalArgumentException("Câu trắc nghiệm thiếu danh sách lựa chọn");
        }

        List<AnswerOptionRequest> options = new ArrayList<>();
        for (int i = 0; i < optionsNode.size(); i++) {
            JsonNode option = optionsNode.get(i);
            options.add(new AnswerOptionRequest(
                    StringUtils.hasText(optionalText(option, "optionId")) ? optionalText(option, "optionId") : optionIdOf(i),
                    requiredText(option, "content"),
                    booleanOrDefault(option, "isCorrect", false),
                    numberOrDefault(option.path("orderIndex"), i + 1)
            ));
        }
        return options;
    }

    private List<MatchingPairRequest> toMatchingPairs(JsonNode pairsNode) {
        if (!pairsNode.isArray()) {
            throw new IllegalArgumentException("Câu nối đáp án thiếu danh sách cặp nối");
        }

        List<MatchingPairRequest> pairs = new ArrayList<>();
        for (int i = 0; i < pairsNode.size(); i++) {
            JsonNode pair = pairsNode.get(i);
            pairs.add(new MatchingPairRequest(
                    StringUtils.hasText(optionalText(pair, "matchingId")) ? optionalText(pair, "matchingId") : "M" + (i + 1),
                    requiredText(pair, "leftText"),
                    requiredText(pair, "rightText"),
                    numberOrDefault(pair.path("orderIndex"), i + 1)
            ));
        }
        return pairs;
    }

    private List<String> toStringList(JsonNode node) {
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            return List.of(node.asText().trim());
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("Câu điền khuyết thiếu acceptedAnswers");
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (StringUtils.hasText(item.asText())) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Thiếu field " + field);
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText().trim();
    }

    private BigDecimal parseScore(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Thiếu điểm câu hỏi");
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Điểm câu hỏi không hợp lệ");
        }
    }

    private Boolean parseBoolean(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isTextual()) {
            String normalized = value.asText().trim().toLowerCase();
            if (normalized.equals("true") || normalized.equals("đúng") || normalized.equals("dung")) {
                return true;
            }
            if (normalized.equals("false") || normalized.equals("sai")) {
                return false;
            }
        }
        throw new IllegalArgumentException("Thiếu hoặc sai field " + field);
    }

    private Boolean booleanOrDefault(JsonNode node, String field, boolean defaultValue) {
        JsonNode value = node.path(field);
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isTextual()) {
            String normalized = value.asText().trim().toLowerCase();
            if (normalized.equals("true")) {
                return true;
            }
            if (normalized.equals("false")) {
                return false;
            }
        }
        return defaultValue;
    }

    private int numberOrDefault(JsonNode node, int defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, String errorMessage) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String optionIdOf(int index) {
        if (index >= 0 && index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "O" + (index + 1);
    }
}
