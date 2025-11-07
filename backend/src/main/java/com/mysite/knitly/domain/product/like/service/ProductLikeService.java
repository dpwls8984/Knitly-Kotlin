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
        log.info("[Like] [Add] 좋아요 추가 시작 - userId={}, productId={}", userId, productId);

        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        redisTemplate.opsForSet().add(redisKey, userKey);
        log.debug("[Like] [Add] Redis에 좋아요 추가 완료 - redisKey={}, userKey={}", redisKey, userKey);

        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, LIKE_ROUTING_KEY, eventDto);
        log.debug("[Like] [Add] RabbitMQ 이벤트 전송 완료 - exchange={}, routingKey={}", EXCHANGE_NAME, LIKE_ROUTING_KEY);

        log.info("[Like] [Add] 좋아요 추가 완료 - userId={}, productId={}", userId, productId);
    }

    @Transactional
    public void deleteLike(Long userId, Long productId) {
        log.info("[Like] [Delete] 좋아요 삭제 시작 - userId={}, productId={}", userId, productId);

        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        // Redis에서 제거
        redisTemplate.opsForSet().remove(redisKey, userKey);
        log.debug("[Like] [Delete] Redis에서 좋아요 제거 완료 - redisKey={}, userKey={}", redisKey, userKey);

        // DB 삭제는 항상 수행
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, DISLIKE_ROUTING_KEY, eventDto);

        log.debug("[Like] [Delete] RabbitMQ 이벤트 전송 완료 - exchange={}, routingKey={}", EXCHANGE_NAME, DISLIKE_ROUTING_KEY);

        log.info("[Like] [Delete] 좋아요 삭제 완료 - userId={}, productId={}", userId, productId);
    }
}
