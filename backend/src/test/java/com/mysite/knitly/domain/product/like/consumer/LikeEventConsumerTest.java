package com.mysite.knitly.domain.product.like.consumer;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeEventConsumerTest {

    @Mock
    private ProductLikeRepository productLikeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private Product product;

    @InjectMocks
    private LikeEventConsumer likeEventConsumer;

    private User user;
    private LikeEventRequest request;

    @BeforeEach
    void setUp() {
        request = new LikeEventRequest(3L, 1L);
        user = User.builder().userId(request.userId).build();
    }

    @Test
    @DisplayName("찜하기: 정상 (DB에 저장 및 카운트 증가)")
    void handleLikeEvent_Success() {
        // given
        when(userRepository.findById(request.userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(request.productId)).thenReturn(Optional.of(product));
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        // when
        likeEventConsumer.handleLikeEvent(request);

        // then
        verify(productLikeRepository).save(any(ProductLike.class));
        verify(product, times(1)).increaseLikeCount();
    }

    @Test
    @DisplayName("찜하기: 이미 찜한 상품(아무것도 안 함)")
    void handleLikeEvent_AlreadyExists_ThrowsException() {
        // given
        when(userRepository.findById(request.userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(request.productId)).thenReturn(Optional.of(product));

        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(true);

        // when
        likeEventConsumer.handleLikeEvent(request);

        // then
        verify(productLikeRepository, never()).save(any());
        verify(product, never()).increaseLikeCount();
    }

    @Test
    @DisplayName("찜하기: 실패 (DB 저장 실패 시 Redis 롤백 및 예외 Requeue)")
    void handleLikeEvent_Failure_ShouldRollbackRedisAndRequeue() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        when(userRepository.findById(request.userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(request.productId)).thenReturn(Optional.of(product));

        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        when(productLikeRepository.save(any(ProductLike.class)))
                .thenThrow(new RuntimeException("DB Connection Failed"));

        // when & then
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            likeEventConsumer.handleLikeEvent(request);
        });

        // then
        String redisKey = "likes:product:" + request.productId;
        String userKey = request.userId.toString();
        verify(redisTemplate.opsForSet()).remove(redisKey, userKey);
    }

    @Test
    @DisplayName("찜 삭제: 정상 (DB에서 삭제 및 카운트 감소)")
    void handleDislikeEvent_Success() {
        //given
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(true);
        when(productRepository.findById(request.productId)).thenReturn(Optional.of(product));

        // when
        likeEventConsumer.handleDislikeEvent(request);

        // then
        verify(productLikeRepository).deleteById(any(ProductLikeId.class));
        verify(product, times(1)).decreaseLikeCount();
    }

    @Test
    @DisplayName("찜 삭제: 삭제할 찜이 없음 (아무것도 안 함)")
    void handleDislikeEvent_NotFound_ThrowsException() {
        // given
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        // when
        likeEventConsumer.handleDislikeEvent(request);

        // then
        verify(productRepository, never()).findById(any());
        verify(product, never()).decreaseLikeCount();
        verify(productLikeRepository, never()).deleteById(any());
    }
}