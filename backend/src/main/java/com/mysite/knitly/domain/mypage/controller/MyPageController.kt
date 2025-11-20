package com.mysite.knitly.domain.mypage.controller

import com.mysite.knitly.domain.mypage.dto.*
import com.mysite.knitly.domain.mypage.service.MyPageService
import com.mysite.knitly.domain.payment.dto.PaymentDetailResponse
import com.mysite.knitly.domain.payment.service.PaymentService
import com.mysite.knitly.domain.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/mypage")
@PreAuthorize("isAuthenticated()")
class MyPageController(
    private val service: MyPageService,
    private val paymentService: PaymentService
) {

    private val log = LoggerFactory.getLogger(MyPageController::class.java)

    // 프로필 조회 (이름 + 이메일)
    @GetMapping("/profile")
    fun profile(
        @AuthenticationPrincipal principal: User
    ): ProfileResponse {
        log.info("[MyPageController] 프로필 조회 요청 - userId={}", principal.userId)
        val resp = ProfileResponse(principal.name, principal.email)
        log.info("[MyPageController] 프로필 조회 완료 - userId={}", principal.userId)
        return resp
    }

    // 주문 내역 조회
    @GetMapping("/orders")
    fun orders(
        @AuthenticationPrincipal principal: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "3") size: Int
    ): PageResponse<OrderCardResponse> {
        log.info(
            "[MyPageController] 주문 내역 조회 요청 - userId={}, page={}, size={}",
            principal.userId,
            page,
            size
        )
        val result = PageResponse.of(
            service.getOrderCards(
                principal.userId,
                PageRequest.of(page, size)
            )
        )
        log.info(
            "[MyPageController] 주문 내역 조회 완료 - userId={}, returned={}",
            principal.userId,
            result.content.size
        )
        return result
    }

    // 주문 내역별 결제 정보 조회
    @GetMapping("/orders/{orderId}/payment")
    fun myOrderPayment(
        @AuthenticationPrincipal principal: User,
        @PathVariable orderId: Long
    ): ResponseEntity<PaymentDetailResponse> {
        log.info(
            "[MyPageController] 결제 상세 조회 요청 - userId={}, orderId={}",
            principal.userId,
            orderId
        )
        val detail = paymentService.getPaymentDetailByOrder(principal, orderId)
        log.info(
            "[MyPageController] 결제 상세 조회 완료 - userId={}, orderId={}",
            principal.userId,
            orderId
        )
        return ResponseEntity.ok(detail)
    }

    // 내가 쓴 글 조회 (검색 + 정렬)
    @GetMapping("/posts")
    fun myPosts(
        @AuthenticationPrincipal principal: User,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponse<MyPostListItemResponse> {
        log.info(
            "[MyPageController] 내 글 조회 요청 - userId={}, query='{}', page={}, size={}",
            principal.userId,
            query,
            page,
            size
        )
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = PageResponse.of(
            service.getMyPosts(principal.userId, query, pageable)
        )
        log.info(
            "[MyPageController] 내 글 조회 완료 - userId={}, returned={}",
            principal.userId,
            result.content.size
        )
        return result
    }

    // 내가 쓴 댓글 조회 (검색 + 정렬)
    @GetMapping("/comments")
    fun myComments(
        @AuthenticationPrincipal principal: User,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponse<MyCommentListItem> {
        log.info(
            "[MyPageController] 내 댓글 조회 요청 - userId={}, query='{}', page={}, size={}",
            principal.userId,
            query,
            page,
            size
        )
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = PageResponse.of(
            service.getMyComments(principal.userId, query, pageable)
        )
        log.info(
            "[MyPageController] 내 댓글 조회 완료 - userId={}, returned={}",
            principal.userId,
            result.content.size
        )
        return result
    }

    // 내가 찜한 상품 조회
    @GetMapping("/favorites")
    fun myFavorites(
        @AuthenticationPrincipal principal: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponse<FavoriteProductItem> {
        log.info(
            "[MyPageController] 찜 목록 조회 요청 - userId={}, page={}, size={}",
            principal.userId,
            page,
            size
        )
        val pageable = PageRequest.of(page, size)
        val result = PageResponse.of(
            service.getMyFavorites(principal.userId, pageable)
        )
        log.info(
            "[MyPageController] 찜 목록 조회 완료 - userId={}, returned={}",
            principal.userId,
            result.content.size
        )
        return result
    }

    // 내가 작성한 리뷰 조회
    @GetMapping("/reviews")
    fun myReviews(
        @AuthenticationPrincipal principal: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponse<ReviewListItem> {
        log.info(
            "[MyPageController] 내 리뷰 조회 요청 - userId={}, page={}, size={}",
            principal.userId,
            page,
            size
        )
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = PageResponse.of(
            service.getMyReviewsV2(principal.userId, pageable)
        )
        log.info(
            "[MyPageController] 내 리뷰 조회 완료 - userId={}, returned={}",
            principal.userId,
            result.content.size
        )
        return result
    }
}
