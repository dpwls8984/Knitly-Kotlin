package com.mysite.knitly.domain.product.review.service

import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderItemRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductImage
import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.product.review.entity.ReviewImage
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.spy
import org.mockito.kotlin.*
import org.springframework.data.domain.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDateTime
import java.util.Optional

internal class ReviewServiceTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var orderItemRepository: OrderItemRepository
    private lateinit var localFileStorage: LocalFileStorage

    private lateinit var reviewService: ReviewService

    private lateinit var user: User
    private lateinit var product: Product
    private lateinit var orderItem: OrderItem

    private val orderItemId = 1L
    private val userId = 3L
    private val productId = 10L

    @BeforeEach
    fun setUp() {
        reviewRepository = mock()
        orderItemRepository = mock()
        localFileStorage = mock()

        reviewService = ReviewService(reviewRepository, localFileStorage, orderItemRepository)

        user = mock {
            on { this.userId } doReturn userId
            on { this.name } doReturn "테스트유저"
        }
        product = mock {
            on { this.productId } doReturn productId
            on { this.title } doReturn "테스트상품"
        }
        orderItem = mock {
            on { this.orderItemId } doReturn orderItemId
            on { this.product } doReturn product
        }
    }

    @Test
    @DisplayName("리뷰 폼 조회: 정상")
    fun getReviewFormInfo_Success() {
        val thumbnail: ProductImage = mock {
            on { productImageUrl } doReturn "http://test.com/thumbnail.jpg"
        }
        whenever(product.productImages).thenReturn(mutableListOf(thumbnail))

        whenever(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem))

        val response = reviewService.getReviewFormInfo(orderItemId)

        assertThat(response).isNotNull()
        assertThat(response.productTitle).isEqualTo("테스트상품")
        assertThat(response.productThumbnailUrl).isEqualTo("http://test.com/thumbnail.jpg")
    }

    @Test
    @DisplayName("리뷰 폼 조회: OrderItem 없음")
    fun getReviewFormInfo_OrderItemNotFound_ShouldThrowException() {
        whenever(orderItemRepository.findById(orderItemId))
            .thenReturn(Optional.empty())

        val ex = assertThrows<ServiceException> {
            reviewService.getReviewFormInfo(orderItemId)
        }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND)
    }

    @Test
    @DisplayName("리뷰 등록: 정상 (이미지 없음)")
    fun createReview_ValidInputNoImages_ShouldSaveReview() {
        val request = ReviewCreateRequest(5, "좋아요", emptyList())

        whenever(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem))

        val reviewCaptor = argumentCaptor<Review>()

        reviewService.createReview(orderItemId, user, request)

        verify(reviewRepository).save(reviewCaptor.capture())
        val savedReview = reviewCaptor.firstValue

        assertThat(savedReview.user).isEqualTo(user)
        assertThat(savedReview.product).isEqualTo(product)
        assertThat(savedReview.orderItem).isEqualTo(orderItem)
        assertThat(savedReview.rating).isEqualTo(5)
        assertThat(savedReview.content).isEqualTo("좋아요")
        assertThat(savedReview.reviewImages).isEmpty()

        verify(localFileStorage, never()).saveReviewImage(any())
    }

    @Test
    @DisplayName("리뷰 등록: 정상 (이미지 포함)")
    fun createReview_ValidInputWithImages_ShouldSaveReviewAndImages() {
        val mockFile = MockMultipartFile("file", "image.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
        val images = listOf(mockFile)
        val request = ReviewCreateRequest(5, "이미지 리뷰", images)

        val savedImageUrl = "/static/review/saved_image.jpg"

        whenever(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem))
        whenever(localFileStorage.saveReviewImage(mockFile)).thenReturn(savedImageUrl)

        val reviewCaptor = argumentCaptor<Review>()

        reviewService.createReview(orderItemId, user, request)

        verify(reviewRepository).save(reviewCaptor.capture())
        val savedReview = reviewCaptor.firstValue

        assertThat(savedReview.content).isEqualTo("이미지 리뷰")

        verify(localFileStorage).saveReviewImage(mockFile)

        assertThat(savedReview.reviewImages).hasSize(1)
        assertThat(savedReview.reviewImages.first().reviewImageUrl).isEqualTo(savedImageUrl)
        assertThat(savedReview.reviewImages.first().review).isEqualTo(savedReview)
    }

    @Test
    @DisplayName("리뷰 삭제: 정상 (소유자)")
    fun deleteReview_ValidUser_ShouldSetDeleted() {
        val reviewId = 1L
        val review = spy(
            Review(rating = 5, content = "test", orderItem = orderItem, product = product, user = user)
        )
        whenever(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review))

        reviewService.deleteReview(reviewId, user)
        assertThat(review.isDeleted).isTrue()
    }

    @Test
    @DisplayName("리뷰 삭제: 권한 없는 유저가 요청시 실패")
    fun deleteReview_NotOwner_ShouldThrowException() {
        val reviewId = 1L
        val owner = mock<User> { on { userId } doReturn 99L }
        val requester = user

        val review = Review(rating = 5, content = "test", orderItem = orderItem, product = product, user = owner)
        whenever(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review))

        val ex = assertThrows<ServiceException> {
            reviewService.deleteReview(reviewId, requester)
        }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.REVIEW_NOT_AUTHORIZED)
    }

    @Test
    @DisplayName("리뷰 목록 조회: 정상 (이미지 포함)")
    fun getReviewsByProduct_ShouldReturnPageOfReviewsWithImages() {
        val page = 0
        val size = 10
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val image1 = mock<ReviewImage> { on { reviewImageUrl } doReturn "/static/review/img1.jpg" }
        val image2 = mock<ReviewImage> { on { reviewImageUrl } doReturn "/static/review/img2.png" }
        val review1 = mock<Review> {
            on { reviewId } doReturn 1L
            on { content } doReturn "좋아요"
            on { rating } doReturn 5
            on { user } doReturn user
            on { createdAt } doReturn LocalDateTime.now()
            on { reviewImages } doReturn mutableListOf(image1, image2)
        }
        val review2 = mock<Review> {
            on { reviewId } doReturn 2L
            on { content } doReturn "괜찮아요"
            on { rating } doReturn 4
            on { user } doReturn user
            on { createdAt } doReturn LocalDateTime.now()
            on { reviewImages } doReturn mutableListOf()
        }

        val reviewList = listOf(review1, review2)
        val reviewPage: Page<Review> = PageImpl(reviewList, pageable, reviewList.size.toLong())

        whenever(
            reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(
                eq(productId),
                any<Pageable>()
            )
        ).thenReturn(reviewPage)

        val resultPage = reviewService.getReviewsByProduct(productId, page, size)

        assertThat(resultPage).isNotNull()
        assertThat(resultPage.totalElements).isEqualTo(2)
        assertThat(resultPage.content).hasSize(2)

        val r1 = resultPage.content[0]
        assertThat(r1.reviewId).isEqualTo(1L)
        assertThat(r1.content).isEqualTo("좋아요")
        assertThat(r1.reviewImageUrls)
            .containsExactly("/static/review/img1.jpg", "/static/review/img2.png")

        val r2 = resultPage.content[1]
        assertThat(r2.reviewId).isEqualTo(2L)
        assertThat(r2.content).isEqualTo("괜찮아요")
        assertThat(r2.reviewImageUrls).isEmpty()
    }

    @Test
    @DisplayName("리뷰 목록 조회: 리뷰 없음 (빈 페이지)")
    fun getReviewsByProduct_NoReviews_ShouldReturnEmptyPage() {
        val page = 0
        val size = 10
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val emptyPage: Page<Review> = Page.empty(pageable)

        whenever(
            reviewRepository.findByProduct_ProductIdAndIsDeletedFalse(
                eq(productId),
                any<Pageable>()
            )
        ).thenReturn(emptyPage)

        val resultPage = reviewService.getReviewsByProduct(productId, page, size)

        assertThat(resultPage).isNotNull()
        assertThat(resultPage).isNotNull()
        assertThat(resultPage.totalElements).isEqualTo(0)
        assertThat(resultPage.content).isEmpty()
    }
}