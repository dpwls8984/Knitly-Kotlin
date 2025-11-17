//package com.mysite.knitly.global.email;
//
//import com.mysite.knitly.domain.design.entity.Design;
//import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
//import com.mysite.knitly.domain.order.entity.Order;
//import com.mysite.knitly.domain.order.entity.OrderItem;
//import com.mysite.knitly.domain.order.repository.OrderRepository;
//import com.mysite.knitly.domain.product.product.entity.Product;
//import com.mysite.knitly.domain.user.entity.User;
//import com.mysite.knitly.global.email.service.EmailService;
//import com.mysite.knitly.global.util.FileStorageService;
//import com.mysite.knitly.utility.handler.OAuth2FailureHandler;
//import com.mysite.knitly.utility.handler.OAuth2SuccessHandler;
//import jakarta.mail.internet.MimeMessage;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//
//@Slf4j
//@SpringBootTest
//@ActiveProfiles("test") //
//class EmailNotificationConsumerTest {
//
//    @Autowired
//    private EmailNotificationConsumer emailNotificationConsumer;
//
//    @Autowired
//    private EmailService emailService;
//
//    @MockBean
//    private JavaMailSender javaMailSender; // 실제 Gmail 발송 방지
//
//    @MockBean
//    private OrderRepository orderRepository;
//
//    @MockBean
//    private FileStorageService fileStorageService; // 실제 파일 I/O 방지
//
//    @MockBean
//    private OAuth2SuccessHandler oAuth2SuccessHandler;
//
//    @MockBean
//    private OAuth2FailureHandler oAuth2FailureHandler;
//
//    private EmailNotificationDto emailDto;
//    private Order mockOrder;
//
//    @BeforeEach
//    void setUp() {
//        emailDto = new EmailNotificationDto(1L, 100L, "customer@knitly.com");
//
//        User mockUser = User.builder().userId(100L).name("김니트").build();
//        Design mockDesign = Design.builder().pdfUrl("/fake/path/to/design.pdf").build();
//        Product mockProduct = Product.builder().productId(1L).title("테스트 도안").design(mockDesign).build();
//        OrderItem mockItem = OrderItem.builder().product(mockProduct).build();
//        mockOrder = Order.builder().orderId(1L).user(mockUser).orderItems(List.of(mockItem)).build();
//
//        MDC.put("testName", "EmailNotificationConsumerTest");
//    }
//
//    @Test
//    @DisplayName("receiveOrderCompletionMessage: 성공 시, JavaMailSender.send가 1회 호출됨")
//    void testReceive_Success() throws Exception {
//
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
//
//        when(fileStorageService.loadFileAsBytes(anyString())).thenReturn(new byte[]{1, 2, 3});
//
//        when(javaMailSender.createMimeMessage())
//                .thenReturn(new org.springframework.mail.javamail.JavaMailSenderImpl().createMimeMessage());
//
//        doNothing().when(javaMailSender).send(any(MimeMessage.class));
//
//        log.info("[Test] [When] receiveOrderCompletionMessage 메서드 호출");
//        emailNotificationConsumer.receiveOrderCompletionMessage(emailDto);
//
//        log.info("[Test] [Then] FileStorageService.loadFileAsBytes 검증");
//        verify(fileStorageService, times(1)).loadFileAsBytes("/fake/path/to/design.pdf");
//
//        // JavaMailSender.send가 1번 호출되었는지 검증
//        log.info("[Test] [Then] JavaMailSender.send 검증");
//        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
//
//        MDC.clear();
//    }
//
//    @Test
//    @DisplayName("receiveOrderCompletionMessage: Order 조회 실패 시, 예외를 던져 DLQ로 보냄")
//    void testReceive_OrderNotFound_ShouldThrowException() {
//        log.info("[Test] [Given] OrderRepository가 Order를 찾지 못하도록 모킹");
//        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
//
//        log.info("[Test] [When] receiveOrderCompletionMessage 메서드 호출");
//
//        // Consumer가 예외를 다시 던져서(re-throw) RabbitMQ가 DLQ 처리하도록 함
//        log.info("[Test] [Then] RuntimeException (IllegalArgumentException) 발생 검증");
//        assertThrows(RuntimeException.class, () -> {
//            emailNotificationConsumer.receiveOrderCompletionMessage(emailDto);
//        });
//
//        // 예외가 발생했으므로, 메일 발송은 절대 시도되지 않음
//        verify(javaMailSender, never()).send(any(MimeMessage.class));
//
//        MDC.clear();
//    }
//}