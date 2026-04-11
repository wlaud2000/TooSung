package com.project.toosung_back.global.openai.client;

import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;
import com.project.toosung_back.global.openai.dto.OpenAiChatResponse;
import com.project.toosung_back.global.openai.exception.OpenAiErrorCode;
import com.project.toosung_back.global.openai.exception.OpenAiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final int MAX_RETRY = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(8);

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;

    @Value("${openai.api-key}")
    private String apiKey;

    public OpenAiChatResponse chat(OpenAiChatRequest request) {
        long startMs = System.currentTimeMillis();
        log.info("[OpenAiClient] 호출 시작 - model={}, messages={}개", request.model(), request.messages().size());

        try {
            OpenAiChatResponse response = openAiWebClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 401,
                            resp -> Mono.error(new OpenAiException(OpenAiErrorCode.UNAUTHORIZED))
                    )
                    .onStatus(
                            status -> status.is4xxClientError() && status.value() != 429,
                            resp -> Mono.error(new OpenAiException(OpenAiErrorCode.API_CALL_FAILED))
                    )
                    .bodyToMono(OpenAiChatResponse.class)
                    .retryWhen(Retry.backoff(MAX_RETRY, INITIAL_BACKOFF)
                            .maxBackoff(MAX_BACKOFF)
                            .jitter(0.5)
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn(
                                    "[OpenAiClient] 재시도 {}/{}회 - cause={}",
                                    signal.totalRetries() + 1, MAX_RETRY,
                                    signal.failure().getMessage())))
                    .block();

            validateResponse(response);

            long elapsed = System.currentTimeMillis() - startMs;
            assert response != null;
            log.info("[OpenAiClient] 호출 성공 - promptTokens={}, completionTokens={}, totalTokens={}, elapsed={}ms",
                    response.usage().promptTokens(),
                    response.usage().completionTokens(),
                    response.usage().totalTokens(),
                    elapsed);

            return response;

        } catch (OpenAiException e) {
            log.error("[OpenAiClient] 호출 실패 - code={}, message={}", e.getCode().getCode(), e.getCode().getMessage());
            throw e;
        } catch (Exception e) {
            Throwable cause = Exceptions.isRetryExhausted(e) ? e.getCause() : e;
            log.error("[OpenAiClient] 호출 최종 실패 - error={}", cause.getMessage());

            if (cause instanceof WebClientResponseException ex && ex.getStatusCode().value() == 429) {
                throw new OpenAiException(OpenAiErrorCode.RATE_LIMIT_EXCEEDED);
            }
            throw new OpenAiException(OpenAiErrorCode.API_CALL_FAILED);
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof OpenAiException) {
            return false;
        }
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError()
                    || ex.getStatusCode().value() == 429;
        }
        // 타임아웃, 연결 오류 등 네트워크 레벨 예외는 재시도
        return true;
    }

    private void validateResponse(OpenAiChatResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null) {
            throw new OpenAiException(OpenAiErrorCode.RESPONSE_PARSE_FAILED);
        }
    }
}
