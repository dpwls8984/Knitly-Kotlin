package com.mysite.knitly.domain.home.service;

import com.mysite.knitly.domain.home.dto.HomeSummaryResponse;
import com.mysite.knitly.domain.home.dto.LatestPostItem;
import com.mysite.knitly.domain.home.dto.LatestReviewItem;
import com.mysite.knitly.domain.home.repository.HomeQueryRepository;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class HomeSectionService {

    private final RedisProductService redisProductService;
    private final ProductRepository productRepository;
    private final HomeQueryRepository homeQueryRepository;
    private final ProductLikeRepository productLikeRepository;

    // 인기 Top5 조회 - 홈 화면용
    public List<ProductListResponse> getPopularTop5(User user) {
        List<Long> topIds = redisProductService.getTopNPopularProducts(5);

        if (topIds.isEmpty()) {
            // Redis에 데이터 없으면 DB에서 직접 조회
            Pageable top5 = PageRequest.of(0, 5, Sort.by("purchaseCount").descending());
            List<Product> products = productRepository.findByIsDeletedFalse(top5).getContent();

            return mapProductsToResponse(user, products);
        }

        List<Product> unorderedProducts = productRepository.findByProductIdInAndIsDeletedFalse(topIds);

        // [수정] 찜 여부 확인
        Set<Long> likedProductIds = getLikedProductIds(user, unorderedProducts);

        // Redis 순서대로 정렬
        Map<Long, Product> productMap = unorderedProducts.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        // [수정] 람다를 사용해 "from(Product, boolean)" 호출
        return topIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .map(product -> ProductListResponse.from(
                        product,
                        likedProductIds.contains(product.getProductId())
                ))
                .collect(Collectors.toList());
    }
    // 최신 리뷰 N개
    public List<LatestReviewItem> getLatestReviews(int limit) {
        return homeQueryRepository.findLatestReviews(limit);
    }

    // 최신 커뮤니티 글 N개
    public List<LatestPostItem> getLatestPosts(int limit) {
        return homeQueryRepository.findLatestPosts(limit);
    }
    // 홈 (인기 5 + 최신 리뷰 3 + 최신 글 3)
    public HomeSummaryResponse getHomeSummary(User user) {
        var popular = getPopularTop5(user); // user 전달
        var reviews = getLatestReviews(3);
        var posts   = getLatestPosts(3);
        return new HomeSummaryResponse(popular, reviews, posts);
    }

    private List<ProductListResponse> mapProductsToResponse(User user, List<Product> products) {
        Set<Long> likedProductIds = getLikedProductIds(user, products);
        return products.stream()
                .map(product -> ProductListResponse.from(
                        product,
                        likedProductIds.contains(product.getProductId())
                ))
                .toList();
    }

    private Set<Long> getLikedProductIds(User user, List<Product> products) {
        if (user == null || products.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .toList();
        // (가정) user.getUserId()
        return productLikeRepository.findLikedProductIdsByUserId(user.getUserId(), productIds);
    }
}