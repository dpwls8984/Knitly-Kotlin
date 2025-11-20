package com.mysite.knitly.domain.product.review.service

import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.product.review.entity.ReviewImage
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.order.repository.OrderItemRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val localFileStorage: LocalFileStorage,
    private val orderItemRepository: OrderItemRepository
) {

    fun getReviewFormInfo(orderItemId: Long): ReviewCreateResponse {
        log.info { "[Review] [Form] 리뷰 작성 폼 조회 시작 - orderItemId=$orderItemId" }

        val orderItem = orderItemRepository.findByIdOrNull(orderItemId)
            ?: throw ServiceException(ErrorCode.ORDER_ITEM_NOT_FOUND)

        val product = orderItem.product ?: throw ServiceException(ErrorCode.PRODUCT_NOT_FOUND)

        val thumbnailUrl = product?.productImages?.firstOrNull()?.productImageUrl

        log.debug { "[Review] [Form] 썸네일 URL 추출 완료 - thumbnailUrl=$thumbnailUrl" }
        log.info { "[Review] [Form] 리뷰 작성 폼 조회 완료 - productTitle=${product?.title}" }

        return ReviewCreateResponse(product?.title ?: throw ServiceException(ErrorCode.PRODUCT_NOT_FOUND), thumbnailUrl)
    }

    // 1. 리뷰 등록
    @Transactional
    fun createReview(orderItemId: Long, user: User, request: ReviewCreateRequest) {
        log.info { "[Review] [Create] 리뷰 생성 시작 - orderItemId=$orderItemId, userId=${user.userId}" }

        val orderItem = orderItemRepository.findByIdOrNull(orderItemId)
            ?: throw ServiceException(ErrorCode.ORDER_ITEM_NOT_FOUND)

        val product = orderItem.product

        val review = Review(
            user = user,
            product = product,
            orderItem = orderItem,
            rating = request.rating,
            content = request.content
        )

        val reviewImages = request.reviewImageUrls
            .filterNot { it.isEmpty }
            .mapIndexed { index, file ->
                val url = localFileStorage.saveReviewImage(file)
                log.debug { "[Review] [Create] 이미지 저장 완료 - index=$index, url=$url" }

                ReviewImage(
                    reviewImageUrl = url,
                    sortOrder = index
                )
            }

        review.updateReviewImages(reviewImages)
        reviewRepository.save(review)

        log.info {
            "[Review] [Create] 리뷰 생성 완료 - reviewId=${review.reviewId}, productId=${product?.productId}"
        }
    }

    // 2. 리뷰 소프트 삭제 (본인 리뷰만)
    @Transactional
    fun deleteReview(reviewId: Long, user: User) {
        log.info { "[Review] [Delete] 리뷰 삭제 요청 - reviewId=$reviewId, userId=${user.userId}" }

        val review = reviewRepository.findByIdOrNull(reviewId)
            ?: throw ServiceException(ErrorCode.REVIEW_NOT_FOUND)

        if (review.user.userId != user.userId) {
            log.warn {
                "[Review] [Delete] 삭제 권한 없음 - reviewUserId=${review.user.userId}, requesterId=${user.userId}"
            }
            throw ServiceException(ErrorCode.REVIEW_NOT_AUTHORIZED)
        }

        review.softDelete()
        log.info { "[Review] [Delete] 리뷰 소프트 삭제 완료 - reviewId=$reviewId" }
    }

    fun getReviewsByProduct(productId: Long, page: Int, size: Int): Page<ReviewListResponse> {
        log.info { "[Review] [List] 상품 리뷰 목록 조회 시작 - productId=$productId, page=$page" }

        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val reviews: Page<Review> = reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(productId, pageable)

        val result = reviews.map { review ->
            val imageUrls = review.reviewImages.mapNotNull { it.reviewImageUrl }
            ReviewListResponse.from(review, imageUrls)
        }

        log.debug { "[Review] [List] 조회된 리뷰 수 - count=${result.totalElements}" }
        log.info { "[Review] [List] 상품 리뷰 목록 조회 완료 - productId=$productId" }

        return result
    }
}