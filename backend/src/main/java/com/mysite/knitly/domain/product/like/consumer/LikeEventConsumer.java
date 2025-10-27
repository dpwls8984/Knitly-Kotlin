package com.mysite.knitly.domain.product.like.consumer;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventConsumer {

    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String LIKE_QUEUE_NAME = "like.add.queue";
    private static final String DISLIKE_QUEUE_NAME = "like.delete.queue";

    @Transactional
    @RabbitListener(queues = LIKE_QUEUE_NAME)
    public void handleLikeEvent(LikeEventRequest eventDto) {
        String redisKey = "likes:product:" + eventDto.productId();
        String userKey = eventDto.userId().toString();

        try {
            User user = userRepository.findById(eventDto.userId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

            Product product = productRepository.findById(eventDto.productId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

            ProductLikeId productLikeId = new ProductLikeId(user.getUserId(), product.getProductId());

            if (productLikeRepository.existsById(productLikeId)) {
                log.info("Like already exists in DB. Skipping: {}", productLikeId);
                return;
            }

            ProductLike productLike = ProductLike.builder()
                    .user(user)
                    .product(product)
                    .build();

            product.increaseLikeCount();

            // 5. DB 저장
            productLikeRepository.save(productLike);
            log.info("Successfully saved like to DB: {}", productLikeId);

        } catch (Exception e) {
            log.error("Failed to save like to DB. Rolling back Redis cache for {}.", redisKey, e);

            redisTemplate.opsForSet().remove(redisKey, userKey);

            throw new AmqpRejectAndDontRequeueException("DB save failed, cache rolled back.", e);
        }
    }

    @Transactional
    @RabbitListener(queues = DISLIKE_QUEUE_NAME)
    public void handleDislikeEvent(LikeEventRequest eventDto) {
        log.info("[handleDislikeEvent] received event: {}", eventDto);
        ProductLikeId productLikeId = new ProductLikeId(eventDto.userId(), eventDto.productId());

        try {
            if (productLikeRepository.existsById(productLikeId)) {

                Product product = productRepository.findById(eventDto.productId())
                        .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

                product.decreaseLikeCount();

                productLikeRepository.deleteById(productLikeId);

                log.info("[handleDislikeEvent] ProductLike deleted and count decremented: {}", productLikeId);

            } else {
                log.warn("[handleDislikeEvent] ProductLike not found in DB, skipping: {}", productLikeId);
            }
        } catch (Exception e) {
            log.error("[handleDislikeEvent] Error processing dislike event: {}", eventDto, e);
            throw new AmqpRejectAndDontRequeueException("DB operation failed during dislike.", e);
        }
    }
}