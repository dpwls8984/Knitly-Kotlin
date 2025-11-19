package com.mysite.knitly.domain.product.like.repository

import com.mysite.knitly.domain.product.like.entity.ProductLike
import com.mysite.knitly.domain.product.like.entity.ProductLikeId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ProductLikeRepository : JpaRepository<ProductLike, ProductLikeId> {

    fun findByUserUserId(userId: Long, pageable: Pageable): Page<ProductLike>

    @Transactional
    @Modifying
    fun deleteByUserIdAndProductId(userId: Long, productId: Long)

    @Query("SELECT pl.productId FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId IN :productIds")
    fun findLikedProductIdsByUserId(
        @Param("userId") userId: Long,
        @Param("productIds") productIds: List<Long>
    ): Set<Long>

    fun existsByUserIdAndProductId(userId: Long?, productId: Long): Boolean
}