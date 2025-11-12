package com.mysite.knitly.domain.product.service;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisProductServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisProductService redisProductService;

    private static final String POPULAR_KEY = "product:popular";

    @Test
    @DisplayName("상품 인기도 증가 - 정상 동작")
    void incrementPurchaseCount_Success() {
        // given
        Long productId = 1L;
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.incrementScore(POPULAR_KEY, "1", 1))
                .willReturn(11.0);

        // when
        redisProductService.incrementPurchaseCount(productId);

        // then
        verify(zSetOperations).incrementScore(POPULAR_KEY, "1", 1);
    }

    @Test
    @DisplayName("상품 인기도 증가 - Redis 오류 발생해도 예외 안 던짐")
    void incrementPurchaseCount_RedisError_NoException() {
        // given
        Long productId = 1L;
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.incrementScore(POPULAR_KEY, "1", 1))
                .willThrow(new RuntimeException("Redis connection error"));

        // when & then - 예외가 발생하지 않아야 함
        redisProductService.incrementPurchaseCount(productId);

        verify(zSetOperations).incrementScore(POPULAR_KEY, "1", 1);
    }

    @Test
    @DisplayName("Top N 조회 - 데이터 있음")
    void getTopNPopularProducts_WithData() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        Set<String> mockData = new LinkedHashSet<>(Arrays.asList("3", "1", "2"));
        given(zSetOperations.reverseRange(POPULAR_KEY, 0, 2))
                .willReturn(mockData);

        // when
        List<Long> result = redisProductService.getTopNPopularProducts(3);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(3L, 1L, 2L);
        verify(zSetOperations).reverseRange(POPULAR_KEY, 0, 2);
    }

    @Test
    @DisplayName("Top N 조회 - 데이터 없음")
    void getTopNPopularProducts_NoData() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
                .willReturn(Collections.emptySet());

        // when
        List<Long> result = redisProductService.getTopNPopularProducts(5);

        // then
        assertThat(result).isEmpty();
        verify(zSetOperations).reverseRange(POPULAR_KEY, 0, 4);
    }

    @Test
    @DisplayName("Top N 조회 - null 반환")
    void getTopNPopularProducts_NullResult() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
                .willReturn(null);

        // when
        List<Long> result = redisProductService.getTopNPopularProducts(5);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Top N 조회 - Redis 오류 발생")
    void getTopNPopularProducts_RedisError() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
                .willThrow(new RuntimeException("Redis connection error"));

        // when
        List<Long> result = redisProductService.getTopNPopularProducts(5);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 정상 동작")
    void syncFromDatabase_Success() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        List<Product> products = Arrays.asList(
                createProduct(1L, 100),
                createProduct(2L, 200),
                createProduct(3L, 150)
        );

        given(zSetOperations.add(eq(POPULAR_KEY), eq("1"), eq(100.0)))
                .willReturn(true);
        given(zSetOperations.add(eq(POPULAR_KEY), eq("2"), eq(200.0)))
                .willReturn(true);
        given(zSetOperations.add(eq(POPULAR_KEY), eq("3"), eq(150.0)))
                .willReturn(true);

        // when
        redisProductService.syncFromDatabase(products);

        // then
        verify(zSetOperations, times(3)).add(eq(POPULAR_KEY), anyString(), anyDouble());
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 빈 리스트")
    void syncFromDatabase_EmptyList() {
        // when
        redisProductService.syncFromDatabase(Collections.emptyList());

        // then - redisTemplate이 호출되지 않아야 함
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("DB → Redis 동기화 - null 리스트")
    void syncFromDatabase_NullList() {
        // when
        redisProductService.syncFromDatabase(null);

        // then - redisTemplate이 호출되지 않아야 함
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 일부 실패")
    void syncFromDatabase_PartialFailure() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        List<Product> products = Arrays.asList(
                createProduct(1L, 100),
                createProduct(2L, 200),
                createProduct(3L, 150)
        );

        given(zSetOperations.add(eq(POPULAR_KEY), eq("1"), eq(100.0)))
                .willReturn(true);
        given(zSetOperations.add(eq(POPULAR_KEY), eq("2"), eq(200.0)))
                .willThrow(new RuntimeException("Redis error"));
        given(zSetOperations.add(eq(POPULAR_KEY), eq("3"), eq(150.0)))
                .willReturn(true);

        // when
        redisProductService.syncFromDatabase(products);

        // then
        verify(zSetOperations, times(3)).add(eq(POPULAR_KEY), anyString(), anyDouble());
    }

    @Test
    @DisplayName("DB → Redis 동기화 - purchaseCount가 0인 상품도 동기화")
    void syncFromDatabase_ZeroPurchaseCount() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        List<Product> products = Arrays.asList(
                createProduct(1L, 0),
                createProduct(2L, 100)
        );

        given(zSetOperations.add(eq(POPULAR_KEY), anyString(), anyDouble()))
                .willReturn(true);

        // when
        redisProductService.syncFromDatabase(products);

        // then
        verify(zSetOperations).add(POPULAR_KEY, "1", 0.0);
        verify(zSetOperations).add(POPULAR_KEY, "2", 100.0);
    }

    private Product createProduct(Long productId, Integer purchaseCount) {
        return Product.builder()
                .productId(productId)
                .title("상품" + productId)
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(purchaseCount)
                .isDeleted(false)
                .build();
    }
}