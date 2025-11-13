package com.mysite.knitly.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.dto.ProductDetailResponse;
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private RedisProductService redisProductService;
    @Mock
    private DesignRepository designRepository;
    @Mock
    private LocalFileStorage localFileStorage;

    @Mock
    private ProductLikeRepository productLikeRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private ProductService productService;

    private User seller;
    private Design design;
    private Product product1;
    private MockMultipartFile imageFile;
    private static final String CACHE_KEY_PREFIX = "product:detail:";

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .userId(1L)
                .name("판매자")
                .build();

        design = Design.builder()
                .designId(1L)
                .user(seller)
                .designState(DesignState.BEFORE_SALE)
                .build();

        product1 = Product.builder()
                .productId(1L)
                .title("테스트 상품")
                .description("상세 설명")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .user(seller)
                .design(design)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .productImages(new ArrayList<>(List.of(ProductImage.builder().productImageUrl("/static/product/old-image.jpg").build())))
                .build();

        imageFile = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1_content".getBytes());
    }

    @Test
    @DisplayName("상품 등록 성공 - 이미지 포함")
    void registerProduct_Success_WithImages() {
        ProductRegisterRequest request = new ProductRegisterRequest(
                "새 상품", "설명", ProductCategory.TOP, "M", 10000.0, List.of(imageFile), 10);

        given(designRepository.findById(1L)).willReturn(Optional.of(design));

        given(localFileStorage.saveProductImage(any(MultipartFile.class)))
                .willReturn("/static/product/mock-image.jpg");

        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0);
            return Product.builder()
                    .productId(1L)
                    .user(productToSave.getUser())
                    .design(productToSave.getDesign())
                    .title(productToSave.getTitle())
                    .createdAt(LocalDateTime.now())
                    .productImages(List.of(ProductImage.builder().productImageUrl("/static/product/mock-image.jpg").build()))
                    .build();
        });

        ProductRegisterResponse response = productService.registerProduct(seller, 1L, request);

        verify(designRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(localFileStorage, times(1)).saveProductImage(any(MultipartFile.class));
        assertThat(design.getDesignState()).isEqualTo(DesignState.ON_SALE);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 교체 (기존 이미지 삭제, 새 이미지 추가)")
    void modifyProduct_Success_ImageUpdate() {

        given(productRepository.findById(1L)).willReturn(Optional.of(spy(product1)));
        Product productSpy = productRepository.findById(1L).get();

        MockMultipartFile newImage = new MockMultipartFile("images", "new.jpg", "image/jpeg", "new_content".getBytes());

        ProductModifyRequest request = new ProductModifyRequest(
                "수정된 설명",
                ProductCategory.BOTTOM,
                "L",
                List.of(newImage),
                new ArrayList<>(),
                20
        );

        given(localFileStorage.saveProductImage(any(MultipartFile.class)))
                .willReturn("/static/product/new-image.jpg");

        productService.modifyProduct(seller, 1L, request);

        verify(productSpy).update("수정된 설명", ProductCategory.BOTTOM, "L", 20);
        verify(localFileStorage).saveProductImage(any(MultipartFile.class));

        verify(localFileStorage).deleteProductImage("/static/product/old-image.jpg");
    }

    @Test
    @DisplayName("소프트 삭제 성공")
    void deleteProduct_Success() {

        Design designToStop = spy(design);
        designToStop.startSale();
        Product productToDelete = spy(Product.builder().productId(1L).user(seller).design(designToStop).isDeleted(false).build());

        given(productRepository.findById(1L)).willReturn(Optional.of(productToDelete));

        productService.deleteProduct(seller, 1L);

        verify(productToDelete).softDelete();
        verify(designToStop).stopSale();
    }

    @Test
    @DisplayName("재판매 성공")
    void relistProduct_Success() {

        Design designToRelist = spy(Design.builder()
                .designId(1L)
                .user(seller)
                .designState(DesignState.STOPPED)
                .build());

        Product productToRelist = spy(Product.builder()
                .productId(1L)
                .user(seller)
                .design(designToRelist)
                .isDeleted(true)
                .build());

        given(productRepository.findById(1L)).willReturn(Optional.of(productToRelist));

        productService.relistProduct(seller, 1L);

        verify(productToRelist).relist();
        verify(designToRelist).relist();
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - Cache Miss (DB 조회 및 캐시 저장)")
    void getProductDetail_Success_CacheMiss() throws Exception {

        String cacheKey = CACHE_KEY_PREFIX + "1";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        given(valueOperations.get(cacheKey)).willReturn(null);

        given(productRepository.findByProductIdAndIsDeletedFalse(1L)).willReturn(Optional.of(product1));

        given(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(1L, 1L)).willReturn(true);

        given(reviewRepository.countByProductAndIsDeletedFalse(product1)).willReturn(5L);

        ProductDetailResponse response = productService.getProductDetail(seller, 1L);

        verify(productRepository).findByProductIdAndIsDeletedFalse(1L);
        verify(productLikeRepository).existsByUser_UserIdAndProduct_ProductId(1L, 1L);
        verify(reviewRepository).countByProductAndIsDeletedFalse(product1);

        String expectedJson = objectMapper.writeValueAsString(response);
        verify(valueOperations).set(eq(cacheKey), eq(expectedJson), any(Duration.class));

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 상품");
        assertThat(response.isLikedByUser()).isTrue();
        assertThat(response.reviewCount()).isEqualTo(5);
        assertThat(response.createdAt()).isEqualTo(product1.getCreatedAt().toString());
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - Cache Hit (DB 조회 안 함, 캐시된 DTO 그대로 반환)")
    void getProductDetail_Success_CacheHit() throws Exception {

        String cacheKey = CACHE_KEY_PREFIX + "1";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        List<String> imageUrls = List.of("/static/img.jpg");
        boolean isLiked = true;
        ProductDetailResponse cachedDto = ProductDetailResponse.from(product1, imageUrls, isLiked);

        cachedDto = new ProductDetailResponse(
                cachedDto.productId(), cachedDto.title(), cachedDto.description(), cachedDto.productCategory(),
                cachedDto.sizeInfo(), cachedDto.price(), cachedDto.createdAt(), cachedDto.stockQuantity(),
                cachedDto.likeCount(), cachedDto.isLikedByUser(), cachedDto.avgReviewRating(),
                cachedDto.productImageUrls(), 5
        );

        String cachedJson = objectMapper.writeValueAsString(cachedDto);

        given(valueOperations.get(cacheKey)).willReturn(cachedJson);

        ProductDetailResponse response = productService.getProductDetail(seller, 1L);

        verify(productRepository, never()).findByProductIdAndIsDeletedFalse(anyLong());
        verify(productLikeRepository, never()).existsByUser_UserIdAndProduct_ProductId(anyLong(), anyLong());
        verify(reviewRepository, never()).countByProductAndIsDeletedFalse(any(Product.class));

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 상품");
        assertThat(response.isLikedByUser()).isTrue();
        assertThat(response.reviewCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품 ID (Cache Miss & DB Miss)")
    void getProductDetail_Fail_NotFound() {

        String cacheKey = CACHE_KEY_PREFIX + "999";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        given(valueOperations.get(cacheKey)).willReturn(null);

        given(productRepository.findByProductIdAndIsDeletedFalse(999L)).willReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.getProductDetail(seller, 999L);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}