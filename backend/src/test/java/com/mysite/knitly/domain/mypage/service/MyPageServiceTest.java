package com.mysite.knitly.domain.mypage.service;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductImage;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    MyPageQueryRepository repo;

    @Mock
    ProductLikeRepository productLikeRepository;

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    MyPageService service;

    @Test
    @DisplayName("주문 카드 조회")
    void getOrderCards() {
        var card = OrderCardResponse.of(1L, LocalDateTime.now(), 10000.0);
        var page = new PageImpl<>(List.of(card), PageRequest.of(0, 3), 1);

        given(repo.findOrderCards(eq(10L), any())).willReturn(page);

        var result = service.getOrderCards(10L, PageRequest.of(0, 3));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("내가 쓴 글 조회")
    void getMyPosts() {
        var dto = new MyPostListItemResponse(
                100L, "제목", "요약", "thumb.jpg",
                LocalDateTime.of(2025,1,1,10,0)
        );

        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(repo.findMyPosts(eq(10L), eq("키워드"), any()))
                .willReturn(page);

        var result = service.getMyPosts(10L, "키워드", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("내가 쓴 댓글 조회")
    void getMyComments() {
        var dto = new MyCommentListItem(
                7L, 100L,
                LocalDateTime.of(2025,1,2,10,0),
                "미리보기"
        );

        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(repo.findMyComments(eq(10L), eq("단어"), any()))
                .willReturn(page);

        var result = service.getMyComments(10L, "단어", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("내가 찜한 상품 조회")
    void getMyFavorites() {

        // Mock 준비
        ProductLike like = Mockito.mock(ProductLike.class);
        Product product = Mockito.mock(Product.class);
        ProductImage image = Mockito.mock(ProductImage.class);
        User user = Mockito.mock(User.class);

        // Like → Product
        given(like.getProduct()).willReturn(product);

        // Product 기본 정보
        given(product.getProductId()).willReturn(9001L);
        given(product.getTitle()).willReturn("인기 도안");
        given(product.getUser()).willReturn(user);

        // 판매자 이름
        given(user.getName()).willReturn("홍길동");

        // Product → 이미지 리스트
        given(product.getProductImages()).willReturn(List.of(image));

        // ProductImage → URL
        given(image.getProductImageUrl()).willReturn("t.jpg");

        var page = new PageImpl<>(List.of(like), PageRequest.of(0, 10), 1);

        given(productLikeRepository.findByUser_UserId(eq(10L), any()))
                .willReturn(page);

        var result = service.getMyFavorites(10L, PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).productId()).isEqualTo(9001L);
        assertThat(result.getContent().get(0).thumbnailUrl()).isEqualTo("t.jpg");
    }

    @Test
    @DisplayName("내 리뷰 조회")
    void getMyReviewsV2() {
        Review review = Mockito.mock(Review.class);
        Product product = Mockito.mock(Product.class);
        ProductImage image = Mockito.mock(ProductImage.class);

        given(review.getReviewId()).willReturn(301L);
        given(review.getContent()).willReturn("좋아요");
        given(review.getRating()).willReturn(5);
        given(review.getCreatedAt()).willReturn(LocalDateTime.of(2025,1,4,10,0));
        given(review.getReviewImages()).willReturn(List.of());

        // Review → Product
        given(review.getProduct()).willReturn(product);

        // Product 정보
        given(product.getProductId()).willReturn(9001L);
        given(product.getTitle()).willReturn("인기 도안");
        given(product.getProductImages()).willReturn(List.of(image));

        // ProductImage → URL
        given(image.getProductImageUrl()).willReturn("t.jpg");

        given(reviewRepository.findByUser_UserIdAndIsDeletedFalse(eq(10L), any()))
                .willReturn(List.of(review));

        given(reviewRepository.countByUser_UserIdAndIsDeletedFalse(10L))
                .willReturn(1L);

        var result = service.getMyReviewsV2(10L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).productId()).isEqualTo(9001L);
    }
}
