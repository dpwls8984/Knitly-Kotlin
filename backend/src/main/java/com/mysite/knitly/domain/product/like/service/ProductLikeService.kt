package com.mysite.knitly.domain.product.like.service;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;

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

    private static final String PRODUCT_DETAIL_CACHE_PREFIX = "product:detail:";

    public void addLike(Long userId, Long productId) {
        log.info("[Like] [Add] 좋아요 추가 시작 - userId={}, productId={}", userId, productId);

        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        redisTemplate.opsForSet().add(redisKey, userKey);
        redisTemplate.expire(redisKey, Duration.ofDays(7));
        log.debug("[Like] [Add] Redis 찜 Set에 추가 완료");

        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, LIKE_ROUTING_KEY, eventDto);
        log.debug("[Like] [Add] RabbitMQ 이벤트 전송 완료");

        String productCacheKey = PRODUCT_DETAIL_CACHE_PREFIX + productId;
        redisTemplate.delete(productCacheKey);
        log.info("[Like] [Invalidate] 상품 상세 캐시 삭제 완료 - key={}", productCacheKey);

        log.info("[Like] [Add] 좋아요 추가 완료");
    }

    @Transactional
    public void deleteLike(Long userId, Long productId) {
        log.info("[Like] [Delete] 좋아요 삭제 시작 - userId={}, productId={}", userId, productId);

        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        // Redis에서 제거
        redisTemplate.opsForSet().remove(redisKey, userKey);
        log.debug("[Like] [Delete] Redis 찜 Set에서 제거 완료");

        // DB 삭제는 항상 수행
        LikeEventRequest eventDto = new LikeEventRequest(userId, productId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, DISLIKE_ROUTING_KEY, eventDto);
        log.debug("[Like] [Delete] RabbitMQ 이벤트 전송 완료");

        String productCacheKey = PRODUCT_DETAIL_CACHE_PREFIX + productId;
        redisTemplate.delete(productCacheKey);
        log.info("[Like] [Invalidate] 상품 상세 캐시 삭제 완료 - key={}", productCacheKey);

        log.info("[Like] [Delete] 좋아요 삭제 완료");
    }

    public boolean isLiked(Long userId, Long productId) {
        String redisKey = "likes:product:" + productId;
        String userKey = userId.toString();

        Boolean existsInRedis = redisTemplate.opsForSet().isMember(redisKey, userKey);
        boolean existsInDb = productLikeRepository.existsById(new ProductLikeId(userId, productId));

        // 불일치 시 보정
        if (!Objects.equals(existsInRedis, existsInDb)) {
            log.warn("[Like] Redis/DB 불일치 - userId={}, productId={}", userId, productId);

            if (existsInDb && Boolean.FALSE.equals(existsInRedis)) {
                redisTemplate.opsForSet().add(redisKey, userKey);
            } else if (!existsInDb && Boolean.TRUE.equals(existsInRedis)) {
                redisTemplate.opsForSet().remove(redisKey, userKey);
            }
        }

        return existsInDb;
    }
}
