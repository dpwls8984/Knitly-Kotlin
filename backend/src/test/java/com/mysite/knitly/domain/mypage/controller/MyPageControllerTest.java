package com.mysite.knitly.domain.mypage.controller;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.service.MyPageService;
import com.mysite.knitly.domain.payment.service.PaymentService;
import com.mysite.knitly.domain.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MyPageControllerTest {

    private MockMvc mvc;
    private MyPageService service;
    private PaymentService paymentService;
    private User principal;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(MyPageService.class);
        paymentService = Mockito.mock(PaymentService.class);

        MyPageController controller = new MyPageController(service, paymentService);

        // AuthenticationPrincipal 강제 주입
        HandlerMethodArgumentResolver forceUserResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.hasParameterAnnotation(
                        org.springframework.security.core.annotation.AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(User.class);
            }

            @Override
            public Object resolveArgument(
                    org.springframework.core.MethodParameter parameter,
                    org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                    org.springframework.web.context.request.NativeWebRequest webRequest,
                    org.springframework.web.bind.support.WebDataBinderFactory binderFactory
            ) {
                return principal;
            }
        };

        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(forceUserResolver)
                .build();

        // principal 기본 값 설정
        principal = Mockito.mock(User.class);
        given(principal.getUserId()).willReturn(1L);
        given(principal.getName()).willReturn("홍길동");
        given(principal.getEmail()).willReturn("hong@example.com");
    }

    @Test
    @DisplayName("GET /mypage/profile → profile 반환")
    void profile() throws Exception {
        mvc.perform(get("/mypage/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@example.com"));
    }

    @Test
    @DisplayName("GET /mypage/orders → 주문 카드 목록 반환")
    void orders() throws Exception {
        OrderCardResponse card = OrderCardResponse.of(
                101L,
                LocalDateTime.of(2025, 1, 2, 10, 0),
                30000.0
        );

        card.items().add(new OrderLine(
                11L, 1001L, "도안 A", 1, 10000.0, false
        ));
        card.items().add(new OrderLine(
                12L, 1002L, "도안 B", 2, 20000.0, true
        ));

        var page = new PageImpl<>(List.of(card), PageRequest.of(0, 3), 1);

        given(service.getOrderCards(eq(1L), Mockito.<Pageable>any()))
                .willReturn(page);

        mvc.perform(get("/mypage/orders")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId").value(101))
                .andExpect(jsonPath("$.content[0].items", hasSize(2)))
                .andExpect(jsonPath("$.content[0].items[0].productTitle").value("도안 A"));
    }

    @Test
    @DisplayName("GET /mypage/posts → 내가 쓴 글 목록")
    void myPosts() throws Exception {
        var dto = new MyPostListItemResponse(
                501L, "제목1", "요약1", "thumb1.jpg",
                LocalDateTime.of(2025, 1, 3, 9, 0)
        );

        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(service.getMyPosts(eq(1L), eq("키워드"), Mockito.<Pageable>any()))
                .willReturn(page);

        mvc.perform(get("/mypage/posts")
                        .param("query", "키워드")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(501))
                .andExpect(jsonPath("$.content[0].title").value("제목1"));
    }

    @Test
    @DisplayName("GET /mypage/comments → 내가 쓴 댓글 목록")
    void myComments() throws Exception {
        var dto = new MyCommentListItem(
                701L, 501L,
                LocalDateTime.of(2025, 1, 4, 12, 0),
                "내용 미리보기"
        );

        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(service.getMyComments(eq(1L), eq("단어"), Mockito.<Pageable>any()))
                .willReturn(page);

        mvc.perform(get("/mypage/comments")
                        .param("query", "단어")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(701))
                .andExpect(jsonPath("$.content[0].postId").value(501))
                .andExpect(jsonPath("$.content[0].preview").value("내용 미리보기"));
    }

    @Test
    @DisplayName("GET /mypage/favorites → 내가 찜한 상품 목록")
    void myFavorites() throws Exception {
        var f = new FavoriteProductItem(
                9001L,
                "인기 도안",
                "홍길동",
                "t.jpg"
        );

        var page = new PageImpl<>(List.of(f), PageRequest.of(0, 10), 1);

        given(service.getMyFavorites(eq(1L), Mockito.<Pageable>any()))
                .willReturn(page);

        mvc.perform(get("/mypage/favorites")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(9001))
                .andExpect(jsonPath("$.content[0].productTitle").value("인기 도안"))
                .andExpect(jsonPath("$.content[0].sellerName").value("홍길동"))
                .andExpect(jsonPath("$.content[0].thumbnailUrl").value("t.jpg"));
    }

    @Test
    @DisplayName("GET /mypage/reviews → 내가 작성한 리뷰 목록")
    void myReviews() throws Exception {

        var r = new ReviewListItem(
                301L,
                9001L,
                "인기 도안",
                "t.jpg",
                5,
                "좋아요",
                List.of("r1.jpg", "r2.jpg"),
                LocalDate.of(2025,1,6)
        );

        var page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);

        given(service.getMyReviewsV2(eq(1L), Mockito.<Pageable>any()))
                .willReturn(page);

        mvc.perform(get("/mypage/reviews")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewId").value(301))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].reviewImageUrls", hasSize(2)));
    }
}
