package com.mysite.knitly.domain.home.controller;

import com.mysite.knitly.domain.home.service.HomeSectionService;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
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
}
