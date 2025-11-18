package com.mysite.knitly.domain.mypage.service

import com.mysite.knitly.domain.mypage.dto.MyCommentListItem
import com.mysite.knitly.domain.mypage.dto.MyPostListItemResponse
import com.mysite.knitly.domain.mypage.dto.OrderCardResponse
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository
import com.mysite.knitly.domain.product.like.entity.ProductLike
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductImage
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.product.review.entity.ReviewImage
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MyPageServiceTest {

    @Mock
    lateinit var repo: MyPageQueryRepository

    @Mock
    lateinit var productLikeRepository: ProductLikeRepository

    @Mock
    lateinit var reviewRepository: ReviewRepository

    @InjectMocks
    lateinit var service: MyPageService

    @Test
    @DisplayName("주문 카드 조회")
    fun getOrderCards() {
        val card = OrderCardResponse.of(1L, LocalDateTime.now(), 10000.0)
        val page = PageImpl(listOf(card), PageRequest.of(0, 3), 1)

        whenever(repo.findOrderCards(eq(10L), any()))
            .thenReturn(page)

        val result = service.getOrderCards(10L, PageRequest.of(0, 3))

        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    @DisplayName("내가 쓴 글 조회")
    fun getMyPosts() {
        val dto = MyPostListItemResponse(
            100L, "제목", "요약", "thumb.jpg",
            LocalDateTime.of(2025, 1, 1, 10, 0)
        )

        val page = PageImpl(listOf(dto), PageRequest.of(0, 10), 1)

        whenever(repo.findMyPosts(eq(10L), eq("키워드"), any()))
            .thenReturn(page)

        val result = service.getMyPosts(10L, "키워드", PageRequest.of(0, 10))

        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    @DisplayName("내가 쓴 댓글 조회")
    fun getMyComments() {
        val dto = MyCommentListItem(
            7L, 100L,
            LocalDateTime.of(2025, 1, 2, 10, 0),
            "미리보기"
        )

        val page = PageImpl(listOf(dto), PageRequest.of(0, 10), 1)

        whenever(repo.findMyComments(eq(10L), eq("단어"), any()))
            .thenReturn(page)

        val result = service.getMyComments(10L, "단어", PageRequest.of(0, 10))

        assertThat(result.content).hasSize(1)
    }

    @Test
    @DisplayName("내가 찜한 상품 조회")
    fun getMyFavorites() {
        val like = Mockito.mock(ProductLike::class.java)
        val product = Mockito.mock(Product::class.java)
        val image = Mockito.mock(ProductImage::class.java)
        val user = Mockito.mock(User::class.java)

        whenever(like.product).thenReturn(product)

        whenever(product.productId).thenReturn(9001L)
        whenever(product.title).thenReturn("인기 도안")
        whenever(product.user).thenReturn(user)

        whenever(user.name).thenReturn("홍길동")

        whenever(product.productImages).thenReturn(listOf(image))

        whenever(image.productImageUrl).thenReturn("t.jpg")

        val page = PageImpl(listOf(like), PageRequest.of(0, 10), 1)

        whenever(productLikeRepository.findByUser_UserId(eq(10L), any()))
            .thenReturn(page)

        val result = service.getMyFavorites(10L, PageRequest.of(0, 10))

        assertThat(result.content[0].productId).isEqualTo(9001L)
        assertThat(result.content[0].thumbnailUrl).isEqualTo("t.jpg")
    }

    @Test
    @DisplayName("내 리뷰 조회")
    fun getMyReviewsV2() {
        val review = Mockito.mock(Review::class.java)
        val product = Mockito.mock(Product::class.java)
        val image = Mockito.mock(ProductImage::class.java)

        whenever(review.reviewId).thenReturn(301L)
        whenever(review.content).thenReturn("좋아요")
        whenever(review.rating).thenReturn(5)
        whenever(review.createdAt).thenReturn(LocalDateTime.of(2025, 1, 4, 10, 0))
        whenever(review.reviewImages).thenReturn(mutableListOf<ReviewImage>())

        whenever(review.product).thenReturn(product)

        whenever(product.productId).thenReturn(9001L)
        whenever(product.title).thenReturn("인기 도안")
        whenever(product.productImages).thenReturn(listOf(image))

        whenever(image.productImageUrl).thenReturn("t.jpg")

        whenever(reviewRepository.findByUser_UserIdAndIsDeletedFalse(eq(10L), any()))
            .thenReturn(listOf(review))

        whenever(reviewRepository.countByUser_UserIdAndIsDeletedFalse(10L))
            .thenReturn(1L)

        val result = service.getMyReviewsV2(10L, PageRequest.of(0, 10))

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].productId).isEqualTo(9001L)
    }
}
