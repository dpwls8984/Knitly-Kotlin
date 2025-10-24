package com.mysite.knitly.domain.mypage.controller;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.service.MyPageService;
import com.mysite.knitly.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService service;

    // 프로필 조회 (이름 + 이메일)
    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal User principal) {
        return new ProfileResponse(principal.getName(), principal.getEmail());
    }

    // 주문 내역 조회
    @GetMapping("/orders")
    public PageResponse<OrderCardResponse> orders(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        return PageResponse.of(service.getOrderCards(principal.getUserId(), PageRequest.of(page, size)));
    }

    // 내가 쓴 글 조회 (검색기능 + 정렬)
    @GetMapping("/posts")
    public PageResponse<MyPostListItemResponse> myPosts(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(service.getMyPosts(principal.getUserId(), query, pageable));
    }

    // 내가 쓴 댓글 조회 (검색 + 정렬)
    @GetMapping("/comments")
    public PageResponse<MyCommentListItem> myComments(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(service.getMyComments(principal.getUserId(), query, pageable));
    }
    // 내가 찜한 상품 조회
    @GetMapping("/favorites")
    public PageResponse<FavoriteProductItem> myFavorites(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = PageRequest.of(page, size);
        return PageResponse.of(service.getMyFavorites(principal.getUserId(), pageable));
    }

    // 리뷰
    @GetMapping("/reviews")
    public PageResponse<ReviewListItem> myReviews(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(service.getMyReviewsV2(principal.getUserId(), pageable));
    }
}
