package com.mysite.knitly.domain.product.like.consumer;

import com.mysite.knitly.domain.product.like.dto.LikeEventRequest;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepositoryTmp2;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp2;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeEventConsumerTest {

    @Mock
    private ProductLikeRepository productLikeRepository;
    @Mock
    private UserRepositoryTmp2 userRepository;
    @Mock
    private ProductRepositoryTmp2 productRepository;

    @InjectMocks
    private LikeEventConsumer likeEventConsumer;

    @Test
    @DisplayName("찜하기: 정상")
    void handleLikeEvent_Success() {
        LikeEventRequest request = new LikeEventRequest(UUID.randomUUID(), 1L);
        User user = User.builder().userId(request.userId()).build();
        Product product = Product.builder().productId(request.productId()).build();

        when(userRepository.findById(request.userId())).thenReturn(Optional.of(user));
        when(productRepository.findById(request.productId())).thenReturn(Optional.of(product));
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(false);

        likeEventConsumer.handleLikeEvent(request);

        verify(productLikeRepository).save(any(ProductLike.class));
    }

    @Test
    @DisplayName("찜하기: 이미 찜한 상품일 경우 예외 발생")
    void handleLikeEvent_AlreadyExists_ThrowsException() {
        LikeEventRequest request = new LikeEventRequest(UUID.randomUUID(), 1L);
        User user = User.builder().userId(request.userId()).build();
        Product product = Product.builder().productId(request.productId()).build();

        when(userRepository.findById(request.userId())).thenReturn(Optional.of(user));
        when(productRepository.findById(request.productId())).thenReturn(Optional.of(product));
        when(productLikeRepository.existsById(any(ProductLikeId.class))).thenReturn(true);

        assertThrows(ServiceException.class, () -> likeEventConsumer.handleLikeEvent(request));
        verify(productLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("찜 삭제: 정상")
    void handleDislikeEvent_Success() {
        LikeEventRequest request = new LikeEventRequest(UUID.randomUUID(), 1L);
        ProductLikeId id = new ProductLikeId(request.userId(), request.productId());
        ProductLike like = ProductLike.builder().build();

        when(userRepository.existsById(request.userId())).thenReturn(true);
        when(productRepository.existsById(request.productId())).thenReturn(true);
        when(productLikeRepository.findById(id)).thenReturn(Optional.of(like));

        likeEventConsumer.handleDislikeEvent(request);

        verify(productLikeRepository).delete(like);
    }

    @Test
    @DisplayName("찜 삭제: 삭제할 찜이 없을 경우 예외 발생")
    void handleDislikeEvent_NotFound_ThrowsException() {
        LikeEventRequest request = new LikeEventRequest(UUID.randomUUID(), 1L);
        ProductLikeId id = new ProductLikeId(request.userId(), request.productId());

        when(userRepository.existsById(request.userId())).thenReturn(true);
        when(productRepository.existsById(request.productId())).thenReturn(true);
        when(productLikeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> likeEventConsumer.handleDislikeEvent(request));
        verify(productLikeRepository, never()).delete(any());
    }
}