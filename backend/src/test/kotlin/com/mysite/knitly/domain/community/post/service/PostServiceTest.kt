package com.mysite.knitly.domain.community.post.service

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest
import com.mysite.knitly.domain.community.post.dto.PostResponse
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest
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
class PostServiceTest {

    @Autowired
    lateinit var postService: PostService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var postRepository: PostRepository

    private lateinit var author: User
    private lateinit var other: User

    @BeforeEach
    fun setUp() {
        author = userRepository.save(
            User.builder()
                .socialId("social-author")
                .email("author@test.com")
                .name("Author")
                .provider(Provider.GOOGLE)
                .build()
        )
        other = userRepository.save(
            User.builder()
                .socialId("social-other")
                .email("other@test.com")
                .name("Other")
                .provider(Provider.KAKAO)
                .build()
        )
    }

    @Test
    @DisplayName("게시글 생성 성공")
    fun create_success() {
        // given
        val req = PostCreateRequest(
            PostCategory.FREE, "첫 글", "내용",
            listOf("https://example.com/a.jpg")
        )

        // when
        val res: PostResponse = postService.create(req, author)

        // then
        assertThat(res.id).isNotNull()
        assertThat(res.title).isEqualTo("첫 글")
        assertThat(res.authorId).isEqualTo(author.userId)
        assertThat(res.mine).isTrue()
        assertThat(res.imageUrls).containsExactly("https://example.com/a.jpg")
    }

    @Test
    @DisplayName("이미지 확장자 제한 위반 시 ServiceException")
    fun create_invalid_image_extension_throws() {
        // given: png/jpg/jpeg 외 확장자 (예: gif) 금지라고 가정
        val req = PostCreateRequest(
            PostCategory.FREE, "이미지 확장자 실패", "내용",
            listOf("http://x/evil.gif")
        )

        // expect
        assertThatThrownBy { postService.create(req, author) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_IMAGE_EXTENSION_INVALID.message)
    }

    @Test
    @DisplayName("제목 100자 초과 시 ServiceException")
    fun create_title_too_long_throws() {
        val longTitle = "a".repeat(101)
        val req = PostCreateRequest(
            PostCategory.FREE, longTitle, "내용",
            listOf("https://example.com/a.jpg")
        )

        assertThatThrownBy { postService.create(req, author) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_TITLE_LENGTH_INVALID.message)
    }

    @Test
    @DisplayName("이미지 6개 이상 시 ServiceException")
    fun create_too_many_images_throws() {
        val six = listOf(
            "https://example.com/1.jpg",
            "https://example.com/2.jpg",
            "https://example.com/3.jpg",
            "https://example.com/4.jpg",
            "https://example.com/5.jpg",
            "https://example.com/6.jpg"
        )
        val req = PostCreateRequest(
            PostCategory.FREE, "이미지 과다", "내용", six
        )

        assertThatThrownBy { postService.create(req, author) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_IMAGE_COUNT_EXCEEDED.message)
    }

    @Test
    @DisplayName("로그인 없이 생성 시 ServiceException(UNAUTHORIZED)")
    fun create_unauthorized_throws() {
        val req = PostCreateRequest(
            PostCategory.FREE, "비로그인 생성", "내용",
            listOf("https://example.com/a.jpg")
        )

        assertThatThrownBy { postService.create(req, null) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_UNAUTHORIZED.message)
    }

    @Test
    @DisplayName("단건 조회 – 존재하지 않으면 404(ServiceException)")
    fun getPost_not_found_throws() {
        assertThatThrownBy { postService.getPost(99999L, author) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_NOT_FOUND.message)
    }

    @Test
    @DisplayName("게시글 수정 – 작성자 아님 → FORBIDDEN")
    fun update_forbidden_when_not_author() {
        // given
        val created = postService.create(
            PostCreateRequest(
                PostCategory.TIP, "원본", "내용",
                listOf("https://example.com/tip.jpg")
            ),
            author
        )

        val update = PostUpdateRequest(
            PostCategory.TIP, "수정제목", "수정내용",
            listOf("https://example.com/new.jpg")
        )

        // expect
        assertThatThrownBy { postService.update(created.id, update, other) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_UPDATE_FORBIDDEN.message)
    }

    @Test
    @DisplayName("게시글 삭제 – 작성자 아님 → FORBIDDEN")
    fun delete_forbidden_when_not_author() {
        // given
        val created = postService.create(
            PostCreateRequest(
                PostCategory.QUESTION, "질문", "질문내용",
                listOf("https://example.com/q.jpg")
            ),
            author
        )

        // expect
        assertThatThrownBy { postService.delete(created.id, other) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.POST_DELETE_FORBIDDEN.message)
    }

    @Test
    @DisplayName("목록 – 카테고리/페이지 필터 동작")
    fun list_paging_and_filter() {
        // given: FREE 2건, TIP 1건
        repeat(2) { i ->
            postService.create(
                PostCreateRequest(
                    PostCategory.FREE, "free-$i", "c",
                    listOf("https://example.com/i.jpg")
                ),
                author
            )
        }
        postService.create(
            PostCreateRequest(
                PostCategory.TIP, "tip", "c",
                listOf("https://example.com/i.jpg")
            ),
            author
        )

        // when
        val freePage = postService.getPostList(PostCategory.FREE, null, 0, 10)
        val allPage = postService.getPostList(null, null, 0, 10)

        // then
        assertThat(freePage.totalElements).isEqualTo(2)
        assertThat(allPage.totalElements).isEqualTo(3)
    }
}
