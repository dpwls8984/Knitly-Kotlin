package com.mysite.knitly.domain.product.like.consumer

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest
import com.mysite.knitly.domain.product.like.entity.ProductLike
import com.mysite.knitly.domain.product.like.entity.ProductLikeId
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mock.Strictness
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.* // eq() 사용을 위해 필수
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class LikeEventConsumerTest {

    @Mock
    private lateinit var productLikeRepository: ProductLikeRepository

    @Mock(strictness = Strictness.LENIENT)
    private lateinit var userRepository: UserRepository

    @Mock(strictness = Strictness.LENIENT)
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var setOperations: SetOperations<String, String>

    @Mock
    private lateinit var product: Product

    @InjectMocks
    private lateinit var likeEventConsumer: LikeEventConsumer

    private lateinit var user: User
    private lateinit var request: LikeEventRequest

    @BeforeEach
    fun setUp() {
        request = LikeEventRequest(3L, 1L)
        user = User.builder()
            .userId(request.userId)
            .provider(Provider.GOOGLE)
            .build()
    }

    @Disabled
    @Test
    @DisplayName("찜하기: 정상 (DB에 저장 및 카운트 증가)")
    fun handleLikeEvent_Success() {
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)

        whenever(userRepository.findById(eq(request.userId))).thenReturn(Optional.of(user))
        whenever(productRepository.findById(eq(request.productId))).thenReturn(Optional.of(product))

        whenever(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId)))
            .thenReturn(false)

        likeEventConsumer.handleLikeEvent(request)

        verify(productLikeRepository).save(any<ProductLike>())
        verify(product, times(1)).increaseLikeCount()
    }

    @Disabled
    @Test
    @DisplayName("찜하기: 이미 찜한 상품(아무것도 안 함)")
    fun handleLikeEvent_AlreadyExists() {
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)

        whenever(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId)))
            .thenReturn(true)

        likeEventConsumer.handleLikeEvent(request)

        verify(productLikeRepository, never()).save(any<ProductLike>())
        verify(product, never()).increaseLikeCount()
    }

    @Disabled
    @Test
    @DisplayName("찜하기: 실패 (DB 저장 실패 시 Redis 롤백 및 예외 Requeue)")
    fun handleLikeEvent_Failure_ShouldRollbackRedisAndRequeue() {
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)

        whenever(userRepository.findById(eq(request.userId))).thenReturn(Optional.of(user))
        whenever(productRepository.findById(eq(request.productId))).thenReturn(Optional.of(product))

        whenever(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId)))
            .thenReturn(false)

        whenever(productLikeRepository.save(any<ProductLike>())).thenThrow(RuntimeException("DB Connection Failed"))

        assertThrows<AmqpRejectAndDontRequeueException> {
            likeEventConsumer.handleLikeEvent(request)
        }

        val redisKey = "likes:product:${request.productId}"
        val userKey = request.userId.toString()
        verify(setOperations).remove(redisKey, userKey)
    }

    @Test
    @DisplayName("찜 삭제: 정상 (DB에서 삭제 및 카운트 감소)")
    fun handleDislikeEvent_Success() {
        whenever(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId)))
            .thenReturn(true)

        whenever(productRepository.findById(eq(request.productId))).thenReturn(Optional.of(product))

        likeEventConsumer.handleDislikeEvent(request)

        verify(productLikeRepository, never()).deleteById(any<ProductLikeId>())

        verify(productLikeRepository).deleteByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId))
        verify(product, times(1)).decreaseLikeCount()
    }

    @Test
    @DisplayName("찜 삭제: 삭제할 찜이 없음 (아무것도 안 함)")
    fun handleDislikeEvent_NotFound() {
        whenever(productLikeRepository.existsByUser_UserIdAndProduct_ProductId(eq(request.userId), eq(request.productId)))
            .thenReturn(false)

        likeEventConsumer.handleDislikeEvent(request)

        verify(productRepository, never()).findById(any<Long>())
        verify(product, never()).decreaseLikeCount()

        verify(productLikeRepository, never()).deleteById(any<ProductLikeId>())
        verify(productLikeRepository, never()).deleteByUser_UserIdAndProduct_ProductId(anyLong(), anyLong())
    }
}