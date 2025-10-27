package com.mysite.knitly.domain.product.like.service;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLikeServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private ProductLikeService productLikeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("찜하기: Redis에 추가하고 RabbitMQ에 메시지 발행")
    void addLike_ShouldAddToRedisAndPublishMessage() {
        Long userId = 3L;
        Long productId = 1L;
        String redisKey = "likes:product:" + productId;
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);

        productLikeService.addLike(userId, productId);

        verify(setOperations).add(redisKey, userId.toString());
        verify(rabbitTemplate).convertAndSend("like.exchange", "like.add.routingkey", eventDto);
    }

    @Test
    @DisplayName("찜 삭제: Redis에서 제거하고 RabbitMQ에 메시지 발행")
    void deleteLike_ShouldRemoveFromRedisAndPublishMessage() {
        Long userId = 3L;
        Long productId = 1L;
        String redisKey = "likes:product:" + productId;
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(redisKey, userId.toString())).thenReturn(true);

        productLikeService.deleteLike(userId, productId);

        verify(setOperations).remove(redisKey, userId.toString());
        verify(rabbitTemplate).convertAndSend("like.exchange", "like.delete.routingkey", eventDto);
    }
}