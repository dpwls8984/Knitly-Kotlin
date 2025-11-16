package com.mysite.knitly.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.client.TossApiClient;
import com.mysite.knitly.domain.payment.dto.*;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.email.entity.EmailOutbox;
import com.mysite.knitly.global.email.repository.EmailOutboxRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RedisProductService redisProductService;
    private final TossApiClient tossApiClient;
    private final ObjectMapper objectMapper;
    private final EmailOutboxRepository emailOutboxRepository;

    /**
     * 결제 승인 처리
     * 1. READY 상태의 Payment 조회
     * 2. IN_PROGRESS 상태로 변경 + paymentKey 업데이트
     * 3. 토스 API 호출 (재시도 자동 적용)
     * 4. 성공 시 DONE으로 업데이트, 실패 시 FAILED로 업데이트
     */

    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        long startTime = System.currentTimeMillis();
        String orderId = request.orderId();
        String paymentKey = request.paymentKey();
        Long amount = request.amount();

        log.info("[Payment] [Confirm] 결제 승인 시작 - orderId={}, paymentKey={}, amount={}",
                orderId, paymentKey, amount);

        Payment savedPayment = null;

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

            // 3. READY 상태의 Payment 조회
            Optional<Payment> existingPayment = paymentRepository.findByOrder_OrderId(order.getOrderId());

            if (existingPayment.isEmpty()) {
                log.error("[Payment] [Confirm] Payment가 존재하지 않음 - orderId={}", orderId);
                throw new ServiceException(ErrorCode.PAYMENT_NOT_FOUND);
            }

            savedPayment = existingPayment.get();

            // READY 상태가 아니면 에러
            if (savedPayment.getPaymentStatus() != PaymentStatus.READY) {
                log.warn("[Payment] [Confirm] Payment가 READY 상태가 아님 - paymentId={}, status={}",
                        savedPayment.getPaymentId(), savedPayment.getPaymentStatus());

                if (savedPayment.getPaymentStatus() == PaymentStatus.DONE) {
                    throw new ServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS);
                } else {
                    throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
                }
            }

            // 4. Payment를 IN_PROGRESS 상태로 변경 + paymentKey 저장
            savedPayment.setTossPaymentKey(paymentKey);
            savedPayment.setPaymentStatus(PaymentStatus.IN_PROGRESS);
            savedPayment = paymentRepository.save(savedPayment);

            log.info("[Payment] [Confirm] Payment 상태 변경 - paymentId={}, READY → IN_PROGRESS",
                    savedPayment.getPaymentId());

            // 5. 토스페이먼츠 결제 승인 API 호출 (재시도 자동 적용)
            long apiStartTime = System.currentTimeMillis();
            JsonNode tossResponse = tossApiClient.confirmPayment(request);
            long apiDuration = System.currentTimeMillis() - apiStartTime;

            log.info("[Payment] [Confirm] 토스 API 호출 완료 - orderId={}, paymentKey={}, apiDuration={}ms",
                    orderId, paymentKey, apiDuration);

            // 6. Payment 엔티티 업데이트 (DONE 상태로 변경)
            updatePaymentFromTossResponse(savedPayment, tossResponse);
            savedPayment = paymentRepository.save(savedPayment);

            log.debug("[Payment] [Confirm] Payment 엔티티 업데이트 완료 - paymentId={}, status=DONE",
                    savedPayment.getPaymentId());

            // 7. 결제 완료 시 redis 상품 인기도 증가
            incrementProductPopularity(order);

            // 8. 응답 데이터 생성
            MDC.put("orderId", order.getOrderId().toString());
            MDC.put("userId", order.getUser().getUserId().toString());
            try {
                log.info("[Payment] [Outbox] EmailOutbox 작업 생성 시작");
                User user = order.getUser();
                EmailNotificationDto emailDto = new EmailNotificationDto(order.getOrderId(), user.getUserId(), user.getEmail());
                String payload = objectMapper.writeValueAsString(emailDto);

                EmailOutbox emailJob = EmailOutbox.builder()
                        .payload(payload)
                        .build();
                emailOutboxRepository.save(emailJob);
                MDC.put("outboxId", emailJob.getId().toString());
                log.info("[Payment] [Outbox] EmailOutbox 작업 생성 완료");

            } catch (Exception e) {
                //페이로드 생성/저장 실패 시, 결제 트랜잭션 전체를 롤백시킴
                log.error("[Payment] [Outbox] EmailOutbox 작업 저장 실패. 결제 트랜잭션을 롤백합니다.", e);
                throw new ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED);
            } finally {
                MDC.remove("outboxId");
            }

            // 6. 응답 데이터 생성
            PaymentConfirmResponse response = buildPaymentConfirmResponse(savedPayment, tossResponse);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Payment] [Confirm] 결제 승인 완료 - orderId={}, paymentKey={}, paymentId={}, amount={}, totalDuration={}ms",
                    orderId, paymentKey, savedPayment.getPaymentId(), amount, totalDuration);

            return response;

        } catch (ServiceException e) {
            long duration = System.currentTimeMillis() - startTime;

            // Payment가 생성되어 있으면 FAILED 상태로 업데이트
            if (savedPayment != null) {
                savedPayment.fail("ServiceException: " + e.getErrorCode());
                paymentRepository.save(savedPayment);
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}",
                        savedPayment.getPaymentId());
            }

            log.error("[Payment] [Confirm] 결제 승인 실패 (ServiceException) - orderId={}, paymentKey={}, error={}, duration={}ms",
                    orderId, paymentKey, e.getErrorCode(), duration);
            throw e;

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;

            // Payment가 생성되어 있으면 FAILED 상태로 업데이트
            if (savedPayment != null) {
                savedPayment.fail("IOException: " + e.getMessage());
                paymentRepository.save(savedPayment);
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}",
                        savedPayment.getPaymentId());
            }

            log.error("[Payment] [Confirm] 토스 API 호출 실패 (IOException) - orderId={}, paymentKey={}, duration={}ms",
                    orderId, paymentKey, duration, e);
            throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Payment가 생성되어 있으면 FAILED 상태로 업데이트
            if (savedPayment != null) {
                savedPayment.fail("UnexpectedException: " + e.getMessage());
                paymentRepository.save(savedPayment);
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}",
                        savedPayment.getPaymentId());
            }

            log.error("[Payment] [Confirm] 결제 승인 실패 (UnexpectedException) - orderId={}, paymentKey={}, duration={}ms",
                    orderId, paymentKey, duration, e);
            throw new ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    /**
     * 웹훅 처리 - 토스페이먼츠에서 결제 상태 변경 알림
     */
    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        String eventType = (String) webhookData.get("eventType");
        Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
        String paymentKey = (String) data.get("paymentKey");
        String status = (String) data.get("status");

        log.info("[Payment] [Webhook] 웹훅 수신 - eventType={}, paymentKey={}, status={}",
                eventType, paymentKey, status);

        // paymentKey로 결제 정보 조회
        Payment payment = paymentRepository.findByTossPaymentKey(paymentKey)
                .orElseGet(() -> {
                    log.warn("[Payment] [Webhook] 결제 정보 없음, 토스에서 동기화 - paymentKey={}", paymentKey);
                    return syncPaymentFromToss(paymentKey);
                });

        // 상태 업데이트
        PaymentStatus newStatus = PaymentStatus.fromString(status);
        if (payment.getPaymentStatus() != newStatus) {
            payment.setPaymentStatus(newStatus);

            if (newStatus == PaymentStatus.DONE) {
                String approvedAtStr = (String) data.get("approvedAt");
                if (approvedAtStr != null) {
                    LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr,
                            DateTimeFormatter.ISO_DATE_TIME);
                    payment.approve(approvedAt);
                }
            }

            paymentRepository.save(payment);
            log.info("[Payment] [Webhook] 결제 상태 업데이트 완료 - paymentId={}, oldStatus={}, newStatus={}",
                    payment.getPaymentId(), payment.getPaymentStatus(), newStatus);
        }
    }

    /**
     * 결제 상태 조회 - 토스 API에서 최신 상태 가져오기
     */
    @Transactional
    public PaymentStatus queryPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        try {
            JsonNode tossResponse = tossApiClient.queryPayment(payment.getTossPaymentKey());
            String status = tossResponse.get("status").asText();
            PaymentStatus newStatus = PaymentStatus.fromString(status);

            // DB 상태 업데이트
            if (payment.getPaymentStatus() != newStatus) {
                payment.setPaymentStatus(newStatus);
                paymentRepository.save(payment);
                log.info("[Payment] [Query] 결제 상태 동기화 - paymentId={}, oldStatus={}, newStatus={}",
                        paymentId, payment.getPaymentStatus(), newStatus);
            }

            return newStatus;

        } catch (IOException e) {
            log.error("[Payment] [Query] 결제 조회 API 호출 실패 - paymentId={}", paymentId, e);
            // API 장애 시 DB의 상태 반환
            return payment.getPaymentStatus();
        }
    }

    /**
     * 결제 취소
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(Long paymentId, PaymentCancelRequest request) {
        log.info("[Payment] [Cancel] 결제 취소 시작 - paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isCancelable()) {
            log.error("[Payment] [Cancel] 취소 불가능한 상태 - paymentId={}, status={}",
                    paymentId, payment.getPaymentStatus());
            throw new ServiceException(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

        try {
            tossApiClient.cancelPayment(payment.getTossPaymentKey(), request.cancelReason());

            payment.cancel(request.cancelReason());
            paymentRepository.save(payment);

            PaymentCancelResponse response = PaymentCancelResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .paymentKey(payment.getTossPaymentKey())
                    .orderId(payment.getTossOrderId())
                    .status(payment.getPaymentStatus())
                    .cancelAmount(payment.getTotalAmount())
                    .cancelReason(request.cancelReason())
                    .canceledAt(payment.getCanceledAt())
                    .build();

            log.info("[Payment] [Cancel] 결제 취소 성공 - paymentId={}, amount={}",
                    paymentId, payment.getTotalAmount());

            return response;

        } catch (IOException e) {
            log.error("[Payment] [Cancel] 토스페이먼츠 취소 API 호출 실패 - paymentId={}", paymentId, e);
            throw new ServiceException(ErrorCode.PAYMENT_CANCEL_API_FAILED);
        }
    }

    /**
     * 마이페이지에서 주문의 결제 내역 단건 조회
     */
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetailByOrder(User user, Long orderId) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getBuyer().getUserId().equals(user.getUserId())) {
            throw new ServiceException(ErrorCode.PAYMENT_UNAUTHORIZED_ACCESS);
        }
        return PaymentDetailResponse.from(payment);
    }

    /**
     * 주문의 모든 상품에 대해 Redis 인기도, purchaseCount 증가
     */
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
                    product.increasePurchaseCount(quantity);

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
            // 인기순 목록 캐시 삭제
            redisProductService.evictPopularListCache();

            long duration = System.currentTimeMillis() - startTime;

            log.info("[Payment] [Popularity] 인기도 증가 완료 - orderId={}, totalItems={}, successCount={}, failCount={}, duration={}ms",
                    order.getOrderId(), order.getOrderItems().size(), successCount, failCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Payment] [Popularity] 인기도 증가 처리 실패 - orderId={}, duration={}ms",
                    order.getOrderId(), duration, e);
        }
    }

    /**
     * 토스 응답으로 Payment 엔티티 업데이트
     */
    private void updatePaymentFromTossResponse(Payment payment, JsonNode tossResponse) {
        String method = tossResponse.get("method").asText();
        String status = tossResponse.get("status").asText();

        payment.setPaymentMethod(PaymentMethod.fromString(method));
        payment.setPaymentStatus(PaymentStatus.fromString(status));

        if (tossResponse.has("mId")) {
            payment.setMid(tossResponse.get("mId").asText());
        }

        if (tossResponse.has("approvedAt")) {
            String approvedAtStr = tossResponse.get("approvedAt").asText();
            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr,
                    DateTimeFormatter.ISO_DATE_TIME);
            payment.approve(approvedAt);
        }
    }

    /**
     * PaymentConfirmResponse 생성
     */
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

    /**
     * 토스에서 결제 정보 동기화 (웹훅 수신 시 Payment가 없는 경우)
     */
    private Payment syncPaymentFromToss(String paymentKey) {
        try {
            JsonNode tossResponse = tossApiClient.queryPayment(paymentKey);
            String tossOrderId = tossResponse.get("orderId").asText();

            Order order = orderRepository.findByTossOrderId(tossOrderId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

            Payment payment = Payment.builder()
                    .tossPaymentKey(paymentKey)
                    .tossOrderId(tossOrderId)
                    .order(order)
                    .buyer(order.getUser())
                    .totalAmount(tossResponse.get("totalAmount").asLong())
                    .paymentMethod(PaymentMethod.fromString(tossResponse.get("method").asText()))
                    .paymentStatus(PaymentStatus.fromString(tossResponse.get("status").asText()))
                    .build();

            if (tossResponse.has("approvedAt")) {
                String approvedAtStr = tossResponse.get("approvedAt").asText();
                LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr,
                        DateTimeFormatter.ISO_DATE_TIME);
                payment.approve(approvedAt);
            }

            return paymentRepository.save(payment);

        } catch (IOException e) {
            log.error("[Payment] [Sync] 토스에서 결제 정보 동기화 실패 - paymentKey={}", paymentKey, e);
            throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);
        }
    }
}