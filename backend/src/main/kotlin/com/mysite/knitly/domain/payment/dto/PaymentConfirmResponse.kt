import com.fasterxml.jackson.annotation.JsonInclude
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentConfirmResponse(
    val paymentId: Long?,
    val paymentKey: String?,
    val orderId: String,
    val orderName: String? = null,
    val method: PaymentMethod,
    val totalAmount: Long,
    val status: PaymentStatus,
    val requestedAt: LocalDateTime?,
    val approvedAt: LocalDateTime? = null,
    val mid: String? = null,
    val card: CardInfo? = null,
    val virtualAccount: VirtualAccountInfo? = null,
    val easyPay: EasyPayInfo? = null
) {

    data class CardInfo(
        val company: String,
        val number: String,
        val installmentPlanMonths: String,
        val approveNo: String,
        val ownerType: String
    )

    data class VirtualAccountInfo(
        val accountNumber: String,
        val bankCode: String,
        val customerName: String,
        val dueDate: LocalDateTime
    )

    data class EasyPayInfo(
        val provider: String,
        val amount: Long
    )

    companion object {
        /**
         * Payment 엔티티로부터 Response 생성
         */
        @JvmStatic
        fun from(payment: Payment): PaymentConfirmResponse {
            return PaymentConfirmResponse(
                paymentId = payment.paymentId,
                paymentKey = payment.tossPaymentKey,
                orderId = payment.tossOrderId,
                method = payment.paymentMethod,
                totalAmount = payment.totalAmount,
                status = payment.paymentStatus,
                requestedAt = payment.requestedAt,
                approvedAt = payment.approvedAt,
                mid = payment.mid
            )
        }
    }
}