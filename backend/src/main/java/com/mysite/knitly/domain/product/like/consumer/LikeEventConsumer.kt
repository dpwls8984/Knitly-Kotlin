package com.mysite.knitly.domain.product.like.consumer

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest
import com.mysite.knitly.domain.product.like.entity.ProductLike
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import mu.KotlinLogging
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class LikeEventConsumer(
    private val productLikeRepository: ProductLikeRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {

    @Transactional
    @RabbitListener(queues = [LIKE_QUEUE_NAME])
    fun handleLikeEvent(eventDto: LikeEventRequest) {
        val (userId, productId) = eventDto
        log.info { "[Like] [Consume] 좋아요 이벤트 수신 - userId=$userId, productId=$productId" }

        val redisKey = "likes:product:$productId"
        val userKey = userId.toString()

        try {
            if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {
                log.debug { "[Like] [Consume] 이미 DB에 존재하는 좋아요 - userId=$userId, productId=$productId" }
                return
            }

            val user = findUser(userId)
            val product = findProduct(productId)

            val productLike = ProductLike(
                userId = userId,
                productId = productId,
                user = user,
                product = product
            )

            product.increaseLikeCount()
            productLikeRepository.save(productLike)

            log.info { "[Like] [Consume] 좋아요 DB 반영 완료 - userId=$userId, productId=$productId" }

        } catch (e: Exception) {
            log.error(e) { "[Like] [Consume] DB 반영 실패 - redisKey=$redisKey, userKey=$userKey" }

            redisTemplate.opsForSet().remove(redisKey, userKey)

            throw AmqpRejectAndDontRequeueException("DB save failed, cache rolled back.", e)
        }
    }

    @Transactional
    @RabbitListener(queues = [DISLIKE_QUEUE_NAME])
    fun handleDislikeEvent(eventDto: LikeEventRequest) {
        val (userId, productId) = eventDto
        log.info { "[Like] [Consume] 좋아요 취소 이벤트 수신 - userId=$userId, productId=$productId" }

        try {
            if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {

                val product = findProduct(productId)

                product.decreaseLikeCount()

                productLikeRepository.deleteByUserIdAndProductId(userId, productId)

                log.info { "[Like] [Consume] 좋아요 삭제 및 카운트 감소 완료 - userId=$userId, productId=$productId" }
            } else {
                log.warn { "[Like] [Consume] DB에 존재하지 않는 좋아요 - userId=$userId, productId=$productId" }
            }
        } catch (e: Exception) {
            log.error(e) { "[Like] [Consume] 좋아요 삭제 처리 중 오류 - $eventDto" }
            throw AmqpRejectAndDontRequeueException("DB operation failed during dislike.", e)
        }
    }

    private fun findUser(userId: Long): User = userRepository.findByIdOrNull(userId)
        ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

    private fun findProduct(productId: Long): Product = productRepository.findByIdOrNull(productId)
        ?: throw ServiceException(ErrorCode.PRODUCT_NOT_FOUND)

    companion object {
        private const val LIKE_QUEUE_NAME = "like.add.queue"
        private const val DISLIKE_QUEUE_NAME = "like.delete.queue"
    }
}