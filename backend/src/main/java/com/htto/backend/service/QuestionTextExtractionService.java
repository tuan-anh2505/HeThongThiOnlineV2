package com.htto.backend.service;

import com.htto.backend.domain.DomainEnums.ImportSourceType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@Service
public class QuestionTextExtractionService {

    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public ExtractedQuestionContent extractFromFile(MultipartFile file, ImportSourceType requestedType) {
        validateFile(file);
        byte[] content = readFile(file);
        ImportSourceType sourceType = resolveSourceType(
                requestedType,
                file.getContentType(),
                file.getOriginalFilename()
        );
        return new ExtractedQuestionContent(sourceType, extractText(content, sourceType));
    }

    public ExtractedQuestionContent extractFromUrl(FetchedUrlContent fetchedContent, ImportSourceType requestedType) {
        if (fetchedContent == null || fetchedContent.content() == null || fetchedContent.content().length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL content is empty");
        }

        String path = fetchedContent.uri() == null ? "" : fetchedContent.uri().getPath();
        ImportSourceType sourceType = resolveSourceType(requestedType, fetchedContent.contentType(), path);
        return new ExtractedQuestionContent(sourceType, extractText(fetchedContent.content(), sourceType));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import file is required");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import file exceeds 10MB limit");
        }
    }

    private byte[] readFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read import file");
        }
    }

    private ImportSourceType resolveSourceType(
            ImportSourceType requestedType,
            String contentType,
            String nameOrPath
    ) {
        if (requestedType != null && requestedType != ImportSourceType.AUTO) {
            return requestedType;
        }

        String normalizedContentType = normalizeContentType(contentType);
        if (normalizedContentType.equals("text/plain")) {
            return ImportSourceType.TXT;
        }
        if (normalizedContentType.equals("text/html")) {
            return ImportSourceType.HTML;
        }
        if (normalizedContentType.equals("application/pdf")) {
            return ImportSourceType.PDF;
        }
        if (normalizedContentType.equals(DOCX_CONTENT_TYPE)) {
            return ImportSourceType.DOCX;
        }

        String lowerName = nameOrPath == null ? "" : nameOrPath.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".txt")) {
            return ImportSourceType.TXT;
        }
        if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return ImportSourceType.HTML;
        }
        if (lowerName.endsWith(".pdf")) {
            return ImportSourceType.PDF;
        }
        if (lowerName.endsWith(".docx")) {
            return ImportSourceType.DOCX;
        }

        return ImportSourceType.TXT;
    }

    private String extractText(byte[] content, ImportSourceType sourceType) {
        return switch (sourceType) {
            case TXT, AUTO -> new String(content, StandardCharsets.UTF_8);
            case HTML -> htmlToText(new String(content, StandardCharsets.UTF_8));
            case DOCX -> docxToText(content);
            case PDF -> pdfToText(content);
        };
    }

    private String htmlToText(String html) {
        if (html == null) {
            return "";
        }
        String withoutScripts = html
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "");
        String withLineBreaks = withoutScripts
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|tr|h1|h2|h3|h4|h5|h6|pre|section|article)>", "\n");
        String withoutTags = withLineBreaks.replaceAll("(?s)<[^>]+>", "");
        return HtmlUtils.htmlUnescape(withoutTags).replace('\u00a0', ' ');
    }

    private String docxToText(byte[] content) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                    return docxXmlToText(xml);
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read DOCX import file");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOCX document text was not found");
    }

    private String docxXmlToText(String xml) {
        String withLineBreaks = xml
                .replaceAll("(?i)<w:br[^>]*/>", "\n")
                .replaceAll("(?i)</w:p>", "\n")
                .replaceAll("(?i)</w:tr>", "\n");
        String withoutTags = withLineBreaks.replaceAll("(?s)<[^>]+>", "");
        return HtmlUtils.htmlUnescape(withoutTags).replace('\u00a0', ' ');
    }

    private String pdfToText(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read PDF import file");
        }
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
}
