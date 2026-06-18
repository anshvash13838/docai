package com.eleventech.docai.service;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TesseractOcrService {

    private final Tesseract tesseract;

    public String extractText(MultipartFile file) throws IOException, TesseractException {
        String contentType = file.getContentType();

        if ("application/pdf".equals(contentType)) {
            return extractFromPdf(file);
        } else {
            BufferedImage image = ImageIO.read(file.getInputStream());
            image = toGrayscale(image);
            return tesseract.doOCR(image);
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException, TesseractException {
        StringBuilder fullText = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);
                pageImage = toGrayscale(pageImage);
                fullText.append(tesseract.doOCR(pageImage));
                fullText.append("\n--- PAGE BREAK ---\n");
            }
        }

        return fullText.toString();
    }

    private BufferedImage toGrayscale(BufferedImage original) {
        BufferedImage gray = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics g = gray.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return gray;
    }
}
