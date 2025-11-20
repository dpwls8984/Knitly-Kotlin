package com.mysite.knitly.domain.design

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.dto.DesignRequest
import com.mysite.knitly.domain.design.dto.DesignUploadRequest
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.service.DesignService
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DesignServiceIntegrationTest {

    @Autowired
    private lateinit var designService: DesignService

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var localFileStorage: LocalFileStorage

    @Autowired
    private lateinit var redisProductService: RedisProductService

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(
            User.builder()
                .name("테스트유저")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .build()
        )
    }

    @Test
    @DisplayName("도안 생성 - 정상")
    fun createDesign_success() {
        // given
        val request = DesignRequest(
            designName = "하트 패턴",
            gridData = create10x10Grid(),
            fileName = "heart_pattern"
        )

        // when
        val response = designService.createDesign(testUser, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.designName).isEqualTo("하트 패턴")
        assertThat(response.designState).isEqualTo(DesignState.BEFORE_SALE)
        assertThat(response.pdfUrl).isNotNull()

        // DB 확인
        val savedDesign = designRepository.findById(response.designId!!).get()
        assertThat(savedDesign.designName).isEqualTo("하트 패턴")
        assertThat(savedDesign.designState).isEqualTo(DesignState.BEFORE_SALE)
    }

    @Test
    @DisplayName("도안 생성 - 잘못된 그리드 크기")
    fun createDesign_invalidGridSize() {
        // given
        val request = DesignRequest(
            designName = "잘못된 도안",
            gridData = listOf(listOf("A")),  // 10x10이 아님
            fileName = "invalid"
        )

        // when & then
        assertThatThrownBy { designService.createDesign(testUser, request) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DESIGN_INVALID_GRID_SIZE)
    }

    @Test
    @DisplayName("PDF 업로드 - 정상")
    fun uploadPdfDesign_success() {
        // given
        val pdfContent = "PDF 내용".toByteArray()
        val file = MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            pdfContent
        )
        val request = DesignUploadRequest(
            designName = "업로드 도안",
            pdfFile = file
        )

        // when
        val response = designService.uploadPdfDesign(testUser, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.designName).contains("업로드 도안")
        assertThat(response.pdfUrl).isNotNull()

        // 실제 파일 저장 확인
        val savedDesign = designRepository.findById(response.designId!!).get()
        assertThat(savedDesign.pdfUrl).isNotNull()
    }

    @Test
    @DisplayName("도안 삭제 - BEFORE_SALE 상태일 때만 가능")
    fun deleteDesign_onlyBeforeSale() {
        // given - BEFORE_SALE 상태
        val design = designRepository.save(
            Design(
                user = testUser,
                designName = "삭제 테스트",
                pdfUrl = "/files/test.pdf",
                gridData = "{}",
                designState = DesignState.BEFORE_SALE
            )
        )

        // when
        designService.deleteDesign(testUser, design.designId!!)

        // then
        assertThat(designRepository.findById(design.designId!!)).isEmpty
    }

    @Test
    @DisplayName("도안 삭제 - ON_SALE 상태면 실패")
    fun deleteDesign_failsWhenOnSale() {
        // given - ON_SALE 상태
        val design = designRepository.save(
            Design(
                user = testUser,
                designName = "판매중 도안",
                pdfUrl = "/files/test.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        // when & then
        assertThatThrownBy { designService.deleteDesign(testUser, design.designId!!) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DESIGN_NOT_DELETABLE)
    }

    @Test
    @DisplayName("판매 중지 - Redis에서 상품 제거 확인")
    fun stopDesignSale_removesFromRedis() {
        // given - ON_SALE 도안과 상품 생성
        val design = designRepository.save(
            Design(
                user = testUser,
                designName = "판매중 도안",
                pdfUrl = "/files/test.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        val product = productRepository.save(
            Product(
                user = testUser,
                title = "테스트 상품",
                description = "설명",
                productCategory = ProductCategory.BAG,
                price = 10000.0,
                sizeInfo = "Free",
                design = design,
                purchaseCount = 5
            )
        )
        design.assignProduct(product)
        designRepository.save(design)

        // Redis에 상품 추가
        redisProductService.addProduct(product.productId!!, 5)

        // when - 판매 중지
        designService.stopDesignSale(testUser, design.designId!!)

        // then
        val updatedDesign = designRepository.findById(design.designId!!).get()
        assertThat(updatedDesign.designState).isEqualTo(DesignState.STOPPED)

        // Redis에서 제거되었는지 확인
        val score = redisTemplate.opsForZSet().score("product:popular", product.productId.toString())
        assertThat(score).isNull()
    }

    @Test
    @DisplayName("판매 재개 - Redis에 상품 추가 확인")
    fun relistDesign_addsToRedis() {
        // given - STOPPED 도안과 상품 생성
        val design = designRepository.save(
            Design(
                user = testUser,
                designName = "중지된 도안",
                pdfUrl = "/files/test.pdf",
                gridData = "{}",
                designState = DesignState.STOPPED
            )
        )

        val product = productRepository.save(
            Product(
                user = testUser,
                title = "테스트 상품",
                description = "설명",
                productCategory = ProductCategory.BAG,
                price = 10000.0,
                sizeInfo = "Free",
                design = design,
                purchaseCount = 10
            )
        )
        design.assignProduct(product)
        designRepository.save(design)

        // when - 판매 재개
        designService.relistDesign(testUser, design.designId!!)

        // then
        val updatedDesign = designRepository.findById(design.designId!!).get()
        assertThat(updatedDesign.designState).isEqualTo(DesignState.ON_SALE)

        // Redis에 추가되었는지 확인
        val score = redisTemplate.opsForZSet().score(RedisProductService.POPULAR_KEY, product.productId.toString())
        assertThat(score).isNotNull()
        assertThat(score).isEqualTo(10.0)
    }

    @Test
    @DisplayName("본인 도안 조회 - 최신순 정렬")
    fun getMyDesigns_sortedByLatest() {
        // given - 여러 도안 생성
        val design1 = designRepository.save(
            Design(
                user = testUser,
                designName = "도안1",
                pdfUrl = "/files/1.pdf",
                gridData = "{}",
                designState = DesignState.BEFORE_SALE
            )
        )

        Thread.sleep(10) // 시간 차이

        val design2 = designRepository.save(
            Design(
                user = testUser,
                designName = "도안2",
                pdfUrl = "/files/2.pdf",
                gridData = "{}",
                designState = DesignState.ON_SALE
            )
        )

        // when
        val designs = designService.getMyDesigns(testUser)

        // then - 최신순 (역순)
        assertThat(designs).hasSize(2)
        assertThat(designs[0].designName).isEqualTo("도안2")
        assertThat(designs[1].designName).isEqualTo("도안1")
    }


    private fun create10x10Grid(): List<List<String>> {
        return (0 until 10).map { (0 until 10).map { "◯" } }
    }
}
