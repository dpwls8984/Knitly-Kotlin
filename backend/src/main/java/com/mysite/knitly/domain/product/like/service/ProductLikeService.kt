package com.mysite.knitly.domain.product.like.service

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
class ProductLikeService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val rabbitTemplate: RabbitTemplate,
    private val productLikeRepository: ProductLikeRepository
) {

    fun addLike(userId: Long, productId: Long) {
        log.info { "[Like] [Add] 좋아요 추가 시작 - userId=$userId, productId=$productId" }

        val redisKey = "likes:product:$productId"
        val userKey = userId.toString()

        redisTemplate.opsForSet().add(redisKey, userKey)
        redisTemplate.expire(redisKey, Duration.ofDays(7))
        log.debug { "[Like] [Add] Redis 찜 Set에 추가 완료" }

        val eventDto = LikeEventRequest(userId, productId)
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, LIKE_ROUTING_KEY, eventDto)
        log.debug { "[Like] [Add] RabbitMQ 이벤트 전송 완료" }

        val productCacheKey = PRODUCT_DETAIL_CACHE_PREFIX + productId
        redisTemplate.delete(productCacheKey)
        log.info { "[Like] [Invalidate] 상품 상세 캐시 삭제 완료 - key=$productCacheKey" }

        log.info { "[Like] [Add] 좋아요 추가 완료" }
    }

    @Transactional
    fun deleteLike(userId: Long, productId: Long) {
        log.info { "[Like] [Delete] 좋아요 삭제 시작 - userId=$userId, productId=$productId" }

        val redisKey = "likes:product:$productId"
        val userKey = userId.toString()

        redisTemplate.opsForSet().remove(redisKey, userKey)
        log.debug { "[Like] [Delete] Redis 찜 Set에서 제거 완료" }

        val eventDto = LikeEventRequest(userId, productId)
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, DISLIKE_ROUTING_KEY, eventDto)
        log.debug { "[Like] [Delete] RabbitMQ 이벤트 전송 완료" }

        val productCacheKey = PRODUCT_DETAIL_CACHE_PREFIX + productId
        redisTemplate.delete(productCacheKey)
        log.info { "[Like] [Invalidate] 상품 상세 캐시 삭제 완료 - key=$productCacheKey" }

        log.info { "[Like] [Delete] 좋아요 삭제 완료" }
    }

    fun isLiked(userId: Long, productId: Long): Boolean {
        val redisKey = "likes:product:$productId"
        val userKey = userId.toString()

        val existsInRedis = redisTemplate.opsForSet().isMember(redisKey, userKey)

        val existsInDb = productLikeRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId)

        if (existsInRedis != existsInDb) {
            log.warn { "[Like] Redis/DB 불일치 - userId=$userId, productId=$productId" }

            if (existsInDb && existsInRedis == false) {
                redisTemplate.opsForSet().add(redisKey, userKey)
            } else if (!existsInDb && existsInRedis == true) {
                redisTemplate.opsForSet().remove(redisKey, userKey)
            }
        }

        return existsInDb
    }

    companion object {
        private const val EXCHANGE_NAME = "like.exchange"
        private const val LIKE_ROUTING_KEY = "like.add.routingkey"
        private const val DISLIKE_ROUTING_KEY = "like.delete.routingkey"
        private const val PRODUCT_DETAIL_CACHE_PREFIX = "product:detail:"
    }
}