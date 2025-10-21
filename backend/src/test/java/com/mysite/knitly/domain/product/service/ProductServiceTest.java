package com.mysite.knitly.domain.product.service;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.entity.ProductFilterType;
import com.mysite.knitly.domain.product.product.entity.ProductSortType;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisProductService redisProductService;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;
    private Product product3;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        product1 = Product.builder()
                .productId(1L)
                .title("상의 패턴 1")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(100)
                .likeCount(50)
                .isDeleted(false)
                .build();

        product2 = Product.builder()
                .productId(2L)
                .title("무료 패턴")
                .productCategory(ProductCategory.BOTTOM)
                .price(0.0)
                .purchaseCount(200)
                .likeCount(80)
                .isDeleted(false)
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
                .build();
    }

    @Test
    @DisplayName("전체 상품 조회 - 최신순")
    void getProducts_All_Latest() {
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product1, product2, product3));
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(productPage);

        Page<ProductListResponse> result = productService.getProducts(
                null, ProductFilterType.ALL, ProductSortType.LATEST, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("카테고리별 조회 - 상의만")
    void getProducts_Category_Top() {
        Page<Product> productPage = new PageImpl<>(List.of(product1));
        given(productRepository.findByProductCategoryAndIsDeletedFalse(
                eq(ProductCategory.TOP), any(Pageable.class)))
                .willReturn(productPage);

        Page<ProductListResponse> result = productService.getProducts(
                ProductCategory.TOP, ProductFilterType.ALL, ProductSortType.LATEST, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductCategory()).isEqualTo(ProductCategory.TOP);
        verify(productRepository).findByProductCategoryAndIsDeletedFalse(
                eq(ProductCategory.TOP), any(Pageable.class));
    }

    @Test
    @DisplayName("무료 상품만 조회")
    void getProducts_Free() {
        Page<Product> productPage = new PageImpl<>(List.of(product2));
        given(productRepository.findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class)))
                .willReturn(productPage);

        Page<ProductListResponse> result = productService.getProducts(
                null, ProductFilterType.FREE, ProductSortType.LATEST, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(0.0);
        assertThat(result.getContent().get(0).getIsFree()).isTrue();
        verify(productRepository).findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class));
    }

    @Test
    @DisplayName("한정판매 상품만 조회")
    void getProducts_Limited() {
        Page<Product> productPage = new PageImpl<>(List.of(product3));
        given(productRepository.findByStockQuantityIsNotNullAndIsDeletedFalse(any(Pageable.class)))
                .willReturn(productPage);

        Page<ProductListResponse> result = productService.getProducts(
                null, ProductFilterType.LIMITED, ProductSortType.LATEST, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStockQuantity()).isNotNull();
        assertThat(result.getContent().get(0).getIsLimited()).isTrue();
        verify(productRepository).findByStockQuantityIsNotNullAndIsDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("인기순 조회 - Redis 데이터 있음")
    void getProducts_Popular_WithRedis() {
        List<Long> popularIds = Arrays.asList(2L, 1L, 3L); // 인기순
        given(redisProductService.getTopNPopularProducts(1000)).willReturn(popularIds);
        given(productRepository.findByProductIdInAndIsDeletedFalse(popularIds))
                .willReturn(Arrays.asList(product2, product1, product3));

        Page<ProductListResponse> result = productService.getProducts(
                null, ProductFilterType.ALL, ProductSortType.POPULAR, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getProductId()).isEqualTo(2L); // 가장 인기있는 상품
        verify(redisProductService).getTopNPopularProducts(1000);
    }



    @Test
    @DisplayName("가격 낮은순 정렬")
    void getProducts_SortByPrice_Asc() {
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product2, product1, product3));
        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
                .willReturn(productPage);

        Page<ProductListResponse> result = productService.getProducts(
                null, ProductFilterType.ALL, ProductSortType.PRICE_ASC, pageable);

        assertThat(result.getContent()).hasSize(3);
        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("filter=FREE이면 카테고리 무시하고 무료 전체에서 조회")
    void freeFilter_ignoresCategory() {
        Pageable pageable = PageRequest.of(0, 20);
        // popular 분기 안 타는 케이스로 최신 정렬 가정
        given(productRepository.findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product2))); // product2: price 0.0

        Page<ProductListResponse> result = productService.getProducts(
                ProductCategory.TOP, ProductFilterType.FREE, ProductSortType.LATEST, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsFree()).isTrue();
        // TOP로 제한되지 않음을 간접 확인(리포 호출 검증)
        verify(productRepository).findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class));
    }
}