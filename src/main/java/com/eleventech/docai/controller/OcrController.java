package com.eleventech.docai.controller;

import com.eleventech.docai.model.DocumentType;
import com.eleventech.docai.model.OcrResponse;
import com.eleventech.docai.service.DocumentParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final DocumentParserService documentParserService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OcrResponse> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType) {

        if (file.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(OcrResponse.builder()
                            .processingStatus("ERROR")
                            .errorMessage("Please upload a file.")
                            .build());
        }

        try {
            OcrResponse result = documentParserService.processDocument(file, documentType);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OcrResponse.builder()
                            .processingStatus("ERROR")
                            .errorMessage("Processing failed: " + e.getMessage())
                            .build());
        }
    }
}