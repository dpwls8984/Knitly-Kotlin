package com.mysite.knitly.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.dto.*;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final RedisProductService redisProductService;

    @Value("${payment.toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_PAYMENT_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel";

    // 결제 승인
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        long startTime = System.currentTimeMillis();
        String orderId = request.orderId();
        String paymentKey = request.paymentKey();
        Long amount = request.amount();

        log.info("[Payment] [Confirm] 결제 승인 시작 - orderId={}, paymentKey={}, amount={}",
                orderId, paymentKey, amount);

        try {
            // 1. 주문 정보 조회 및 검증
            Order order = orderRepository.findByTossOrderId(orderId)
                    .orElseThrow(() -> {
                        log.warn("[Payment] [Confirm] 주문을 찾을 수 없음 - orderId={}", orderId);
                        return new ServiceException(ErrorCode.ORDER_NOT_FOUND);
                    });

            Long userId = order.getUser().getUserId();
            log.debug("[Payment] [Confirm] 주문 정보 조회 완료 - orderId={}, userId={}, orderAmount={}",
                    orderId, userId, order.getTotalPrice());

            // 2. 주문 금액 검증
            long orderAmount = order.getTotalPrice().longValue();
            if (orderAmount != amount) {
                log.warn("[Payment] [Confirm] 결제 금액 불일치 - orderId={}, orderAmount={}, requestAmount={}",
                        orderId, orderAmount, amount);
                throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

            // 3. 중복 결제 방지
            if (paymentRepository.findByOrder_OrderId(order.getOrderId()).isPresent()) {
                log.warn("[Payment] [Confirm] 중복 결제 시도 - orderId={}, orderInternalId={}",
                        orderId, order.getOrderId());
                throw new ServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS);
            }

            // 4. 토스페이먼츠 결제 승인 API 호출
            long apiStartTime = System.currentTimeMillis();
            JsonNode tossResponse = callTossPaymentConfirmApi(request);
            long apiDuration = System.currentTimeMillis() - apiStartTime;

            log.info("[Payment] [Confirm] 토스 API 호출 완료 - orderId={}, paymentKey={}, apiDuration={}ms",
                    orderId, paymentKey, apiDuration);

            // 5. Payment 엔티티 생성 및 저장
            Payment payment = createPaymentFromTossResponse(order, tossResponse);
            Payment savedPayment = paymentRepository.save(payment);

            log.debug("[Payment] [Confirm] Payment 엔티티 저장 완료 - paymentId={}, orderId={}",
                    savedPayment.getPaymentId(), orderId);

            // 결제 완료 시 redis 상품 인기도 증가
            incrementProductPopularity(order);

            // 6. 응답 데이터 생성
            PaymentConfirmResponse response = buildPaymentConfirmResponse(savedPayment, tossResponse);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Payment] [Confirm] 결제 승인 완료 - orderId={}, paymentKey={}, paymentId={}, amount={}, totalDuration={}ms",
                    orderId, paymentKey, savedPayment.getPaymentId(), amount, totalDuration);

            return response;

        } catch (ServiceException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Confirm] 결제 승인 실패 (ServiceException) - orderId={}, paymentKey={}, error={}, duration={}ms",
                    orderId, paymentKey, e.getErrorCode(), duration);
            throw e;

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Confirm] 토스 API 호출 실패 (IOException) - orderId={}, paymentKey={}, duration={}ms",
                    orderId, paymentKey, duration, e);
            throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Confirm] 결제 승인 실패 (UnexpectedException) - orderId={}, paymentKey={}, duration={}ms",
                    orderId, paymentKey, duration, e);
            throw new ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    //주문의 모든 상품에 대해 Redis 인기도, purchaseCount 증가
    private void incrementProductPopularity(Order order) {
        long startTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int failCount = 0;

            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                Long productId = product.getProductId();
                int quantity = orderItem.getQuantity();

                try {
                    // DB의 purchaseCount 증가 (수량만큼)
                    product.increasePurchaseCount(quantity);

                    // Redis 인기도 증가 (수량만큼)
                    for (int i = 0; i < quantity; i++) {
                        redisProductService.incrementPurchaseCount(productId);
                    }

                    successCount++;

                    log.debug("[Payment] [Popularity] 상품 인기도 증가 완료 - productId={}, quantity={}, newPurchaseCount={}",
                            productId, quantity, product.getPurchaseCount());

                } catch (Exception e) {
                    failCount++;
                    log.error("[Payment] [Popularity] 상품 인기도 증가 실패 - productId={}, quantity={}",
                            productId, quantity, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            log.info("[Payment] [Popularity] 인기도 증가 완료 - orderId={}, totalItems={}, successCount={}, failCount={}, duration={}ms",
                    order.getOrderId(), order.getOrderItems().size(), successCount, failCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Popularity] 인기도 증가 처리 실패 - orderId={}, duration={}ms",
                    order.getOrderId(), duration, e);
        }
    }

    // TODO : 결제 취소 기능 삭제 예정
    @Transactional
    public PaymentCancelResponse cancelPayment(Long paymentId, PaymentCancelRequest request) {
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 취소 가능 여부 확인
        if (!payment.isCancelable()) {
            throw new ServiceException(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

        // 3. 토스페이먼츠 결제 취소 API 호출
        try {
            callTossPaymentCancelApi(payment.getTossPaymentKey(), request.cancelReason());

            // 4. 취소 처리
            payment.cancel(request.cancelReason());
            paymentRepository.save(payment);

            // 5. 응답 생성
            PaymentCancelResponse response = PaymentCancelResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .paymentKey(payment.getTossPaymentKey())
                    .orderId(payment.getTossOrderId())
                    .status(payment.getPaymentStatus())
                    .cancelAmount(payment.getTotalAmount())
                    .cancelReason(request.cancelReason())
                    .canceledAt(payment.getCanceledAt())
                    .build();

            log.info("결제 취소 성공 - paymentId: {}, amount: {}", paymentId, payment.getTotalAmount());

            return response;

        } catch (IOException e) {
            log.error("토스페이먼츠 취소 API 호출 실패", e);
            throw new ServiceException(ErrorCode.PAYMENT_CANCEL_API_FAILED);
        }
    }

    // 토스페이먼츠 결제 승인 API 호출
    private JsonNode callTossPaymentConfirmApi(PaymentConfirmRequest request) throws IOException {
        log.debug("[Payment] [TossAPI] 결제 승인 API 호출 - orderId={}, paymentKey={}",
                request.orderId(), request.paymentKey());

        String authorization = createBasicAuthHeader(tossSecretKey);
        String requestBody = objectMapper.writeValueAsString(request);

        URL url = new URL(TOSS_PAYMENT_CONFIRM_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        return handleTossApiResponse(connection, request.orderId(), request.paymentKey());
    }

    // 토스페이먼츠 결제 취소 API 호출
    // TODO : 취소 기능 삭제 예정
    private JsonNode callTossPaymentCancelApi(String paymentKey, String cancelReason) throws IOException {
        String authorization = createBasicAuthHeader(tossSecretKey);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("cancelReason", cancelReason);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        URL url = new URL(String.format(TOSS_PAYMENT_CANCEL_URL, paymentKey));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBodyJson.getBytes(StandardCharsets.UTF_8));
        }

        return handleTossApiResponse(connection, "CANCEL", paymentKey);
    }

    // 토스 API 응답 처리
    private JsonNode handleTossApiResponse(HttpURLConnection connection, String orderId, String paymentKey) throws IOException {
        int responseCode = connection.getResponseCode();
        boolean isSuccess = responseCode == 200;
        log.debug("[Payment] [TossAPI] 응답 수신 - orderId={}, paymentKey={}, responseCode={}",
                orderId, paymentKey, responseCode);

        try (InputStream is = isSuccess ? connection.getInputStream() : connection.getErrorStream()) {
            JsonNode responseJson = objectMapper.readTree(is);

            if (!isSuccess) {
                String errorCode = responseJson.has("code") ? responseJson.get("code").asText() : "UNKNOWN";
                String errorMessage = responseJson.has("message") ? responseJson.get("message").asText() : "API 호출 실패";
                log.error("[Payment] [TossAPI] API 실패 - orderId={}, paymentKey={}, responseCode={}, errorCode={}, errorMessage={}",
                        orderId, paymentKey, responseCode, errorCode, errorMessage);
                throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);
            }

            log.debug("[Payment] [TossAPI] API 성공 - orderId={}, paymentKey={}",
                    orderId, paymentKey);
            return responseJson;
        }
    }

    // Basic 인증 헤더 생성
    private String createBasicAuthHeader(String secretKey) {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    // 토스 응답으로부터 Payment 엔티티 생성
    private Payment createPaymentFromTossResponse(Order order, JsonNode tossResponse) {
        String method = tossResponse.get("method").asText();
        String status = tossResponse.get("status").asText();

        Payment payment = Payment.builder()
                .tossPaymentKey(tossResponse.get("paymentKey").asText())
                .tossOrderId(tossResponse.get("orderId").asText())
                .mid(tossResponse.has("mId") ? tossResponse.get("mId").asText() : null)
                .order(order)
                .buyer(order.getUser())
                .totalAmount(tossResponse.get("totalAmount").asLong())
                .paymentMethod(PaymentMethod.fromString(method))
                .paymentStatus(PaymentStatus.fromString(status))
                .build();

        if (tossResponse.has("approvedAt")) {
            String approvedAtStr = tossResponse.get("approvedAt").asText();
            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME);
            payment.approve(approvedAt);
        }

        return payment;
    }

    // PaymentConfirmResponse 생성
    private PaymentConfirmResponse buildPaymentConfirmResponse(Payment payment, JsonNode tossResponse) {
        PaymentConfirmResponse.PaymentConfirmResponseBuilder builder = PaymentConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getTossPaymentKey())
                .orderId(payment.getTossOrderId())
                .orderName(tossResponse.has("orderName") ? tossResponse.get("orderName").asText() : null)
                .method(payment.getPaymentMethod())
                .totalAmount(payment.getTotalAmount())
                .status(payment.getPaymentStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .mid(tossResponse.has("mId") ? tossResponse.get("mId").asText() : null);

        // 카드 결제 정보
        if (tossResponse.has("card")) {
            JsonNode card = tossResponse.get("card");
            builder.card(PaymentConfirmResponse.CardInfo.builder()
                    .company(card.has("company") ? card.get("company").asText() : null)
                    .number(card.has("number") ? card.get("number").asText() : null)
                    .installmentPlanMonths(card.has("installmentPlanMonths") ?
                            card.get("installmentPlanMonths").asText() : null)
                    .approveNo(card.has("approveNo") ? card.get("approveNo").asText() : null)
                    .ownerType(card.has("ownerType") ? card.get("ownerType").asText() : null)
                    .build());
        }

        // 가상계좌 정보
        if (tossResponse.has("virtualAccount")) {
            JsonNode va = tossResponse.get("virtualAccount");
            PaymentConfirmResponse.VirtualAccountInfo.VirtualAccountInfoBuilder vaBuilder =
                    PaymentConfirmResponse.VirtualAccountInfo.builder()
                            .accountNumber(va.has("accountNumber") ? va.get("accountNumber").asText() : null)
                            .bankCode(va.has("bankCode") ? va.get("bankCode").asText() : null)
                            .customerName(va.has("customerName") ? va.get("customerName").asText() : null);

            if (va.has("dueDate")) {
                String dueDateStr = va.get("dueDate").asText();
                vaBuilder.dueDate(LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_DATE_TIME));
            }

            builder.virtualAccount(vaBuilder.build());
        }

        // 간편결제 정보
        if (tossResponse.has("easyPay")) {
            JsonNode easyPay = tossResponse.get("easyPay");
            builder.easyPay(PaymentConfirmResponse.EasyPayInfo.builder()
                    .provider(easyPay.has("provider") ? easyPay.get("provider").asText() : null)
                    .amount(easyPay.has("amount") ? easyPay.get("amount").asLong() : null)
                    .build());
        }

        return builder.build();
    }

    // 마이페이지에서 주문의 결제 내역 단건 조회

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetailByOrder(User user, Long orderId) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제 내역인지 확인
        if (!payment.getBuyer().getUserId().equals(user.getUserId())) {
            throw new ServiceException(ErrorCode.PAYMENT_UNAUTHORIZED_ACCESS);
        }
        return PaymentDetailResponse.from(payment);
    }
}