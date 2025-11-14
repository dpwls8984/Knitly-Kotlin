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
        log.info("[Like] [Consume] 좋아요 이벤트 수신 - userId={}, productId={}",
                eventDto.userId(), eventDto.productId());

        String redisKey = "likes:product:" + eventDto.productId();
        String userKey = eventDto.userId().toString();

        try {
            User user = userRepository.findById(eventDto.userId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

            Product product = productRepository.findById(eventDto.productId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

            ProductLikeId productLikeId = new ProductLikeId(user.getUserId(), product.getProductId());

            if (productLikeRepository.existsById(productLikeId)) {
                log.debug("[Like] [Consume] 이미 DB에 존재하는 좋아요 - {}", productLikeId);

                return;
            }

            ProductLike productLike = ProductLike.builder()
                    .user(user)
                    .product(product)
                    .build();

            product.increaseLikeCount();

            // 5. DB 저장
            productLikeRepository.save(productLike);
            log.info("[Like] [Consume] 좋아요 DB 반영 완료 - {}", productLikeId);

        } catch (Exception e) {
            log.error("[Like] [Consume] DB 반영 실패 - redisKey={}, userKey={}", redisKey, userKey, e);

            redisTemplate.opsForSet().remove(redisKey, userKey);

            throw new AmqpRejectAndDontRequeueException("DB save failed, cache rolled back.", e);
        }
    }

    @Transactional
    @RabbitListener(queues = DISLIKE_QUEUE_NAME)
    public void handleDislikeEvent(LikeEventRequest eventDto) {
        log.info("[Like] [Consume] 좋아요 취소 이벤트 수신 - userId={}, productId={}",
                eventDto.userId(), eventDto.productId());
        ProductLikeId productLikeId = new ProductLikeId(eventDto.userId(), eventDto.productId());

        try {
            if (productLikeRepository.existsById(productLikeId)) {

                Product product = productRepository.findById(eventDto.productId())
                        .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

                product.decreaseLikeCount();

                productLikeRepository.deleteById(productLikeId);

                log.info("[Like] [Consume] 좋아요 삭제 및 카운트 감소 완료 - {}", productLikeId);

            } else {
                log.warn("[Like] [Consume] DB에 존재하지 않는 좋아요 - {}", productLikeId);
            }
        } catch (Exception e) {
            log.error("[Like] [Consume] 좋아요 삭제 처리 중 오류 - {}", eventDto, e);
            throw new AmqpRejectAndDontRequeueException("DB operation failed during dislike.", e);
        }
    }
}