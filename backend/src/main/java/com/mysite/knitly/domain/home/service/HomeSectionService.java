package com.mysite.knitly.domain.home.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeSectionService {

    private final RedisProductService redisProductService;
    private final ProductRepository productRepository;
    private final HomeQueryRepository homeQueryRepository;
    private final ProductLikeRepository productLikeRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5";

    // 인기 Top5 조회 - 홈 화면용
    public List<ProductListResponse> getPopularTop5(User user) {
        long startTime = System.currentTimeMillis();
        Long userId = user != null ? user.getUserId() : null;

        log.info("[Home] [Top5] 인기 Top5 조회 시작 - userId={}", userId);

        boolean cacheable = (userId == null);

        if (cacheable) {
            try {
                String cachedJson = stringRedisTemplate.opsForValue()
                        .get(HOME_POPULAR_TOP5_CACHE_KEY);

                if (cachedJson != null) {
                    List<ProductListResponse> cachedResult =
                            objectMapper.readValue(cachedJson, new TypeReference<>() {});

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[Home] [Top5] 캐시 히트 - key={}, resultCount={}, duration={}ms",
                            HOME_POPULAR_TOP5_CACHE_KEY, cachedResult.size(), duration);

                    return cachedResult;
                }
            } catch (Exception e) {
                log.error("[Home] [Top5] 캐시 조회 실패 - key={}", HOME_POPULAR_TOP5_CACHE_KEY, e);
            }
        }

        try {
            long redisStartTime = System.currentTimeMillis();
            List<Long> topIds = redisProductService.getTopNPopularProducts(5);
            long redisDuration = System.currentTimeMillis() - redisStartTime;

            log.debug("[Home] [Top5] Redis 조회 완료 - count={}, redisDuration={}ms",
                    topIds.size(), redisDuration);

            List<ProductListResponse> result;

            if (topIds.isEmpty()) {
                // Redis에 데이터 없으면 DB에서 직접 조회
                log.warn("[Home] [Top5] Redis 데이터 없음, DB에서 직접 조회");

                long dbStartTime = System.currentTimeMillis();
                Pageable top5 = PageRequest.of(0, 5, Sort.by("purchaseCount").descending());
                List<Product> products = productRepository.findByIsDeletedFalse(top5).getContent();

                long dbDuration = System.currentTimeMillis() - dbStartTime;

                log.debug("[Home] [Top5] DB 직접 조회 완료 - count={}, dbDuration={}ms",
                        products.size(), dbDuration);

                result = mapProductsToResponse(user, products);
            } else {
                // Redis에서 받은 ID로 DB에서 조회
                long dbStartTime = System.currentTimeMillis();
                List<Product> unorderedProducts = productRepository.findByProductIdInAndIsDeletedFalse(topIds);
                long dbDuration = System.currentTimeMillis() - dbStartTime;

                log.debug("[Home] [Top5] DB 상품 정보 조회 완료 - requestedCount={}, foundCount={}, dbDuration={}ms",
                        topIds.size(), unorderedProducts.size(), dbDuration);

                // [수정] 찜 여부 확인
                long likeStartTime = System.currentTimeMillis();
                Set<Long> likedProductIds = getLikedProductIds(user, unorderedProducts);
                long likeDuration = System.currentTimeMillis() - likeStartTime;
                log.debug("[Home] [Top5] 좋아요 정보 조회 완료 - likedCount={}, likeDuration={}ms",
                        likedProductIds.size(), likeDuration);

                // Redis 순서대로 정렬
                Map<Long, Product> productMap = unorderedProducts.stream()
                        .collect(Collectors.toMap(Product::getProductId, p -> p));

                result = topIds.stream()
                        .map(productMap::get)
                        .filter(Objects::nonNull)
                        .map(product -> ProductListResponse.from(
                                product,
                                likedProductIds.contains(product.getProductId())
                        ))
                        .collect(Collectors.toList());
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Home] [Top5] 인기 Top5 조회 완료 - userId={}, resultCount={}, totalDuration={}ms",
                    userId, result.size(), totalDuration);

            if (cacheable) {
                try {
                    String jsonData = objectMapper.writeValueAsString(result);
                    stringRedisTemplate.opsForValue()
                            .set(HOME_POPULAR_TOP5_CACHE_KEY, jsonData, Duration.ofSeconds(60));

                    log.info("[Home] [Top5] 캐시 쓰기 완료 - key={}, ttl={}s",
                            HOME_POPULAR_TOP5_CACHE_KEY, 60);
                } catch (Exception e) {
                    log.error("[Home] [Top5] 캐시 쓰기 실패 - key={}", HOME_POPULAR_TOP5_CACHE_KEY, e);
                }
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Home] [Top5] 인기 Top5 조회 실패 - userId={}, duration={}ms",
                    userId, duration, e);
            throw e;
        }
    }
    // 최신 리뷰 N개
    public List<LatestReviewItem> getLatestReviews(int limit) {
        long startTime = System.currentTimeMillis();

        log.debug("[Home] [LatestReviews] 최신 리뷰 조회 시작 - limit={}", limit);

        try{
            List<LatestReviewItem> result = homeQueryRepository.findLatestReviews(limit);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[Home] [LatestReviews] 최신 리뷰 조회 완료 - limit={}, resultCount={}, duration={}ms",
                    limit, result.size(), duration);

            return result;
        } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("[Home] [LatestReviews] 최신 리뷰 조회 실패 - limit={}, duration={}ms",
                limit, duration, e);
        throw e;
        }
    }

    // 최신 커뮤니티 글 N개
    public List<LatestPostItem> getLatestPosts(int limit) {
        long startTime = System.currentTimeMillis();

        log.debug("[Home] [LatestPosts] 최신 커뮤니티 글 조회 시작 - limit={}", limit);

        try{
            List<LatestPostItem> result = homeQueryRepository.findLatestPosts(limit);
            ;long duration = System.currentTimeMillis() - startTime;
            log.info("[Home] [LatestPosts] 최신 커뮤니티 글 조회 완료 - limit={}, resultCount={}, duration={}ms",
                    limit, result.size(), duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Home] [LatestPosts] 최신 커뮤니티 글 조회 실패 - limit={}, duration={}ms",
                    limit, duration, e);
            throw e;
        }
    }
    // 홈 (인기 5 + 최신 리뷰 3 + 최신 글 3)
    public HomeSummaryResponse getHomeSummary(User user) {
        long startTime = System.currentTimeMillis();
        Long userId = user != null ? user.getUserId() : null;

        log.info("[Home] [Summary] 홈 요약 정보 조회 시작 - userId={}", userId);

        try{
            var popular = getPopularTop5(user); // user 전달
            var reviews = getLatestReviews(3);
            var posts   = getLatestPosts(3);
            HomeSummaryResponse response = new HomeSummaryResponse(popular, reviews, posts);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Home] [Summary] 홈 요약 정보 조회 완료 - userId={}, popularCount={}, reviewCount={}, postCount={}, totalDuration={}ms",
                    userId, popular.size(), reviews.size(), posts.size(), totalDuration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Home] [Summary] 홈 요약 정보 조회 실패 - userId={}, duration={}ms",
                    userId, duration, e);
            throw e;
        }
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