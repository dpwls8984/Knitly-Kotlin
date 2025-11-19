package com.mysite.knitly.global.email

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.order.dto.EmailNotificationDto
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.email.service.EmailService
import com.mysite.knitly.global.util.FileStorageService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailServiceIntegrationTest {

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @MockBean
    private lateinit var fileStorageService: FileStorageService

    @Value("\${spring.mail.username}")
    private lateinit var injectedUsername: String

    @Value("\${spring.mail.password:null}")
    private lateinit var injectedPassword: String

    private var testOrderId: Long? = null

    /**
     * 각 테스트(@Test) 실행 전에 호출되어 H2 DB에 테스트용 데이터를 미리 생성합니다.
     */
    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        // Mocking: 파일 저장소에서 PDF 바이트를 로드
        given(fileStorageService.loadFileAsBytes(any())).willReturn(byteArrayOf(1, 2, 3, 4))

        // ------------------ User ------------------
        val testUser = User(
            email = "test@knitly.com",
            name = "김예진",
            socialId = "concurrentSocialId",
            provider = Provider.GOOGLE
        )
        userRepository.save(testUser)

        // ------------------ Design 1 ------------------
        val testDesign = Design(
            user = testUser,
            pdfUrl = "/fake/path/design.pdf",
            designState = DesignState.ON_SALE,
            designName = "테스트 도안",
            gridData = "{\"key\":\"value\"}"
        )
        designRepository.save(testDesign)

        // ------------------ Design 2 ------------------
        val secondDesign = Design(
            user = testUser,
            pdfUrl = "/fake/path/hat_design.pdf",
            designState = DesignState.ON_SALE,
            designName = "겨울 니트 모자 도안",
            gridData = "{\"pattern\":\"hat\"}"
        )
        designRepository.save(secondDesign)

        // ------------------ Product 1 ------------------
        val testProduct = Product(
            title = "테스트용 니트 상품",
            description = "이메일 테스트용 상품 설명입니다.",
            productCategory = ProductCategory.TOP,
            sizeInfo = "Free Size",
            price = 15000.0,
            user = testUser,
            design = testDesign,
            purchaseCount = 0,
            isDeleted = false,
            stockQuantity = 0,
            likeCount = 0
        )
        productRepository.save(testProduct)

        // ------------------ Product 2 ------------------
        val secondProduct = Product(
            title = "겨울 니트 모자 도안",
            description = "따뜻한 겨울 니트 모자 패턴",
            productCategory = ProductCategory.ETC,
            sizeInfo = "Free Size",
            price = 9800.0,
            user = testUser,
            design = secondDesign,
            purchaseCount = 0,
            isDeleted = false,
            stockQuantity = 50,
            likeCount = 0
        )
        productRepository.save(secondProduct)

        // ------------------ Order & OrderItems ------------------
        val testOrder = Order(
            user = testUser,
            totalPrice = 0.0,
            tossOrderId = UUID.randomUUID().toString()
        )

        val testOrderItem = OrderItem(
            product = testProduct,
            orderPrice = testProduct.price,
            quantity = 1
        )

        val secondOrderItem = OrderItem(
            product = secondProduct,
            orderPrice = secondProduct.price,
            quantity = 1
        )

        testOrder.addOrderItem(testOrderItem)
        testOrder.addOrderItem(secondOrderItem) // totalPrice = 24800.0

        orderRepository.save(testOrder)

        this.testOrderId = testOrder.orderId // ID는 DB 저장 후 생성됨

        // ------------------ Payment ------------------
        val testPayment = Payment(
            order = testOrder,
            buyer = testUser,
            tossPaymentKey = "test-payment-key",
            tossOrderId = testOrder.tossOrderId,
            totalAmount = testOrder.totalPrice.toLong(),
            paymentMethod = PaymentMethod.EASY_PAY,
            paymentStatus = PaymentStatus.DONE
        )
        paymentRepository.save(testPayment)
    }

    @Test
    @DisplayName("실제 Gmail 계정으로 주문 완료 이메일 발송 테스트 (통합)")
    fun sendOrderConfirmationEmail_IntegrationTest() {

        println("==================================================")
        println("### DEBUG USERNAME: $injectedUsername")
        println(
            "### DEBUG PASSWORD: " + if (injectedPassword.isNotEmpty()) injectedPassword.substring(
                0,
                4
            ) + "..." else "null"
        )
        println("==================================================")

        val orderId = testOrderId
            ?: throw RuntimeException("Order ID가 DB에 의해 생성되지 않았습니다.")

        val testOrder = orderRepository.findById(orderId)
            .orElseThrow { RuntimeException("테스트용 주문(ID: $orderId)을 찾을 수 없습니다.") }

        val testDto = EmailNotificationDto(
            orderId = testOrder.orderId!!,
            userId = testOrder.user!!.userId,
            userEmail = "test@knitly.com"
        )

        // EmailService.kt의 포맷 인수 갯수(7개)가 수정되었다면, 이 테스트는 통과해야 합니다.
        assertDoesNotThrow {
            emailService.sendOrderConfirmationEmail(testDto)
        }
    }
}