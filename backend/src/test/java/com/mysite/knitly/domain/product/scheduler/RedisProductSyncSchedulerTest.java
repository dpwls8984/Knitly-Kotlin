package com.mysite.knitly.domain.product.scheduler;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.scheduler.RedisProductSyncScheduler;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisProductSyncSchedulerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisProductService redisProductService;

    @InjectMocks
    private RedisProductSyncScheduler scheduler;

    private List<Product> mockProducts;

    @BeforeEach
    void setUp() {
        mockProducts = Arrays.asList(
                createProduct(1L, 100),
                createProduct(2L, 200),
                createProduct(3L, 150)
        );
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - 정상 동작")
    void initializeRedisData_Success() {
        // given
        Page<Product> firstPage = new PageImpl<>(mockProducts, Pageable.ofSize(1000), 3);
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(firstPage);
        doNothing().when(redisProductService).syncFromDatabase(anyList());

        // when
        scheduler.initializedRedisData();

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService).syncFromDatabase(argThat(list -> list.size() == 3));
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - 상품 없음")
    void initializeRedisData_NoProducts() {
        // given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        scheduler.initializedRedisData();

        // then
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService, never()).syncFromDatabase(anyList());
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - DB 조회 실패")
    void initializeRedisData_DbError() {
        // given
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willThrow(new RuntimeException("Database error"));

        // when - 예외가 발생해도 애플리케이션은 정상 시작되어야 함
        scheduler.initializedRedisData();

        // then
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService, never()).syncFromDatabase(anyList());
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - Redis 동기화 실패")
    void initializeRedisData_RedisSyncError() {
        // given
        Page<Product> firstPage = new PageImpl<>(mockProducts, Pageable.ofSize(1000), 3);
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(firstPage);
        doThrow(new RuntimeException("Redis sync error"))
                .when(redisProductService).syncFromDatabase(anyList());

        // when - 예외가 발생해도 애플리케이션은 정상 시작되어야 함
        scheduler.initializedRedisData();

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService).syncFromDatabase(anyList());
    }

    @Test
    @DisplayName("애플리케이션 시작 시 - 대량 데이터 페이징 처리")
    void initializeRedisData_LargeDataset() {
        // given - 2500개 상품 (3페이지)
        List<Product> page1Products = createProductList(1, 1000);
        List<Product> page2Products = createProductList(1001, 2000);
        List<Product> page3Products = createProductList(2001, 2500);

        Page<Product> page1 = new PageImpl<>(page1Products, PageRequest.of(0, 1000), 2500);
        Page<Product> page2 = new PageImpl<>(page2Products, PageRequest.of(1, 1000), 2500);
        Page<Product> page3 = new PageImpl<>(page3Products, PageRequest.of(2, 1000), 2500);

        // 페이지별로 반환 설정
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(page1, page2, page3);

        doNothing().when(redisProductService).syncFromDatabase(anyList());

        // when
        scheduler.initializedRedisData();

        // then
        verify(productRepository, times(3)).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService).syncFromDatabase(argThat(list -> list.size() == 2500));
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - 정상 동작")
    void syncPurchaseCountFromDB_Success() {
        // given
        Page<Product> firstPage = new PageImpl<>(mockProducts, Pageable.ofSize(1000), 3);
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(firstPage);
        doNothing().when(redisProductService).syncFromDatabase(anyList());

        // when
        scheduler.syncPurchaseCountFromDB();

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService).syncFromDatabase(argThat(list -> list.size() == 3));
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - 상품 없음")
    void syncPurchaseCountFromDB_NoProducts() {
        // given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        scheduler.syncPurchaseCountFromDB();

        // then
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService, never()).syncFromDatabase(anyList());
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - DB 조회 실패")
    void syncPurchaseCountFromDB_DbError() {
        // given
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willThrow(new RuntimeException("Database error"));

        // when - 예외가 발생해도 스케줄러는 다음 실행을 기다려야 함
        scheduler.syncPurchaseCountFromDB();

        // then
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService, never()).syncFromDatabase(anyList());
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - Redis 동기화 실패")
    void syncPurchaseCountFromDB_RedisSyncError() {
        // given
        Page<Product> firstPage = new PageImpl<>(mockProducts, Pageable.ofSize(1000), 3);
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(firstPage);
        doThrow(new RuntimeException("Redis sync error"))
                .when(redisProductService).syncFromDatabase(anyList());

        // when - 예외가 발생해도 스케줄러는 다음 실행을 기다려야 함
        scheduler.syncPurchaseCountFromDB();

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any(Pageable.class));
        verify(redisProductService).syncFromDatabase(anyList());
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

    private List<Product> createProductList(int startId, int endId) {
        List<Product> products = new java.util.ArrayList<>();
        for (int i = startId; i <= endId; i++) {
            products.add(createProduct((long) i, i * 10));
        }
        return products;
    }
}