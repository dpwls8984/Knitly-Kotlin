package com.mysite.knitly.domain.home.controller;

import com.mysite.knitly.domain.home.service.HomeSectionService;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.home.dto.HomeSummaryResponse;
import com.mysite.knitly.domain.home.dto.LatestPostItem;
import com.mysite.knitly.domain.home.dto.LatestReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {
    private final HomeSectionService homeSectionService;

    // 메인화면: 인기 상품 Top5
    @GetMapping("/popular/top5")
    public ResponseEntity<List<ProductListResponse>> popularTop5() {
        return ResponseEntity.ok(homeSectionService.getPopularTop5());
    }
    // 추가: 최신 리뷰
    @GetMapping("/latest/reviews")
    public ResponseEntity<List<LatestReviewItem>> latestReviews() {
        return ResponseEntity.ok(homeSectionService.getLatestReviews(3));
    }

    // 추가: 최신 커뮤니티 글 3개
    @GetMapping("/latest/posts")
    public ResponseEntity<List<LatestPostItem>> latestPosts() {
        return ResponseEntity.ok(homeSectionService.getLatestPosts(3));
    }

    // 추가: 홈 요약 (한 번 호출로 3섹션 모두)
    @GetMapping("/summary")
    public ResponseEntity<HomeSummaryResponse> summary() {
        return ResponseEntity.ok(homeSectionService.getHomeSummary());
    }
}
