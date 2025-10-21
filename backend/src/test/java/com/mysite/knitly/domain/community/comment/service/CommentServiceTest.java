package com.mysite.knitly.domain.community.comment.service;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.repository.CommentRepository;
import com.mysite.knitly.domain.community.post.entity.Post;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Long author1Id;
    private Long author2Id;
    private Long postId;

    @BeforeEach
    void setUp() {
        // 유저
        User u1 = User.builder()
                .socialId("s1")
                .email("u1@test.com")
                .name("U1")
                .provider(Provider.GOOGLE)
                .build();
        User u2 = User.builder()
                .socialId("s2")
                .email("u2@test.com")
                .name("U2")
                .provider(Provider.KAKAO)
                .build();
        author1Id = userRepository.save(u1).getUserId();
        author2Id = userRepository.save(u2).getUserId();

        // 게시글
        Post post = Post.builder()
                .category(PostCategory.FREE)
                .title("댓글 테스트용 글")
                .content("본문")
                .imageUrls(List.of("https://ex.com/a.jpg"))
                .author(u1)
                .build();
        postId = postRepository.save(post).getId();
    }

    @Test
    void create_success() {
        CommentCreateRequest req = new CommentCreateRequest(postId, null, "첫 댓글!");
        CommentResponse res = commentService.create(req, author1Id);

        assertThat(res.id()).isNotNull();
        assertThat(res.content()).isEqualTo("첫 댓글!");
        assertThat(res.authorId()).isEqualTo(author1Id);
        assertThat(res.authorDisplay()).isEqualTo("익명의 털실 1"); // 첫 등장 사용자 = 1번
        assertThat(res.mine()).isTrue();
    }

    @Test
    void update_forbidden_when_not_author() {
        CommentResponse created = commentService.create(new CommentCreateRequest(postId, null, "원본"), author1Id);
        assertThatThrownBy(() ->
                commentService.update(created.id(), new CommentUpdateRequest("수정"), author2Id)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.COMMENT_UPDATE_FORBIDDEN.getMessage());
    }

    @Test
    void delete_forbidden_when_not_author() {
        CommentResponse created = commentService.create(new CommentCreateRequest(postId, null, "삭제대상"), author1Id);
        assertThatThrownBy(() ->
                commentService.delete(created.id(), author2Id)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.COMMENT_DELETE_FORBIDDEN.getMessage());
    }

    @Test
    void list_sorting_and_count_and_anonymous_numbering() throws Exception {
        // 루트 댓글 3개로
        commentService.create(new CommentCreateRequest(postId, null, "c1"), author1Id);
        Thread.sleep(5);
        commentService.create(new CommentCreateRequest(postId, null, "c2"), author2Id);
        Thread.sleep(5);
        commentService.create(new CommentCreateRequest(postId, null, "c3"), author1Id);

        // 등록순 (루트 페이징)
        var asc = commentService.getComments(postId, "asc", 0, 10, author1Id);
        assertThat(asc.getTotalElements()).isEqualTo(3);
        assertThat(asc.getContent().get(0).content()).isEqualTo("c1");
        assertThat(asc.getContent().get(2).content()).isEqualTo("c3");

        // 최신순
        var desc = commentService.getComments(postId, "desc", 0, 10, author1Id);
        assertThat(desc.getContent().get(0).content()).isEqualTo("c3");
        assertThat(desc.getContent().get(2).content()).isEqualTo("c1");

        // 개수
        long cnt = commentService.count(postId);
        assertThat(cnt).isEqualTo(3);

        // 익명 번호 매핑
        assertThat(asc.getContent().get(0).authorDisplay()).isEqualTo("익명의 털실 1");
        assertThat(asc.getContent().get(1).authorDisplay()).isEqualTo("익명의 털실 2");
        assertThat(asc.getContent().get(2).authorDisplay()).isEqualTo("익명의 털실 1");
    }

    @Test
    void reply_tree_is_returned_under_root() {
        // 루트 댓글
        var root = commentService.create(new CommentCreateRequest(postId, null, "root"), author1Id);

        // 대댓글 2개로
        commentService.create(new CommentCreateRequest(postId, root.id(), "re-1"), author2Id);
        commentService.create(new CommentCreateRequest(postId, root.id(), "re-2"), author1Id);

        var page = commentService.getComments(postId, "asc", 0, 10, author1Id);
        assertThat(page.getTotalElements()).isEqualTo(1);

        CommentTreeResponse first = page.getContent().get(0);
        assertThat(first.content()).isEqualTo("root");
        assertThat(first.children()).hasSize(2);
        assertThat(first.children().get(0).content()).isEqualTo("re-1");
        assertThat(first.children().get(1).content()).isEqualTo("re-2");
        assertThat(first.children().get(0).parentId()).isEqualTo(root.id());
    }

    @Test
    void reply_with_parent_from_other_post_throws_bad_request() {
        // 다른 게시글일 경우
        User owner = userRepository.findById(author1Id).orElseThrow();
        Post otherPost = Post.builder()
                .category(PostCategory.FREE)
                .title("다른 글")
                .content("x")
                .imageUrls(List.of())
                .author(owner)
                .build();
        Long otherPostId = postRepository.save(otherPost).getId();

        // 다른 글의 루트 댓글
        var otherRoot = commentService.create(new CommentCreateRequest(otherPostId, null, "other-root"), author1Id);

        // 현재 글에 '다른 글의 부모'로 대댓글 시도 -> BAD_REQUEST
        assertThatThrownBy(() ->
                commentService.create(new CommentCreateRequest(postId, otherRoot.id(), "invalid"), author2Id)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    void get_comments_on_missing_post_throws() {
        assertThatThrownBy(() -> commentService.getComments(999_999L, "asc", 0, 10, author1Id))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_NOT_FOUND.getMessage());
    }
}

