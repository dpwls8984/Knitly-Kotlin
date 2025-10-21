package com.mysite.knitly.domain.home.service;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
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
        assertThat(result).extracting(ProductListResponse::getProductId)
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
        assertThat(result).extracting(ProductListResponse::getProductId)
                .containsExactly(2L, 3L, 1L);
        verify(productRepository).findByIsDeletedFalse(PageRequest.of(0, 5, Sort.by("purchaseCount").descending()));
    }

}
