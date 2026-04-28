package com.project.toosung_back.global.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("oauthWebClient")
    public WebClient oauthWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(5));              // 응답 타임아웃

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
    }

    @Bean
    @Qualifier("naverWebClient")
    public WebClient naverWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(10));              // 응답 타임아웃

        return WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
    }

    @Bean
    @Qualifier("dartWebClient")
    public WebClient dartWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)   // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(15));              // 응답 타임아웃

        return WebClient.builder()
                .baseUrl("https://opendart.fss.or.kr")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024)) // 100MB (사업보고서 ZIP 대응)
                .build();
    }

    @Bean
    @Qualifier("edgarWebClient")
    public WebClient edgarWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15))
                .compress(true); // gzip 자동 해제

        return WebClient.builder()
                .defaultHeader("User-Agent", "TooSung contact@toosung.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean
    @Qualifier("openAiWebClient")
    public WebClient openAiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(30));              // 응답 타임아웃 (AI 추론 시간 여유)

        return WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
}
