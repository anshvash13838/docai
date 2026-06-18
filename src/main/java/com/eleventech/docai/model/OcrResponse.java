package com.eleventech.docai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
    private String documentType;
    private String rawText;
    private Map<String, Object> extractedFields;
    private String processingStatus;
    private String errorMessage;
}