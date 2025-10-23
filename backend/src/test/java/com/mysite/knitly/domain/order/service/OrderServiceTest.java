package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
class OrderServiceTest {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceTest.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DesignRepository designRepository;

    @Autowired
    private OrderFacade orderFacade;

    private Product testProduct;

    private Product testProduct2;
    private User testUser;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        designRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(
                User.builder()
                        .email("test@knitly.com")
                        .name("concurrentUser")
                        .socialId("concurrentSocialId")
                        .provider(Provider.GOOGLE)
                        .build()
        );

        Design testDesign = designRepository.save(
                Design.builder()
                        .user(testUser)
                        .designName("하트 패턴")
                        .pdfUrl("/files/2025/10/17/uuid_하트패턴.pdf")
                        .gridData("[]")
                        .designState(DesignState.ON_SALE)
                        .build()
        );

        testProduct = productRepository.save(
                Product.builder()
                        .title("한정판 니트")
                        .design(testDesign)
                        .user(testUser)
                        .description("한정판으로 제작된 특별한 니트입니다.")
                        .sizeInfo("Free")
                        .productCategory(ProductCategory.TOP)
                        .price(10000.0)
                        .purchaseCount(0)
                        .likeCount(50)
                        .stockQuantity(10)
                        .isDeleted(false)
                        .build()
        );

        Design testDesign2 = designRepository.save(
                Design.builder()
                        .user(testUser)
                        .designName("별 패턴")
                        .pdfUrl("/files/2025/10/17/uuid_별패턴.pdf")
                        .gridData("[]")
                        .designState(DesignState.ON_SALE)
                        .build()
        );

        testProduct2 = productRepository.save(
                Product.builder()
                        .title("한정판 코스터")
                        .design(testDesign2)
                        .user(testUser)
                        .description("한정판으로 제작된 특별한 코스터입니다.")
                        .sizeInfo("Free")
                        .productCategory(ProductCategory.ETC)
                        .price(5000.0)
                        .purchaseCount(0)
                        .likeCount(50)
                        .stockQuantity(3)
                        .isDeleted(false)
                        .build()
        );
    }

    @Test
    @DisplayName("동시에 100개의 주문 요청이 들어와도 실제 주문은 10개만 생성된다.")
    void concurrent_order_creation_test() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0); // 성공/실패 추적용
        AtomicInteger failCount = new AtomicInteger(0); // 실패 횟수도 추적

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = new OrderCreateRequest(
                            List.of(testProduct.getProductId())
                    );
                    // Facade를 통해 주문 생성 로직 호출
                    orderFacade.createOrderWithLock(testUser, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 락 획득 실패, 재고 부족 등의 예외는 의도된 실패이므로 무시
                    // 실패시 카운트 증가
                    failCount.incrementAndGet();
                    // 실패 로그는 DEBUG 레벨로 남겨서 평소엔 안 보이게 처리
                    log.debug("Order failed as expected: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS); // 이전보다 시간을 줄여도 충분할 겁니다.
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
        long dbOrderCount = orderRepository.count();

        log.info("""
                
                ===========================================================
                [Test Summary] : {}
                -----------------------------------------------------------
                - 총 요청 수 : {}
                - 초기 재고  : {}
                -----------------------------------------------------------
                - 성공 (Atomic): {}
                - 실패 (Atomic): {}
                - DB에 생성된 주문 수: {}
                - 남은 최종 재고: {}
                ===========================================================
                """,
                "100 requests for 10 stocks",
                threadCount,
                10,
                successCount.get(),
                failCount.get(),
                dbOrderCount,
                updatedProduct.getStockQuantity()
        );

        // 검증: 재고는 0, DB에 저장된 주문은 10개, 성공 카운트도 10개
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        assertThat(orderRepository.count()).isEqualTo(10);
        assertThat(successCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("동시에 200개의 주문 요청이 들어와도 실제 주문은 3개만 생성된다.")
    void concurrent_order_creation_test2() throws InterruptedException {
        // given
        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0); // 성공/실패 추적용
        AtomicInteger failCount = new AtomicInteger(0); // 실패 횟수도 추적

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = new OrderCreateRequest(
                            List.of(testProduct2.getProductId())
                    );
                    // Facade를 통해 주문 생성 로직 호출
                    orderFacade.createOrderWithLock(testUser, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 락 획득 실패, 재고 부족 등의 예외는 의도된 실패이므로 무시
                    // 실패시 카운트 증가
                    failCount.incrementAndGet();
                    log.debug("Order failed as expected: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct2.getProductId()).orElseThrow();
        long dbOrderCount = orderRepository.count();

        log.info("""
                
                ===========================================================
                [Test Summary] : {}
                -----------------------------------------------------------
                - 총 요청 수 : {}
                - 초기 재고  : {}
                -----------------------------------------------------------
                - 성공 (Atomic): {}
                - 실패 (Atomic): {}
                - DB에 생성된 주문 수: {}
                - 남은 최종 재고: {}
                ===========================================================
                """,
                "200 requests for 3 stocks",
                threadCount,
                3,
                successCount.get(),
                failCount.get(),
                dbOrderCount,
                updatedProduct.getStockQuantity()
        );

        // 검증: 재고는 0, DB에 저장된 주문은 3개, 성공 카운트도 3개
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        assertThat(orderRepository.count()).isEqualTo(3);
        assertThat(successCount.get()).isEqualTo(3);
    }
}