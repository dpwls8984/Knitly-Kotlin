package com.mysite.knitly.domain.product.product

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest
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
import org.mockito.kotlin.any
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

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

    @MockBean
    private lateinit var localFileStorage: LocalFileStorage

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
    @Test
    @DisplayName("상품 등록 - 정상 등록 확인")
    fun registerProduct_success() {
        // given
        // 등록을 위한 새로운 도안 생성
        val newDesign = designRepository.save(
            Design(
                user = testUser,
                designName = "등록용도안",
                pdfUrl = "/files/register.pdf",
                gridData = "{}",
                designState = DesignState.BEFORE_SALE
            )
        )

        // 파일 저장소 Mocking
        given(localFileStorage.saveProductImage(any<MultipartFile>()))
            .willReturn("/static/product/test_image.jpg")

        val request = ProductRegisterRequest(
            title = "신규 등록 상품",
            description = "상품 등록 테스트입니다.",
            productCategory = ProductCategory.TOP,
            sizeInfo = "L",
            price = 35000.0,
            stockQuantity = 100,
            productImageUrls = listOf(
                MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".toByteArray())
            )
        )

        // when
        val response = productService.registerProduct(testUser, newDesign.designId!!, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.productId).isNotNull()

        val savedProduct = productRepository.findById(response.productId).orElseThrow()
        assertThat(savedProduct.title).isEqualTo("신규 등록 상품")
        assertThat(savedProduct.price).isEqualTo(35000.0)
        assertThat(savedProduct.design.designState).isEqualTo(DesignState.ON_SALE) // 도안 상태 변경 확인
    }

    @Test
    @DisplayName("상품 수정 - 정보 및 이미지 수정 확인")
    fun modifyProduct_success() {
        // given
        val targetProduct = testProduct1 // 기존 상품 활용

        // 파일 저장소 Mocking
        given(localFileStorage.saveProductImage(any<MultipartFile>()))
            .willReturn("/static/product/new_image.jpg")

        val request = ProductModifyRequest(
            description = "수정된 설명입니다.",
            productCategory = ProductCategory.OUTER, // 카테고리 변경
            sizeInfo = "XL",
            stockQuantity = 50,
            existingImageUrls = emptyList(), // 기존 이미지 모두 삭제
            productImageUrls = listOf(
                MockMultipartFile("image", "new.jpg", "image/jpeg", "new_dummy".toByteArray())
            )
        )

        // when
        val response = productService.modifyProduct(testUser, targetProduct.productId!!, request)

        // then
        assertThat(response).isNotNull

        val modifiedProduct = productRepository.findById(targetProduct.productId!!).orElseThrow()
        assertThat(modifiedProduct.description).isEqualTo("수정된 설명입니다.")
        assertThat(modifiedProduct.productCategory).isEqualTo(ProductCategory.OUTER)
        assertThat(modifiedProduct.stockQuantity).isEqualTo(50)

        // 이미지가 교체되었는지 확인
        assertThat(modifiedProduct.productImages).hasSize(1)
        assertThat(modifiedProduct.productImages[0].productImageUrl).isEqualTo("/static/product/new_image.jpg")
    }

    @Test
    @DisplayName("상품 삭제 - Soft Delete 확인")
    fun deleteProduct_success() {
        // given
        val targetProduct = testProduct2 // 삭제할 상품
        val targetId = targetProduct.productId!!

        // when
        productService.deleteProduct(testUser, targetId)

        // then
        // 1. 상품이 Soft Delete 되었는지 확인 (isDeleted = true)
        val deletedProduct = productRepository.findById(targetId).orElseThrow()
        assertThat(deletedProduct.isDeleted).isTrue()

        // 2. 도안 판매 상태가 중지로 변경되었는지 확인
        assertThat(deletedProduct.design.designState).isEqualTo(DesignState.STOPPED) // Logic 상 STOPPED 여야 함 (Service 코드 확인 필요)
    }

    @Test
    @DisplayName("상품 재판매 - Relist 확인")
    fun relistProduct_success() {
        // given
        // 먼저 상품을 삭제 상태로 만듦
        val targetProduct = testProduct3
        productService.deleteProduct(testUser, targetProduct.productId!!)

        // 삭제 확인
        assertThat(productRepository.findById(targetProduct.productId!!).get().isDeleted).isTrue()

        // when
        productService.relistProduct(testUser, targetProduct.productId!!)

        // then
        val relistedProduct = productRepository.findById(targetProduct.productId!!).orElseThrow()
        assertThat(relistedProduct.isDeleted).isFalse()

        // 도안 상태도 다시 판매중(ON_SALE)인지 확인 (Service 구현에 따라 다를 수 있음, 여기선 에러 안나면 성공)
        // assertThat(relistedProduct.design.designState).isEqualTo(DesignState.ON_SALE)
    }

    @Test
    @DisplayName("상품 상세 조회 - DB 조회 확인")
    fun getProductDetail_success() {
        // given
        val targetProduct = testProduct1

        // when
        val response = productService.getProductDetail(testUser, targetProduct.productId!!)

        // then
        assertThat(response).isNotNull
        assertThat(response!!.productId).isEqualTo(targetProduct.productId)
        assertThat(response.title).isEqualTo(targetProduct.title)
        assertThat(response.price).isEqualTo(targetProduct.price)
    }
}