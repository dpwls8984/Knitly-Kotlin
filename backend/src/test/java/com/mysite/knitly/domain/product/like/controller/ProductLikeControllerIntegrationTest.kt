package com.mysite.knitly.domain.product.like.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.product.like.service.ProductLikeService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.TestSecurityUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.verify
import org.mockito.Mockito.times

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductLikeControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var redissonClient: org.redisson.api.RedissonClient


    @MockBean
    private lateinit var productLikeService: ProductLikeService

    private lateinit var testUser: User
    private var testProductId: Long = 10L

    @BeforeEach
    fun setup() {
        testUser = User.builder()
            .userId(1L)
            .socialId("test_social_id")
            .email("test@example.com")
            .name("Test User")
            .provider(Provider.GOOGLE)
            .build()

        testProductId = 10L
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    @DisplayName("POST /products/{productId}/like - 찜 추가 성공")
    fun addLike_Success() {
        val productId = testProductId

        mockMvc.perform(
            post("/products/$productId/like")
                .contentType(MediaType.APPLICATION_JSON)
                .with(TestSecurityUtil.createPrincipal(testUser)!!)
        )
            .andExpect(status().isOk)

        verify(productLikeService, times(1)).addLike(testUser.userId, productId)
    }

    @Test
    @DisplayName("POST /products/{productId}/like - 이미 찜한 경우")
    fun addLike_AlreadyLiked() {
        val productId = testProductId

        mockMvc.perform(
            post("/products/$productId/like")
                .contentType(MediaType.APPLICATION_JSON)
                .with(TestSecurityUtil.createPrincipal(testUser)!!)
        )
            .andExpect(status().isOk)

        verify(productLikeService, times(1)).addLike(testUser.userId, productId)
    }

    @Test
    @DisplayName("DELETE /products/{productId}/like - 찜 취소 성공")
    fun deleteLike_Success() {
        val productId = testProductId

        mockMvc.perform(
            delete("/products/$productId/like")
                .with(TestSecurityUtil.createPrincipal(testUser)!!)
        )
            .andExpect(status().isOk)

        verify(productLikeService, times(1)).deleteLike(testUser.userId, productId)
    }

    @Test
    @DisplayName("DELETE /products/{productId}/like - 찜이 없는 상태에서 취소 시도")
    fun deleteLike_NotLiked() {
        val productId = testProductId

        mockMvc.perform(
            delete("/products/$productId/like")
                .with(TestSecurityUtil.createPrincipal(testUser)!!)
        )
            .andExpect(status().isOk)

        verify(productLikeService, times(1)).deleteLike(testUser.userId, productId)
    }
}