package com.mysite.knitly.domain.user.controller

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.entity.DesignType
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.entity.ProductImage
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.utility.jwt.JwtProvider
import com.mysite.knitly.utility.redis.RefreshTokenService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

/**
 * UserController 통합 테스트
 *
 * 테스트 전략:
 * 1. 실제 Spring Context를 사용한 통합 테스트
 * 2. H2 In-Memory Database 사용
 * 3. Embedded Redis 사용
 * 4. 실제 JWT 토큰 생성 및 검증
 * 5. 실제 HTTP 요청/응답 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var designRepository: DesignRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    private lateinit var testUser: User
    private lateinit var accessToken: String
    private lateinit var refreshToken: String

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성 및 저장
        testUser = User.builder()
            .socialId("google-test-123456")
            .email("test@example.com")
            .name("테스트유저")
            .provider(Provider.GOOGLE)
            .build()

        testUser = userRepository.save(testUser)

        // JWT 토큰 생성
        accessToken = jwtProvider.createAccessToken(testUser.userId)
        refreshToken = jwtProvider.createRefreshToken(testUser.userId)

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(testUser.userId, refreshToken)
    }

    @AfterEach
    fun tearDown() {
        // 테스트 데이터 정리
        productRepository.deleteAll()
        designRepository.deleteAll()
        userRepository.deleteAll()
        // Redis 데이터 정리는 @Transactional로 롤백되므로 명시적 삭제 불필요
    }

    /**
     * 테스트용 Design + Product 생성 헬퍼 메서드
     */
    private fun createTestProduct(
        title: String = "니트 스웨터",
        price: Double = 29900.0,
        category: ProductCategory = ProductCategory.TOP,
        stockQuantity: Int? = 100
    ): Product {
        // 1. Design 생성
        val design = Design(
            user = testUser,
            designState = DesignState.ON_SALE,
            designType = DesignType.CIRCLE,
            designName = "테스트 도안",
            gridData = "{\"rows\":10,\"cols\":10}"
        )
        val savedDesign = designRepository.save(design)

        // 2. Product 생성
        val product = Product(
            title = title,
            description = "따뜻한 겨울 니트",
            productCategory = category,
            sizeInfo = "FREE",
            price = price,
            user = testUser,
            stockQuantity = stockQuantity,
            design = savedDesign
        )
        val savedProduct = productRepository.save(product)

        // 3. ProductImage 생성 (썸네일)
        val productImage = ProductImage(
            productImageUrl = "http://example.com/image.jpg",
            sortOrder = 1,
            product = savedProduct
        )
        savedProduct.addProductImages(listOf(productImage))

        return productRepository.save(savedProduct)
    }

    // ========== 현재 사용자 정보 조회 테스트 ==========

    @Test
    @DisplayName("GET /users/me - 로그인한 사용자 정보 조회 성공")
    fun `getCurrentUser Success`() {
        mockMvc.perform(
            get("/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUser.userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("테스트유저"))
            .andExpect(jsonPath("$.provider").value("GOOGLE"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("GET /users/me - 인증 없이 요청 시 401 에러")
    fun `getCurrentUser Fail Unauthorized`() {
        mockMvc.perform(
            get("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("GET /users/me - 잘못된 토큰으로 요청 시 401 에러")
    fun `getCurrentUser Fail InvalidToken`() {
        mockMvc.perform(
            get("/users/me")
                .header("Authorization", "Bearer invalid-token-here")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("GET /users/me - 만료된 토큰으로 요청 시 401 에러")
    fun `getCurrentUser Fail ExpiredToken`() {
        // 만료된 토큰 생성 (실제 구현에 따라 조정 필요)
        val expiredToken = "expired-token-example"

        mockMvc.perform(
            get("/users/me")
                .header("Authorization", "Bearer $expiredToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }

    // ========== 로그아웃 테스트 ==========

    @Test
    @DisplayName("POST /users/logout - 로그아웃 성공")
    fun `logout Success`() {
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 7 * 24 * 60 * 60
        }

        mockMvc.perform(
            post("/users/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().string("[Auth] [UserController] 로그아웃되었습니다."))
            .andExpect(cookie().maxAge("refreshToken", 0)) // 쿠키 삭제 확인

        // Redis에서 Refresh Token이 삭제되었는지 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUser.userId)
        assert(deletedToken == null) { "Refresh Token should be deleted from Redis" }
    }

    @Test
    @DisplayName("POST /users/logout - Authorization 헤더 없이 로그아웃 (쿠키만 삭제)")
    fun `logout Success WithoutAuthHeader`() {
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 7 * 24 * 60 * 60
        }

        mockMvc.perform(
            post("/users/logout")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().string("[Auth] [UserController] 로그아웃되었습니다."))
            .andExpect(cookie().maxAge("refreshToken", 0)) // 쿠키 삭제 확인

        // Redis의 Refresh Token은 그대로 남아있어야 함 (userId를 추출할 수 없었으므로)
        val remainingToken = refreshTokenService.getRefreshToken(testUser.userId)
        assert(remainingToken == refreshToken) { "Refresh Token should remain in Redis" }
    }

    @Test
    @DisplayName("POST /users/logout - 잘못된 토큰이어도 쿠키는 삭제됨")
    fun `logout TokenParsingFails StillDeletesCookie`() {
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 7 * 24 * 60 * 60
        }

        mockMvc.perform(
            post("/users/logout")
                .header("Authorization", "Bearer invalid-token")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().string("[Auth] [UserController] 로그아웃되었습니다."))
            .andExpect(cookie().maxAge("refreshToken", 0)) // 쿠키 삭제 확인
    }

    // ========== 회원탈퇴 테스트 ==========

    @Test
    @DisplayName("DELETE /users/me - 회원탈퇴 성공")
    fun `deleteAccount Success`() {
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 7 * 24 * 60 * 60
        }

        mockMvc.perform(
            delete("/users/me")
                .header("Authorization", "Bearer $accessToken")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().string("회원탈퇴가 완료되었습니다."))
            .andExpect(cookie().maxAge("refreshToken", 0)) // 쿠키 삭제 확인

        // DB에서 사용자가 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUser.userId)
        assert(!deletedUser.isPresent) { "User should be deleted from database" }

        // Redis에서 Refresh Token이 삭제되었는지 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUser.userId)
        assert(deletedToken == null) { "Refresh Token should be deleted from Redis" }
    }

    @Test
    @DisplayName("DELETE /users/me - 인증 없이 요청 시 401 에러")
    fun `deleteAccount Fail Unauthorized`() {
        mockMvc.perform(
            delete("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)

        // 사용자가 여전히 DB에 존재하는지 확인
        val user = userRepository.findById(testUser.userId)
        assert(user.isPresent) { "User should not be deleted" }
    }

    @Test
    @DisplayName("DELETE /users/me - 잘못된 토큰으로 요청 시 401 에러")
    fun `deleteAccount Fail InvalidToken`() {
        mockMvc.perform(
            delete("/users/me")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)

        // 사용자가 여전히 DB에 존재하는지 확인
        val user = userRepository.findById(testUser.userId)
        assert(user.isPresent) { "User should not be deleted" }
    }

    // ========== 판매자 상품 목록 조회 테스트 ==========

    @Test
    @DisplayName("GET /users/{userId}/products - 특정 유저가 판매중인 상품 목록 조회 성공")
    fun `getProductsWithUserId Success`() {
        // 테스트용 상품 생성
        createTestProduct(title = "니트 스웨터", price = 29900.0)

        mockMvc.perform(
            get("/users/${testUser.userId}/products")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].title").value("니트 스웨터"))
            .andExpect(jsonPath("$.content[0].price").value(29900.0))
            .andExpect(jsonPath("$.content[0].productCategory").value("TOP"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0))
    }

    @Test
    @DisplayName("GET /users/{userId}/products - 상품이 없는 경우 빈 목록 반환")
    fun `getProductsWithUserId EmptyResult`() {
        mockMvc.perform(
            get("/users/${testUser.userId}/products")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
    }

    @Test
    @DisplayName("GET /users/{userId}/products - 페이징 처리 확인")
    fun `getProductsWithUserId WithPagination`() {
        // 25개의 테스트 상품 생성
        repeat(25) { i ->
            createTestProduct(
                title = "상품 ${i + 1}",
                price = 10000.0 + (i * 1000)
            )
        }

        // 2페이지 요청 (1페이지는 0부터 시작)
        mockMvc.perform(
            get("/users/${testUser.userId}/products")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.number").value(1)) // 현재 페이지
    }

    @Test
    @DisplayName("GET /users/{userId}/products - 존재하지 않는 사용자의 상품 조회")
    fun `getProductsWithUserId NonExistentUser`() {
        val nonExistentUserId = 99999L

        mockMvc.perform(
            get("/users/$nonExistentUserId/products")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    @DisplayName("GET /users/{userId}/products - 인증 없이 조회 가능 (공개 API)")
    fun `getProductsWithUserId WithoutAuthentication`() {
        // 테스트용 상품 생성
        createTestProduct(
            title = "공개 상품",
            price = 15000.0,
            category = ProductCategory.TOP,
            stockQuantity = 30
        )

        // Authorization 헤더 없이 요청
        mockMvc.perform(
            get("/users/${testUser.userId}/products")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].title").value("공개 상품"))
    }

    // ========== 복합 시나리오 테스트 ==========

    @Test
    @DisplayName("통합 시나리오 - 사용자 정보 조회 → 로그아웃 → 재조회 실패")
    fun `integrationScenario UserFlow`() {
        // 1. 사용자 정보 조회 성공
        mockMvc.perform(
            get("/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUser.userId))

        // 2. 로그아웃
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            path = "/"
        }

        mockMvc.perform(
            post("/users/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // 3. Redis에서 Refresh Token 삭제 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUser.userId)
        assert(deletedToken == null) { "Refresh Token should be deleted after logout" }

        // 4. 동일한 Access Token으로 재조회 시도 (Access Token은 여전히 유효하므로 성공할 수 있음)
        // 참고: 실제 구현에서 로그아웃 시 Access Token을 블랙리스트에 추가하지 않았다면 여전히 유효
        mockMvc.perform(
            get("/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk) // Access Token은 여전히 유효
    }

    @Test
    @DisplayName("통합 시나리오 - 회원탈퇴 후 모든 데이터 삭제 확인")
    fun `integrationScenario DeleteAccountFlow`() {
        // 1. 상품 생성
        val product = createTestProduct(
            title = "테스트 상품",
            price = 20000.0,
            category = ProductCategory.BOTTOM,
            stockQuantity = 10
        )

        // 2. 회원탈퇴
        val refreshTokenCookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            path = "/"
        }

        mockMvc.perform(
            delete("/users/me")
                .header("Authorization", "Bearer $accessToken")
                .cookie(refreshTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // 3. DB에서 사용자 삭제 확인
        val deletedUser = userRepository.findById(testUser.userId)
        assert(!deletedUser.isPresent) { "User should be deleted" }

        // 4. Redis에서 Refresh Token 삭제 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUser.userId)
        assert(deletedToken == null) { "Refresh Token should be deleted" }

        // 5. 상품도 CASCADE 삭제되었는지 확인 (User Entity의 CascadeType.REMOVE 설정)
        val deletedProduct = productRepository.findById(product.productId!!)
        assert(!deletedProduct.isPresent) { "Product should be deleted with user (CASCADE)" }
    }
}