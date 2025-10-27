package com.mysite.knitly.domain.product.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.product.dto.ProductDetailResponse;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private FileStorageService fileStorageService;
    @InjectMocks
    private ProductService productService;

    private User seller;
    private Design design;
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

        seller = User.builder()
                .userId(1L)
                .build();

        design = Design.builder()
                .designId(1L)
                .user(seller)
                .designState(DesignState.BEFORE_SALE)
                .build();
    }

//    @Test
//    @DisplayName("전체 상품 조회 - 최신순")
//    void getProducts_All_Latest() {
//        Page<Product> productPage = new PageImpl<>(Arrays.asList(product1, product2, product3));
//        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
//                .willReturn(productPage);
//
//        Page<ProductListResponse> result = productService.getProducts(
//                null, ProductFilterType.ALL, ProductSortType.LATEST, pageable);
//
//        assertThat(result.getContent()).hasSize(3);
//        assertThat(result.getTotalElements()).isEqualTo(3);
//        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
//    }

//    @Test
//    @DisplayName("카테고리별 조회 - 상의만")
//    void getProducts_Category_Top() {
//        Page<Product> productPage = new PageImpl<>(List.of(product1));
//        given(productRepository.findByProductCategoryAndIsDeletedFalse(
//                eq(ProductCategory.TOP), any(Pageable.class)))
//                .willReturn(productPage);
//
//        Page<ProductListResponse> result = productService.getProducts(
//                ProductCategory.TOP, ProductFilterType.ALL, ProductSortType.LATEST, pageable);
//
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).productCategory()).isEqualTo(ProductCategory.TOP);
//        verify(productRepository).findByProductCategoryAndIsDeletedFalse(
//                eq(ProductCategory.TOP), any(Pageable.class));
//    }

//    @Test
//    @DisplayName("무료 상품만 조회")
//    void getProducts_Free() {
//        Page<Product> productPage = new PageImpl<>(List.of(product2));
//        given(productRepository.findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class)))
//                .willReturn(productPage);
//
//        Page<ProductListResponse> result = productService.getProducts(
//                null, ProductFilterType.FREE, ProductSortType.LATEST, pageable);
//
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).price()).isEqualTo(0.0);
//        assertThat(result.getContent().get(0).isFree()).isTrue();
//        verify(productRepository).findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class));
//    }

//    @Test
//    @DisplayName("한정판매 상품만 조회")
//    void getProducts_Limited() {
//        Page<Product> productPage = new PageImpl<>(List.of(product3));
//        given(productRepository.findByStockQuantityIsNotNullAndIsDeletedFalse(any(Pageable.class)))
//                .willReturn(productPage);
//
//        Page<ProductListResponse> result = productService.getProducts(
//                null, ProductFilterType.LIMITED, ProductSortType.LATEST, pageable);
//
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).stockQuantity()).isNotNull();
//        assertThat(result.getContent().get(0).isLimited()).isTrue();
//        verify(productRepository).findByStockQuantityIsNotNullAndIsDeletedFalse(any(Pageable.class));
//    }

//    @Test
//    @DisplayName("인기순 조회 - Redis 데이터 있음")
//    void getProducts_Popular_WithRedis() {
//        List<Long> popularIds = Arrays.asList(2L, 1L, 3L); // 인기순
//        given(redisProductService.getTopNPopularProducts(1000)).willReturn(popularIds);
//        given(productRepository.findByProductIdInAndIsDeletedFalse(popularIds))
//                .willReturn(Arrays.asList(product2, product1, product3));
//
//        Page<ProductListResponse> result = productService.getProducts(
//                null, ProductFilterType.ALL, ProductSortType.POPULAR, pageable);
//
//        assertThat(result.getContent()).hasSize(3);
//        assertThat(result.getContent().get(0).productId()).isEqualTo(2L); // 가장 인기있는 상품
//        verify(redisProductService).getTopNPopularProducts(1000);
//    }


//    @Test
//    @DisplayName("가격 낮은순 정렬")
//    void getProducts_SortByPrice_Asc() {
//        Page<Product> productPage = new PageImpl<>(Arrays.asList(product2, product1, product3));
//        given(productRepository.findByIsDeletedFalse(any(Pageable.class)))
//                .willReturn(productPage);
//
//        Page<ProductListResponse> result = productService.getProducts(
//                null, ProductFilterType.ALL, ProductSortType.PRICE_ASC, pageable);
//
//        assertThat(result.getContent()).hasSize(3);
//        verify(productRepository).findByIsDeletedFalse(any(Pageable.class));
//    }

//    @Test
//    @DisplayName("filter=FREE이면 카테고리 무시하고 무료 전체에서 조회")
//    void freeFilter_ignoresCategory() {
//        Pageable pageable = PageRequest.of(0, 20);
//        // popular 분기 안 타는 케이스로 최신 정렬 가정
//        given(productRepository.findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(product2))); // product2: price 0.0
//
//        Page<ProductListResponse> result = productService.getProducts(
//                ProductCategory.TOP, ProductFilterType.FREE, ProductSortType.LATEST, pageable);
//
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).isFree()).isTrue();
//        // TOP로 제한되지 않음을 간접 확인(리포 호출 검증)
//        verify(productRepository).findByPriceAndIsDeletedFalse(eq(0.0), any(Pageable.class));
//    }


    @Test
    @DisplayName("상품 등록 성공 - 이미지 포함")
    void registerProduct_Success_WithImages() {
        // given
        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1_content".getBytes());
        ProductRegisterRequest request = new ProductRegisterRequest("새 상품", "설명", ProductCategory.TOP, "M", 10000.0, List.of(image1), 10);

        given(designRepository.findById(1L)).willReturn(Optional.of(design));
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0);
            return Product.builder()
                    .productId(1L) // 테스트용 임의 ID
                    .user(productToSave.getUser())
                    .design(productToSave.getDesign())
                    .title(productToSave.getTitle())
                    .createdAt(LocalDateTime.now())
                    .productImages(productToSave.getProductImages())
                    .build();
        });
        given(fileStorageService.storeFile(any(MockMultipartFile.class), eq("product"))).willReturn("/static/product/mock-image.jpg");

        // when
        productService.registerProduct(seller, 1L, request);

        // then
        verify(designRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(fileStorageService, times(1)).storeFile(any(MockMultipartFile.class), eq("product"));
        assertThat(design.getDesignState()).isEqualTo(DesignState.ON_SALE); // Design 상태 변경 확인
    }

    @Test
    @DisplayName("상품 등록 실패(예외) - 이미 판매중인 도안으로 등록 시도")
    void registerProduct_Fail_DesignAlreadyOnSale() {
        // given
        design.startSale(); // 도안 상태를 ON_SALE으로 미리 변경
        ProductRegisterRequest request = new ProductRegisterRequest("새 상품", "설명", ProductCategory.TOP, "M", 10000.0, Collections.emptyList(), 10);

        given(designRepository.findById(1L)).willReturn(Optional.of(design));

        // when & then
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.registerProduct(seller, 1L, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DESIGN_ALREADY_ON_SALE);
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 교체")
    void modifyProduct_Success_ImageUpdate() {
        // given
        Product existingProduct = spy(Product.builder()
                .productId(1L)
                .user(seller)
                .isDeleted(false)
                .productImages(new ArrayList<>(List.of(ProductImage.builder().productImageUrl("/static/product/old-image.jpg").build())))
                .build());

        MockMultipartFile newImage = new MockMultipartFile("images", "new.jpg", "image/jpeg", "new_content".getBytes());
        ProductModifyRequest request = new ProductModifyRequest("수정된 설명", ProductCategory.BOTTOM, "L", List.of(newImage), 20);

        given(productRepository.findByIdWithUser(1L)).willReturn(Optional.of(existingProduct));
        given(fileStorageService.storeFile(any(MockMultipartFile.class), eq("product"))).willReturn("/static/product/new-image.jpg");

        // when
        productService.modifyProduct(seller, 1L, request);

        // then
        verify(existingProduct).update("수정된 설명", ProductCategory.BOTTOM, "L", 20); // 정보 업데이트 확인
        verify(fileStorageService).storeFile(any(MockMultipartFile.class), eq("product")); // 새 파일 저장 확인
        verify(fileStorageService).deleteFile("/static/product/old-image.jpg"); // 기존 파일 삭제 확인
    }

    @Test
    @DisplayName("상품 수정 실패(예외) - 다른 사람의 상품 수정 시도")
    void modifyProduct_Fail_Unauthorized() {
        // given
        User attacker = User.builder().userId(999L).build();
        Product targetProduct = Product.builder().productId(1L).user(seller).isDeleted(false).build();
        ProductModifyRequest request = new ProductModifyRequest("해킹", ProductCategory.ETC, "S", List.of(), 0);

        given(productRepository.findByIdWithUser(1L)).willReturn(Optional.of(targetProduct));

        // when & then
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.modifyProduct(attacker, 1L, request);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
    }

    @Test
    @DisplayName("소프트 삭제 성공")
    void deleteProduct_Success() {
        // given
        Design designToStop = spy(design);
        designToStop.startSale(); // ON_SALE 상태로 변경
        Product productToDelete = spy(Product.builder().productId(1L).user(seller).design(designToStop).isDeleted(false).build());

        given(productRepository.findByIdWithUser(1L)).willReturn(Optional.of(productToDelete));

        // when
        productService.deleteProduct(seller, 1L);

        // then
        verify(productToDelete).softDelete(); // Product.isDeleted = true 확인
        verify(designToStop).stopSale(); // Design.designState = STOPPED 확인
    }

    @Test
    @DisplayName("재판매 성공")
    void relistProduct_Success() {
        // given
        Design designToRelist = spy(design);
        designToRelist.startSale();
        designToRelist.stopSale(); // STOPPED 상태로 변경
        Product productToRelist = spy(Product.builder().productId(1L).user(seller).design(designToRelist).isDeleted(true).build());

        given(productRepository.findByIdWithUser(1L)).willReturn(Optional.of(productToRelist));

        // when
        productService.relistProduct(seller, 1L);

        // then
        verify(productToRelist).relist(); // Product.isDeleted = false 확인
        verify(designToRelist).relist(); // Design.designState = ON_SALE 확인
    }

    @Test
    @DisplayName("재판매 실패(예외) - 이미 판매 중인 상품을 재판매 시도")
    void relistProduct_Fail_AlreadyOnSale() {
        // given
        Product productOnSale = Product.builder().productId(1L).user(seller).isDeleted(false).build();

        given(productRepository.findByIdWithUser(1L)).willReturn(Optional.of(productOnSale));

        // when & then
        // Product.relist() 내부에서 던지는 예외를 검증
        assertThrows(ServiceException.class, () -> {
            productService.relistProduct(seller, 1L);
        });
    }

//    @Test
//    @DisplayName("상품 상세 조회 성공")
//    void getProductDetail_Success() {
//        // given
//        Long productId = 1L;
//        // 테스트에 사용할 상세 정보가 포함된 Product 객체 생성
//        Product detailedProduct = Product.builder()
//                .productId(productId)
//                .title("테스트 상품")
//                .description("상세 설명입니다.")
//                .price(20000.0)
//                .isDeleted(false)
//                .user(seller) // setUp()에서 생성된 공통 User 객체 사용
//                .design(design) // setUp()에서 생성된 공통 Design 객체 사용
//                .productImages(List.of(
//                        ProductImage.builder().productImageUrl("/static/img1.jpg").build(),
//                        ProductImage.builder().productImageUrl("/static/img2.jpg").build()
//                ))
//                .build();
//
//        // Repository가 findProductDetailById 호출 시 위에서 만든 객체를 반환하도록 설정
//        given(productRepository.findProductDetailById(productId)).willReturn(Optional.of(detailedProduct));
//
//        // when
//        ProductDetailResponse response = productService.getProductDetail(productId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.title()).isEqualTo("테스트 상품");
//        assertThat(response.description()).isEqualTo("상세 설명입니다.");
//        assertThat(response.productImageUrls()).hasSize(2);
//        assertThat(response.productImageUrls()).contains("/static/img1.jpg", "/static/img2.jpg");
//    }

//    @Test
//    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품 ID")
//    void getProductDetail_Fail_NotFound() {
//        // given
//        Long nonExistentProductId = 999L;
//        // Repository가 어떤 Long 값을 받더라도 Optional.empty()를 반환하도록 설정
//        given(productRepository.findProductDetailById(nonExistentProductId)).willReturn(Optional.empty());
//
//        // when & then
//        ServiceException exception = assertThrows(ServiceException.class, () -> {
//            productService.getProductDetail(nonExistentProductId);
//        });
//
//        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
//    }

//    @Test
//    @DisplayName("상품 상세 조회 실패 - 이미 삭제(판매 중지)된 상품")
//    void getProductDetail_Fail_IsDeleted() {
//        // given
//        Long productId = 1L;
//        Product deletedProduct = Product.builder()
//                .productId(productId)
//                .isDeleted(true) // ⚠️ 삭제된 상태의 상품
//                .user(seller)
//                .design(design)
//                .build();
//
//        // Repository는 일단 DB에서 데이터를 찾았다고 가정 (서비스 로직의 방어 코드를 테스트하기 위함)
//        given(productRepository.findProductDetailById(productId)).willReturn(Optional.of(deletedProduct));
//
//        // when & then
//        // Service의 if (product.getIsDeleted()) 분기에서 예외가 발생하는지 검증
//        ServiceException exception = assertThrows(ServiceException.class, () -> {
//            productService.getProductDetail(productId);
//        });
//
//        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
//    }
}