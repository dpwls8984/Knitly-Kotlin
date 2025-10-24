package com.mysite.knitly.domain.product.like.service;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLikeService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ProductLikeRepository productLikeRepository;

    private static final String EXCHANGE_NAME = "like.exchange";

    private static final String LIKE_ROUTING_KEY = "like.add.routingkey";
    private static final String DISLIKE_ROUTING_KEY = "like.delete.routingkey";

    public void addLike(Long userId, Long productId) {
        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisKey, userKey))) {
            return;
        }

        redisTemplate.opsForSet().add(redisKey, userKey);
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, LIKE_ROUTING_KEY, eventDto);
    }

    @Transactional
    public void deleteLike(Long userId, Long productId) {
        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        // Redis에서 제거
        redisTemplate.opsForSet().remove(redisKey, userKey);

        // DB 삭제는 항상 수행
        productLikeRepository.deleteByUserIdAndProductId(userId, productId);

        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, DISLIKE_ROUTING_KEY, eventDto);

        log.info("[deleteLike] Deleted like for userId={}, productId={}", userId, productId);
    }
}
