//package com.mysite.knitly.global.email
//
//import com.mysite.knitly.domain.design.entity.Design
//import com.mysite.knitly.domain.design.entity.DesignState
//import com.mysite.knitly.domain.design.repository.DesignRepository
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto
//import com.mysite.knitly.domain.order.entity.Order
//import com.mysite.knitly.domain.order.entity.Order.tossOrderId
//import com.mysite.knitly.domain.order.entity.Order.user
//import com.mysite.knitly.domain.order.entity.OrderItem
//import com.mysite.knitly.domain.order.repository.OrderRepository
//import com.mysite.knitly.domain.payment.entity.Payment
//import com.mysite.knitly.domain.payment.entity.PaymentMethod
//import com.mysite.knitly.domain.payment.entity.PaymentStatus
//import com.mysite.knitly.domain.payment.repository.PaymentRepository
//import com.mysite.knitly.domain.product.product.entity.Product
//import com.mysite.knitly.domain.product.product.entity.Product.price
//import com.mysite.knitly.domain.product.product.entity.Product.productId
//import com.mysite.knitly.domain.product.product.entity.Product.stockQuantity
//import com.mysite.knitly.domain.product.product.entity.ProductCategory
//import com.mysite.knitly.domain.product.product.repository.ProductRepository
//import com.mysite.knitly.domain.user.entity.Provider
//import com.mysite.knitly.domain.user.entity.User
//import com.mysite.knitly.domain.user.repository.UserRepository
//import com.mysite.knitly.global.email.service.EmailService
//import com.mysite.knitly.global.util.FileStorageService
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.transaction.annotation.Transactional
//import java.io.IOException
//import java.util.*
//
//
//// (UUID 임포트 추가)
//@SpringBootTest
//@ActiveProfiles("test")
//@Transactional
//internal class EmailServiceIntegrationTest {
//    @Autowired
//    private val emailService: EmailService? = null
//
//    @Autowired
//    private val orderRepository: OrderRepository? = null
//
//    @Autowired
//    private val userRepository: UserRepository? = null
//
//    @Autowired
//    private val productRepository: ProductRepository? = null
//
//    @Autowired
//    private val designRepository: DesignRepository? = null
//
//    @Autowired
//    private val paymentRepository: PaymentRepository? = null
//
//    @MockBean
//    private val fileStorageService: FileStorageService? = null
//
//    @Value("\${spring.mail.username}")
//    private val injectedUsername: String? = null
//
//    @Value("\${spring.mail.password}")
//    private val injectedPassword: String? = null
//
//    private var testOrderId: Long? = null
//
//    /**
//     * 각 테스트(@Test) 실행 전에 호출되어
//     * H2 DB에 테스트용 데이터를 미리 생성합니다.
//     */
//    @BeforeEach
//    @Throws(IOException::class)
//    fun setUp() {
//        Mockito.`when`(fileStorageService!!.loadFileAsBytes(ArgumentMatchers.anyString()))
//            .thenReturn(byteArrayOf(1, 2, 3, 4))
//
//        val testUser = User.builder()
//            .email("test@knitly.com")
//            .name("김예진")
//            .socialId("concurrentSocialId")
//            .provider(Provider.GOOGLE)
//            .build()
//
//        userRepository!!.save(testUser)
//
//        val testDesign = Design.builder()
//            .user(testUser)
//            .pdfUrl("/fake/path/design.pdf")
//            .designState(DesignState.ON_SALE)
//            .designName("테스트 도안")
//            .gridData("{\"key\":\"value\"}")
//            .build()
//        designRepository!!.save(testDesign)
//
//        val secondDesign = Design.builder()
//            .user(testUser)
//            .pdfUrl("/fake/path/hat_design.pdf")
//            .designState(DesignState.ON_SALE)
//            .designName("겨울 니트 모자 도안")
//            .gridData("{\"pattern\":\"hat\"}")
//            .build()
//        designRepository.save(secondDesign)
//
//        val testProduct: Product = Product.builder()
//            .productId(null) // ID는 자동 생성
//            .title("테스트용 니트 상품")
//            .description("이메일 테스트용 상품 설명입니다.")
//            .productCategory(ProductCategory.TOP)
//            .sizeInfo("Free Size")
//            .price(15000.0)
//            .user(testUser)
//            .purchaseCount(0)
//            .isDeleted(false)
//            .stockQuantity(0)
//            .likeCount(0)
//            .design(testDesign)
//            .avgReviewRating(0.0)
//            .reviewCount(0)
//            .build()
//        productRepository!!.save(testProduct)
//
//        val secondProduct: Product = Product.builder()
//            .title("겨울 니트 모자 도안")
//            .description("따뜻한 겨울 니트 모자 패턴")
//            .productCategory(ProductCategory.ETC)
//            .sizeInfo("Free Size")
//            .price(9800.0)
//            .user(testUser)
//            .purchaseCount(0)
//            .isDeleted(false)
//            .stockQuantity(50)
//            .likeCount(0)
//            .design(secondDesign)
//            .avgReviewRating(0.0)
//            .reviewCount(0)
//            .build()
//        productRepository.save(secondProduct)
//
//        val secondOrderItem: OrderItem = OrderItem.builder()
//            .product(secondProduct)
//            .orderPrice(secondProduct.price)
//            .quantity(1)
//            .build()
//
//        val testOrderItem: OrderItem = OrderItem.builder()
//            .product(testProduct)
//            .orderPrice(testProduct.price)
//            .quantity(1)
//            .build()
//
//        val testOrder: Order = Order.builder()
//            .user(testUser)
//            .tossOrderId(UUID.randomUUID().toString())
//            .build()
//
//        testOrder.addOrderItem(testOrderItem)
//        testOrder.addOrderItem(secondOrderItem)
//
//        orderRepository!!.save(testOrder)
//
//        this.testOrderId = testOrder.orderId
//
//        val testPayment = Payment.builder()
//            .order(testOrder)
//            .buyer(testUser)
//            .tossPaymentKey("test-payment-key")
//            .tossOrderId(testOrder.tossOrderId)
//            .totalAmount(testOrder.totalPrice.longValue())
//            .paymentMethod(PaymentMethod.EASY_PAY)
//            .paymentStatus(PaymentStatus.DONE)
//            .build()
//
//        paymentRepository!!.save(testPayment)
//    }
//
//    @Test
//    @DisplayName("실제 Gmail 계정으로 주문 완료 이메일 발송 테스트")
//    fun sendOrderConfirmationEmail_IntegrationTest() {
//        println("==================================================")
//        println("### DEBUG USERNAME: $injectedUsername")
//        println(
//            "### DEBUG PASSWORD: " + (if (injectedPassword != null) injectedPassword.substring(
//                0,
//                4
//            ) + "..." else "null")
//        )
//        println("==================================================")
//
//        val testOrder = orderRepository!!.findById(testOrderId!!)
//            .orElseThrow { RuntimeException("테스트용 주문(ID: $testOrderId)을 찾을 수 없습니다.") }
//
//        val testDto = EmailNotificationDto(
//            testOrder.orderId!!,
//            testOrder.user!!.userId,
//            "test@knitly.com"
//        )
//
//        Assertions.assertDoesNotThrow { emailService!!.sendOrderConfirmationEmail(testDto) }
//    }
//}