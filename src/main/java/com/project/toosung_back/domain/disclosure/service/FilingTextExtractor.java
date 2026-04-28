package com.project.toosung_back.domain.disclosure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilingTextExtractor {

    private static final int MAX_TEXT_LENGTH = 4000;

    @Qualifier("edgarWebClient")
    private final WebClient edgarWebClient;

    @Qualifier("dartWebClient")
    private final WebClient dartWebClient;

    @Value("${dart.api-key}")
    private String dartApiKey;

    public String extractFromEdgar(String fileUrl) {
        try {
            String html = edgarWebClient.get()
                    .uri(fileUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return truncate(stripHtml(html));
        } catch (Exception e) {
            log.warn("[FilingTextExtractor] EDGAR 텍스트 추출 실패: url={}, error={}", fileUrl, e.getMessage());
            return null;
        }
    }

    public String extractFromDart(String rcpNo) {
        try {
            byte[] zipBytes = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/document.xml")
                            .queryParam("crtfc_key", dartApiKey)
                            .queryParam("rcept_no", rcpNo)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (zipBytes == null) {
                log.warn("[FilingTextExtractor] DART 응답 null: rcpNo={}", rcpNo);
                return null;
            }

            // ZIP magic bytes 확인 (PK = 0x50 0x4B)
            if (zipBytes.length < 4 || zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                log.warn("[FilingTextExtractor] DART 응답이 ZIP이 아님: rcpNo={}, 응답 앞부분={}",
                        rcpNo, new String(zipBytes, 0, Math.min(200, zipBytes.length)));
                return null;
            }

            log.info("[FilingTextExtractor] DART ZIP 수신: rcpNo={}, size={}KB", rcpNo, zipBytes.length / 1024);
            String html = extractLargestHtmlFromZip(zipBytes);
            if (html == null) {
                log.warn("[FilingTextExtractor] DART ZIP에서 HTM 파일 없음: rcpNo={}", rcpNo);
                return null;
            }
            return truncate(stripHtml(html));
        } catch (Exception e) {
            log.warn("[FilingTextExtractor] DART 텍스트 추출 실패: rcpNo={}, error={}", rcpNo, e.getMessage());
            return null;
        }
    }

    private String extractLargestHtmlFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            String mainContent = null;
            int maxLength = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                log.debug("[FilingTextExtractor] ZIP 항목: {}", entry.getName());

                if (name.endsWith(".htm") || name.endsWith(".html") || name.endsWith(".xml")) {
                    String content = readPartial(zis);
                    if (content.length() > maxLength) {
                        maxLength = content.length();
                        mainContent = content;
                    }
                } else {
                    zis.closeEntry();
                }
            }
            return mainContent;
        }
    }

    private String readPartial(ZipInputStream zis) throws Exception {
        byte[] buffer = new byte[8192];
        StringBuilder sb = new StringBuilder();
        int bytesRead;
        while ((bytesRead = zis.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            if (sb.length() >= MAX_TEXT_LENGTH * 4) break; // HTML 태그 감안해 여유있게 읽음
        }
        zis.closeEntry();
        return sb.toString();
    }

    private String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&[a-zA-Z]+;", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }
}
