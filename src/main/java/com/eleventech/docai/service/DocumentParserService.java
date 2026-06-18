package com.eleventech.docai.service;

import com.eleventech.docai.model.DocumentType;
import com.eleventech.docai.model.OcrResponse;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentParserService {

    private final TesseractOcrService ocrService;
    private final LlmExtractionService llmService;

    public OcrResponse processDocument(MultipartFile file, DocumentType docType)
            throws IOException, TesseractException {

        // Step A: Run Tesseract on the file → get raw text
        String rawText = ocrService.extractText(file);

        // Step B: Send raw text + doc type to LLM → get structured fields
        Map<String, Object> fields = llmService.extractFields(rawText, docType);

        // Step C: Bundle everything into a response
        return OcrResponse.builder()
                .documentType(docType.name())
                .rawText(rawText)
                .extractedFields(fields)
                .processingStatus("SUCCESS")
                .build();
    }
}