package com.mysite.knitly.domain.community.comment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.community.comment.dto.CommentResponse
import com.mysite.knitly.domain.community.comment.service.CommentService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.UUID
import org.mockito.BDDMockito.given
import org.mockito.ArgumentMatchers.any

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CommentControllerValidationTest(

    @Autowired
    private val mvc: MockMvc,

    @Autowired
    private val om: ObjectMapper,

    @MockBean
    private val commentService: CommentService
) {

    private var postId: Long = 0L
    private lateinit var author: User

    @BeforeEach
    fun setUp() {
        author = User.builder()
            .socialId("cval-auth-" + UUID.randomUUID())
            .email("author@test.com")
            .name("Author")
            .provider(Provider.GOOGLE)
            .build()

        postId = 123L
    }

    private fun withAuth(u: User): RequestPostProcessor =
        RequestPostProcessor { request ->
            val auth = UsernamePasswordAuthenticationToken(u, null, listOf())
            SecurityContextHolder.getContext().authentication = auth
            request
        }

    @Test
    fun create_comment_blank_or_too_long_returns_400() {
        // 빈 댓글 → 400
        val bodyBlank = om.writeValueAsString(
            mapOf(
                "postId" to postId,
                "content" to "   "
            )
        )

        mvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyBlank)
                .with(withAuth(author))
        )
            .andExpect(status().isBadRequest)

        // 301자 → 400
        val longText = "가".repeat(301)

        val bodyLong = om.writeValueAsString(
            mapOf(
                "postId" to postId,
                "content" to longText
            )
        )

        mvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyLong)
                .with(withAuth(author))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun create_comment_ok_returns_201() {
        given(commentService.create(any(), any())).willReturn(
            CommentResponse(
                id = 1000L,
                content = "정상 댓글",
                authorId = 999L,
                authorDisplay = "익명의 털실",
                createdAt = LocalDateTime.now(),
                mine = true
            )
        )

        val body = om.writeValueAsString(
            mapOf(
                "postId" to postId,
                "content" to "정상 댓글"
            )
        )

        mvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(withAuth(author))
        )
            .andExpect(status().isCreated)
            .andExpect(
                header().string(
                    "Location",
                    startsWith("/community/posts/$postId/comments/")
                )
            )
            .andExpect(jsonPath("$.content").value("정상 댓글"))
    }

    @Test
    fun create_reply_ok_returns_201() {
        given(commentService.create(any(), any()))
            .willReturn(
                CommentResponse(
                    id = 2000L,
                    content = "루트",
                    authorId = 999L,
                    authorDisplay = "익명의 털실",
                    createdAt = LocalDateTime.now(),
                    mine = true
                )
            )
            .willReturn(
                CommentResponse(
                    id = 2001L,
                    content = "대댓글",
                    authorId = 999L,
                    authorDisplay = "익명의 털실 1",
                    createdAt = LocalDateTime.now(),
                    mine = true
                )
            )

        // 루트 댓글
        val rootBody = om.writeValueAsString(
            mapOf(
                "postId" to postId,
                "content" to "루트"
            )
        )

        mvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootBody)
                .with(withAuth(author))
        )
            .andExpect(status().isCreated)
            .andExpect(
                header().string(
                    "Location",
                    startsWith("/community/posts/$postId/comments/")
                )
            )
            .andExpect(jsonPath("$.content").value("루트"))

        // 대댓글
        val replyBody = om.writeValueAsString(
            mapOf(
                "postId" to postId,
                "parentId" to 2000L,
                "content" to "대댓글"
            )
        )

        mvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(replyBody)
                .with(withAuth(author))
        )
            .andExpect(status().isCreated)
            .andExpect(
                header().string(
                    "Location",
                    startsWith("/community/posts/$postId/comments/")
                )
            )
            .andExpect(jsonPath("$.content").value("대댓글"))
    }
}
