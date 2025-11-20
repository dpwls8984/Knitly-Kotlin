package com.mysite.knitly.domain.home.controller

import com.mysite.knitly.domain.home.dto.HomeSummaryResponse
import com.mysite.knitly.domain.home.dto.LatestPostItem
import com.mysite.knitly.domain.home.dto.LatestReviewItem
import com.mysite.knitly.domain.home.service.HomeSectionService
import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.user.entity.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/home")
class HomeController(
    private val homeSectionService: HomeSectionService
) {

    // 메인화면: 인기 상품 Top5
    fun popularTop5(
        @AuthenticationPrincipal(errorOnInvalidType = false) user: User?
    ): ResponseEntity<List<ProductListResponse>> {
        return ResponseEntity.ok(homeSectionService.getPopularTop5(user))
    }

    // 추가: 최신 리뷰
    @GetMapping("/latest/reviews")
    fun latestReviews(): ResponseEntity<List<LatestReviewItem>> {
        return ResponseEntity.ok(homeSectionService.getLatestReviews(3))
    }

    // 추가: 최신 커뮤니티 글 3개
    @GetMapping("/latest/posts")
    fun latestPosts(): ResponseEntity<List<LatestPostItem>> {
        return ResponseEntity.ok(homeSectionService.getLatestPosts(3))
    }

    // 추가: 홈 요약 (한 번 호출로 3섹션 모두)
    @GetMapping("/summary")
    fun summary(
        @AuthenticationPrincipal(errorOnInvalidType = false) user: User?
    ): ResponseEntity<HomeSummaryResponse> {
        return ResponseEntity.ok(homeSectionService.getHomeSummary(user))
    }
}