package com.mysite.knitly.domain.home.service;

import com.mysite.knitly.domain.home.dto.HomeSummaryResponse;
import com.mysite.knitly.domain.home.dto.LatestPostItem;
import com.mysite.knitly.domain.home.dto.LatestReviewItem;
import com.mysite.knitly.domain.home.repository.HomeQueryRepository;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HomeSectionServiceTest {

    @Mock
    private RedisProductService redisProductService;
    @InjectMocks
    private HomeSectionService homeSectionService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private HomeQueryRepository homeQueryRepository;

    private Product product1; // id=1
    private Product product2; // id=2
    private Product product3; // id=3

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        product1 = Product.builder()
                .productId(1L)
                .title("상의 패턴 1")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(100)
                .likeCount(50)
                .isDeleted(false)
                .createdAt(now.minusDays(1))
                .build();

        product2 = Product.builder()
                .productId(2L)
                .title("무료 패턴")
                .productCategory(ProductCategory.BOTTOM)
                .price(0.0)
                .purchaseCount(200)
                .likeCount(80)
                .isDeleted(false)
                .createdAt(now.minusDays(2))
                .build();

        product3 = Product.builder()
                .productId(3L)
                .title("한정판매 패턴")
                .productCategory(ProductCategory.OUTER)
                .price(15000.0)
                .stockQuantity(10)
                .purchaseCount(150)
                .likeCount(60)
                .isDeleted(false)
                .createdAt(now.minusDays(3))
                .build();
    }

    @Test
    @DisplayName("인기 Top5 조회 - Redis 데이터 있음")
    void getTop5Products_WithRedis() {
        List<Long> top5Ids = Arrays.asList(2L, 3L, 1L);
        given(redisProductService.getTopNPopularProducts(5)).willReturn(top5Ids);
        given(productRepository.findByProductIdInAndIsDeletedFalse(top5Ids))
                .willReturn(Arrays.asList(product2, product3, product1));

        List<ProductListResponse> result = homeSectionService.getPopularTop5();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductListResponse::productId)
                .containsExactly(2L, 3L, 1L); // edis ZSET 순서 보존 검증
        verify(redisProductService).getTopNPopularProducts(5);
        verify(productRepository).findByProductIdInAndIsDeletedFalse(top5Ids);
    }

    @Test
    @DisplayName("인기 Top5 조회 - Redis 데이터 없음 (DB 조회)")
    void getTop5Products_WithoutRedis() {
        given(redisProductService.getTopNPopularProducts(5)).willReturn(List.of());
        Page<Product> top5Page = new PageImpl<>(Arrays.asList(product2, product3, product1));
        given(productRepository.findByIsDeletedFalse(any(Pageable.class))).willReturn(top5Page);

        List<ProductListResponse> result = homeSectionService.getPopularTop5();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductListResponse::productId)
                .containsExactly(2L, 3L, 1L);
        verify(productRepository).findByIsDeletedFalse(PageRequest.of(0, 5, Sort.by("purchaseCount").descending()));
    }

    @Test
    @DisplayName("최신 리뷰 3개 조회 - Repository 결과 반환")
    void getLatestReviews_ReturnsTop3() {
        var r1 = new LatestReviewItem(101L, 10L, "니트 스웨터", null, 5, "아주 좋아요", LocalDate.now());
        var r2 = new LatestReviewItem(102L, 11L, "울 머플러", null, 4, "따뜻합니다", LocalDate.now().minusDays(1));
        var r3 = new LatestReviewItem(103L, 12L, "가디건", null, 5, "부드러워요", LocalDate.now().minusDays(2));

        given(homeQueryRepository.findLatestReviews(3)).willReturn(List.of(r1, r2, r3));

        var result = homeSectionService.getLatestReviews(3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).reviewId()).isEqualTo(101L);
        assertThat(result.get(1).productTitle()).isEqualTo("울 머플러");
        verify(homeQueryRepository).findLatestReviews(3);
    }
    @Test
    @DisplayName("최신 커뮤니티 글 3개 조회 - Repository 결과 반환")
    void getLatestPosts_ReturnsTop3() {
        var p1 = new LatestPostItem(201L, "첫 글", "FREE", null, LocalDateTime.now());
        var p2 = new LatestPostItem(202L, "둘째 글", "QUESTION", null, LocalDateTime.now().minusHours(1));
        var p3 = new LatestPostItem(203L, "셋째 글", "TIP", null, LocalDateTime.now().minusDays(1));

        given(homeQueryRepository.findLatestPosts(3)).willReturn(List.of(p1, p2, p3));

        var result = homeSectionService.getLatestPosts(3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).postId()).isEqualTo(201L);
        assertThat(result.get(2).category()).isEqualTo("TIP");
        verify(homeQueryRepository).findLatestPosts(3);
    }
    @Test
    @DisplayName("홈 요약 조회 - 인기 Top5 + 최신 리뷰 3 + 최신 글 3을 모아서 반환")
    void getHomeSummary_AggregatesAllSections() {
        // popular
        List<Long> ids = Arrays.asList(2L, 3L, 1L);
        given(redisProductService.getTopNPopularProducts(5)).willReturn(ids);
        given(productRepository.findByProductIdInAndIsDeletedFalse(ids))
                .willReturn(Arrays.asList(product2, product3, product1));

        // latest reviews
        var r1 = new LatestReviewItem(101L, 10L, "니트 스웨터", null, 5, "굿", LocalDate.now());
        var r2 = new LatestReviewItem(102L, 11L, "울 머플러", null, 4, "따뜻", LocalDate.now());
        var r3 = new LatestReviewItem(103L, 12L, "가디건", null, 5, "부드러움", LocalDate.now());
        given(homeQueryRepository.findLatestReviews(3)).willReturn(List.of(r1, r2, r3));

        // latest posts
        var p1 = new LatestPostItem(201L, "첫 글", "FREE", null, LocalDateTime.now());
        var p2 = new LatestPostItem(202L, "둘째 글", "QUESTION", null, LocalDateTime.now());
        var p3 = new LatestPostItem(203L, "셋째 글", "TIP", null, LocalDateTime.now());
        given(homeQueryRepository.findLatestPosts(3)).willReturn(List.of(p1, p2, p3));

        HomeSummaryResponse response = homeSectionService.getHomeSummary();

        assertThat(response.popularProducts()).hasSize(3);
        assertThat(response.latestReviews()).hasSize(3);
        assertThat(response.latestPosts()).hasSize(3);
        assertThat(response.popularProducts()).extracting(ProductListResponse::productId)
                .containsExactly(2L, 3L, 1L);

        assertThat(response.latestReviews().get(0).rating()).isEqualTo(5);
        assertThat(response.latestPosts().get(1).category()).isEqualTo("QUESTION");
    }
}