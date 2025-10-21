package com.mysite.knitly.domain.product.like.service;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductLikeService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE_NAME = "like.exchange";

    private static final String LIKE_ROUTING_KEY = "like.add.routingkey";
    private static final String DISLIKE_ROUTING_KEY = "like.delete.routingkey";

    public void addLike(Long userId, Long productId) {
        String redisKey = "likes:product:" + productId;
        redisTemplate.opsForSet().add(redisKey, userId.toString());
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, LIKE_ROUTING_KEY, eventDto);
    }

    public void deleteLike(Long userId, Long productId) {
        String redisKey = "likes:product:" + productId;
        redisTemplate.opsForSet().remove(redisKey, userId.toString());
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, DISLIKE_ROUTING_KEY, eventDto);
    }
}
