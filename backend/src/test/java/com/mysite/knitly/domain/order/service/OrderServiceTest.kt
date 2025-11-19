package com.mysite.knitly.domain.order.service

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.order.dto.OrderCreateRequest
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

@SpringBootTest(properties = ["spring.profiles.active=test"])
class OrderServiceTest {

    // [개선 1] Nullable(? = null) 제거 및 lateinit var 사용
    // 테스트 클래스에서는 생성자 주입보다 필드 주입이 설정상 편할 때가 많음.
    // lateinit을 쓰면 test 코드 내부에서 !!를 쓸 필요가 없어짐.
    @Autowired lateinit var productRepository: ProductRepository
    @Autowired lateinit var orderRepository: OrderRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var designRepository: DesignRepository
    @Autowired lateinit var paymeRepository: PaymentRepository
    @Autowired lateinit var orderFacade: OrderFacade

    // 테스트 데이터는 lateinit으로 선언하여 setup에서 확실히 초기화
    private lateinit var testUser: User
    private lateinit var testProduct: Product
    private lateinit var testProduct2: Product

    @BeforeEach
    fun setUp() {
        // 외래키 제약조건 등을 고려하여 자식 테이블부터 삭제하거나,
        // 전체 삭제 순서를 보장해야 함. (여기서는 기존 로직 유지)

        paymeRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()
        designRepository.deleteAll()
        userRepository.deleteAll()

        testUser = userRepository.save(
            User(
                email = "test@knitly.com",
                name = "concurrentUser",
                socialId = "concurrentSocialId",
                provider = Provider.GOOGLE,
            )
        )

        // Design 생성
        val testDesign = designRepository.save(
            Design(
                user = testUser,
                designName = "하트 패턴",
                pdfUrl = "/files/2025/10/17/uuid_하트패턴.pdf",
                gridData = "[]",
                designState = DesignState.ON_SALE
            )
        )
        val testDesign2 = designRepository.save(
            Design(
                user = testUser,
                designName = "별 패턴",
                pdfUrl = "/files/2025/10/17/uuid_별패턴.pdf",
                gridData = "[]",
                designState = DesignState.ON_SALE
            )
        )

        // Product 생성
        testProduct = productRepository.save(
            Product(
                title = "한정판 니트",
                design = testDesign,
                user = testUser,
                description = "한정판으로 제작된 특별한 니트입니다.",
                sizeInfo = "Free",
                productCategory = ProductCategory.TOP,
                price = 10000.0,
                purchaseCount = 0,
                likeCount = 50,
                stockQuantity = 10, // 초기 재고 10개
                isDeleted = false
            )
        )

        testProduct2 = productRepository.save(
            Product(
                title = "한정판 코스터",
                design = testDesign2,
                user = testUser,
                description = "한정판으로 제작된 특별한 코스터입니다.",
                sizeInfo = "Free",
                productCategory = ProductCategory.ETC,
                price = 5000.0,
                purchaseCount = 0,
                likeCount = 50,
                stockQuantity = 3, // 초기 재고 3개
                isDeleted = false
            )
        )
    }

    @Test
    @DisplayName("동시에 100개의 주문 요청이 들어와도 실제 주문은 10개만 생성된다.")
    fun concurrent_order_creation_test() {
        // given
        val threadCount = 100
        val initialStock = 10
        val request = OrderCreateRequest(productIds = listOf(testProduct.productId!!))

        runConcurrencyTest(
            threadCount = threadCount,
            stockQuantity = initialStock,
            user = testUser,
            request = request,
            productToCheck = testProduct
        )
    }

    @Test
    @DisplayName("동시에 200개의 주문 요청이 들어와도 실제 주문은 3개만 생성된다.")
    fun concurrent_order_creation_test2() {
        // given
        val threadCount = 200
        val initialStock = 3
        val request = OrderCreateRequest(productIds = listOf(testProduct2.productId!!))

        runConcurrencyTest(
            threadCount = threadCount,
            stockQuantity = initialStock,
            user = testUser,
            request = request,
            productToCheck = testProduct2
        )
    }

    /**
     * [개선 3] 중복되는 동시성 테스트 로직을 함수로 추출
     * - 테스트 코드의 가독성을 높이고 유지보수를 용이하게 함
     */
    private fun runConcurrencyTest(
        threadCount: Int,
        stockQuantity: Int,
        user: User,
        request: OrderCreateRequest,
        productToCheck: Product
    ) {
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // when
        // [개선 4] 0..n 루프 대신 repeat 사용
        repeat(threadCount) {
            executorService.submit {
                try {
                    orderFacade.createOrderWithLock(user, request)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    // 락 획득 실패, 재고 부족 등은 의도된 실패
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executorService.shutdown()

        // then
        // lateinit으로 선언했기 때문에 !! 없이 바로 접근 가능
        // findById가 Optional을 반환하므로, Kotlin에서는 getOrNull() 등을 쓸 수도 있지만
        // 테스트에서는 확실한 값 존재를 위해 orElseThrow()가 적합
        val updatedProduct = productRepository.findById(productToCheck.productId!!).orElseThrow()
        val dbOrderCount = orderRepository.count()

        // [개선 5] KotlinLogging의 문자열 템플릿 활용
        log.info {
            """
            
            ===========================================================
            [Test Summary]
            -----------------------------------------------------------
            - 총 요청 수 : $threadCount
            - 초기 재고  : $stockQuantity
            -----------------------------------------------------------
            - 성공 (Atomic): ${successCount.get()}
            - 실패 (Atomic): ${failCount.get()}
            - DB 생성 주문 수: $dbOrderCount
            - 남은 최종 재고: ${updatedProduct.stockQuantity}
            ===========================================================
            """.trimIndent()
        }

        // 검증
        assertThat(updatedProduct.stockQuantity).isEqualTo(0)
        // DB에 쌓인 주문 수는 재고 수량과 정확히 일치해야 함 (동시성 제어 성공 시)
        // 주의: 이 테스트는 전체 DB count를 세고 있으므로, setUp()에서 deleteAll()이 필수임
        assertThat(dbOrderCount).isEqualTo(stockQuantity.toLong())
        assertThat(successCount.get()).isEqualTo(stockQuantity)
    }
}