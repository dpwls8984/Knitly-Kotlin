package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.community.post.repository.UserRepositoryTmp;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.UserProvider;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepositoryTmp userRepository; // 유지

    @Autowired
    private PostRepository postRepository;

    private UUID authorId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        // author
        User author = User.builder()
                .socialId("social-author")
                .name("Author")
                .provider(UserProvider.GOOGLE)
                .build();
        author = userRepository.save(author);
        authorId = author.getUserId();

        // other user
        User other = User.builder()
                .socialId("social-other")
                .name("Other")
                .provider(UserProvider.KAKAO)
                .build();
        other = userRepository.save(other);
        otherUserId = other.getUserId();
    }

    @Test
    void create_success() {
        PostCreateRequest req = PostCreateRequest.builder()
                .category(PostCategory.FREE)
                .title("첫 글")
                .content("내용")
                .imageUrl("https://example.com/a.jpg")
                .authorId(authorId)
                .build();

        PostResponse res = postService.create(req);

        assertThat(res.getId()).isNotNull();
        assertThat(res.getTitle()).isEqualTo("첫 글");
        assertThat(res.getAuthorId()).isEqualTo(authorId);
        assertThat(res.isMine()).isTrue();
    }

    @Test
    void create_invalid_image_extension_throws() {
        PostCreateRequest req = PostCreateRequest.builder()
                .category(PostCategory.FREE)
                .title("이미지 확장자 실패")
                .content("내용")
                .imageUrl("http://x/evil.gif") // png/jpg/jpeg만 허용
                .authorId(authorId)
                .build();

        assertThatThrownBy(() -> postService.create(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_IMAGE_EXTENSION_INVALID.getMessage());
    }

    @Test
    void getPost_not_found_throws() {
        assertThatThrownBy(() -> postService.getPost(99999L, authorId))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void update_forbidden_when_not_author() {
        // given
        PostCreateRequest req = PostCreateRequest.builder()
                .category(PostCategory.TIP)
                .title("원본")
                .content("내용")
                .imageUrl("https://example.com/tip.jpg")
                .authorId(authorId)
                .build();
        PostResponse created = postService.create(req);

        PostUpdateRequest update = PostUpdateRequest.builder()
                .category(PostCategory.TIP)
                .title("수정제목")
                .content("수정내용")
                .imageUrl("https://example.com/new.jpg")
                .build();

        // when + then
        assertThatThrownBy(() -> postService.update(created.getId(), update, otherUserId))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_UPDATE_FORBIDDEN.getMessage());
    }

    @Test
    void delete_forbidden_when_not_author() {
        // given
        PostCreateRequest req = PostCreateRequest.builder()
                .category(PostCategory.QUESTION)
                .title("질문")
                .content("질문내용")
                .imageUrl("https://example.com/q.jpg")
                .authorId(authorId)
                .build();
        PostResponse created = postService.create(req);

        // when + then
        assertThatThrownBy(() -> postService.delete(created.getId(), otherUserId))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_DELETE_FORBIDDEN.getMessage());
    }

    @Test
    void list_paging_and_filter() {
        // given: FREE 2건, TIP 1건
        for (int i = 0; i < 2; i++) {
            postService.create(PostCreateRequest.builder()
                    .category(PostCategory.FREE)
                    .title("free-" + i)
                    .content("c")
                    .imageUrl("https://example.com/i.jpg")
                    .authorId(authorId)
                    .build());
        }
        postService.create(PostCreateRequest.builder()
                .category(PostCategory.TIP)
                .title("tip")
                .content("c")
                .imageUrl("https://example.com/i.jpg")
                .authorId(authorId)
                .build());

        // when
        var page0 = postService.getPostList(PostCategory.FREE, 0, 10);
        var all = postService.getPostList(null, 0, 10);

        // then
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(all.getTotalElements()).isEqualTo(3);
    }
}
