package com.mysite.knitly.domain.mypage.controller;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.service.MyPageService;
import com.mysite.knitly.domain.payment.dto.PaymentDetailResponse;
import com.mysite.knitly.domain.payment.service.PaymentService;
import com.mysite.knitly.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyPageController {

    private final MyPageService service;
    private final PaymentService paymentService;

    // 프로필 조회 (이름 + 이메일)
    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal User principal) {
        log.info("[MyPageController] 프로필 조회 요청 - userId={}", principal.getUserId());
        ProfileResponse resp = new ProfileResponse(principal.getName(), principal.getEmail());
        log.info("[MyPageController] 프로필 조회 완료 - userId={}", principal.getUserId());
        return resp;
    }

    // 주문 내역 조회
    @GetMapping("/orders")
    public PageResponse<OrderCardResponse> orders(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        log.info("[MyPageController] 주문 내역 조회 요청 - userId={}, page={}, size={}", principal.getUserId(), page, size);
        var result = PageResponse.of(service.getOrderCards(principal.getUserId(), PageRequest.of(page, size)));
        log.info("[MyPageController] 주문 내역 조회 완료 - userId={}, returned={}", principal.getUserId(), result.content().size());
        return result;
    }

    // 주문 내역별 결제 정보 조회
    @GetMapping("/orders/{orderId}/payment")
    public ResponseEntity<?> myOrderPayment(
            @AuthenticationPrincipal User principal,
            @PathVariable Long orderId
    ) {
        log.info("[MyPageController] 결제 상세 조회 요청 - userId={}, orderId={}", principal.getUserId(), orderId);
        PaymentDetailResponse detail = paymentService.getPaymentDetailByOrder(principal, orderId);
        log.info("[MyPageController] 결제 상세 조회 완료 - userId={}, orderId={}", principal.getUserId(), orderId);
        return ResponseEntity.ok(detail);
    }

    // 내가 쓴 글 조회 (검색 + 정렬)
    @GetMapping("/posts")
    public PageResponse<MyPostListItemResponse> myPosts(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[MyPageController] 내 글 조회 요청 - userId={}, query='{}', page={}, size={}", principal.getUserId(), query, page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = PageResponse.of(service.getMyPosts(principal.getUserId(), query, pageable));
        log.info("[MyPageController] 내 글 조회 완료 - userId={}, returned={}", principal.getUserId(), result.content().size());
        return result;
    }

    // 내가 쓴 댓글 조회 (검색 + 정렬)
    @GetMapping("/comments")
    public PageResponse<MyCommentListItem> myComments(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[MyPageController] 내 댓글 조회 요청 - userId={}, query='{}', page={}, size={}", principal.getUserId(), query, page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = PageResponse.of(service.getMyComments(principal.getUserId(), query, pageable));
        log.info("[MyPageController] 내 댓글 조회 완료 - userId={}, returned={}", principal.getUserId(), result.content().size());
        return result;
    }

    // 내가 찜한 상품 조회
    @GetMapping("/favorites")
    public PageResponse<FavoriteProductItem> myFavorites(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[MyPageController] 찜 목록 조회 요청 - userId={}, page={}, size={}", principal.getUserId(), page, size);
        var pageable = PageRequest.of(page, size);
        var result = PageResponse.of(service.getMyFavorites(principal.getUserId(), pageable));
        log.info("[MyPageController] 찜 목록 조회 완료 - userId={}, returned={}", principal.getUserId(), result.content().size());
        return result;
    }

    // 내가 작성한 리뷰 조회
    @GetMapping("/reviews")
    public PageResponse<ReviewListItem> myReviews(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[MyPageController] 내 리뷰 조회 요청 - userId={}, page={}, size={}", principal.getUserId(), page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = PageResponse.of(service.getMyReviewsV2(principal.getUserId(), pageable));
        log.info("[MyPageController] 내 리뷰 조회 완료 - userId={}, returned={}", principal.getUserId(), result.content().size());
        return result;
    }
}
