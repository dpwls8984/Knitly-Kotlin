package com.mysite.knitly.domain.mypage.service;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageQueryRepository repo;
    private final ReviewRepository reviewRepository;
    private final ProductLikeRepository productLikeRepository;

    // 주문 내역 조회
    @Transactional(readOnly = true)
    public Page<OrderCardResponse> getOrderCards(Long userId, Pageable pageable) {
        log.info("[MyPageService] 주문 내역 조회 - userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<OrderCardResponse> page = repo.findOrderCards(userId, pageable);
        log.info("[MyPageService] 주문 내역 조회 완료 - userId={}, totalElements={}", userId, page.getTotalElements());
        return page;
    }

    // 내가 쓴 글 조회
    @Transactional(readOnly = true)
    public Page<MyPostListItemResponse> getMyPosts(Long userId, String query, Pageable pageable) {
        log.info("[MyPageService] 내 글 조회 - userId={}, query='{}', page={}, size={}", userId, query, pageable.getPageNumber(), pageable.getPageSize());
        Page<MyPostListItemResponse> page = repo.findMyPosts(userId, query, pageable);
        log.info("[MyPageService] 내 글 조회 완료 - userId={}, totalElements={}", userId, page.getTotalElements());
        return page;
    }

    // 내가 쓴 댓글 조회
    @Transactional(readOnly = true)
    public Page<MyCommentListItem> getMyComments(Long userId, String query, Pageable pageable) {
        log.info("[MyPageService] 내 댓글 조회 - userId={}, query='{}', page={}, size={}", userId, query, pageable.getPageNumber(), pageable.getPageSize());
        Page<MyCommentListItem> page = repo.findMyComments(userId, query, pageable);
        log.info("[MyPageService] 내 댓글 조회 완료 - userId={}, totalElements={}", userId, page.getTotalElements());
        return page;
    }

    // 내가 찜한 상품 조회
    @Transactional(readOnly = true)
    public Page<FavoriteProductItem> getMyFavorites(Long userId, Pageable pageable) {
        log.info("[MyPageService] 찜 목록 조회 - userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<FavoriteProductItem> page = productLikeRepository.findByUser_UserId(userId, pageable)
                .map(this::convertToDto);
        log.info("[MyPageService] 찜 목록 조회 완료 - userId={}, totalElements={}", userId, page.getTotalElements());
        return page;
    }

    private FavoriteProductItem convertToDto(ProductLike pl) {
        var p = pl.getProduct();

        String thumbnailUrl = p.getProductImages().isEmpty()
                ? null
                : p.getProductImages().get(0).getProductImageUrl(); // 첫 번째 이미지

        return new FavoriteProductItem(
                p.getProductId(),
                p.getTitle(),
                p.getUser().getName(),
                thumbnailUrl
        );
    }

    // 사용자가 작성한 리뷰 조회
    @Transactional(readOnly = true)
    public Page<ReviewListItem> getMyReviewsV2(Long userId, Pageable pageable) {
        log.info("[MyPageService] 내 리뷰 조회 - userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());

        List<Review> reviews = reviewRepository.findByUser_UserIdAndIsDeletedFalse(userId, pageable);

        List<ReviewListItem> list = reviews.stream().map(r -> {
            var product = r.getProduct();
            String productThumbnail = product.getProductImages().isEmpty() ? null :
                    product.getProductImages().get(0).getProductImageUrl();

            List<String> reviewImages = r.getReviewImages().stream()
                    .map(ri -> ri.getReviewImageUrl())
                    .toList();

            return new ReviewListItem(
                    r.getReviewId(),
                    product.getProductId(),
                    product.getTitle(),
                    productThumbnail,
                    r.getRating(),
                    r.getContent(),
                    reviewImages,
                    r.getCreatedAt().toLocalDate()
            );
        }).toList();

        long total = reviewRepository.countByUser_UserIdAndIsDeletedFalse(userId);
        log.info("[MyPageService] 내 리뷰 조회 완료 - userId={}, totalElements={}", userId, total);

        return new PageImpl<>(list, pageable, total);
    }
}
