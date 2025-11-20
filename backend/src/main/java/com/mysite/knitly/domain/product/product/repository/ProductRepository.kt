package com.mysite.knitly.domain.product.product.repository

import com.mysite.knitly.domain.design.entity.Design
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
    // 전체 상품 조회 (삭제되지 않은 것만, Design 상태가 ON_SALE인 것만)
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByIsDeletedFalse(pageable: Pageable): Page<Product>

    // productId로 여러 개 조회 (인기순용 - Redis에서 받은 ID로 조회)
    // Design 상태가 ON_SALE인 것만 조회
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByProductIdInAndIsDeletedFalse(@Param("productIds") productIds: List<Long>): List<Product>

    // 단일 상품 조회 (Design 상태가 ON_SALE인 것만)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByProductIdAndIsDeletedFalse(@Param("productId") productId: Long): Product?


    /**
     * 전체 상품 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findAllWithImagesAndNotDeleted(pageable: Pageable): Page<Product>

    /**
     * 판매자 스토어 _ 특정 유저의 전체 상품 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query(
        countQuery = """
        SELECT COUNT(DISTINCT p.productId)
        FROM Product p
        LEFT JOIN p.design d
        WHERE p.user.userId = :userId
        AND p.isDeleted = false
        AND d.designState = 'ON_SALE'
    """,
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
            (SELECT pi2.productImageUrl 
             FROM ProductImage pi2 
             WHERE pi2.product = p 
             AND pi2.sortOrder = 1),
            p.user.userId
        )
        FROM Product p
        LEFT JOIN p.design d
        WHERE p.user.userId = :userId
        AND p.isDeleted = false
        AND d.designState = 'ON_SALE'
        ORDER BY p.createdAt DESC
    """
    )
    fun findOnSaleProductsByUserIdWithThumbnail(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<ProductWithThumbnailDto>

    /**
     * 카테고리별 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productCategory = :category AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByCategoryWithImagesAndNotDeleted(
        @Param("category") category: ProductCategory,
        pageable: Pageable
    ): Page<Product>

    /**
     * 무료 상품 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.price = :price AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByPriceWithImagesAndNotDeleted(
        @Param("price") price: Double,
        pageable: Pageable
    ): Page<Product>

    /**
     * 한정판매 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity IS NOT NULL AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findLimitedWithImagesAndNotDeleted(pageable: Pageable): Page<Product>

    /**
     * productId 리스트로 여러 개 조회 (batch size로 N+1 방지)
     * fetch join 제거 - @BatchSize 어노테이션이 동작하도록 변경
     * Design 상태가 ON_SALE인 것만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.isDeleted = false AND p.design.designState = 'ON_SALE'")
    fun findByProductIdInWithImagesAndNotDeleted(
        @Param("productIds") productIds: List<Long>
    ): List<Product>
}
