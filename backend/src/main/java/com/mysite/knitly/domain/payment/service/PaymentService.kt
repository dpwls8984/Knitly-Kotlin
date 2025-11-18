package com.mysite.knitly.domain.payment.service

import PaymentConfirmResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.client.TossApiClient
import com.mysite.knitly.domain.payment.dto.*
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.email.repository.EmailOutboxRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PaymentService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val redisProductService: RedisProductService,
    private val tossApiClient: TossApiClient,
    private val objectMapper: ObjectMapper,
    private val emailOutboxRepository: EmailOutboxRepository
) {

    /**
     * 결제 승인 처리
     * 1. READY 상태의 Payment 조회
     * 2. IN_PROGRESS 상태로 변경 + paymentKey 업데이트
     * 3. 토스 API 호출 (재시도 자동 적용)
     * 4. 성공 시 DONE으로 업데이트, 실패 시 FAILED로 업데이트
     */
    @Transactional
    fun confirmPayment(request: PaymentConfirmRequest): PaymentConfirmResponse {
        val startTime = System.currentTimeMillis()
        val orderId = request.orderId
        val paymentKey = request.paymentKey
        val amount = request.amount

        log.info(
            "[Payment] [Confirm] 결제 승인 시작 - orderId={}, paymentKey={}, amount={}",
            orderId, paymentKey, amount
        )

        var savedPayment: Payment? = null

        try {
            // 1. 주문 정보 조회 및 검증
            val order = orderRepository.findByTossOrderId(orderId)
                ?: run {
                    log.warn("[Payment] [Confirm] 주문을 찾을 수 없음 - orderId={}", orderId)
                    throw ServiceException(ErrorCode.ORDER_NOT_FOUND)
                }

            val userId = order.user?.userId
            log.debug(
                "[Payment] [Confirm] 주문 정보 조회 완료 - orderId={}, userId={}, orderAmount={}",
                orderId, userId, order.totalPrice
            )

            // 2. 주문 금액 검증
            val orderAmount = order.totalPrice.toLong()
            if (orderAmount != amount) {
                log.warn(
                    "[Payment] [Confirm] 결제 금액 불일치 - orderId={}, orderAmount={}, requestAmount={}",
                    orderId, orderAmount, amount
                )
                throw ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
            }

            // 3. READY 상태의 Payment 조회
            savedPayment = paymentRepository.findByOrder_OrderId(order.orderId)
                ?: run {
                    log.error("[Payment] [Confirm] Payment가 존재하지 않음 - orderId={}", orderId)
                    throw ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
                }

            // READY 상태가 아니면 에러
            if (savedPayment.paymentStatus != PaymentStatus.READY) {
                log.warn(
                    "[Payment] [Confirm] Payment가 READY 상태가 아님 - paymentId={}, status={}",
                    savedPayment.paymentId, savedPayment.paymentStatus
                )

                throw when (savedPayment.paymentStatus) {
                    PaymentStatus.DONE -> ServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS)
                    else -> ServiceException(ErrorCode.INVALID_PAYMENT_STATUS)
                }
            }

            // 4. Payment를 IN_PROGRESS 상태로 변경 + paymentKey 저장
            savedPayment.tossPaymentKey = paymentKey
            savedPayment.paymentStatus = PaymentStatus.IN_PROGRESS
            savedPayment = paymentRepository.save(savedPayment)

            log.info(
                "[Payment] [Confirm] Payment 상태 변경 - paymentId={}, READY → IN_PROGRESS",
                savedPayment.paymentId
            )

            // 5. 토스페이먼츠 결제 승인 API 호출 (재시도 자동 적용)
            val apiStartTime = System.currentTimeMillis()
            val tossResponse = tossApiClient.confirmPayment(request)
            val apiDuration = System.currentTimeMillis() - apiStartTime

            log.info(
                "[Payment] [Confirm] 토스 API 호출 완료 - orderId={}, paymentKey={}, apiDuration={}ms",
                orderId, paymentKey, apiDuration
            )

            // 6. Payment 엔티티 업데이트 (DONE 상태로 변경)
            updatePaymentFromTossResponse(savedPayment, tossResponse)
            savedPayment = paymentRepository.save(savedPayment)

            log.debug(
                "[Payment] [Confirm] Payment 엔티티 업데이트 완료 - paymentId={}, status=DONE",
                savedPayment.paymentId
            )

            // 7. 결제 완료 시 redis 상품 인기도 증가
            incrementProductPopularity(order)

            // 8. EmailOutbox 생성
//            MDC.put("orderId", order.orderId.toString())
//            MDC.put("userId", order.user.userId.toString())
//
//            try {
//                log.info("[Payment] [Outbox] EmailOutbox 작업 생성 시작")
//
//                val user = order.user
//                val emailDto = EmailNotificationDto(order.orderId, user?.userId ?: , user.email)
//                val payload = objectMapper.writeValueAsString(emailDto)
//
//                val emailJob = EmailOutbox(payload = payload)
//                emailOutboxRepository.save(emailJob)
//                MDC.put("outboxId", emailJob.id.toString())
//
//                log.info("[Payment] [Outbox] EmailOutbox 작업 생성 완료")
//            } catch (e: Exception) {
//                // 페이로드 생성/저장 실패 시, 결제 트랜잭션 전체를 롤백
//                log.error("[Payment] [Outbox] EmailOutbox 작업 저장 실패. 결제 트랜잭션을 롤백합니다.", e)
//                throw ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED)
//            } finally {
//                MDC.remove("outboxId")
//                MDC.remove("orderId")
//                MDC.remove("userId")
//            }

            // 9. 응답 데이터 생성
            val response = buildPaymentConfirmResponse(savedPayment, tossResponse)

            val totalDuration = System.currentTimeMillis() - startTime
            log.info(
                "[Payment] [Confirm] 결제 승인 완료 - orderId={}, paymentKey={}, paymentId={}, amount={}, totalDuration={}ms",
                orderId, paymentKey, savedPayment.paymentId, amount, totalDuration
            )

            return response

        } catch (e: ServiceException) {
            val duration = System.currentTimeMillis() - startTime

            savedPayment?.let {
                it.fail("ServiceException: ${e.errorCode}")
                paymentRepository.save(it)
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}", it.paymentId)
            }

            log.error(
                "[Payment] [Confirm] 결제 승인 실패 (ServiceException) - orderId={}, paymentKey={}, error={}, duration={}ms",
                orderId, paymentKey, e.errorCode, duration
            )
            throw e

        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime

            savedPayment?.let {
                it.fail("IOException: ${e.message}")
                paymentRepository.save(it)
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}", it.paymentId)
            }

            log.error(
                "[Payment] [Confirm] 토스 API 호출 실패 (IOException) - orderId={}, paymentKey={}, duration={}ms",
                orderId, paymentKey, duration, e
            )
            throw ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED)

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime

            savedPayment?.let {
                it.fail("UnexpectedException: ${e.message}")
                paymentRepository.save(it)
                log.warn("[Payment] [Confirm] Payment를 FAILED 상태로 업데이트 - paymentId={}", it.paymentId)
            }

            log.error(
                "[Payment] [Confirm] 결제 승인 실패 (UnexpectedException) - orderId={}, paymentKey={}, duration={}ms",
                orderId, paymentKey, duration, e
            )
            throw ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED)
        }
    }

    /**
     * 웹훅 처리 - 토스페이먼츠에서 결제 상태 변경 알림
     */
    @Transactional
    fun handleWebhook(webhookData: Map<String, Any>) {
        val eventType = webhookData["eventType"] as String
        @Suppress("UNCHECKED_CAST")
        val data = webhookData["data"] as Map<String, Any>
        val paymentKey = data["paymentKey"] as String
        val status = data["status"] as String

        log.info(
            "[Payment] [Webhook] 웹훅 수신 - eventType={}, paymentKey={}, status={}",
            eventType, paymentKey, status
        )

        // paymentKey로 결제 정보 조회
        val payment = paymentRepository.findByTossPaymentKey(paymentKey)
            ?: run {
                log.warn("[Payment] [Webhook] 결제 정보 없음, 토스에서 동기화 - paymentKey={}", paymentKey)
                syncPaymentFromToss(paymentKey)
            }

        val newStatus = PaymentStatus.fromString(status)
        val oldStatus = payment.paymentStatus

        if (oldStatus != newStatus) {
            payment.paymentStatus = newStatus

            if (newStatus == PaymentStatus.DONE) {
                val approvedAtStr = data["approvedAt"] as? String
                approvedAtStr?.let {
                    val approvedAt = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                    payment.approve(approvedAt)
                }
            }

            paymentRepository.save(payment)
            log.info(
                "[Payment] [Webhook] 결제 상태 업데이트 완료 - paymentId={}, oldStatus={}, newStatus={}",
                payment.paymentId, oldStatus, newStatus
            )
        }
    }

    /**
     * 결제 상태 조회 - 토스 API에서 최신 상태 가져오기
     */
    @Transactional
    fun queryPaymentStatus(paymentId: Long): PaymentStatus {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { ServiceException(ErrorCode.PAYMENT_NOT_FOUND) }

        val tossPaymentKey = payment.tossPaymentKey
            ?: throw ServiceException(ErrorCode.PAYMENT_NOT_FOUND)

        return try {
            val tossResponse = tossApiClient.queryPayment(tossPaymentKey)
            val status = tossResponse.get("status").asText()
            val newStatus = PaymentStatus.fromString(status)
            val oldStatus = payment.paymentStatus

            // DB 상태 업데이트
            if (oldStatus != newStatus) {
                payment.paymentStatus = newStatus
                paymentRepository.save(payment)
                log.info(
                    "[Payment] [Query] 결제 상태 동기화 - paymentId={}, oldStatus={}, newStatus={}",
                    paymentId, oldStatus, newStatus
                )
            }

            newStatus
        } catch (e: IOException) {
            log.error("[Payment] [Query] 결제 조회 API 호출 실패 - paymentId={}", paymentId, e)
            // API 장애 시 DB의 상태 반환
            payment.paymentStatus
        }
    }

    /**
     * 결제 취소
     */
    @Transactional
    fun cancelPayment(paymentId: Long, request: PaymentCancelRequest): PaymentCancelResponse {
        log.info("[Payment] [Cancel] 결제 취소 시작 - paymentId={}", paymentId)

        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { ServiceException(ErrorCode.PAYMENT_NOT_FOUND) }

        if (!payment.isCancelable) {
            log.error(
                "[Payment] [Cancel] 취소 불가능한 상태 - paymentId={}, status={}",
                paymentId, payment.paymentStatus
            )
            throw ServiceException(ErrorCode.PAYMENT_NOT_CANCELABLE)
        }

        val paymentKey = payment.tossPaymentKey
            ?: throw ServiceException(ErrorCode.PAYMENT_NOT_FOUND)

        return try {
            tossApiClient.cancelPayment(paymentKey, request.cancelReason)

            payment.cancel(request.cancelReason)
            paymentRepository.save(payment)

            val response = PaymentCancelResponse(
                paymentId = requireNotNull(payment.paymentId) { "paymentId is null" },
                paymentKey = paymentKey,
                orderId = payment.tossOrderId,
                status = payment.paymentStatus,
                cancelAmount = payment.totalAmount,
                cancelReason = request.cancelReason,
                canceledAt = requireNotNull(payment.canceledAt) { "canceledAt is null after cancel()" }
            )

            log.info(
                "[Payment] [Cancel] 결제 취소 성공 - paymentId={}, amount={}",
                paymentId, payment.totalAmount
            )

            response
        } catch (e: IOException) {
            log.error("[Payment] [Cancel] 토스페이먼츠 취소 API 호출 실패 - paymentId={}", paymentId, e)
            throw ServiceException(ErrorCode.PAYMENT_CANCEL_API_FAILED)
        }
    }

    /**
     * 마이페이지에서 주문의 결제 내역 단건 조회
     */
    @Transactional(readOnly = true)
    fun getPaymentDetailByOrder(user: User, orderId: Long): PaymentDetailResponse {
        val payment = paymentRepository.findByOrder_OrderId(orderId)
            ?: throw ServiceException(ErrorCode.PAYMENT_NOT_FOUND)

        if (payment.buyer?.userId != user.userId) {
            throw ServiceException(ErrorCode.PAYMENT_UNAUTHORIZED_ACCESS)
        }
        return PaymentDetailResponse.from(payment)
    }

    /**
     * 주문의 모든 상품에 대해 Redis 인기도, purchaseCount 증가
     */
    private fun incrementProductPopularity(order: Order) {
        val startTime = System.currentTimeMillis()

        try {
            var successCount = 0
            var failCount = 0

            order.orderItems.forEach { orderItem ->
                val product = orderItem.product
                val productId = product?.productId
                val quantity = orderItem.quantity

                try {
                    product?.increasePurchaseCount(quantity)

                    repeat(quantity) {
                        redisProductService.incrementPurchaseCount(requireNotNull(productId) { "productId is null" })
                    }

                    successCount++

                    log.debug(
                        "[Payment] [Popularity] 상품 인기도 증가 완료 - productId={}, quantity={}, newPurchaseCount={}",
                        productId, quantity, product?.purchaseCount
                    )
                } catch (e: Exception) {
                    failCount++
                    log.error(
                        "[Payment] [Popularity] 상품 인기도 증가 실패 - productId={}, quantity={}",
                        productId, quantity, e
                    )
                }
            }

            // 인기순 목록 캐시 삭제
            redisProductService.evictPopularListCache()

            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Payment] [Popularity] 인기도 증가 완료 - orderId={}, totalItems={}, successCount={}, failCount={}, duration={}ms",
                order.orderId, order.orderItems.size, successCount, failCount, duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Payment] [Popularity] 인기도 증가 처리 실패 - orderId={}, duration={}ms",
                order.orderId, duration, e
            )
        }
    }

    /**
     * 토스 응답으로 Payment 엔티티 업데이트
     */
    private fun updatePaymentFromTossResponse(payment: Payment, tossResponse: JsonNode) {
        val method = tossResponse.get("method").asText()
        val status = tossResponse.get("status").asText()

        payment.paymentMethod = PaymentMethod.fromString(method)
        payment.paymentStatus = PaymentStatus.fromString(status)

        tossResponse.get("mId")?.asText()?.let { payment.mid = it }

        tossResponse.get("approvedAt")?.asText()?.let { approvedAtStr ->
            val approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME)
            payment.approve(approvedAt)
        }
    }

    /**
     * PaymentConfirmResponse 생성
     */
    private fun buildPaymentConfirmResponse(payment: Payment, tossResponse: JsonNode): PaymentConfirmResponse {
        val paymentId = requireNotNull(payment.paymentId) { "paymentId is null" }
        val paymentKey = requireNotNull(payment.tossPaymentKey) { "tossPaymentKey is null" }
        val requestedAt = requireNotNull(payment.requestedAt) { "requestedAt is null" }

        return PaymentConfirmResponse(
            paymentId = paymentId,
            paymentKey = paymentKey,
            orderId = payment.tossOrderId,
            orderName = tossResponse.get("orderName")?.asText(),
            method = payment.paymentMethod,
            totalAmount = payment.totalAmount,
            status = payment.paymentStatus,
            requestedAt = requestedAt,
            approvedAt = payment.approvedAt,
            mid = tossResponse.get("mId")?.asText(),
            card = tossResponse.get("card")?.let { buildCardInfo(it) },
            virtualAccount = tossResponse.get("virtualAccount")?.let { buildVirtualAccountInfo(it) },
            easyPay = tossResponse.get("easyPay")?.let { buildEasyPayInfo(it) }
        )
    }

    private fun buildCardInfo(card: JsonNode) =
        PaymentConfirmResponse.CardInfo(
            company = card.get("company")?.asText() ?: "",
            number = card.get("number")?.asText() ?: "",
            installmentPlanMonths = card.get("installmentPlanMonths")?.asText() ?: "",
            approveNo = card.get("approveNo")?.asText() ?: "",
            ownerType = card.get("ownerType")?.asText() ?: ""
        )

    private fun buildVirtualAccountInfo(va: JsonNode) =
        PaymentConfirmResponse.VirtualAccountInfo(
            accountNumber = va.get("accountNumber")?.asText() ?: "",
            bankCode = va.get("bankCode")?.asText() ?: "",
            customerName = va.get("customerName")?.asText() ?: "",
            dueDate = va.get("dueDate")?.asText()?.let {
                LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            } ?: LocalDateTime.now()
        )

    private fun buildEasyPayInfo(easyPay: JsonNode) =
        PaymentConfirmResponse.EasyPayInfo(
            provider = easyPay.get("provider")?.asText() ?: "",
            amount = easyPay.get("amount")?.asLong() ?: 0L
        )

    /**
     * 토스에서 결제 정보 동기화 (웹훅 수신 시 Payment가 없는 경우)
     */
    private fun syncPaymentFromToss(paymentKey: String): Payment {
        return try {
            val tossResponse = tossApiClient.queryPayment(paymentKey)
            val tossOrderId = tossResponse.get("orderId").asText()

            val order = orderRepository.findByTossOrderId(tossOrderId)
                ?: throw ServiceException(ErrorCode.ORDER_NOT_FOUND)

            val payment = Payment(
                tossPaymentKey = paymentKey,
                tossOrderId = tossOrderId,
                order = order,
                buyer = order.user,
                totalAmount = tossResponse.get("totalAmount").asLong(),
                paymentMethod = PaymentMethod.fromString(tossResponse.get("method").asText()),
                paymentStatus = PaymentStatus.fromString(tossResponse.get("status").asText())
            )

            tossResponse.get("approvedAt")?.asText()?.let { approvedAtStr ->
                val approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME)
                payment.approve(approvedAt)
            }

            paymentRepository.save(payment)
        } catch (e: IOException) {
            log.error("[Payment] [Sync] 토스에서 결제 정보 동기화 실패 - paymentKey={}", paymentKey, e)
            throw ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentService::class.java)
    }
}
