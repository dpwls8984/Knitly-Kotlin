package com.mysite.knitly.domain.mypage.service

import com.mysite.knitly.domain.mypage.dto.*
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository
import com.mysite.knitly.domain.product.like.entity.ProductLike
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyPageService(
    private val repo: MyPageQueryRepository,
    private val reviewRepository: ReviewRepository,
    private val productLikeRepository: ProductLikeRepository
) {

    private val log = LoggerFactory.getLogger(MyPageService::class.java)

    @Transactional(readOnly = true)
    fun getOrderCards(userId: Long, pageable: Pageable): Page<OrderCardResponse> {
        log.info(
            "[MyPageService] 주문 내역 조회 - userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )
        val page = repo.findOrderCards(userId, pageable)
        log.info(
            "[MyPageService] 주문 내역 조회 완료 - userId={}, totalElements={}",
            userId,
            page.totalElements
        )
        return page
    }

    @Transactional(readOnly = true)
    fun getMyPosts(userId: Long, query: String?, pageable: Pageable): Page<MyPostListItemResponse> {
        log.info(
            "[MyPageService] 내 글 조회 - userId={}, query='{}', page={}, size={}",
            userId,
            query,
            pageable.pageNumber,
            pageable.pageSize
        )
        val page = repo.findMyPosts(userId, query, pageable)
        log.info(
            "[MyPageService] 내 글 조회 완료 - userId={}, totalElements={}",
            userId,
            page.totalElements
        )
        return page
    }

    @Transactional(readOnly = true)
    fun getMyComments(userId: Long, query: String?, pageable: Pageable): Page<MyCommentListItem> {
        log.info(
            "[MyPageService] 내 댓글 조회 - userId={}, query='{}', page={}, size={}",
            userId,
            query,
            pageable.pageNumber,
            pageable.pageSize
        )
        val page = repo.findMyComments(userId, query, pageable)
        log.info(
            "[MyPageService] 내 댓글 조회 완료 - userId={}, totalElements={}",
            userId,
            page.totalElements
        )
        return page
    }

    @Transactional(readOnly = true)
    fun getMyFavorites(userId: Long, pageable: Pageable): Page<FavoriteProductItem> {
        log.info(
            "[MyPageService] 찜 목록 조회 - userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )
        val page: Page<FavoriteProductItem> = productLikeRepository
            .findByUser_UserId(userId, pageable)
            .map { convertToDto(it) }
        log.info(
            "[MyPageService] 찜 목록 조회 완료 - userId={}, totalElements={}",
            userId,
            page.totalElements
        )
        return page
    }

    private fun convertToDto(pl: ProductLike): FavoriteProductItem {
        val p = pl.product

        val thumbnailUrl = if (p.productImages.isEmpty()) {
            null
        } else {
            p.productImages[0].productImageUrl
        }

        return FavoriteProductItem(
            productId = p.productId!!,
            productTitle = p.title,
            sellerName = p.user.name,
            thumbnailUrl = thumbnailUrl
        )
    }

    @Transactional(readOnly = true)
    fun getMyReviewsV2(userId: Long, pageable: Pageable): Page<ReviewListItem> {
        log.info(
            "[MyPageService] 내 리뷰 조회 - userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val reviews: List<Review> = reviewRepository.findByUser_UserIdAndIsDeletedFalse(userId, pageable)

        val list: List<ReviewListItem> = reviews.map { r ->
            val product = r.product!!

            val productThumbnail = if (product.productImages.isEmpty()) {
                null
            } else {
                product.productImages[0].productImageUrl
            }

            val reviewImages: List<String> = r.reviewImages
                .mapNotNull { ri -> ri.reviewImageUrl }

            ReviewListItem(
                reviewId = r.reviewId,
                productId = product.productId!!,
                productTitle = product.title,
                productThumbnailUrl = productThumbnail,
                rating = r.rating,
                content = r.content,
                reviewImageUrls = reviewImages,
                createdDate = r.createdAt.toLocalDate()
            )
        }

        val total: Long = reviewRepository.countByUser_UserIdAndIsDeletedFalse(userId)
        log.info(
            "[MyPageService] 내 리뷰 조회 완료 - userId={}, totalElements={}",
            userId,
            total
        )

        return PageImpl(list, pageable, total)
    }
}
