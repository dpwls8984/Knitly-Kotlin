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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

// 초기 구상했던 N분 단위 DB 동기화 대신, 더 나은 사용자 경험과 부하 분산을 위해 큐를 이용한 실시간 처리 방식으로 구현함
@Component
@RequiredArgsConstructor
public class LikeEventConsumer {

    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private static final String LIKE_QUEUE_NAME = "like.add.queue";
    private static final String DISLIKE_QUEUE_NAME = "like.delete.queue";

    @Transactional
    @RabbitListener(queues = LIKE_QUEUE_NAME)
    public void handleLikeEvent(LikeEventRequest eventDto) {
        User user = userRepository.findById(eventDto.userId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(eventDto.productId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductLikeId productLikeId = new ProductLikeId(user.getUserId(), product.getProductId());

        if (productLikeRepository.existsById(productLikeId)) {
            throw new ServiceException(ErrorCode.LIKE_ALREADY_EXISTS);
        }

        ProductLike productLike = ProductLike.builder()
                .user(user)
                .product(product)
                .build();

        productLikeRepository.save(productLike);
    }

    @Transactional
    @RabbitListener(queues = DISLIKE_QUEUE_NAME)
    public void handleDislikeEvent(LikeEventRequest eventDto) {
        if (!userRepository.existsById(eventDto.userId())) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }
        if (!productRepository.existsById(eventDto.productId())) {
            throw new ServiceException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductLikeId productLikeId = new ProductLikeId(eventDto.userId(), eventDto.productId());

        ProductLike productLikeToDelete = productLikeRepository.findById(productLikeId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKE_NOT_FOUND));

        productLikeRepository.delete(productLikeToDelete);
    }
}