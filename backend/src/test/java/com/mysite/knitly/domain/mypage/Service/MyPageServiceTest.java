package com.mysite.knitly.domain.mypage.Service;

import com.mysite.knitly.domain.mypage.dto.*;
import com.mysite.knitly.domain.mypage.repository.MyPageQueryRepository;
import com.mysite.knitly.domain.mypage.service.MyPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    MyPageQueryRepository repo;

    @InjectMocks
    MyPageService service;

    @Test
    @DisplayName("주문 카드 조회 - Repository 위임 확인")
    void getOrderCards() {
        var card = OrderCardResponse.of(1L, LocalDateTime.now(), 10000.0);
        var page = new PageImpl<>(List.of(card), PageRequest.of(0, 3), 1);

        given(repo.findOrderCards(eq(10L), any())).willReturn(page);

        var result = service.getOrderCards(10L, PageRequest.of(0, 3));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).orderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("내가 쓴 글 조회 - 검색어 포함")
    void getMyPosts() {
        var dto = new MyPostListItemResponse(100L, "제목", "요약", "thumb.jpg",
                LocalDateTime.of(2025,1,1,10,0));
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(repo.findMyPosts(eq(10L), eq("키워드"), any())).willReturn(page);

        var result = service.getMyPosts(10L, "키워드", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
        assertThat(result.getContent().get(0).title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("내가 쓴 댓글 조회 - 검색어 포함")
    void getMyComments() {
        var c = new MyCommentListItem(7L, 100L, LocalDate.of(2025,1,2), "미리보기");
        var page = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);

        given(repo.findMyComments(eq(10L), eq("단어"), any())).willReturn(page);

        var result = service.getMyComments(10L, "단어", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).commentId()).isEqualTo(7L);
        assertThat(result.getContent().get(0).postId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("내가 찜한 상품 조회")
    void getMyFavorites() {
        var f = new FavoriteProductItem(9001L, "인기 도안", "t.jpg", 9900.0, 4.2, LocalDate.of(2025,1,3));
        var page = new PageImpl<>(List.of(f), PageRequest.of(0, 10), 1);

        given(repo.findMyFavoriteProducts(eq(10L), any())).willReturn(page);

        var result = service.getMyFavorites(10L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productId()).isEqualTo(9001L);
        assertThat(result.getContent().get(0).averageRating()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("내가 남긴 리뷰 조회")
    void getMyReviews() {
        var r = new ReviewListItem(301L, 9001L, "인기 도안", "t.jpg", 5, "좋아요", LocalDate.of(2025,1,4));
        var page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);

        given(repo.findMyReviews(eq(10L), any())).willReturn(page);

        var result = service.getMyReviews(10L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).rating()).isEqualTo(5);
        assertThat(result.getContent().get(0).productId()).isEqualTo(9001L);
    }
}
