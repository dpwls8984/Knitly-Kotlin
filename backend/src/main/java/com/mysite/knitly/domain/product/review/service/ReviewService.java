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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    //conflict 안생기도록 일단 이름 임시로. 추후에 Tmp > 그냥 리포로 변경하고 tmp 파일 삭제
    private final ProductRepositoryTmp productRepository;
    private final UserRepositoryTmp userRepository;

    @Value("${review.upload-dir}")
    String uploadDir;
    @Value("${review.url-prefix}")
    String urlPrefix;


    // 1️. 리뷰 등록
    @Transactional
    public ReviewListResponse createReview(Long productId, UUID userId, ReviewCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.rating())
                .content(request.content())
                .isDeleted(false)
                .build();

        Review savedReview = reviewRepository.save(review);

        List<String> reviewImageUrls = new ArrayList<>();
        List<ReviewImage> reviewImages = new ArrayList<>();
        new File(uploadDir).mkdirs();

        for (MultipartFile file : request.reviewImageUrls()) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.matches("(?i).*\\.(jpg|jpeg|png)$")) {
                throw new ServiceException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
            }

            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir + filename);
                Files.write(path, file.getBytes());

                String url = "/static/review/" + filename; // 클라이언트 접근 URL
                reviewImageUrls.add(url);

                ReviewImage reviewImage = ReviewImage.builder()
                        .review(savedReview)
                        .reviewImageUrl(url)
                        .build();
                reviewImages.add(reviewImage);

            } catch (IOException e) {
                throw new ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED);
            }
        }

        reviewImageRepository.saveAll(reviewImages);

        return ReviewListResponse.from(savedReview, reviewImageUrls);
    }

    // 2. 리뷰 소프트 삭제 (본인 리뷰만)
    @Transactional
    public void deleteReview(Long reviewId, ReviewDeleteRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(request.userId())) {
            throw new ServiceException(ErrorCode.REVIEW_NOT_AUTHORIZED);
        }

        // DTO에서 하지 않음
        review.setIsDeleted(true);
    }

    // 3️. 특정 상품 리뷰 목록 조회
    @Transactional(readOnly = true)
    public List<ReviewListResponse> getReviewsByProduct(Long productId) {
        //delete 되지 않은 상품을 조회
        List<Review> reviews = reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId);

        //해당 리뷰의 이미지 조회
        return reviews.stream()
                .map(review -> {
                    List<String> imageUrls = reviewImageRepository.findByReview_ReviewId(review.getReviewId())
                            .stream()
                            .map(ReviewImage::getReviewImageUrl)
                            .toList();
                    return ReviewListResponse.from(review, imageUrls);
                })
                .toList();
    }
}
