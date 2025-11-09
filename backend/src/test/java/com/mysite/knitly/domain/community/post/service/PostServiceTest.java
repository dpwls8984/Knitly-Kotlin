// ========== path: src/test/java/com/mysite/knitly/domain/community/post/service/PostServiceTest.java ==========
package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired private PostService postService;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;

    private User author;
    private User other;

    @BeforeEach
    void setUp() {
        author = userRepository.save(
                User.builder()
                        .socialId("social-author")
                        .email("author@test.com")
                        .name("Author")
                        .provider(Provider.GOOGLE)
                        .build()
        );
        other = userRepository.save(
                User.builder()
                        .socialId("social-other")
                        .email("other@test.com")
                        .name("Other")
                        .provider(Provider.KAKAO)
                        .build()
        );
    }

    @Test
    @DisplayName("게시글 생성 성공")
    void create_success() {
        // given
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "첫 글", "내용",
                List.of("https://example.com/a.jpg")
        );

        // when
        PostResponse res = postService.create(req, author);

        // then
        assertThat(res.id()).isNotNull();
        assertThat(res.title()).isEqualTo("첫 글");
        assertThat(res.authorId()).isEqualTo(author.getUserId());
        assertThat(res.mine()).isTrue();
        assertThat(res.imageUrls()).containsExactly("https://example.com/a.jpg");
    }

    @Test
    @DisplayName("이미지 확장자 제한 위반 시 ServiceException")
    void create_invalid_image_extension_throws() {
        // given: png/jpg/jpeg 외 확장자 (예: gif) 금지라고 가정
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "이미지 확장자 실패", "내용",
                List.of("http://x/evil.gif")
        );

        // expect
        assertThatThrownBy(() -> postService.create(req, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_IMAGE_EXTENSION_INVALID.getMessage());
    }

    @Test
    @DisplayName("제목 100자 초과 시 ServiceException")
    void create_title_too_long_throws() {
        String longTitle = "a".repeat(101);
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, longTitle, "내용",
                List.of("https://example.com/a.jpg")
        );

        assertThatThrownBy(() -> postService.create(req, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_TITLE_LENGTH_INVALID.getMessage());
    }

    @Test
    @DisplayName("이미지 6개 이상 시 ServiceException")
    void create_too_many_images_throws() {
        List<String> six = List.of(
                "https://example.com/1.jpg",
                "https://example.com/2.jpg",
                "https://example.com/3.jpg",
                "https://example.com/4.jpg",
                "https://example.com/5.jpg",
                "https://example.com/6.jpg"
        );
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "이미지 과다", "내용", six
        );

        assertThatThrownBy(() -> postService.create(req, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_IMAGE_COUNT_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("로그인 없이 생성 시 ServiceException(UNAUTHORIZED)")
    void create_unauthorized_throws() {
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "비로그인 생성", "내용",
                List.of("https://example.com/a.jpg")
        );

        assertThatThrownBy(() -> postService.create(req, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("단건 조회 – 존재하지 않으면 404(ServiceException)")
    void getPost_not_found_throws() {
        assertThatThrownBy(() -> postService.getPost(99999L, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 – 작성자 아님 → FORBIDDEN")
    void update_forbidden_when_not_author() {
        // given
        PostResponse created = postService.create(
                new PostCreateRequest(PostCategory.TIP, "원본", "내용",
                        List.of("https://example.com/tip.jpg")),
                author
        );

        PostUpdateRequest update = new PostUpdateRequest(
                PostCategory.TIP, "수정제목", "수정내용",
                List.of("https://example.com/new.jpg")
        );

        // expect
        assertThatThrownBy(() -> postService.update(created.id(), update, other))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_UPDATE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("게시글 삭제 – 작성자 아님 → FORBIDDEN")
    void delete_forbidden_when_not_author() {
        // given
        PostResponse created = postService.create(
                new PostCreateRequest(PostCategory.QUESTION, "질문", "질문내용",
                        List.of("https://example.com/q.jpg")),
                author
        );

        // expect
        assertThatThrownBy(() -> postService.delete(created.id(), other))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_DELETE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("목록 – 카테고리/페이지 필터 동작")
    void list_paging_and_filter() {
        // given: FREE 2건, TIP 1건
        for (int i = 0; i < 2; i++) {
            postService.create(new PostCreateRequest(
                    PostCategory.FREE, "free-" + i, "c",
                    List.of("https://example.com/i.jpg")
            ), author);
        }
        postService.create(new PostCreateRequest(
                PostCategory.TIP, "tip", "c",
                List.of("https://example.com/i.jpg")
        ), author);

        // when
        var freePage = postService.getPostList(PostCategory.FREE, null, 0, 10);
        var allPage = postService.getPostList(null, null, 0, 10);

        // then
        assertThat(freePage.getTotalElements()).isEqualTo(2);
        assertThat(allPage.getTotalElements()).isEqualTo(3);
    }
}
