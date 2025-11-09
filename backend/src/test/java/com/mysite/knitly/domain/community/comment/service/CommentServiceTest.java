// ========== path: src/test/java/com/mysite/knitly/domain/community/comment/service/CommentServiceTest.java ==========
package com.mysite.knitly.domain.community.comment.service;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.repository.CommentRepository;
import com.mysite.knitly.domain.community.post.entity.Post;
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
class CommentServiceTest {

    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;

    private User author1; // 게시글 작성자
    private User author2; // 다른 유저
    private Long postId;

    @BeforeEach
    void setUp() {
        // 유저
        author1 = userRepository.save(User.builder()
                .socialId("s1")
                .email("u1@test.com")
                .name("U1")
                .provider(Provider.GOOGLE)
                .build());
        author2 = userRepository.save(User.builder()
                .socialId("s2")
                .email("u2@test.com")
                .name("U2")
                .provider(Provider.KAKAO)
                .build());

        // 게시글 (author1이 작성)
        Post post = Post.builder()
                .category(PostCategory.FREE)
                .title("댓글 테스트용 글")
                .content("본문")
                .imageUrls(List.of("https://ex.com/a.jpg"))
                .author(author1)
                .build();
        postId = postRepository.save(post).getId();
    }

    @Test
    @DisplayName("댓글 생성 성공 - 게시글 작성자는 익명 번호 제외")
    void create_success() {
        CommentCreateRequest req = new CommentCreateRequest(postId, null, "첫 댓글!");
        CommentResponse res = commentService.create(req, author1);

        assertThat(res.id()).isNotNull();
        assertThat(res.content()).isEqualTo("첫 댓글!");
        assertThat(res.authorId()).isEqualTo(author1.getUserId());
        // 게시글 작성자는 번호 제외 → "익명의 털실"
        assertThat(res.authorDisplay()).isEqualTo("익명의 털실");
        assertThat(res.mine()).isTrue();
    }

    @Test
    @DisplayName("댓글 수정 - 작성자 아님 → FORBIDDEN")
    void update_forbidden_when_not_author() {
        CommentResponse created = commentService.create(new CommentCreateRequest(postId, null, "원본"), author1);
        assertThatThrownBy(() ->
                commentService.update(created.id(), new CommentUpdateRequest("수정"), author2)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.COMMENT_UPDATE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자 아님 → FORBIDDEN")
    void delete_forbidden_when_not_author() {
        CommentResponse created = commentService.create(new CommentCreateRequest(postId, null, "삭제대상"), author1);
        assertThatThrownBy(() ->
                commentService.delete(created.id(), author2)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.COMMENT_DELETE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("목록/정렬/개수/익명 번호 매핑 검증")
    void list_sorting_and_count_and_anonymous_numbering() throws Exception {
        // 루트 댓글 3개
        commentService.create(new CommentCreateRequest(postId, null, "c1"), author1);
        Thread.sleep(5);
        commentService.create(new CommentCreateRequest(postId, null, "c2"), author2);
        Thread.sleep(5);
        commentService.create(new CommentCreateRequest(postId, null, "c3"), author1);

        // 등록순 (루트 페이징)
        var asc = commentService.getComments(postId, "asc", 0, 10, author1);
        assertThat(asc.getTotalElements()).isEqualTo(3);
        assertThat(asc.getContent().get(0).content()).isEqualTo("c1");
        assertThat(asc.getContent().get(2).content()).isEqualTo("c3");

        // 최신순
        var desc = commentService.getComments(postId, "desc", 0, 10, author1);
        assertThat(desc.getContent().get(0).content()).isEqualTo("c3");
        assertThat(desc.getContent().get(2).content()).isEqualTo("c1");

        // 개수
        long cnt = commentService.count(postId);
        assertThat(cnt).isEqualTo(3);

        // 익명 번호 매핑: 게시글 작성자(author1)는 번호 없음,
        // 다른 사용자(author2)는 첫 등장이라 "익명의 털실 1"
        assertThat(asc.getContent().get(0).authorDisplay()).isEqualTo("익명의 털실");      // author1
        assertThat(asc.getContent().get(1).authorDisplay()).isEqualTo("익명의 털실 1");   // author2
        assertThat(asc.getContent().get(2).authorDisplay()).isEqualTo("익명의 털실");      // author1
    }

    @Test
    @DisplayName("대댓글 트리 구조 검증")
    void reply_tree_is_returned_under_root() {
        // 루트 댓글
        var root = commentService.create(new CommentCreateRequest(postId, null, "root"), author1);

        // 대댓글 2개
        commentService.create(new CommentCreateRequest(postId, root.id(), "re-1"), author2);
        commentService.create(new CommentCreateRequest(postId, root.id(), "re-2"), author1);

        var page = commentService.getComments(postId, "asc", 0, 10, author1);
        assertThat(page.getTotalElements()).isEqualTo(1);

        CommentTreeResponse first = page.getContent().get(0);
        assertThat(first.content()).isEqualTo("root");
        assertThat(first.children()).hasSize(2);
        assertThat(first.children().get(0).content()).isEqualTo("re-1");
        assertThat(first.children().get(1).content()).isEqualTo("re-2");
        assertThat(first.children().get(0).parentId()).isEqualTo(root.id());
    }

    @Test
    @DisplayName("다른 게시글의 parentId로 대댓글 시도 → BAD_REQUEST")
    void reply_with_parent_from_other_post_throws_bad_request() {
        // 다른 게시글
        Post otherPost = Post.builder()
                .category(PostCategory.FREE)
                .title("다른 글")
                .content("x")
                .imageUrls(List.of())
                .author(author1)
                .build();
        Long otherPostId = postRepository.save(otherPost).getId();

        // 다른 글의 루트 댓글
        var otherRoot = commentService.create(new CommentCreateRequest(otherPostId, null, "other-root"), author1);

        // 현재 글에 '다른 글의 부모'로 대댓글 시도 -> BAD_REQUEST
        assertThatThrownBy(() ->
                commentService.create(new CommentCreateRequest(postId, otherRoot.id(), "invalid"), author2)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 목록 요청 → POST_NOT_FOUND")
    void get_comments_on_missing_post_throws() {
        assertThatThrownBy(() -> commentService.getComments(999_999L, "asc", 0, 10, author1))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.POST_NOT_FOUND.getMessage());
    }
}
