package com.mysite.knitly.domain.product.review.service;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.entity.ReviewImage;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // @TempDir을 사용하도록 uploadDir 설정 변경
        reviewService.uploadDir = tempDir.toString();
        reviewService.urlPrefix = "/resources/static/review/";
    }

//    @Test
//    @DisplayName("리뷰 등록: 정상")
//    void createReview_ValidInput_ShouldReturnResponse() {
//        Long productId = 1L;
//        Long userId = 3L;
//        ReviewCreateRequest request = new ReviewCreateRequest((int) 5, "좋아요", Collections.emptyList());
//
//        User user = User.builder().userId(userId).name("홍길동").build();
//        Product product = Product.builder().productId(productId).build();
//
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
//        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
//            Review reviewToSave = invocation.getArgument(0);
//            return Review.builder()
//                    .reviewId(1L)
//                    .user(reviewToSave.getUser())
//                    .product(reviewToSave.getProduct())
//                    .rating(reviewToSave.getRating())
//                    .content(reviewToSave.getContent())
//                    .build();
//        });
//
//        ReviewListResponse response = reviewService.createReview(productId, user, request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.reviewId()).isEqualTo(1L);
//        assertThat(response.content()).isEqualTo("좋아요");
//        assertThat(response.userName()).isEqualTo("홍길동");
//        verify(reviewRepository).save(any(Review.class));
//    }

    @Test
    @DisplayName("리뷰 삭제: 정상")
    void deleteReview_ValidUser_ShouldSetDeleted() {
        Long userId = 3L;
        Long reviewId = 1L;

        User user = User.builder().userId(userId).build();
        Review review = Review.builder().reviewId(reviewId).user(user).isDeleted(false).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, user);

        assertThat(review.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("리뷰 삭제: 권한 없는 유저가 요청시 실패")
    void deleteReview_NotOwner_ShouldThrowException() {
        Long requesterId = 3L;
        Long ownerId = 9L;
        Long reviewId = 1L;

        User owner = User.builder().userId(ownerId).build();
        Review review = Review.builder().reviewId(reviewId).user(owner).build();
        User requester = User.builder().userId(requesterId).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.deleteReview(reviewId, requester));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_AUTHORIZED);
    }

    @Test
    @DisplayName("리뷰 목록 조회: 삭제되지 않은 리뷰만 반환")
    void getReviewsByProduct_ShouldReturnOnlyNonDeletedReviews() {
        Long productId = 1L;
        Long userId = 3L;
        User user = User.builder().userId(userId).name("사용자1").build();

        Review review1 = Review.builder()
                .reviewId(1L)
                .content("좋아요")
                .rating(5)
                .user(user)
                .product(Product.builder().productId(productId).build())
                .isDeleted(false)
                .build();

        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(eq(productId), any(Pageable.class)))
                .thenReturn(List.of(review1));

        List<ReviewListResponse> responses = reviewService.getReviewsByProduct(productId, 0, 10);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).reviewId()).isEqualTo(1L);
        assertThat(responses.get(0).content()).isEqualTo("좋아요");
    }

//    @Test
//    @DisplayName("리뷰 등록: 이미지 URL까지 포함해서 ReviewListResponse 반환")
//    void createReview_WithImages_ShouldReturnResponseWithUrls() throws Exception {
//        Long productId = 1L;
//        Long userId = 3L;
//
//        MultipartFile mockFile = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[]{1, 2, 3});
//        ReviewCreateRequest request = new ReviewCreateRequest(5, "이미지 리뷰", List.of(mockFile));
//
//        User user = User.builder().userId(userId).name("홍길동").build();
//        Product product = Product.builder().productId(productId).build();
//
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
//        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        ReviewListResponse response = reviewService.createReview(productId, user, request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.content()).isEqualTo("이미지 리뷰");
//        assertThat(response.reviewImageUrls()).hasSize(1);
//        assertThat(response.reviewImageUrls().get(0))
//                .startsWith("/resources/static/review/")
//                .contains("image.jpg");
//    }

    @Test
    @DisplayName("리뷰 목록 조회: 삭제되지 않은 리뷰와 이미지 URL 반환")
    void getReviewsByProduct_ShouldReturnOnlyNonDeletedReviewsWithImages() {
        Long productId = 1L;
        Long userId = 3L;
        User user = User.builder().userId(userId).name("홍길동").build();

        Review review = Review.builder()
                .reviewId(1L)
                .content("좋아요")
                .rating(5)
                .user(user)
                .isDeleted(false)
                .build();

        ReviewImage image1 = ReviewImage.builder().reviewImageUrl("/static/review/img1.jpg").build();
        ReviewImage image2 = ReviewImage.builder().reviewImageUrl("/static/review/img2.png").build();
        review.addReviewImages(List.of(image1, image2));

        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(eq(productId), any(Pageable.class)))
                .thenReturn(List.of(review));
        // when
        List<ReviewListResponse> responses = reviewService.getReviewsByProduct(productId, 0, 10);

        // then
        assertThat(responses).hasSize(1);
        ReviewListResponse r = responses.get(0);
        assertThat(r.reviewId()).isEqualTo(1L);

        assertThat(r.reviewImageUrls())
                .containsExactly("/static/review/img1.jpg", "/static/review/img2.png");
    }

    @Test
    @DisplayName("리뷰 등록: 지원하지 않는 이미지 형식")
    void createReview_InvalidImageFormat_ShouldThrowException() throws Exception {
        Long productId = 1L;
        Long userId = 3L;

        MultipartFile invalidFile = new MockMultipartFile("file", "document.txt", "text/plain", new byte[]{1, 2, 3});
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요", List.of(invalidFile));

        User user = User.builder().userId(userId).name("홍길동").build();
        Product product = Product.builder().productId(productId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.createReview(productId, user, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
    }
}
