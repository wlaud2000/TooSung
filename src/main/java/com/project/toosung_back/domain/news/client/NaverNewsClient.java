package com.project.toosung_back.domain.news.client;

import com.project.toosung_back.domain.news.dto.response.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsClient {

    @Qualifier("naverWebClient")
    private final WebClient naverWebClient;

    @Value("${naver.news.client-id}")
    private String clientId;

    @Value("${naver.news.client-secret}")
    private String clientSecret;

    @Value("${naver.news.display}")
    private int display;

    public List<NaverNewsResponse.NaverNewsItem> fetchNews(String query) {
        try {
            NaverNewsResponse response = naverWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/news.json")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("sort", "date")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(NaverNewsResponse.class)
                    .block();

            return response != null ? response.items() : Collections.emptyList();

        } catch (Exception e) {
            log.error("[NaverNewsClient] 뉴스 수집 실패 - query: {}, error: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }
}