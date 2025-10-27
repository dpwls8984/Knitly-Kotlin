package com.mysite.knitly.domain.product.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.utility.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 각 테스트 후 DB 롤백
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private DesignRepository designRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private User testUser;
    private User otherUser;
    private Product testProduct;
    private Design testDesign;
    private String testUserToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 User 생성
        testUser = userRepository.save(User.builder()
                .socialId("google_123456789")
                .email("test@test.com")
                .name("testUser")
                .provider(Provider.GOOGLE)
                .build());

        otherUser = userRepository.save(User.builder()
                .socialId("google_987654321")
                .email("other@test.com")
                .name("otherUser")
                .provider(Provider.GOOGLE)
                .build());

        // 2. 테스트용 Design 생성 (User에 의존)
        testDesign = designRepository.save(Design.builder()
                .user(testUser)
                .designName("테스트 도안")
                .designState(DesignState.BEFORE_SALE)
                .gridData("{}")
                .build());

        // 3. 테스트용 Product 생성 (User와 Design에 의존)
        testProduct = productRepository.save(Product.builder()
                .title("테스트 상품")
                .description("이것은 테스트 상품입니다.")
                .productCategory(ProductCategory.TOP)
                .sizeInfo("Free")
                .price(10000.0)
                .user(testUser)
                .purchaseCount(0)
                .isDeleted(false)
                .stockQuantity(100)
                .likeCount(0)
                .design(testDesign)
                .build());

        // 4. 각 유저에 대한 JWT 토큰 생성
        testUserToken = jwtProvider.createAccessToken(testUser.getUserId());
        otherUserToken = jwtProvider.createAccessToken(otherUser.getUserId());
    }

    @Test
    @DisplayName("리뷰 등록: 성공")
    void createReview_Success() throws Exception {
        String content = "리뷰 내용입니다.";
        int rating = 5;

        ResultActions actions = mockMvc.perform(post("/products/" + testProduct.getProductId() + "/reviews")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", content)
                .param("rating", String.valueOf(rating)));

        actions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.rating").value(rating))
                .andExpect(jsonPath("$.userName").value(testUser.getName()))
                .andExpect(jsonPath("$.reviewId").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.reviewImageUrls").isArray());
    }

    @Test
    @DisplayName("리뷰 등록: 인증 실패 (토큰 없음)")
    void createReview_Fail_NoToken() throws Exception {
        ResultActions actions = mockMvc.perform(post("/products/" + testProduct.getProductId() + "/reviews")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", "내용")
                .param("rating", "5"));

        actions.andExpect(status().isFound()); // 302 Redirect
    }

    @Test
    @DisplayName("리뷰 삭제: 성공")
    void deleteReview_Success() throws Exception {
        // testUser가 작성한 리뷰를 미리 저장
        Review review = reviewRepository.save(Review.builder()
                .user(testUser)
                .product(testProduct)
                .content("삭제될 리뷰")
                .rating(5)
                .build());

        ResultActions actions = mockMvc.perform(delete("/reviews/" + review.getReviewId())
                .header("Authorization", "Bearer " + testUserToken));

        actions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("리뷰 삭제: 인가 실패 (권한 없는 사용자)")
    void deleteReview_Fail_NotOwner() throws Exception {
        // testUser가 작성한 리뷰
        Review review = reviewRepository.save(Review.builder()
                .user(testUser)
                .product(testProduct)
                .content("삭제될 리뷰")
                .rating(5)
                .build());

        // otherUser의 토큰으로 testUser의 리뷰 삭제 시도
        ResultActions actions = mockMvc.perform(delete("/reviews/" + review.getReviewId())
                .header("Authorization", "Bearer " + otherUserToken));

        actions.andExpect(status().isForbidden());
    }
}