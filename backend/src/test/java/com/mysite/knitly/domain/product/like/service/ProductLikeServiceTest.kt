package com.mysite.knitly.domain.product.like.service

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest
import com.mysite.knitly.domain.product.like.entity.ProductLikeId
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
internal class ProductLikeServiceTest {

    @Mock
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    lateinit var rabbitTemplate: RabbitTemplate

    @Mock
    lateinit var setOperations: SetOperations<String, String>

    @Mock
    lateinit var productLikeRepository: ProductLikeRepository

    @InjectMocks
    lateinit var productLikeService: ProductLikeService

    private val userId = 3L
    private val productId = 1L
    private val redisKey = "likes:product:$productId"
    private val userKey = userId.toString()
    private val eventDto = LikeEventRequest(userId, productId)

    @BeforeEach
    fun setUp() {
        Mockito.`when`(redisTemplate.opsForSet()).thenReturn(setOperations)
    }

    @Test
    @DisplayName("찜하기: Redis에 추가하고 RabbitMQ에 메시지 발행")
    fun addLike_ShouldAddToRedisAndPublishMessage() {
        productLikeService.addLike(userId, productId)

        Mockito.verify(setOperations).add(redisKey, userKey)
        Mockito.verify(redisTemplate).expire(redisKey, Duration.ofDays(7))

        // ✨ 수정된 부분: Mockito.eq()를 사용하여 값 동등성을 검증합니다.
        Mockito.verify(rabbitTemplate).convertAndSend(
            Mockito.eq("like.exchange"),
            Mockito.eq("like.add.routingkey"),
            Mockito.eq(eventDto)
        )
    }

    @Test
    @DisplayName("찜 삭제: Redis에서 제거하고 RabbitMQ에 메시지 발행")
    fun deleteLike_ShouldRemoveFromRedisAndPublishMessage() {
        productLikeService.deleteLike(userId, productId)

        Mockito.verify(setOperations).remove(redisKey, userKey)
        Mockito.verify(rabbitTemplate).convertAndSend("like.exchange", "like.delete.routingkey", eventDto)
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(O), DB(O) - 동기화 상태")
    fun isLiked_WhenRedisTrueAndDbTrue_ShouldReturnTrue() {
        Mockito.lenient().`when`(redisTemplate.opsForSet()).thenReturn(setOperations)
        Mockito.lenient().`when`(setOperations.isMember(Mockito.anyString(), Mockito.anyString())).thenReturn(true)
        Mockito.lenient().`when`(productLikeRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true)

        val isLiked = productLikeService.isLiked(userId, productId)

        assertThat(isLiked).isTrue
        Mockito.verify(setOperations, Mockito.never()).add(any(), any())
        Mockito.verify(setOperations, Mockito.never()).remove(any(), any())
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(X), DB(X) - 동기화 상태")
    fun isLiked_WhenRedisFalseAndDbFalse_ShouldReturnFalse() {
        Mockito.lenient().`when`(setOperations.isMember(redisKey, userKey)).thenReturn(false)
        Mockito.lenient().`when`(productLikeRepository.existsById(any<ProductLikeId>())).thenReturn(false)

        val isLiked = productLikeService.isLiked(userId, productId)

        assertThat(isLiked).isFalse
        Mockito.verify(setOperations, Mockito.never()).add(any(), any())
        Mockito.verify(setOperations, Mockito.never()).remove(any(), any())
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(X), DB(O) - 불일치 (Redis에 추가 보정)")
    fun isLiked_WhenRedisFalseAndDbTrue_ShouldReturnTrueAndHealRedis() {
        Mockito.lenient().`when`(redisTemplate.opsForSet()).thenReturn(setOperations)
        Mockito.lenient().`when`(setOperations.isMember(Mockito.anyString(), Mockito.anyString())).thenReturn(false)
        Mockito.lenient().`when`(productLikeRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true)

        val isLiked = productLikeService.isLiked(userId, productId)

        assertThat(isLiked).isTrue
        Mockito.verify(setOperations).add(Mockito.eq("likes:product:$productId"), Mockito.eq(userId.toString()))
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(O), DB(X) - 불일치 (Redis에서 삭제 보정)")
    fun isLiked_WhenRedisTrueAndDbFalse_ShouldReturnFalseAndHealRedis() {
        Mockito.lenient().`when`(setOperations.isMember(redisKey, userKey)).thenReturn(true)
        Mockito.lenient().`when`(productLikeRepository.existsById(any<ProductLikeId>())).thenReturn(false)

        val isLiked = productLikeService.isLiked(userId, productId)

        assertThat(isLiked).isFalse
        Mockito.verify(setOperations).remove(redisKey, userKey)
        Mockito.verify(setOperations, Mockito.never()).add(any(), any())
    }
}