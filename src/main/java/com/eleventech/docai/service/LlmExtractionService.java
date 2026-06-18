package com.eleventech.docai.service;

import com.eleventech.docai.model.DocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmExtractionService {

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public Map<String, Object> extractFields(String rawText, DocumentType docType) {
        String systemPrompt = buildSystemPrompt(docType);
        String userMessage = "Extract all relevant fields from this document text:\n\n" + rawText;

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(Map.of(
                    "model", "llama-3.1-8b-instant",
                    "temperature", 0,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    }
            ));
        } catch (Exception e) {
            return Map.of("error", "Failed to build request: " + e.getMessage());
        }

        String responseBody = webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(responseBody);
    }

    private String buildSystemPrompt(DocumentType docType) {
        return switch (docType) {

            case KYC_AADHAAR -> """
                You are a KYC document parser for Aadhaar cards.
                Extract the fields and return ONLY a valid JSON object, no explanation, no markdown.
                {
                  "name": "",
                  "aadhaar_number": "",
                  "dob": "",
                  "gender": "",
                  "address": "",
                  "pincode": "",
                  "state": ""
                }
                Use null for any field not found.
                """;

            case KYC_PAN -> """
                You are a PAN card parser.
                Return ONLY valid JSON, nothing else:
                {
                  "name": "",
                  "pan_number": "",
                  "dob": "",
                  "father_name": ""
                }
                """;

            case BANK_STATEMENT -> """
                You are a bank statement parser.
                Return ONLY valid JSON:
                {
                  "account_holder": "",
                  "account_number": "",
                  "bank_name": "",
                  "ifsc_code": "",
                  "opening_balance": "",
                  "closing_balance": "",
                  "transactions": [
                    {"date": "", "description": "", "debit": "", "credit": ""}
                  ]
                }
                """;

            case MEDICAL_REPORT -> """
                You are a medical report parser.
                Return ONLY valid JSON:
                {
                  "patient_name": "",
                  "patient_id": "",
                  "doctor_name": "",
                  "hospital": "",
                  "report_date": "",
                  "diagnosis": [],
                  "test_results": [
                    {"test": "", "value": "", "unit": "", "normal_range": ""}
                  ]
                }
                """;

            case MEDICAL_PRESCRIPTION -> """
                You are a prescription parser.
                Return ONLY valid JSON:
                {
                  "patient_name": "",
                  "doctor_name": "",
                  "date": "",
                  "medicines": [
                    {"name": "", "dosage": "", "frequency": "", "duration": ""}
                  ]
                }
                """;

            case SALE_DEED -> """
                You are a legal document parser for Indian property sale deeds.
                The text may be in Hindi, English, or any Indian language.
                Extract all fields and return ONLY valid JSON, nothing else:
                {
                  "deed_type": "",
                  "registration_number": "",
                  "registration_date": "",
                  "registration_office": "",
                  "seller_name": "",
                  "seller_father_name": "",
                  "seller_address": "",
                  "seller_aadhaar": "",
                  "buyer_name": "",
                  "buyer_father_name": "",
                  "buyer_address": "",
                  "buyer_aadhaar": "",
                  "property_description": "",
                  "property_area": "",
                  "property_area_unit": "",
                  "property_address": "",
                  "survey_number": "",
                  "khasra_number": "",
                  "khata_number": "",
                  "consideration_amount": "",
                  "consideration_amount_words": "",
                  "stamp_duty": "",
                  "witnesses": [{"name": "", "address": ""}],
                  "sub_registrar": "",
                  "document_writer": "",
                  "possession_date": ""
                }
                Use null for missing fields. Translate non-English values to English.
                """;

            default -> """
                Extract all key-value pairs from this document and return as a flat JSON object.
                Return ONLY valid JSON, no explanation.
                """;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Try to find JSON in the response even if there's extra text
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");

            if (start != -1 && end != -1) {
                String jsonPart = content.substring(start, end + 1);
                return objectMapper.readValue(jsonPart, Map.class);
            }

            return Map.of("raw_response", content);

        } catch (Exception e) {
            return Map.of(
                    "error", "Could not parse LLM response",
                    "details", e.getMessage()
            );
        }
    }
}