package com.mysite.knitly.domain.product.product.repository;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    //n+1 문제 해결을 위한 fetch join
    @Query("SELECT p FROM Product p JOIN FETCH p.user WHERE p.productId = :productId")
    Optional<Product> findByIdWithUser(Long productId);

    // 전체 상품 조회 (삭제되지 않은 것만)
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    // 카테고리별 조회
    Page<Product> findByProductCategoryAndIsDeletedFalse(
            ProductCategory category, Pageable pageable);

    // 무료 상품 조회 (price = 0)
    Page<Product> findByPriceAndIsDeletedFalse(Double price, Pageable pageable);

    // 한정판매 조회 (stockQuantity != null)
    Page<Product> findByStockQuantityIsNotNullAndIsDeletedFalse(Pageable pageable);

    // productId로 여러 개 조회 (인기순용 - Redis에서 받은 ID로 조회)
    List<Product> findByProductIdInAndIsDeletedFalse(List<Long> productIds);

    // userId로 여러 개 조회
    Page<Product> findByUser_userIdAndIsDeletedFalse(Long userId, Pageable pageable);
}
