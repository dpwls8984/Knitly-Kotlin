package com.mysite.knitly.domain.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
import com.mysite.knitly.domain.user.entity.Provider
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
import org.mockito.kotlin.*
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
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
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerModule(KotlinModule.Builder().build())

        pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())

        // [수정] User 생성자는 실제 Entity의 주 생성자를 따라야 합니다.
        // (실제 User Entity의 생성자 파라미터 순서/필드에 맞게 조정해야 합니다)
        seller = User(
            userId = 1L,
            name = "판매자",
            socialId = "seller123",
            provider = Provider.GOOGLE, // 임의의 값
            email = "seller@test.com",
        )

        buyer = User(
            userId = 2L,
            name = "구매자",
            socialId = "buyer123",
            provider = Provider.GOOGLE, // 임의의 값
            email = "buyer@test.com",
        )

        // [수정] Design 생성자도 실제 Entity의 주 생성자를 따릅니다.
        design = Design(
            designId = 1L,
            user = seller,
            designState = DesignState.BEFORE_SALE,
            designName = "테스트 도안",
            gridData = "{}"
        )

        // [수정] Product 생성자를 리팩토링한 '새로운' 생성자에 맞게 수정합니다.
        // createdAt, productImages 등은 생성자에서 빠졌습니다.
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
            design = design
        )
        product1::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(product1, LocalDateTime.now().minusDays(1))
        }

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
            design = design
        )
        product2::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(product2, LocalDateTime.now().minusDays(2))
        }

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
            design = design
        )
        product3::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(product3, LocalDateTime.now().minusDays(3))
        }

        // [수정] product4도 새로운 생성자로 생성합니다.
        product4 = Product(
            productId = 4L,
            title = "테스트 상품",
            description = "상세 설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            user = seller,
            design = design,
            isDeleted = false
        )

        product4.addProductImages(
            listOf(
                ProductImage(
                    productImageUrl = "/static/product/old-image.jpg",
                    sortOrder = 1L
                )
            )
        )

        val testCreatedAt = LocalDateTime.now()
        product4::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(product4, testCreatedAt)
        }


        imageFile = MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1_content".toByteArray())
    }

    @Test
    @DisplayName("상품 등록 성공 - 이미지 포함")
    fun registerProduct_Success_WithImages() {

        val request = ProductRegisterRequest(
            title = "새 상품",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            productImageUrls = listOf(imageFile),
            stockQuantity = 10
        )

        given(designRepository.findById(4L)).willReturn(Optional.of(design))

        given(localFileStorage.saveProductImage(any<MultipartFile>()))
            .willReturn("/static/product/mock-image.jpg")

        // [수정] any(Product::class.java) -> any()
        given(productRepository.save(any<Product>())).willAnswer { invocation ->
            val productToSave = invocation.getArgument<Product>(0)
            val savedProduct = Product(
                productId = 4L,
                user = productToSave.user,
                design = productToSave.design,
                title = productToSave.title,
                description = productToSave.description,
                productCategory = productToSave.productCategory,
                sizeInfo = productToSave.sizeInfo,
                price = productToSave.price
            )
            savedProduct.addProductImages(
                listOf(
                    ProductImage(
                        productImageUrl = "/static/product/mock-image.jpg",
                        sortOrder = 1L
                    )
                )
            )
            savedProduct::class.java.getDeclaredField("createdAt").apply {
                isAccessible = true
                set(savedProduct, LocalDateTime.now())
            }
            savedProduct
        }

        val response = productService.registerProduct(seller, 4L, request)

        verify(designRepository).findById(4L)
        verify(productRepository).save(any<Product>())
        verify(localFileStorage, times(1)).saveProductImage(any<MultipartFile>())
        assertThat(design.designState).isEqualTo(DesignState.ON_SALE)

        assertThat(response).isNotNull
        assertThat(response.productId).isEqualTo(4L)
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - Cache Miss (DB 조회 및 캐시 저장)")
    fun getProductDetail_Success_CacheMiss() {
        val cacheKey = CACHE_KEY_PREFIX + "4"
        given(redisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.get(cacheKey)).willReturn(null)
        given(productRepository.findByProductIdAndIsDeletedFalse(4L)).willReturn(product4)
        given(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(1L, 4L)).willReturn(true)
        given(reviewRepository.countByProductAndIsDeletedFalse(product4)).willReturn(5L)

        val response = productService.getProductDetail(seller, 4L)

        verify(productRepository).findByProductIdAndIsDeletedFalse(4L)
        verify(productLikeRepository).existsByUser_UserIdAndProduct_ProductId(1L, 4L)
        verify(reviewRepository).countByProductAndIsDeletedFalse(product4)

        assertThat(response).isNotNull
        val expectedJson = objectMapper.writeValueAsString(response!!)
        verify(valueOperations).set(eq(cacheKey), eq(expectedJson), any<Duration>())

        assertThat(response.title).isEqualTo("테스트 상품")
        assertThat(response.isLikedByUser).isTrue
        assertThat(response.reviewCount).isEqualTo(5)
        assertThat(response.createdAt).isEqualTo(product4.createdAt)
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - Cache Hit (DB 조회 안 함, 캐시된 DTO 그대로 반환)")
    fun getProductDetail_Success_CacheHit() {
        val cacheKey = CACHE_KEY_PREFIX + "4" // product4의 ID는 4L
        given(redisTemplate.opsForValue()).willReturn(valueOperations)

        val imageUrls = listOf("/static/img.jpg")
        val isLiked = true
        var cachedDto = ProductDetailResponse.from(product4, imageUrls, isLiked)

        cachedDto = cachedDto.copy(reviewCount = 5)

        val cachedJson = objectMapper.writeValueAsString(cachedDto)
        given(valueOperations.get(cacheKey)).willReturn(cachedJson)

        val response = productService.getProductDetail(seller, 4L)

        // [수정] anyLong() -> any<Long>()
        verify(productRepository, never()).findByProductIdAndIsDeletedFalse(any<Long>())
        // [수정] anyLong() -> any<Long>()
        verify(productLikeRepository, never()).existsByUser_UserIdAndProduct_ProductId(any<Long>(), any<Long>())
        // [수정] any(Product::class.java) -> any<Product>()
        verify(reviewRepository, never()).countByProductAndIsDeletedFalse(any<Product>())
        // [수정] anyString() -> any<String>(), any(Duration::class.java) -> any<Duration>()
        verify(valueOperations, never()).set(any<String>(), any<String>(), any<Duration>())

        assertThat(response).isNotNull
        assertThat(response!!.title).isEqualTo("테스트 상품")
        assertThat(response.isLikedByUser).isTrue
        assertThat(response.reviewCount).isEqualTo(5)
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 교체 (기존 이미지 삭제, 새 이미지 추가)")
    fun modifyProduct_Success_ImageUpdate() {
        // [수정] spy(product4) -> spy(product4)
        // (spy는 org.mockito.kotlin.*을 임포트했다면 그대로 잘 동작합니다.)
        val productSpy = spy(product4)

        given(productRepository.findById(4L)).willReturn(Optional.of(productSpy))

        val newImage = MockMultipartFile("images", "new.jpg", "image/jpeg", "new_content".toByteArray())

        val request = ProductModifyRequest(
            description = "수정된 설명",
            productCategory = ProductCategory.BOTTOM,
            sizeInfo = "L",
            productImageUrls = listOf(newImage),
            existingImageUrls = emptyList(),
            stockQuantity = 20
        )

        // [수정] any(MultipartFile::class.java) -> any<MultipartFile>()
        given(localFileStorage.saveProductImage(any<MultipartFile>()))
            .willReturn("/static/product/new-image.jpg")

        productService.modifyProduct(seller, 4L, request)

        verify(productSpy).update("수정된 설명", ProductCategory.BOTTOM, "L", 20)
        // [수정] any(MultipartFile::class.java) -> any<MultipartFile>()
        verify(localFileStorage).saveProductImage(any<MultipartFile>())
        verify(localFileStorage).deleteProductImage("/static/product/old-image.jpg")
    }

    @Test
    @DisplayName("소프트 삭제 성공")
    fun deleteProduct_Success() {
        val designToStop = spy(design)
        designToStop.startSale() // 'ON_SALE' 상태로 만듦

        // [수정] '새로운' 생성자로 Product 생성
        val productToDelete = Product(
            productId = 4L,
            user = seller,
            design = designToStop,
            isDeleted = false,
            title = "테스트",
            description = "테스트",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0
        )
        val productSpy = spy(productToDelete)

        given(productRepository.findById(4L)).willReturn(Optional.of(productSpy))

        productService.deleteProduct(seller, 4L)

        verify(productSpy).softDelete()
        verify(designToStop).stopSale()
    }

    @Test
    @DisplayName("재판매 성공")
    fun relistProduct_Success() {
        val designToRelist = spy(
            Design(
                designId = 4L,
                user = seller,
                designState = DesignState.STOPPED, // 'STOPPED' 상태
                designName = "테스트 도안",
                gridData = "{}"
            )
        )

        // [수정] '새로운' 생성자로 Product 생성
        val productToRelist = Product(
            productId = 4L,
            user = seller,
            design = designToRelist,
            isDeleted = true, // 'isDeleted' 상태
            title = "테스트",
            description = "테스트",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0
        )
        val productSpy = spy(productToRelist)

        given(productRepository.findById(4L)).willReturn(Optional.of(productSpy))

        productService.relistProduct(seller, 4L)

        verify(productSpy).relist()
        verify(designToRelist).relist()
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품 ID (Cache Miss & DB Miss)")
    fun getProductDetail_Fail_NotFound() {
        val cacheKey = CACHE_KEY_PREFIX + "999"
        given(redisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.get(cacheKey)).willReturn(null)
        given(productRepository.findByProductIdAndIsDeletedFalse(999L)).willReturn(null)

        val exception = assertThrows(ServiceException::class.java) {
            productService.getProductDetail(seller, 999L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
    }

    //-------------------- Product List 조회 테스트 --------------------
    @Test
    @DisplayName("전체 상품 조회 - 최신순")
    fun getProducts_All_Latest() {
        val productPage = PageImpl(listOf(product1, product2, product3))
        given(productRepository.findAllWithImagesAndNotDeleted(any<Pageable>()))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(3)
        assertThat(result.totalElements).isEqualTo(3)
        verify(productRepository).findAllWithImagesAndNotDeleted(any<Pageable>())
    }

    @Test
    @DisplayName("카테고리별 조회 - 상의만")
    fun getProducts_Category_Top() {
        val productPage = PageImpl(listOf(product1))
        given(
            productRepository.findByCategoryWithImagesAndNotDeleted(
                eq(ProductCategory.TOP), any<Pageable>()
            )
        ).willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, ProductCategory.TOP, ProductFilterType.ALL, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].productCategory).isEqualTo(ProductCategory.TOP)
        verify(productRepository).findByCategoryWithImagesAndNotDeleted(
            eq(ProductCategory.TOP), any<Pageable>()
        )
    }

    @Test
    @DisplayName("무료 상품만 조회")
    fun getProducts_Free() {
        val productPage = PageImpl(listOf(product2))
        given(productRepository.findByPriceWithImagesAndNotDeleted(eq(0.0), any<Pageable>()))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.FREE, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].price).isEqualTo(0.0)
        assertThat(result.content[0].isFree).isTrue
        verify(productRepository).findByPriceWithImagesAndNotDeleted(eq(0.0), any<Pageable>())
    }

    @Test
    @DisplayName("한정판매 상품만 조회")
    fun getProducts_Limited() {
        val productPage = PageImpl(listOf(product3))
        given(productRepository.findLimitedWithImagesAndNotDeleted(any<Pageable>()))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.LIMITED, ProductSortType.LATEST, pageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].stockQuantity).isNotNull
        assertThat(result.content[0].isLimited).isTrue
        verify(productRepository).findLimitedWithImagesAndNotDeleted(any<Pageable>())
    }

    @Test
    @DisplayName("인기순 조회 - Redis 데이터 있음")
    fun getProducts_Popular_WithRedis() {
        val popularIds = listOf(2L, 1L, 3L) // 인기순
        given(redisProductService.getTopNPopularProducts(100)).willReturn(popularIds)
        given(productRepository.findByProductIdInWithImagesAndNotDeleted(popularIds))
            .willReturn(listOf(product2, product1, product3))
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
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
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
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
        given(productRepository.findAllWithImagesAndNotDeleted(any<Pageable>()))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, null, ProductFilterType.ALL, ProductSortType.PRICE_ASC, pageable
        )

        assertThat(result.content).hasSize(3)
        verify(productRepository).findAllWithImagesAndNotDeleted(any<Pageable>())
    }

    @Test
    @DisplayName("filter=FREE이면 카테고리 무시하고 무료 전체에서 조회")
    fun freeFilter_ignoresCategory() {
        val testPageable = PageRequest.of(0, 20)
        val productPage = PageImpl(listOf(product2))
        given(productRepository.findByPriceWithImagesAndNotDeleted(eq(0.0), any<Pageable>()))
            .willReturn(productPage)
        given(productLikeRepository.findLikedProductIdsByUserId(any<Long>(), any()))
            .willReturn(emptySet())

        val result = productService.getProducts(
            buyer, ProductCategory.TOP, ProductFilterType.FREE, ProductSortType.LATEST, testPageable
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].isFree).isTrue
        verify(productRepository).findByPriceWithImagesAndNotDeleted(eq(0.0), any<Pageable>())
    }
}