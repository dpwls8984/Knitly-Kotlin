package com.mysite.knitly.domain.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 토스페이먼츠 API 호출 전용 클래스
 * - @Retryable이 정상 동작하도록 별도 컴포넌트로 분리
 * - Spring AOP 프록시를 통해 재시도 로직 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossApiClient {

    private final ObjectMapper objectMapper;

    @Value("${payment.toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_PAYMENT_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel";
    private static final String TOSS_PAYMENT_QUERY_URL = "https://api.tosspayments.com/v1/payments/%s";

    private static final int CONNECTION_TIMEOUT = 5000; // 5초
    private static final int READ_TIMEOUT = 10000; // 10초

    /**
     * 결제 승인 API 호출 (재시도 포함)
     * - 외부 클래스에서 호출 → Spring AOP 프록시 적용됨
     * - SocketTimeoutException, IOException 발생 시 최대 3번 재시도
     * - 재시도 간격: 1초 → 2초 → 4초
     */
    @Retryable(
            value = {SocketTimeoutException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public JsonNode confirmPayment(PaymentConfirmRequest request) throws IOException {
        log.info("[TossAPI] [Retry] 결제 승인 API 호출 시도 - orderId={}, paymentKey={}",
                request.orderId(), request.paymentKey());

        String authorization = createBasicAuthHeader(tossSecretKey);
        String requestBody = objectMapper.writeValueAsString(request);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(TOSS_PAYMENT_CONFIRM_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            JsonNode response = handleApiResponse(connection, request.orderId(), request.paymentKey());
            log.info("[TossAPI] [Retry] 결제 승인 API 성공 - orderId={}, paymentKey={}",
                    request.orderId(), request.paymentKey());

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 결제 조회 API 호출
     */
    public JsonNode queryPayment(String paymentKey) throws IOException {
        log.info("[TossAPI] 결제 조회 API 호출 - paymentKey={}", paymentKey);

        String authorization = createBasicAuthHeader(tossSecretKey);
        String queryUrl = String.format(TOSS_PAYMENT_QUERY_URL, paymentKey);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(queryUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            return handleApiResponse(connection, "QUERY", paymentKey);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 결제 취소 API 호출
     */
    public JsonNode cancelPayment(String paymentKey, String cancelReason) throws IOException {
        log.info("[TossAPI] 결제 취소 API 호출 - paymentKey={}", paymentKey);

        String authorization = createBasicAuthHeader(tossSecretKey);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("cancelReason", cancelReason);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        String cancelUrl = String.format(TOSS_PAYMENT_CANCEL_URL, paymentKey);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(cancelUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBodyJson.getBytes(StandardCharsets.UTF_8));
            }

            return handleApiResponse(connection, "CANCEL", paymentKey);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * API 응답 처리
     */
    private JsonNode handleApiResponse(HttpURLConnection connection, String orderId, String paymentKey)
            throws IOException {
        int responseCode = connection.getResponseCode();
        boolean isSuccess = responseCode == 200;

        log.debug("[TossAPI] 응답 수신 - orderId={}, paymentKey={}, responseCode={}",
                orderId, paymentKey, responseCode);

        try (InputStream is = isSuccess ? connection.getInputStream() : connection.getErrorStream()) {
            if (is == null) {
                throw new IOException("응답 스트림이 null입니다.");
            }

            JsonNode responseJson = objectMapper.readTree(is);

            if (!isSuccess) {
                String errorCode = responseJson.has("code") ?
                        responseJson.get("code").asText() : "UNKNOWN";
                String errorMessage = responseJson.has("message") ?
                        responseJson.get("message").asText() : "API 호출 실패";

                log.error("[TossAPI] API 실패 - orderId={}, paymentKey={}, responseCode={}, errorCode={}, errorMessage={}",
                        orderId, paymentKey, responseCode, errorCode, errorMessage);

                throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);
            }

            log.debug("[TossAPI] API 성공 - orderId={}, paymentKey={}", orderId, paymentKey);
            return responseJson;
        }
    }

    /**
     * Basic 인증 헤더 생성
     */
    private String createBasicAuthHeader(String secretKey) {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
}