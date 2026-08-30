package com.raahmediq.ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MedicalDocumentTextExtractor {
    private static final int MAX_PAGES = 100;
    private static final int MAX_TOTAL_CHARACTERS = 250_000;

    public Extraction extract(String contentType, byte[] content) {
        if (!"application/pdf".equals(contentType)) {
            return new Extraction(List.of(), "OCR_REQUIRED");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) return new Extraction(List.of(), "ENCRYPTED_PDF");
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPage> pages = new ArrayList<>();
            int total = 0;
            int count = Math.min(document.getNumberOfPages(), MAX_PAGES);
            for (int page = 1; page <= count && total < MAX_TOTAL_CHARACTERS; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = clean(stripper.getText(document));
                if (!text.isBlank()) {
                    int remaining = MAX_TOTAL_CHARACTERS - total;
                    String limited = text.length() <= remaining ? text : text.substring(0, remaining);
                    pages.add(new ExtractedPage(page, limited));
                    total += limited.length();
                }
            }
            if (pages.isEmpty()) return new Extraction(List.of(), "NO_EXTRACTABLE_TEXT");
            return new Extraction(List.copyOf(pages), null);
        } catch (IOException | RuntimeException exception) {
            return new Extraction(List.of(), "PDF_EXTRACTION_FAILED");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record Extraction(List<ExtractedPage> pages, String errorCode) {
    }

    public record ExtractedPage(int pageNumber, String text) {
    }
}
