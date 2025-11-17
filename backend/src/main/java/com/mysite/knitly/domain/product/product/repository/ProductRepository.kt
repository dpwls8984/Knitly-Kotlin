package com.mysite.knitly.domain.product.product.repository

import com.mysite.knitly.domain.product.product.dto.ProductWithThumbnailDto
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    // 전체 상품 조회 (삭제되지 않은 것만)
    fun findByIsDeletedFalse(pageable: Pageable): Page<Product>

    // productId로 여러 개 조회 (인기순용 - Redis에서 받은 ID로 조회)
    fun findByProductIdInAndIsDeletedFalse(productIds: List<Long>): List<Product>

    fun findByProductIdAndIsDeletedFalse(productId: Long): Product?

    /**
     * userId로 판매 상품 조회 (대표 이미지 포함)
     *
     * sortOrder = 1인 대표 이미지만 LEFT JOIN
     * DTO 프로젝션으로 한 번의 쿼리로 조회
     */
    @Query(
        value = """
            SELECT new com.mysite.knitly.domain.product.product.dto.ProductWithThumbnailDto(
                p.productId,
                p.title,
                p.productCategory,
                p.price,
                p.purchaseCount,
                p.likeCount,
                p.stockQuantity,
                p.avgReviewRating,
                p.createdAt,
                pi.productImageUrl,
                p.user.userId
            )
            FROM Product p
            LEFT JOIN ProductImage pi ON pi.product.productId = p.productId 
                AND pi.sortOrder = 1
            WHERE p.user.userId = :userId
            AND p.isDeleted = false
            ORDER BY p.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT p.productId)
            FROM Product p
            WHERE p.user.userId = :userId
            AND p.isDeleted = false
        """
    )
    fun findByUserIdWithThumbnail(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<ProductWithThumbnailDto>

    /**
     * 전체 상품 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     */
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false")
    fun findAllWithImagesAndNotDeleted(pageable: Pageable): Page<Product>

    /**
     * 카테고리별 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     */
    @Query("SELECT p FROM Product p WHERE p.productCategory = :category AND p.isDeleted = false")
    fun findByCategoryWithImagesAndNotDeleted(
        @Param("category") category: ProductCategory,
        pageable: Pageable
    ): Page<Product>

    /**
     * 무료 상품 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     */
    @Query("SELECT p FROM Product p WHERE p.price = :price AND p.isDeleted = false")
    fun findByPriceWithImagesAndNotDeleted(
        @Param("price") price: Double,
        pageable: Pageable
    ): Page<Product>

    /**
     * 한정판매 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity IS NOT NULL AND p.isDeleted = false")
    fun findLimitedWithImagesAndNotDeleted(pageable: Pageable): Page<Product>

    /**
     * productId 리스트로 여러 개 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     */
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.isDeleted = false")
    fun findByProductIdInWithImagesAndNotDeleted(
        @Param("productIds") productIds: List<Long>
    ): List<Product>
}
