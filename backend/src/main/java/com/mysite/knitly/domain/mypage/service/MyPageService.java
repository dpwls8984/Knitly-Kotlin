package com.mysite.knitly.domain.mypage.service;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageQueryRepository repo;

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
        return repo.findMyFavoriteProducts(userId, pageable);
    }

    // 리뷰 조회 연결
    @Transactional(readOnly = true)
    public Page<ReviewListItem> getMyReviews(Long userId, Pageable pageable) {
        return repo.findMyReviews(userId, pageable);
    }
}
