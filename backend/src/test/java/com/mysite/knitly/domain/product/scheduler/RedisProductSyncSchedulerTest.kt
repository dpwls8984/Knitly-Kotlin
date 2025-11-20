package com.mysite.knitly.domain.product.scheduler

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.scheduler.RedisProductSyncScheduler
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@ExtendWith(MockitoExtension::class)
class RedisProductSyncSchedulerTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var redisProductService: RedisProductService

    private lateinit var scheduler: RedisProductSyncScheduler

    private lateinit var mockProducts: List<Product>
    private lateinit var testUser: User
    private lateinit var testDesign: Design

    @BeforeEach
    fun setUp() {
        scheduler = RedisProductSyncScheduler(
            productRepository = productRepository,
            redisProductService = redisProductService
        )

        testUser = User.builder()
            .userId(1L)
            .name("테스트유저")
            .email("test@test.com")
            .provider(Provider.GOOGLE)
            .build()

        testDesign = Design(
            designId = 1L,
            user = testUser,
            pdfUrl = "/files/design.pdf",
            designState = DesignState.ON_SALE,
            designType = null,
            designName = "테스트도안",
            gridData = "[]"
        )

        mockProducts = listOf(
            createProduct(1L, 100),
            createProduct(2L, 200),
            createProduct(3L, 150)
        )
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - 정상 동작")
    fun initializeRedisData_Success() {
        // given
        val firstPage = PageImpl(mockProducts, Pageable.ofSize(1000), 3)
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(firstPage)
        doNothing().whenever(redisProductService).syncFromDatabase(any())

        // when
        scheduler.initializedRedisData()

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService).syncFromDatabase(argThat { list ->
            list != null && list.size == 3
        })
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - 상품 없음")
    fun initializeRedisData_NoProducts() {
        // given
        val emptyPage = PageImpl<Product>(emptyList())
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(emptyPage)

        // when
        scheduler.initializedRedisData()

        // then
        verify(productRepository).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService, never()).syncFromDatabase(any())
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - DB 조회 실패")
    fun initializeRedisData_DbError() {
        // given
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenThrow(RuntimeException("Database error"))

        // when - 예외가 발생해도 애플리케이션은 정상 시작되어야 함
        scheduler.initializedRedisData()

        // then
        verify(productRepository).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService, never()).syncFromDatabase(any())
    }

    @Test
    @DisplayName("애플리케이션 시작 시 초기 데이터 로딩 - Redis 동기화 실패")
    fun initializeRedisData_RedisSyncError() {
        // given
        val firstPage = PageImpl(mockProducts, Pageable.ofSize(1000), 3)
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(firstPage)
        doThrow(RuntimeException("Redis sync error"))
            .whenever(redisProductService).syncFromDatabase(any())

        // when - 예외가 발생해도 애플리케이션은 정상 시작되어야 함
        scheduler.initializedRedisData()

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService).syncFromDatabase(any())
    }

    @Test
    @DisplayName("애플리케이션 시작 시 - 대량 데이터 페이징 처리")
    fun initializeRedisData_LargeDataset() {
        // given - 2500개 상품 (3페이지)
        val page1Products = createProductList(1, 1000)
        val page2Products = createProductList(1001, 2000)
        val page3Products = createProductList(2001, 2500)

        val page1 = PageImpl(page1Products, PageRequest.of(0, 1000), 2500)
        val page2 = PageImpl(page2Products, PageRequest.of(1, 1000), 2500)
        val page3 = PageImpl(page3Products, PageRequest.of(2, 1000), 2500)

        // 페이지별로 반환 설정
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(page1, page2, page3)

        doNothing().whenever(redisProductService).syncFromDatabase(any())

        // when
        scheduler.initializedRedisData()

        // then
        verify(productRepository, times(3)).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService).syncFromDatabase(argThat { list ->
            list != null && list.size == 2500
        })
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - 정상 동작")
    fun syncPurchaseCountFromDB_Success() {
        // given
        val firstPage = PageImpl(mockProducts, Pageable.ofSize(1000), 3)
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(firstPage)
        doNothing().whenever(redisProductService).syncFromDatabase(any())

        // when
        scheduler.syncPurchaseCountFromDB()

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService).syncFromDatabase(argThat { list ->
            list != null && list.size == 3
        })
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - 상품 없음")
    fun syncPurchaseCountFromDB_NoProducts() {
        // given
        val emptyPage = PageImpl<Product>(emptyList())
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(emptyPage)

        // when
        scheduler.syncPurchaseCountFromDB()

        // then
        verify(productRepository).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService, never()).syncFromDatabase(any())
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - DB 조회 실패")
    fun syncPurchaseCountFromDB_DbError() {
        // given
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenThrow(RuntimeException("Database error"))

        // when - 예외가 발생해도 스케줄러는 다음 실행을 기다려야 함
        scheduler.syncPurchaseCountFromDB()

        // then
        verify(productRepository).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService, never()).syncFromDatabase(any())
    }

    @Test
    @DisplayName("정기 동기화 스케줄러 - Redis 동기화 실패")
    fun syncPurchaseCountFromDB_RedisSyncError() {
        // given
        val firstPage = PageImpl(mockProducts, Pageable.ofSize(1000), 3)
        whenever(productRepository.findByIsDeletedFalse(any<Pageable>()))
            .thenReturn(firstPage)
        doThrow(RuntimeException("Redis sync error"))
            .whenever(redisProductService).syncFromDatabase(any())

        // when - 예외가 발생해도 스케줄러는 다음 실행을 기다려야 함
        scheduler.syncPurchaseCountFromDB()

        // then
        verify(productRepository, atLeastOnce()).findByIsDeletedFalse(any<Pageable>())
        verify(redisProductService).syncFromDatabase(any())
    }

    private fun createProduct(productId: Long, purchaseCount: Int): Product {
        return Product(
            productId = productId,
            title = "상품$productId",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            purchaseCount = purchaseCount,
            isDeleted = false,
            user = testUser,
            design = testDesign,
        )
    }

    private fun createProductList(startId: Int, endId: Int): List<Product> {
        return (startId..endId).map { i ->
            createProduct(i.toLong(), i * 10)
        }
    }
}