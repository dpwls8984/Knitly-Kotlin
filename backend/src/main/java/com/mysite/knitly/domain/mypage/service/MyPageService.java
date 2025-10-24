package com.mysite.knitly.domain.mypage.service;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository;
import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageQueryRepository repo;
    private final ReviewRepository reviewRepository;
    private final ProductLikeRepository productLikeRepository;

    // 주문 내역 조회
    @Transactional(readOnly = true)
    public Page<OrderCardResponse> getOrderCards(Long userId, Pageable pageable) {
        return repo.findOrderCards(userId, pageable);
    }

    // 내가 쓴 글 조회
    @Transactional(readOnly = true)
    public Page<MyPostListItemResponse> getMyPosts(Long userId, String query, Pageable pageable) {
        return repo.findMyPosts(userId, query, pageable);
    }

    // 내가 쓴 댓글 조회
    @Transactional(readOnly = true)
    public Page<MyCommentListItem> getMyComments(Long userId, String query, Pageable pageable) {
        return repo.findMyComments(userId, query, pageable);
    }

    // 내가 찜한 상품 조회
    @Transactional(readOnly = true)
    public Page<FavoriteProductItem> getMyFavorites(Long userId, Pageable pageable) {
        return productLikeRepository.findByUser_UserId(userId, pageable)
                .map(this::convertToDto);
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

        return new PageImpl<>(list, pageable, total);
    }
}
