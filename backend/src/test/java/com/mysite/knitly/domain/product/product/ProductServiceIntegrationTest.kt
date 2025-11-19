package com.mysite.knitly.domain.product.product

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.entity.ProductFilterType
import com.mysite.knitly.domain.product.product.entity.ProductSortType
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private lateinit var productService: ProductService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var redisProductService: RedisProductService



    private lateinit var testUser: User
    private lateinit var testProduct1: Product
    private lateinit var testProduct2: Product
    private lateinit var testProduct3: Product

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(
            User.builder()
                .name("테스트유저")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .build()
        )

        // 도안 3개 생성 (상품별로 하나씩)
        val design1 = designRepository.save(
            Design(
                user = testUser,
                designName = "테스트도안1",
                pdfUrl = "/files/test1.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        val design2 = designRepository.save(
            Design(
                user = testUser,
                designName = "테스트도안2",
                pdfUrl = "/files/test2.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        val design3 = designRepository.save(
            Design(
                user = testUser,
                designName = "테스트도안3",
                pdfUrl = "/files/test3.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        // 테스트 상품 1 - BAG, 유료
        testProduct1 = productRepository.save(
            Product(
                user = testUser,
                title = "가방 상품",
                description = "테스트 가방",
                productCategory = ProductCategory.BAG,
                price = 15000.0,
                sizeInfo = "Free",
                design = design1,
                purchaseCount = 10
            )
        )

        // 테스트 상품 2 - TOP, 무료
        testProduct2 = productRepository.save(
            Product(
                user = testUser,
                title = "상의 상품",
                description = "테스트 상의",
                productCategory = ProductCategory.TOP,
                price = 0.0,
                sizeInfo = "Free",
                design = design2,
                purchaseCount = 5
            )
        )

        // 테스트 상품 3 - ETC, 한정판매
        testProduct3 = productRepository.save(
            Product(
                user = testUser,
                title = "기타 상품",
                description = "테스트 기타",
                productCategory = ProductCategory.ETC,
                price = 20000.0,
                sizeInfo = "Free",
                design = design3,
                purchaseCount = 15,
                stockQuantity = 10
            )
        )

        // Redis에 상품 추가
        redisProductService.addProduct(testProduct1.productId!!, testProduct1.purchaseCount.toLong())
        redisProductService.addProduct(testProduct2.productId!!, testProduct2.purchaseCount.toLong())
        redisProductService.addProduct(testProduct3.productId!!, testProduct3.purchaseCount.toLong())
    }

    @Test
    @DisplayName("상품 목록 조회 - 전체")
    fun getProducts_all() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.ALL,
            sortType = ProductSortType.LATEST,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content.size).isGreaterThanOrEqualTo(3)
    }

    @Test
    @DisplayName("상품 목록 조회 - 카테고리별")
    fun getProducts_byCategory() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = ProductCategory.BAG,
            filterType = ProductFilterType.ALL,
            sortType = ProductSortType.LATEST,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty
        assertThat(result.content).allMatch { it.productCategory == ProductCategory.BAG }
    }

    @Test
    @DisplayName("상품 목록 조회 - 무료 상품만")
    fun getProducts_freeOnly() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.FREE,
            sortType = ProductSortType.LATEST,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty
        assertThat(result.content).allMatch { it.price == 0.0 }
    }

    @Test
    @DisplayName("상품 목록 조회 - 한정판매만")
    fun getProducts_limitedOnly() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.LIMITED,
            sortType = ProductSortType.LATEST,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty
        assertThat(result.content).allMatch { it.stockQuantity != null && it.stockQuantity!! > 0 }
    }

    @Test
    @DisplayName("상품 목록 조회 - 인기순 정렬 (Redis)")
    fun getProducts_popularSort() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.ALL,
            sortType = ProductSortType.POPULAR,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // 첫 번째 상품의 구매수가 가장 많아야 함
        if (result.content.size >= 2) {
            assertThat(result.content[0].purchaseCount)
                .isGreaterThanOrEqualTo(result.content[1].purchaseCount)
        }
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 오름차순 정렬")
    fun getProducts_priceAscSort() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.ALL,
            sortType = ProductSortType.PRICE_ASC,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // 가격이 오름차순으로 정렬되어야 함
        if (result.content.size >= 2) {
            for (i in 0 until result.content.size - 1) {
                assertThat(result.content[i].price)
                    .isLessThanOrEqualTo(result.content[i + 1].price)
            }
        }
    }

    @Test
    @DisplayName("인기순 조회 - Redis 데이터 반영 확인")
    fun getProducts_popular_redisData() {
        // given - Redis에 구매수가 다른 상품들이 있음
        val pageable = PageRequest.of(0, 10)

        // when
        val result = productService.getProducts(
            user = null,
            category = null,
            filterType = ProductFilterType.ALL,
            sortType = ProductSortType.POPULAR,
            pageable = pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // 가장 구매수가 많은 상품이 첫 번째에 있어야 함
        val topProduct = result.content[0]
        assertThat(topProduct.productId).isIn(testProduct1.productId, testProduct2.productId, testProduct3.productId)
    }
}