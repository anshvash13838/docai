package com.eleventech.docai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GoogleVisionOcrService {

    @Value("${ocr.space.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GoogleVisionOcrService(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.ocr.space")
                .build();
        this.objectMapper = objectMapper;
    }

    public String extractText(MultipartFile file) throws Exception {

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("apikey", apiKey);
        formData.add("language", "hin");
        formData.add("isOverlayRequired", "false");
        formData.add("detectOrientation", "true");
        formData.add("scale", "true");
        formData.add("OCREngine", "2");
        formData.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        String response = webClient.post()
                .uri("/parse/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(response);
        JsonNode results = root.path("ParsedResults");

        if (results.isArray() && results.size() > 0) {
            return results.get(0).path("ParsedText").asText();
        }

        String errorMessage = root.path("ErrorMessage").asText();
        if (!errorMessage.isEmpty()) {
            throw new RuntimeException("OCR.space error: " + errorMessage);
        }

        return "";
    }
}