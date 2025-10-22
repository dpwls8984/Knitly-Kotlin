package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.*;
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

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository; // 유지

    @Autowired
    private PostRepository postRepository;

    private User author;
    private User other;

    @BeforeEach
    void setUp() {
        // author
        author = User.builder()
                .socialId("social-author")
                .email("author@test.com")
                .name("Author")
                .provider(Provider.GOOGLE)
                .build();
        author = userRepository.save(author);

        // other user
        other = User.builder()
                .socialId("social-other")
                .email("other@test.com")
                .name("Other")
                .provider(Provider.KAKAO)
                .build();
        other = userRepository.save(other);
    }

    @Test
    void create_success() {
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "첫 글", "내용",
                List.of("https://example.com/a.jpg")
        );

        PostResponse res = postService.create(req, author);

        assertThat(res.id()).isNotNull();
        assertThat(res.title()).isEqualTo("첫 글");
        assertThat(res.authorId()).isEqualTo(author.getUserId());
        assertThat(res.mine()).isTrue();
        assertThat(res.imageUrls()).hasSize(1).containsExactly("https://example.com/a.jpg");
    }

    @Test
    void create_invalid_image_extension_throws() {
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.FREE, "이미지 확장자 실패", "내용",
                List.of("http://x/evil.gif")
        );

        assertThatThrownBy(() -> postService.create(req, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_IMAGE_EXTENSION_INVALID.getMessage());
    }

    @Test
    void getPost_not_found_throws() {
        assertThatThrownBy(() -> postService.getPost(99999L, author))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void update_forbidden_when_not_author() {
        // given
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.TIP, "원본", "내용",
                List.of("https://example.com/tip.jpg")
        );
        PostResponse created = postService.create(req, author);

        PostUpdateRequest update = new PostUpdateRequest(
                PostCategory.TIP, "수정제목", "수정내용",
                List.of("https://example.com/new.jpg")
        );
        // when + then
        assertThatThrownBy(() -> postService.update(created.id(), update, other))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_UPDATE_FORBIDDEN.getMessage());
    }

    @Test
    void delete_forbidden_when_not_author() {
        // given
        PostCreateRequest req = new PostCreateRequest(
                PostCategory.QUESTION, "질문", "질문내용",
                List.of("https://example.com/q.jpg")
        );

        PostResponse created = postService.create(req, author);

        // when + then
        assertThatThrownBy(() -> postService.delete(created.id(), other))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_DELETE_FORBIDDEN.getMessage());
    }

    @Test
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
        var page0 = postService.getPostList(PostCategory.FREE, null, 0, 10);
        var all = postService.getPostList(null, null, 0, 10);
        // then
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(all.getTotalElements()).isEqualTo(3);

    }
}