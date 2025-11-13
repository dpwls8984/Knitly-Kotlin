package com.mysite.knitly.domain.product.review.service;

import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderItemRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductImage;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.entity.ReviewImage;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private LocalFileStorage localFileStorage;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Product product;
    private OrderItem orderItem;
    private Long orderItemId = 1L;
    private Long userId = 3L;
    private Long productId = 10L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder().userId(userId).name("테스트유저").build();
        product = Product.builder().productId(productId).title("테스트상품").build();
        orderItem = OrderItem.builder().orderItemId(orderItemId).product(product).build();
    }

    @Test
    @DisplayName("리뷰 폼 조회: 정상")
    void getReviewFormInfo_Success() {
        // given
        ProductImage thumbnail = ProductImage.builder()
                .productImageUrl("http://test.com/thumbnail.jpg")
                .build();

        product.addProductImages(List.of(thumbnail));

        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));

        // when
        ReviewCreateResponse response = reviewService.getReviewFormInfo(orderItemId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.productTitle()).isEqualTo("테스트상품");
        assertThat(response.productThumbnailUrl()).isEqualTo("http://test.com/thumbnail.jpg");
    }

    @Test
    @DisplayName("리뷰 폼 조회: OrderItem 없음")
    void getReviewFormInfo_OrderItemNotFound_ShouldThrowException() {
        // given
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        // when & then
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.getReviewFormInfo(orderItemId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }


    @Test
    @DisplayName("리뷰 등록: 정상 (이미지 없음)")
    void createReview_ValidInputNoImages_ShouldSaveReview() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요", new ArrayList<>());

        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);

        // when
        reviewService.createReview(orderItemId, user, request);

        // then
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();

        assertThat(savedReview.getUser()).isEqualTo(user);
        assertThat(savedReview.getProduct()).isEqualTo(product);
        assertThat(savedReview.getOrderItem()).isEqualTo(orderItem);
        assertThat(savedReview.getRating()).isEqualTo(5);
        assertThat(savedReview.getContent()).isEqualTo("좋아요");
        assertThat(savedReview.getReviewImages()).isEmpty();

        verify(localFileStorage, never()).saveReviewImage(any());
    }

    @Test
    @DisplayName("리뷰 등록: 정상 (이미지 포함)")
    void createReview_ValidInputWithImages_ShouldSaveReviewAndImages() {
        // given
        MultipartFile mockFile = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[]{1, 2, 3});
        List<MultipartFile> images = List.of(mockFile);
        ReviewCreateRequest request = new ReviewCreateRequest(5, "이미지 리뷰", images);

        String savedImageUrl = "/static/review/saved_image.jpg";

        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(localFileStorage.saveReviewImage(mockFile)).thenReturn(savedImageUrl);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);

        // when
        reviewService.createReview(orderItemId, user, request);

        // then
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();

        assertThat(savedReview.getContent()).isEqualTo("이미지 리뷰");

        verify(localFileStorage).saveReviewImage(mockFile);

        assertThat(savedReview.getReviewImages()).hasSize(1);
        assertThat(savedReview.getReviewImages().get(0).getReviewImageUrl()).isEqualTo(savedImageUrl);
        assertThat(savedReview.getReviewImages().get(0).getReview()).isEqualTo(savedReview);
    }

    @Test
    @DisplayName("리뷰 등록: OrderItem 없음")
    void createReview_OrderItemNotFound_ShouldThrowException() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요", new ArrayList<>());
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        // when & then
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.createReview(orderItemId, user, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 등록: 이미지 저장 실패 (예: 형식 오류)")
    void createReview_InvalidImageFormat_ShouldThrowException() {
        // given
        MultipartFile invalidFile = new MockMultipartFile("file", "document.txt", "text/plain", new byte[]{1, 2, 3});
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요", List.of(invalidFile));

        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(localFileStorage.saveReviewImage(invalidFile))
                .thenThrow(new ServiceException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED));

        // when & then
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.createReview(orderItemId, user, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);

        verify(reviewRepository, never()).save(any());
    }


    @Test
    @DisplayName("리뷰 삭제: 정상 (소유자)")
    void deleteReview_ValidUser_ShouldSetDeleted() {
        // given
        Long reviewId = 1L;
        Review review = Review.builder().reviewId(reviewId).user(user).isDeleted(false).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, user);

        // then
        assertThat(review.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("리뷰 삭제: 권한 없는 유저가 요청시 실패")
    void deleteReview_NotOwner_ShouldThrowException() {
        // given
        Long reviewId = 1L;
        User owner = User.builder().userId(99L).build();
        User requester = user;

        Review review = Review.builder().reviewId(reviewId).user(owner).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when & then
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.deleteReview(reviewId, requester));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_AUTHORIZED);
    }

    @Test
    @DisplayName("리뷰 삭제: 리뷰 없음")
    void deleteReview_ReviewNotFound_ShouldThrowException() {
        // given
        Long reviewId = 1L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // when & then
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.deleteReview(reviewId, user));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }


    @Test
    @DisplayName("리뷰 목록 조회: 정상 (이미지 포함)")
    void getReviewsByProduct_ShouldReturnPageOfReviewsWithImages() {
        // given
        int page = 0;
        int size = 10;

        Review review1 = Review.builder()
                .reviewId(1L).content("좋아요").rating(5).user(user).isDeleted(false).build();
        ReviewImage image1 = ReviewImage.builder().reviewImageUrl("/static/review/img1.jpg").build();
        ReviewImage image2 = ReviewImage.builder().reviewImageUrl("/static/review/img2.png").build();
        review1.addReviewImages(List.of(image1, image2)); // 편의 메서드 사용

        Review review2 = Review.builder()
                .reviewId(2L).content("괜찮아요").rating(4).user(user).isDeleted(false).build();
        review2.addReviewImages(new ArrayList<>()); // 빈 리스트 추가

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Review> reviewList = List.of(review1, review2);
        Page<Review> reviewPage = new PageImpl<>(reviewList, pageable, reviewList.size());

        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(eq(productId), any(Pageable.class)))
                .thenReturn(reviewPage);

        // when
        Page<ReviewListResponse> resultPage = reviewService.getReviewsByProduct(productId, page, size);

        // then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getContent()).hasSize(2);

        ReviewListResponse r1 = resultPage.getContent().get(0);
        assertThat(r1.reviewId()).isEqualTo(1L);
        assertThat(r1.content()).isEqualTo("좋아요");
        assertThat(r1.reviewImageUrls())
                .containsExactly("/static/review/img1.jpg", "/static/review/img2.png");

        ReviewListResponse r2 = resultPage.getContent().get(1);
        assertThat(r2.reviewId()).isEqualTo(2L);
        assertThat(r2.content()).isEqualTo("괜찮아요");
        assertThat(r2.reviewImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("리뷰 목록 조회: 리뷰 없음 (빈 페이지)")
    void getReviewsByProduct_NoReviews_ShouldReturnEmptyPage() {
        // given
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(eq(productId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        Page<ReviewListResponse> resultPage = reviewService.getReviewsByProduct(productId, page, size);

        // then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(0);
        assertThat(resultPage.getContent()).isEmpty();
    }
}