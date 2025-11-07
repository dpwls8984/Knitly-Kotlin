package com.mysite.knitly.domain.product.review.service;

import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderItemRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LocalFileStorage localFileStorage;
    private final OrderItemRepository orderItemRepository;

    public ReviewCreateResponse getReviewFormInfo(Long orderItemId) {
        log.info("[Review] [Form] 리뷰 작성 폼 조회 시작 - orderItemId={}", orderItemId);

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_ITEM_NOT_FOUND)); // (ErrorCode 추가 필요)

        Product product = orderItem.getProduct();

        String thumbnailUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            thumbnailUrl = product.getProductImages().get(0).getProductImageUrl();
        }

        log.debug("[Review] [Form] 썸네일 URL 추출 완료 - thumbnailUrl={}", thumbnailUrl);
        log.info("[Review] [Form] 리뷰 작성 폼 조회 완료 - productTitle={}", product.getTitle());

        return new ReviewCreateResponse(product.getTitle(), thumbnailUrl);
    }

    // 1. 리뷰 등록
    @Transactional
    public void createReview(Long orderItemId, User user, ReviewCreateRequest request) {
        log.info("[Review] [Create] 리뷰 생성 시작 - orderItemId={}, userId={}", orderItemId, user.getUserId());

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Product product = orderItem.getProduct();

        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.rating())
                .content(request.content())
                .build();

        List<ReviewImage> reviewImages = new ArrayList<>();

        if (request.reviewImageUrls() != null && !request.reviewImageUrls().isEmpty()) {
            log.debug("[Review] [Create] 첨부 이미지 처리 시작 - count={}", request.reviewImageUrls().size());

            List<MultipartFile> imageFiles = request.reviewImageUrls();

            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                if (file.isEmpty()) continue;

                String url = localFileStorage.saveReviewImage(file);
                log.debug("[Review] [Create] 이미지 저장 완료 - index={}, url={}", i, url);

                ReviewImage reviewImage = ReviewImage.builder()
                        .review(review)
                        .reviewImageUrl(url)
                        .sortOrder(i)
                        .build();
                reviewImages.add(reviewImage);
            }
        }

        review.addReviewImages(reviewImages);
        reviewRepository.save(review);
        log.info("[Review] [Create] 리뷰 생성 완료 - reviewId={}, productId={}", review.getReviewId(), product.getProductId());
    }


    // 2. 리뷰 소프트 삭제 (본인 리뷰만)
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        log.info("[Review] [Delete] 리뷰 삭제 요청 - reviewId={}, userId={}", reviewId, user.getUserId());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            log.warn("[Review] [Delete] 삭제 권한 없음 - reviewUserId={}, requesterId={}",
                    review.getUser().getUserId(), user.getUserId());
            throw new ServiceException(ErrorCode.REVIEW_NOT_AUTHORIZED);
        }

        review.setIsDeleted(true);
        log.info("[Review] [Delete] 리뷰 소프트 삭제 완료 - reviewId={}", reviewId);
    }

    // 3️. 특정 상품 리뷰 목록 조회
    @Transactional(readOnly = true)
    public Page<ReviewListResponse> getReviewsByProduct(Long productId, int page, int size) {
        log.info("[Review] [List] 상품 리뷰 목록 조회 시작 - productId={}, page={}", productId, page);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId, pageable);

        Page<ReviewListResponse> result = reviews.map(review -> {
            List<String> imageUrls = review.getReviewImages().stream()
                    .map(ReviewImage::getReviewImageUrl)
                    .toList();
            return ReviewListResponse.from(review, imageUrls);
        });

        log.debug("[Review] [List] 조회된 리뷰 수 - count={}", result.getTotalElements());
        log.info("[Review] [List] 상품 리뷰 목록 조회 완료 - productId={}", productId);

        return result;
    }
}
