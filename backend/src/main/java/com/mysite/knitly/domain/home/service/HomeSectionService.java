package com.mysite.knitly.domain.home.service;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class HomeSectionService {

    private final RedisProductService redisProductService;
    private final ProductRepository productRepository;

    // 인기 Top5 조회 - 홈 화면용
    public List<ProductListResponse> getPopularTop5() {
        List<Long> topIds = redisProductService.getTopNPopularProducts(5);

        if (topIds.isEmpty()) {
            // Redis에 데이터 없으면 DB에서 직접 조회
            Pageable top5 = PageRequest.of(0, 5, Sort.by("purchaseCount").descending());
            return productRepository.findByIsDeletedFalse(top5)
                    .getContent()
                    .stream()
                    .map(ProductListResponse::from)
                    .toList();
        }

        List<Product> products = productRepository.findByProductIdInAndIsDeletedFalse(topIds);

        // Redis 순서대로 정렬
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        return topIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .map(ProductListResponse::from)
                .collect(Collectors.toList());
    }
}
