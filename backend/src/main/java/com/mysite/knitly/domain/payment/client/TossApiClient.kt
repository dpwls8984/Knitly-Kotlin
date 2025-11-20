package com.mysite.knitly.domain.payment.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class TossApiClient(
    private val objectMapper: ObjectMapper
) {

    @Value("\${payment.toss.secret-key}")
    private lateinit var tossSecretKey: String

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    @Throws(IOException::class)
    fun confirmPayment(request: PaymentConfirmRequest): JsonNode {
        log.info(
            "[TossAPI] [Retry] 결제 승인 API 호출 시도 - orderId={}, paymentKey={}",
            request.orderId, request.paymentKey
        )

        val authorization = createBasicAuthHeader(tossSecretKey)
        val requestBody = objectMapper.writeValueAsString(request)

        val connection = URL(TOSS_PAYMENT_CONFIRM_URL).openConnection() as HttpURLConnection

        return try {
            connection.setRequestProperty("Authorization", authorization)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray(StandardCharsets.UTF_8))
            }

            val response = handleApiResponse(connection, request.orderId, request.paymentKey)
            log.info(
                "[TossAPI] [Retry] 결제 승인 API 성공 - orderId={}, paymentKey={}",
                request.orderId, request.paymentKey
            )

            response
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    fun queryPayment(paymentKey: String): JsonNode {
        log.info("[TossAPI] 결제 조회 API 호출 - paymentKey={}", paymentKey)

        val authorization = createBasicAuthHeader(tossSecretKey)
        val queryUrl = TOSS_PAYMENT_QUERY_URL.format(paymentKey)

        val connection = URL(queryUrl).openConnection() as HttpURLConnection

        return try {
            connection.setRequestProperty("Authorization", authorization)
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            handleApiResponse(connection, "QUERY", paymentKey)
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    fun cancelPayment(paymentKey: String, cancelReason: String): JsonNode {
        log.info("[TossAPI] 결제 취소 API 호출 - paymentKey={}", paymentKey)

        val authorization = createBasicAuthHeader(tossSecretKey)
        val requestBody = mapOf("cancelReason" to cancelReason)
        val requestBodyJson = objectMapper.writeValueAsString(requestBody)
        val cancelUrl = TOSS_PAYMENT_CANCEL_URL.format(paymentKey)

        val connection = URL(cancelUrl).openConnection() as HttpURLConnection

        return try {
            connection.setRequestProperty("Authorization", authorization)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            connection.outputStream.use { os ->
                os.write(requestBodyJson.toByteArray(StandardCharsets.UTF_8))
            }

            handleApiResponse(connection, "CANCEL", paymentKey)
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun handleApiResponse(
        connection: HttpURLConnection,
        orderId: String,
        paymentKey: String
    ): JsonNode {
        val responseCode = connection.responseCode
        val isSuccess = responseCode == 200

        log.debug(
            "[TossAPI] 응답 수신 - orderId={}, paymentKey={}, responseCode={}",
            orderId, paymentKey, responseCode
        )

        val inputStream = (if (isSuccess) connection.inputStream else connection.errorStream)
            ?: throw IOException("응답 스트림이 null입니다.")

        return inputStream.use { stream ->
            val responseJson = objectMapper.readTree(stream)

            if (!isSuccess) {
                val errorCode = responseJson.get("code")?.asText() ?: "UNKNOWN"
                val errorMessage = responseJson.get("message")?.asText() ?: "API 호출 실패"

                log.error(
                    "[TossAPI] API 실패 - orderId={}, paymentKey={}, responseCode={}, errorCode={}, errorMessage={}",
                    orderId, paymentKey, responseCode, errorCode, errorMessage
                )

                throw ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED)
            }

            log.debug("[TossAPI] API 성공 - orderId={}, paymentKey={}", orderId, paymentKey)
            responseJson
        }
    }

    private fun createBasicAuthHeader(secretKey: String): String {
        val credentials = "$secretKey:"
        val encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encodedCredentials"
    }

    companion object {
        private val log = LoggerFactory.getLogger(TossApiClient::class.java)

        private const val TOSS_PAYMENT_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm"
        private const val TOSS_PAYMENT_CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel"
        private const val TOSS_PAYMENT_QUERY_URL = "https://api.tosspayments.com/v1/payments/%s"

        private const val CONNECTION_TIMEOUT = 5000 // 5초
        private const val READ_TIMEOUT = 10000 // 10초
    }
}
