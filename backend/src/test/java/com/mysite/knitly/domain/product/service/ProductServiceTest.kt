package com.mysite.knitly.domain.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.dto.*
import com.mysite.knitly.domain.product.product.entity.*
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var redisProductService: RedisProductService

    @Mock
    private lateinit var productLikeRepository: ProductLikeRepository

    @Mock
    private lateinit var designRepository: DesignRepository

    @Mock
    private lateinit var localFileStorage: LocalFileStorage

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @Spy
    private val objectMapper = ObjectMapper()

    @InjectMocks
    private lateinit var productService: ProductService

    private lateinit var seller: User
    private lateinit var buyer: User
    private lateinit var design: Design
    private lateinit var product1: Product
    private lateinit var product2: Product
    private lateinit var product3: Product
    private lateinit var product4: Product
    private lateinit var imageFile: MockMultipartFile
    private lateinit var pageable: Pageable

    companion object {
        private const val CACHE_KEY_PREFIX = "product:detail:"
    }

    @BeforeEach
    fun setUp() {
        pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())

        seller = User(
            userId = 1L,
            name = "판매자"
        )

        buyer = User(
            userId = 2L,
            name = "구매자"
        )

        design = Design(
            designId = 1L,
            user = seller,
            designState = DesignState.BEFORE_SALE,
            designName = "테스트 도안",
            gridData = "{}"
        )

        product1 = Product(
            productId = 1L,
            title = "상의 패턴 1",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            user = seller,
            purchaseCount = 100,
            likeCount = 50,
            isDeleted = false,
            design = design,
            productImages = mutableListOf()
        )

        product2 = Product(
            productId = 2L,
            title = "무료 패턴",
            description = "설명",
            productCategory = ProductCategory.BOTTOM,
            sizeInfo = "M",
            price = 0.0,
            user = seller,
            purchaseCount = 200,
            likeCount = 80,
            isDeleted = false,
            design = design,
            productImages = mutableListOf()
        )

        product3 = Product(
            productId = 3L,
            title = "한정판매 패턴",
            description = "설명",
            productCategory = ProductCategory.OUTER,
            sizeInfo = "M",
            price = 15000.0,
            user = seller,
            stockQuantity = 10,
            purchaseCount = 150,
            likeCount = 60,
            isDeleted = false,
            design = design,
            productImages = mutableListOf()
        )

        product4 = Product(
            productId = 4L,
            title = "테스트 상품",
            description = "상세 설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            user = seller,
            design = design,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            productImages = mutableListOf(
                ProductImage(
                    productImageUrl = "/static/product/old-image.jpg",
                    sortOrder = 1L
                )
            )
        )

        imageFile = MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1_content".toByteArray())
    }

    // TODO : 예진
//    @Test
//    @DisplayName("상품 등록 성공 - 이미지 포함")
//    fun registerProduct_Success_WithImages() {
//        val request = ProductRegisterRequest(
//            title = "새 상품",
//            description = "설명",
//            productCategory = ProductCategory.TOP,
//            sizeInfo = "M",
//            price = 10000.0,
//            productImageUrls = listOf(imageFile),
//            stockQuantity = 10
//        )
//
//        given(designRepository.findById(4L)).willReturn(Optional.of(design))
//
//        given(localFileStorage.saveProductImage(any(MultipartFile::class.java)))
//            .willReturn("/static/product/mock-image.jpg")
//
//        given(productRepository.save(any(Product::class.java))).willAnswer { invocation ->
//            val productToSave = invocation.getArgument<Product>(0)
//            Product(
//                productId = 4L,
//                user = productToSave.user,
//                design = productToSave.design,
//                title = productToSave.title,
//                description = productToSave.description,
//                productCategory = productToSave.productCategory,
//                sizeInfo = productToSave.sizeInfo,
//                price = productToSave.price,
//                createdAt = LocalDateTime.now(),
//                productImages = mutableListOf(
//                    ProductImage(
//                        productImageUrl = "/static/product/mock-image.jpg",
//                        sortOrder = 1L
//                    )
//                )
//            )
//        }
//
//        val response = productService.registerProduct(seller, 4L, request)
//
//        verify(designRepository).findById(4L)
//        verify(productRepository).save(any(Product::class.java))
//        verify(localFileStorage, times(1)).saveProductImage(any(MultipartFile::class.java))
//        assertThat(design.designState).isEqualTo(DesignState.ON_SALE)
//
//        assertThat(response).isNotNull
//        assertThat(response.productId).isEqualTo(4L)
//    }


    // TODO: 예진
//    @Test
//    @DisplayName("상품 수정 성공 - 이미지 교체 (기존 이미지 삭제, 새 이미지 추가)")
//    fun modifyProduct_Success_ImageUpdate() {
//        given(productRepository.findById(4L)).willReturn(Optional.of(spy(product4)))
//        val productSpy = productRepository.findById(4L).get()
//
//        val newImage = MockMultipartFile("images", "new.jpg", "image/jpeg", "new_content".toByteArray())
//
//        val request = ProductModifyRequest(
//            description = "수정된 설명",
//            productCategory = ProductCategory.BOTTOM,
//            sizeInfo = "L",
//            productImageUrls = listOf(newImage),
//            existingImageUrls = emptyList(),
//            stockQuantity = 20
//        )
//
//        given(localFileStorage.saveProductImage(any(MultipartFile::class.java)))
//            .willReturn("/static/product/new-image.jpg")
//
//        productService.modifyProduct(seller, 4L, request)
//
//        verify(productSpy).update("수정된 설명", ProductCategory.BOTTOM, "L", 20)
//        verify(localFileStorage).saveProductImage(any(MultipartFile::class.java))
//        verify(localFileStorage).deleteProductImage("/static/product/old-image.jpg")
//    }
//
//    @Test
//    @DisplayName("소프트 삭제 성공")
//    fun deleteProduct_Success() {
//        val designToStop = spy(design)
//        designToStop.startSale()
//        val productToDelete = spy(
//            Product(
//                productId = 4L,
//                user = seller,
//                design = designToStop,
//                isDeleted = false,
//                title = "테스트",
//                description = "테스트",
//                productCategory = ProductCategory.TOP,
//                sizeInfo = "M",
//                price = 10000.0
//            )
//        )
//
//        given(productRepository.findById(4L)).willReturn(Optional.of(productToDelete))
//
//        productService.deleteProduct(seller, 4L)
//
//        verify(productToDelete).softDelete()
//        verify(designToStop).stopSale()
//    }
//
//    @Test
//    @DisplayName("재판매 성공")
//    fun relistProduct_Success() {
//        val designToRelist = spy(
//            Design(
//                designId = 4L,
//                user = seller,
//                designState = DesignState.STOPPED,
//                designName = "테스트 도안",
//                gridData = "{}"
//            )
//        )
//
//        val productToRelist = spy(
//            Product(
//                productId = 4L,
//                user = seller,
//                design = designToRelist,
//                isDeleted = true,
//                title = "테스트",
//                description = "테스트",
//                productCategory = ProductCategory.TOP,
//                sizeInfo = "M",
//                price = 10000.0
//            )
//        )
//
//        given(productRepository.findById(4L)).willReturn(Optional.of(productToRelist))
//
//        productService.relistProduct(seller, 4L)
//
//        verify(productToRelist).relist()
//        verify(designToRelist).relist()
//    }
//
//    @Test
//    @DisplayName("상품 상세 조회 성공 - Cache Miss (DB 조회 및 캐시 저장)")
//    fun getProductDetail_Success_CacheMiss() {
//        val cacheKey = CACHE_KEY_PREFIX + "1"
//        given(redisTemplate.opsForValue()).willReturn(valueOperations)
//        given(valueOperations.get(cacheKey)).willReturn(null)
//        given(productRepository.findByProductIdAndIsDeletedFalse(4L)).willReturn(product4)
//        given(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(4L, 4L)).willReturn(true)
//        given(reviewRepository.countByProductAndIsDeletedFalse(product4)).willReturn(5L)
//
//        val response = productService.getProductDetail(seller, 4L)
//
//        verify(productRepository).findByProductIdAndIsDeletedFalse(4L)
//        verify(productLikeRepository).existsByUser_UserIdAndProduct_ProductId(4L, 4L)
//        verify(reviewRepository).countByProductAndIsDeletedFalse(product4)
//
//        val expectedJson = objectMapper.writeValueAsString(response)
//        verify(valueOperations).set(eq(cacheKey), eq(expectedJson), any(Duration::class.java))
//
//        assertThat(response).isNotNull
//        assertThat(response.title).isEqualTo("테스트 상품")
//        assertThat(response.isLikedByUser).isTrue
//        assertThat(response.reviewCount).isEqualTo(5)
//        assertThat(response.createdAt).isEqualTo(product4.createdAt.toString())
//    }
//
//    @Test
//    @DisplayName("상품 상세 조회 성공 - Cache Hit (DB 조회 안 함, 캐시된 DTO 그대로 반환)")
//    fun getProductDetail_Success_CacheHit() {
//        val cacheKey = CACHE_KEY_PREFIX + "1"
//        given(redisTemplate.opsForValue()).willReturn(valueOperations)
//
//        val imageUrls = listOf("/static/img.jpg")
//        val isLiked = true
//        var cachedDto = ProductDetailResponse.from(product4, imageUrls, isLiked)
//
//        cachedDto = ProductDetailResponse(
//            productId = cachedDto.productId,
//            title = cachedDto.title,
//            description = cachedDto.description,
//            productCategory = cachedDto.productCategory,
//            sizeInfo = cachedDto.sizeInfo,
//            price = cachedDto.price,
//            createdAt = cachedDto.createdAt,
//            stockQuantity = cachedDto.stockQuantity,
//            likeCount = cachedDto.likeCount,
//            isLikedByUser = cachedDto.isLikedByUser,
//            avgReviewRating = cachedDto.avgReviewRating,
//            productImageUrls = cachedDto.productImageUrls,
//            reviewCount = 5
//        )
//
//        val cachedJson = objectMapper.writeValueAsString(cachedDto)
//        given(valueOperations.get(cacheKey)).willReturn(cachedJson)
//
//        val response = productService.getProductDetail(seller, 4L)
//
//        verify(productRepository, never()).findByProductIdAndIsDeletedFalse(anyLong())
//        verify(productLikeRepository, never()).existsByUser_UserIdAndProduct_ProductId(anyLong(), anyLong())
//        verify(reviewRepository, never()).countByProductAndIsDeletedFalse(any(Product::class.java))
//        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration::class.java))
//
//        assertThat(response).isNotNull
//        assertThat(response.title).isEqualTo("테스트 상품")
//        assertThat(response.isLikedByUser).isTrue
//        assertThat(response.reviewCount).isEqualTo(5)
//    }
//
//    @Test
//    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품 ID (Cache Miss & DB Miss)")
//    fun getProductDetail_Fail_NotFound() {
//        val cacheKey = CACHE_KEY_PREFIX + "999"
//        given(redisTemplate.opsForValue()).willReturn(valueOperations)
//        given(valueOperations.get(cacheKey)).willReturn(null)
//        given(productRepository.findByProductIdAndIsDeletedFalse(999L)).willReturn(null)
//
//        val exception = assertThrows(ServiceException::class.java) {
//            productService.getProductDetail(seller, 999L)
//        }
//
//        assertThat(exception.errorCode).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
//    }
    @Test
    @DisplayName("전체 상품 조회 - 최신순")
    fun getProducts_All_Latest() {
        val productPage = PageImpl(listOf(product1, product2, product3))
        given(productRepository.findAllWithImagesAndNotDeleted(any(Pageable::class.java)))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(3)
        assertThat(result.totalElements).isEqualTo(3)
        verify(productRepository).findAllWithImagesAndNotDeleted(any(Pageable::class.java))
    }

    @Test
    @DisplayName("카테고리별 조회 - 상의만")
    fun getProducts_Category_Top() {
        val productPage = PageImpl(listOf(product1))
        given(
            productRepository.findByCategoryWithImagesAndNotDeleted(
                eq(ProductCategory.TOP), any(Pageable::class.java)
            )
        ).willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, ProductCategory.TOP, ProductFilterType.ALL, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].productCategory).isEqualTo(ProductCategory.TOP)
        verify(productRepository).findByCategoryWithImagesAndNotDeleted(
            eq(ProductCategory.TOP), any(Pageable::class.java)
        )
    }

    @Test
    @DisplayName("무료 상품만 조회")
    fun getProducts_Free() {
        val productPage = PageImpl(listOf(product2))
        given(productRepository.findByPriceWithImagesAndNotDeleted(eq(0.0), any(Pageable::class.java)))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.FREE, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].price).isEqualTo(0.0)
        assertThat(result.content[0].isFree).isTrue
        verify(productRepository).findByPriceWithImagesAndNotDeleted(eq(0.0), any(Pageable::class.java))
    }

    @Test
    @DisplayName("한정판매 상품만 조회")
    fun getProducts_Limited() {
        val productPage = PageImpl(listOf(product3))
        given(productRepository.findLimitedWithImagesAndNotDeleted(any(Pageable::class.java)))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.LIMITED, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].stockQuantity).isNotNull
        assertThat(result.content[0].isLimited).isTrue
        verify(productRepository).findLimitedWithImagesAndNotDeleted(any(Pageable::class.java))
    }

    @Test
    @DisplayName("인기순 조회 - Redis 데이터 있음")
    fun getProducts_Popular_WithRedis() {
        val popularIds = listOf(2L, 1L, 3L) // 인기순
        given(redisProductService.getTopNPopularProducts(100)).willReturn(popularIds)
        given(productRepository.findByProductIdInWithImagesAndNotDeleted(popularIds))
            .willReturn(listOf(product2, product1, product3))
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.POPULAR, pageable
        )

        assertThat(result.content).hasSize(3)
        assertThat(result.content[0].productId).isEqualTo(2L) // 가장 인기있는 상품
        verify(redisProductService).getTopNPopularProducts(100)
        verify(productRepository).findByProductIdInWithImagesAndNotDeleted(popularIds)
    }

    @Test
    @DisplayName("인기순 조회 - Redis 데이터 없음, DB에서 조회")
    fun getProducts_Popular_WithoutRedis() {
        given(redisProductService.getTopNPopularProducts(100)).willReturn(emptyList())

        val top100 = PageRequest.of(0, 100, Sort.by("purchaseCount").descending())
        val productPage = PageImpl(listOf(product2, product3, product1))
        given(productRepository.findAllWithImagesAndNotDeleted(top100))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.POPULAR, pageable
        )

        assertThat(result.content).hasSize(3)
        verify(redisProductService).getTopNPopularProducts(100)
        verify(productRepository).findAllWithImagesAndNotDeleted(top100)
    }

    @Test
    @DisplayName("가격 낮은순 정렬")
    fun getProducts_SortByPrice_Asc() {
        val productPage = PageImpl(listOf(product2, product1, product3))
        given(productRepository.findAllWithImagesAndNotDeleted(any(Pageable::class.java)))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.PRICE_ASC, pageable
        )

        assertThat(result.content).hasSize(3)
        verify(productRepository).findAllWithImagesAndNotDeleted(any(Pageable::class.java))
    }

    @Test
    @DisplayName("filter=FREE이면 카테고리 무시하고 무료 전체에서 조회")
    fun freeFilter_ignoresCategory() {
        val testPageable = PageRequest.of(0, 20)
        val productPage = PageImpl(listOf(product2))
        given(productRepository.findByPriceWithImagesAndNotDeleted(eq(0.0), any(Pageable::class.java)))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(anyLong(), anyList()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, ProductCategory.TOP, ProductFilterType.FREE, ProductSortType.LATEST, testPageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].isFree).isTrue
        verify(productRepository).findByPriceWithImagesAndNotDeleted(eq(0.0), any(Pageable::class.java))
    }
}