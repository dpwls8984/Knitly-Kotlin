package com.mysite.knitly.domain.product.review.service;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.entity.ReviewImage;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileNameUtils;
import com.mysite.knitly.global.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    String uploadDir = "resources/static/review/";
    String urlPrefix = "/resources/static/review/";


    public ReviewCreateResponse getReviewFormInfo(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        String thumbnailUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            thumbnailUrl = product.getProductImages().get(0).getProductImageUrl();
        }

        return new ReviewCreateResponse(product.getTitle(), thumbnailUrl);
    }

    // 1. 리뷰 등록
    @Transactional
    public void createReview(Long productId, User user, ReviewCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.rating())
                .content(request.content())
                .build();

        List<ReviewImage> reviewImages = new ArrayList<>();

        if (request.reviewImageUrls() != null && !request.reviewImageUrls().isEmpty()) {
            new File(uploadDir).mkdirs();

            List<MultipartFile> imageFiles = request.reviewImageUrls();

            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                try {
                    String filename = UUID.randomUUID() + "_" + originalFilename;
                    Path path = Path.of(uploadDir, filename);
                    Files.write(path, file.getBytes());

                    String url = urlPrefix + filename;

                    ReviewImage reviewImage = ReviewImage.builder()
                            .review(review)          // ✅ 반드시 review 설정
                            .reviewImageUrl(url)
                            .sortOrder(i)
                            .build();
                    reviewImages.add(reviewImage);

                } catch (IOException e) {
                    throw new ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED);
                }
            }
        }

        review.addReviewImages(reviewImages);
        reviewRepository.save(review);
    }


    // 2. 리뷰 소프트 삭제 (본인 리뷰만)
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new ServiceException(ErrorCode.REVIEW_NOT_AUTHORIZED);
        }

        review.setIsDeleted(true);
    }

    // 3️. 특정 상품 리뷰 목록 조회
    @Transactional(readOnly = true)
    public List<ReviewListResponse> getReviewsByProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        //delete 되지 않은 상품을 조회
        List<Review> reviews = reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId, pageable);

        //해당 리뷰의 이미지 조회
        return reviews.stream()
                .map(review -> {
                    List<String> imageUrls = review.getReviewImages().stream()
                            .map(ReviewImage::getReviewImageUrl)
                            .toList();
                    return ReviewListResponse.from(review, imageUrls);
                })
                .toList();
    }
}
