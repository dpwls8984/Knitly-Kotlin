//package com.mysite.knitly.global.email;
//
//
//import com.mysite.knitly.domain.design.entity.Design;
//import com.mysite.knitly.domain.design.entity.DesignState;
//import com.mysite.knitly.domain.design.repository.DesignRepository;
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
//import com.mysite.knitly.domain.order.entity.Order;
//import com.mysite.knitly.domain.order.entity.OrderItem;
//import com.mysite.knitly.domain.order.repository.OrderRepository;
//import com.mysite.knitly.domain.payment.entity.Payment;
//import com.mysite.knitly.domain.payment.entity.PaymentMethod;
//import com.mysite.knitly.domain.payment.entity.PaymentStatus;
//import com.mysite.knitly.domain.payment.repository.PaymentRepository;
//import com.mysite.knitly.domain.product.product.entity.Product;
//import com.mysite.knitly.domain.product.product.entity.ProductCategory;
//import com.mysite.knitly.domain.product.product.repository.ProductRepository;
//import com.mysite.knitly.domain.user.entity.Provider;
//import com.mysite.knitly.domain.user.entity.User;
//import com.mysite.knitly.domain.user.repository.UserRepository;
//import com.mysite.knitly.global.email.service.EmailService;
//import com.mysite.knitly.global.util.FileStorageService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.IOException;
//import java.util.UUID; // (UUID 임포트 추가)
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@Transactional
//class EmailServiceIntegrationTest {
//
//    @Autowired
//    private EmailService emailService;
//
//    @Autowired
//    private OrderRepository orderRepository;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private ProductRepository productRepository;
//    @Autowired
//    private DesignRepository designRepository;
//    @Autowired
//    private PaymentRepository paymentRepository;
//
//    @MockBean
//    private FileStorageService fileStorageService;
//
//    @Value("${spring.mail.username}")
//    private String injectedUsername;
//
//    @Value("${spring.mail.password}")
//    private String injectedPassword;
//
//    private Long testOrderId;
//
//    /**
//     * 각 테스트(@Test) 실행 전에 호출되어
//     * H2 DB에 테스트용 데이터를 미리 생성합니다.
//     */
//    @BeforeEach
//    void setUp() throws IOException {
//        when(fileStorageService.loadFileAsBytes(anyString()))
//                .thenReturn(new byte[]{1, 2, 3, 4});
//
//        User testUser = User.builder()
//                .email("test@knitly.com")
//                .name("김예진")
//                .socialId("concurrentSocialId")
//                .provider(Provider.GOOGLE)
//                .build();
//
//        userRepository.save(testUser);
//
//        Design testDesign = Design.builder()
//                .user(testUser)
//                .pdfUrl("/fake/path/design.pdf")
//                .designState(DesignState.ON_SALE)
//                .designName("테스트 도안")
//                .gridData("{\"key\":\"value\"}")
//                .build();
//        designRepository.save(testDesign);
//
//        Design secondDesign = Design.builder()
//                .user(testUser)
//                .pdfUrl("/fake/path/hat_design.pdf")
//                .designState(DesignState.ON_SALE)
//                .designName("겨울 니트 모자 도안")
//                .gridData("{\"pattern\":\"hat\"}")
//                .build();
//        designRepository.save(secondDesign);
//
//        Product testProduct = Product.builder()
//                .productId(null) // ID는 자동 생성
//                .title("테스트용 니트 상품")
//                .description("이메일 테스트용 상품 설명입니다.")
//                .productCategory(ProductCategory.TOP)
//                .sizeInfo("Free Size")
//                .price(15000.0)
//                .user(testUser)
//                .purchaseCount(0)
//                .isDeleted(false)
//                .stockQuantity(0)
//                .likeCount(0)
//                .design(testDesign)
//                .avgReviewRating(0.0)
//                .reviewCount(0)
//                .build();
//        productRepository.save(testProduct);
//
//        Product secondProduct = Product.builder()
//                .title("겨울 니트 모자 도안")
//                .description("따뜻한 겨울 니트 모자 패턴")
//                .productCategory(ProductCategory.ETC)
//                .sizeInfo("Free Size")
//                .price(9800.0)
//                .user(testUser)
//                .purchaseCount(0)
//                .isDeleted(false)
//                .stockQuantity(50)
//                .likeCount(0)
//                .design(secondDesign)
//                .avgReviewRating(0.0)
//                .reviewCount(0)
//                .build();
//        productRepository.save(secondProduct);
//
//        OrderItem secondOrderItem = OrderItem.builder()
//                .product(secondProduct)
//                .orderPrice(secondProduct.getPrice())
//                .quantity(1)
//                .build();
//
//        OrderItem testOrderItem = OrderItem.builder()
//                .product(testProduct)
//                .orderPrice(testProduct.getPrice())
//                .quantity(1)
//                .build();
//
//        Order testOrder = Order.builder()
//                .user(testUser)
//                .tossOrderId(UUID.randomUUID().toString())
//                .build();
//
//        testOrder.addOrderItem(testOrderItem);
//        testOrder.addOrderItem(secondOrderItem);
//
//        orderRepository.save(testOrder);
//
//        this.testOrderId = testOrder.getOrderId();
//
//        Payment testPayment = Payment.builder()
//                .order(testOrder)
//                .buyer(testUser)
//                .tossPaymentKey("test-payment-key")
//                .tossOrderId(testOrder.getTossOrderId())
//                .totalAmount(testOrder.getTotalPrice().longValue())
//                .paymentMethod(PaymentMethod.EASY_PAY)
//                .paymentStatus(PaymentStatus.DONE)
//                .build();
//
//        paymentRepository.save(testPayment);
//    }
//
//    @Test
//    @DisplayName("실제 Gmail 계정으로 주문 완료 이메일 발송 테스트")
//    void sendOrderConfirmationEmail_IntegrationTest() {
//
//        System.out.println("==================================================");
//        System.out.println("### DEBUG USERNAME: " + injectedUsername);
//        System.out.println("### DEBUG PASSWORD: " + (injectedPassword != null ? injectedPassword.substring(0, 4) + "..." : "null"));
//        System.out.println("==================================================");
//
//        Order testOrder = orderRepository.findById(testOrderId)
//                .orElseThrow(() -> new RuntimeException("테스트용 주문(ID: " + testOrderId + ")을 찾을 수 없습니다."));
//
//        EmailNotificationDto testDto = new EmailNotificationDto(
//                testOrder.getOrderId(),
//                testOrder.getUser().getUserId(),
//                "test@knitly.com"
//        );
//
//        assertDoesNotThrow(() ->
//                emailService.sendOrderConfirmationEmail(testDto)
//        );
//    }
//}