package com.mysite.knitly.domain.product.review.service;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepositoryTmp;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.entity.ReviewImage;
import com.mysite.knitly.domain.product.review.repository.ReviewImageRepository;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private ProductRepositoryTmp productRepository;

    @Mock
    private UserRepositoryTmp userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService.uploadDir = "resources/static/review/";
        reviewService.urlPrefix = "/resources/static/review/";
    }

    @Test
    @DisplayName("리뷰 등록: 정상")
    void createReview_ValidInput_ShouldReturnResponse() {
        Long productId = 1L;
        UUID userId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest((byte) 5, "좋아요", List.of());

        User user = User.builder().userId(userId).name("홍길동").build();
        Product product = Product.builder().productId(productId).build();
        Review savedReview = Review.builder().reviewId(1L).user(user).product(product).rating((byte)5).content("좋아요").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewListResponse response = reviewService.createReview(productId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.reviewId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("좋아요");
    }

    @Test
    @DisplayName("리뷰 등록: 값을 벗어나는 별점 설정")
    void createReview_InvalidRating_ShouldThrowException() {
        ReviewCreateRequest request = new ReviewCreateRequest((byte)6, "내용", List.of());
        UUID userId = UUID.randomUUID();
        Long productId = 1L;

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.createReview(productId, userId, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_RATING_INVALID);
    }

    @Test
    @DisplayName("리뷰 삭제: 정상")
    void deleteReview_ValidUser_ShouldSetDeleted() {
        UUID userId = UUID.randomUUID();
        Long reviewId = 1L;
        ReviewDeleteRequest request = new ReviewDeleteRequest(userId);

        User user = User.builder().userId(userId).build();
        Review review = Review.builder().reviewId(reviewId).user(user).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, request);

        assertThat(review.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("리뷰 삭제: 권한 없는 유저가 요청시 실패")
    void deleteReview_NotOwner_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        Long reviewId = 1L;
        ReviewDeleteRequest request = new ReviewDeleteRequest(userId);

        User user = User.builder().userId(UUID.randomUUID()).build();
        Review review = Review.builder().reviewId(reviewId).user(user).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.deleteReview(reviewId, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_AUTHORIZED);
    }

    @Test
    @DisplayName("리뷰 목록 조회: 삭제되지 않은 리뷰만 반환")
    void getReviewsByProduct_ShouldReturnOnlyNonDeletedReviews() {
        Long productId = 1L;

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        User user1 = User.builder().userId(userId1).name("사용자1").build();
        User user2 = User.builder().userId(userId2).name("사용자2").build();

        Review review1 = Review.builder()
                .reviewId(1L)
                .content("좋아요")
                .rating((byte)5)
                .user(user1)
                .isDeleted(false)
                .build();

        Review review2 = Review.builder()
                .reviewId(2L)
                .content("별로예요")
                .rating((byte)2)
                .user(user2)
                .isDeleted(true)
                .build();

        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId))
                .thenReturn(List.of(review1));
        // 이미지 리포지토리 Mock
        when(reviewImageRepository.findByReview_ReviewId(1L)).thenReturn(List.of());

        List<ReviewListResponse> responses = reviewService.getReviewsByProduct(productId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).reviewId()).isEqualTo(1L);
        assertThat(responses.get(0).content()).isEqualTo("좋아요");
    }

    @Test
    @DisplayName("리뷰 등록: 이미지 URL까지 포함해서 ReviewListResponse 반환")
    void createReview_WithImages_ShouldReturnResponseWithUrls() throws Exception {
        Long productId = 1L;
        UUID userId = UUID.randomUUID();

        // 테스트용 MultipartFile 생성 (실제 파일은 만들지 않고 Mock)
        MultipartFile mockFile1 = new MockMultipartFile(
                "file",          // form field name
                "file1.jpg",     // 실제 파일 이름 (검증에서 사용됨)
                "image/jpeg",    // MIME 타입
                new byte[]{1,2,3} // 파일 내용
        );

        MultipartFile mockFile2 = new MockMultipartFile(
                "file",
                "file2.png",
                "image/png",
                new byte[]{4,5,6}
        );

        ReviewCreateRequest request = new ReviewCreateRequest((byte)5, "좋아요", List.of(mockFile1, mockFile2));

        User user = User.builder().userId(userId).name("홍길동").build();
        Product product = Product.builder().productId(productId).build();
        Review savedReview = Review.builder().reviewId(1L).user(user).product(product).rating((byte)5).content("좋아요").isDeleted(false).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewImageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewListResponse response = reviewService.createReview(productId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.reviewId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("좋아요");
        assertThat(response.reviewImageUrls()).hasSize(2);
        assertThat(response.reviewImageUrls().get(0)).contains(".jpg");
        assertThat(response.reviewImageUrls().get(1)).contains(".png");
    }

    @Test
    @DisplayName("리뷰 목록 조회: 삭제되지 않은 리뷰와 이미지 URL 반환")
    void getReviewsByProduct_ShouldReturnOnlyNonDeletedReviewsWithImages() {
        Long productId = 1L;

        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).name("홍길동").build();

        Review review1 = Review.builder()
                .reviewId(1L)
                .content("좋아요")
                .rating((byte)5)
                .user(user)
                .isDeleted(false)
                .build();

        ReviewImage image1 = ReviewImage.builder().reviewImageUrl("/resources/static/review/img1.jpg").review(review1).build();
        ReviewImage image2 = ReviewImage.builder().reviewImageUrl("/resources/static/review/img2.png").review(review1).build();

        // 리뷰 리포지토리 Mock
        when(reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId))
                .thenReturn(List.of(review1));

        // 이미지 리포지토리 Mock
        when(reviewImageRepository.findByReview_ReviewId(1L)).thenReturn(List.of(image1, image2));

        List<ReviewListResponse> responses = reviewService.getReviewsByProduct(productId);

        assertThat(responses).hasSize(1);
        ReviewListResponse r = responses.get(0);
        assertThat(r.reviewId()).isEqualTo(1L);
        assertThat(r.content()).isEqualTo("좋아요");
        assertThat(r.reviewImageUrls()).containsExactly("/resources/static/review/img1.jpg", "/resources/static/review/img2.png");
    }

    @Test
    @DisplayName("리뷰 등록: 지원하지 않는 이미지 형식")
    void createReview_InvalidImageFormat_ShouldThrowException() throws Exception {
        Long productId = 1L;
        UUID userId = UUID.randomUUID();

        // txt 파일 업로드 시도
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "wrongimg1.txt",
                "text/plain",
                new byte[]{1,2,3}
        );

        ReviewCreateRequest request = new ReviewCreateRequest((byte)5, "좋아요", List.of(invalidFile));

        User user = User.builder().userId(userId).name("홍길동").build();
        Product product = Product.builder().productId(productId).build();

        // Mock 리포지토리 동작 정의
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewService.createReview(productId, userId, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
        assertThat(ex.getMessage()).contains("지원하지 않는 이미지 형식");
    }
}