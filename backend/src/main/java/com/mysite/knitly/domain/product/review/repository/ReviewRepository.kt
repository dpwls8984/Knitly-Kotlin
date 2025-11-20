package com.mysite.knitly.domain.product.review.repository

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.review.entity.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {

    fun findByProduct_ProductIdAndIsDeletedFalse(productId: Long, pageable: Pageable): Page<Review>

    fun findByUser_UserIdAndIsDeletedFalse(userId: Long, pageable: Pageable): List<Review>

    fun countByUser_UserIdAndIsDeletedFalse(userId: Long): Long

    fun countByProductAndIsDeletedFalse(product: Product): Long
}