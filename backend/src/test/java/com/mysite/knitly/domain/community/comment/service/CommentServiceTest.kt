package com.mysite.knitly.domain.community.comment.service

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest
import com.mysite.knitly.domain.community.comment.dto.CommentResponse
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest
import com.mysite.knitly.domain.community.comment.repository.CommentRepository
import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.community.post.entity.PostCategory
import com.mysite.knitly.domain.community.post.repository.PostRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest(

    @Autowired
    private val commentService: CommentService,

    @Autowired
    private val commentRepository: CommentRepository,

    @Autowired
    private val postRepository: PostRepository,

    @Autowired
    private val userRepository: UserRepository
) {

    private lateinit var author1: User
    private lateinit var author2: User
    private var postId: Long = 0L

    @BeforeEach
    fun setUp() {
        author1 = userRepository.save(
            User.builder()
                .socialId("s1")
                .email("u1@test.com")
                .name("U1")
                .provider(Provider.GOOGLE)
                .build()
        )

        author2 = userRepository.save(
            User.builder()
                .socialId("s2")
                .email("u2@test.com")
                .name("U2")
                .provider(Provider.KAKAO)
                .build()
        )

        val post = Post(
            id = null,
            title = "댓글 테스트용 글",
            content = "본문",
            imageUrls = mutableListOf("https://ex.com/a.jpg"),
            category = PostCategory.FREE,
            author = author1,
            deleted = false,
            comments = mutableListOf()
        )

        postId = postRepository.save(post).id!!
    }

    @Test
    @DisplayName("댓글 생성 성공 - 게시글 작성자는 익명 번호 제외")
    fun create_success() {
        val req = CommentCreateRequest(postId, null, "첫 댓글!")
        val res: CommentResponse = commentService.create(req, author1)

        assertThat(res.id).isNotNull()
        assertThat(res.content).isEqualTo("첫 댓글!")
        assertThat(res.authorId).isEqualTo(author1.userId)
        assertThat(res.authorDisplay).isEqualTo("익명의 털실")
        assertThat(res.mine).isTrue()
    }

    @Test
    @DisplayName("댓글 수정 - 작성자 아님 → FORBIDDEN")
    fun update_forbidden_when_not_author() {
        val created = commentService.create(
            CommentCreateRequest(postId, null, "원본"),
            author1
        )

        assertThatThrownBy {
            commentService.update(
                created.id,
                CommentUpdateRequest("수정"),
                author2
            )
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.COMMENT_UPDATE_FORBIDDEN.message)
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자 아님 → FORBIDDEN")
    fun delete_forbidden_when_not_author() {
        val created = commentService.create(
            CommentCreateRequest(postId, null, "삭제대상"),
            author1
        )

        assertThatThrownBy {
            commentService.delete(created.id, author2)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.COMMENT_DELETE_FORBIDDEN.message)
    }

    @Test
    @DisplayName("목록/정렬/개수/익명 번호 매핑 검증")
    @Throws(Exception::class)
    fun list_sorting_and_count_and_anonymous_numbering() {
        commentService.create(CommentCreateRequest(postId, null, "c1"), author1)
        Thread.sleep(5)
        commentService.create(CommentCreateRequest(postId, null, "c2"), author2)
        Thread.sleep(5)
        commentService.create(CommentCreateRequest(postId, null, "c3"), author1)

        val asc = commentService.getComments(postId, "asc", 0, 10, author1)
        assertThat(asc.totalElements).isEqualTo(3)
        assertThat(asc.content[0].content).isEqualTo("c1")
        assertThat(asc.content[2].content).isEqualTo("c3")

        val desc = commentService.getComments(postId, "desc", 0, 10, author1)
        assertThat(desc.content[0].content).isEqualTo("c3")
        assertThat(desc.content[2].content).isEqualTo("c1")

        val cnt = commentService.count(postId)
        assertThat(cnt).isEqualTo(3)

        assertThat(asc.content[0].authorDisplay).isEqualTo("익명의 털실")
        assertThat(asc.content[1].authorDisplay).isEqualTo("익명의 털실 1")
        assertThat(asc.content[2].authorDisplay).isEqualTo("익명의 털실")
    }

    @Test
    @DisplayName("대댓글 트리 구조 검증")
    fun reply_tree_is_returned_under_root() {
        val root: CommentResponse = commentService.create(
            CommentCreateRequest(postId, null, "root"),
            author1
        )

        commentService.create(
            CommentCreateRequest(postId, root.id, "re-1"),
            author2
        )
        commentService.create(
            CommentCreateRequest(postId, root.id, "re-2"),
            author1
        )

        val page = commentService.getComments(postId, "asc", 0, 10, author1)
        assertThat(page.totalElements).isEqualTo(1)

        val first: CommentTreeResponse = page.content[0]
        assertThat(first.content).isEqualTo("root")
        assertThat(first.children).hasSize(2)
        assertThat(first.children[0].content).isEqualTo("re-1")
        assertThat(first.children[1].content).isEqualTo("re-2")
        assertThat(first.children[0].parentId).isEqualTo(root.id)
    }

    @Test
    @DisplayName("다른 게시글의 parentId로 대댓글 시도 → BAD_REQUEST")
    fun reply_with_parent_from_other_post_throws_bad_request() {
        val otherPost = Post(
            id = null,
            title = "다른 글",
            content = "x",
            imageUrls = mutableListOf(),
            category = PostCategory.FREE,
            author = author1,
            deleted = false,
            comments = mutableListOf()
        )

        val savedOther = postRepository.save(otherPost)
        val otherPostId = savedOther.id!!

        val otherRoot: CommentResponse = commentService.create(
            CommentCreateRequest(otherPostId, null, "other-root"),
            author1
        )

        assertThatThrownBy {
            commentService.create(
                CommentCreateRequest(postId, otherRoot.id, "invalid"),
                author2
            )
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.BAD_REQUEST.message)
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 목록 요청 → POST_NOT_FOUND")
    fun get_comments_on_missing_post_throws() {
        assertThatThrownBy {
            commentService.getComments(999_999L, "asc", 0, 10, author1)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_NOT_FOUND.message)
    }
}
