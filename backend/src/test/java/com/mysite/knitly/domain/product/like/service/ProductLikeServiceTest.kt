package com.mysite.knitly.domain.product.like.service;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLikeServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ProductLikeRepository productLikeRepository;

    @InjectMocks
    private ProductLikeService productLikeService;

    private final Long userId = 3L;
    private final Long productId = 1L;
    private final String redisKey = "likes:product:" + productId;
    private final String userKey = userId.toString();
    private final LikeEventRequest eventDto = new LikeEventRequest(userId, productId);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("찜하기: Redis에 추가하고 RabbitMQ에 메시지 발행")
    void addLike_ShouldAddToRedisAndPublishMessage() {
        String redisKey = "likes:product:" + productId;

        // when
        productLikeService.addLike(userId, productId);

        // then
        verify(setOperations).add(redisKey, userKey);

        verify(redisTemplate).expire(redisKey, Duration.ofDays(7));

        verify(rabbitTemplate).convertAndSend("like.exchange", "like.add.routingkey", eventDto);
    }

    @Test
    @DisplayName("찜 삭제: Redis에서 제거하고 RabbitMQ에 메시지 발행")
    void deleteLike_ShouldRemoveFromRedisAndPublishMessage() {
        // when
        productLikeService.deleteLike(userId, productId);

        // then
        verify(setOperations).remove(redisKey, userKey);

        // 2. RabbitMQ 메시지 발행 검증
        verify(rabbitTemplate).convertAndSend("like.exchange", "like.delete.routingkey", eventDto);
    }
    @Test
    @DisplayName("찜 상태 조회: Redis(O), DB(O) - 동기화 상태")
    void isLiked_WhenRedisTrueAndDbTrue_ShouldReturnTrue() {
        // given
        when(setOperations.isMember(redisKey, userKey)).thenReturn(true);
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(true);

        // when
        boolean isLiked = productLikeService.isLiked(userId, productId);

        // then
        assertThat(isLiked).isTrue();
        // 보정 로직(add, remove)이 호출되지 않아야 함
        verify(setOperations, never()).add(anyString(), anyString());
        verify(setOperations, never()).remove(anyString(), anyString());
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(X), DB(X) - 동기화 상태")
    void isLiked_WhenRedisFalseAndDbFalse_ShouldReturnFalse() {
        // given
        when(setOperations.isMember(redisKey, userKey)).thenReturn(false);
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        // when
        boolean isLiked = productLikeService.isLiked(userId, productId);

        // then
        assertThat(isLiked).isFalse();
        // 보정 로직(add, remove)이 호출되지 않아야 함
        verify(setOperations, never()).add(anyString(), anyString());
        verify(setOperations, never()).remove(anyString(), anyString());
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(X), DB(O) - 불일치 (Redis에 추가 보정)")
    void isLiked_WhenRedisFalseAndDbTrue_ShouldReturnTrueAndHealRedis() {
        // given (불일치 상황)
        when(setOperations.isMember(redisKey, userKey)).thenReturn(false);
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(true);

        // when
        boolean isLiked = productLikeService.isLiked(userId, productId);

        // then
        // 1. DB 기준(true)으로 반환
        assertThat(isLiked).isTrue();

        // 2. [보정] Redis에 데이터를 추가해야 함
        verify(setOperations).add(redisKey, userKey);

        // 3. remove는 호출되면 안 됨
        verify(setOperations, never()).remove(anyString(), anyString());
    }

    @Test
    @DisplayName("찜 상태 조회: Redis(O), DB(X) - 불일치 (Redis에서 삭제 보정)")
    void isLiked_WhenRedisTrueAndDbFalse_ShouldReturnFalseAndHealRedis() {
        // given (불일치 상황)
        when(setOperations.isMember(redisKey, userKey)).thenReturn(true);
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        // when
        boolean isLiked = productLikeService.isLiked(userId, productId);

        // then
        // 1. DB 기준(false)으로 반환
        assertThat(isLiked).isFalse();

        // 2. [보정] Redis에서 데이터를 삭제해야 함
        verify(setOperations).remove(redisKey, userKey);

        // 3. add는 호출되면 안 됨
        verify(setOperations, never()).add(anyString(), anyString());
    }
}