package com.mysite.knitly.domain.community.comment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest
import com.mysite.knitly.domain.community.comment.dto.CommentResponse
import com.mysite.knitly.domain.community.comment.service.CommentService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.TestSecurityUtil
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerIntegrationTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper

) {

    // Redis 연동 막기
    @MockBean
    lateinit var redissonClient: RedissonClient

    @MockBean
    lateinit var commentService: CommentService

    private lateinit var user: User
    private var postId: Long = 0L

    @BeforeEach
    fun setup() {
        user = User.builder()
            .userId(1L)
            .socialId("comment-int-test-social")
            .email("comment-int@test.com")
            .name("CommentTester")
            .provider(Provider.GOOGLE)
            .build()

        postId = 123L
    }

    @Test
    @DisplayName("POST /community/posts/{postId}/comments - 루트 댓글 생성 성공")
    fun createRootComment_Success() {
        val req = CommentCreateRequest(
            postId = postId,
            parentId = null,
            content = "통합테스트 루트 댓글"
        )

        val resp = CommentResponse(
            id = 1000L,
            content = req.content,
            authorId = user.userId,
            authorDisplay = "익명의 털실",
            createdAt = LocalDateTime.now(),
            mine = true
        )

        given(commentService.create(any(), any()))
            .willReturn(resp)

        val body = objectMapper.writeValueAsString(req)

        mockMvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", startsWith("/community/posts/$postId/comments/")))
            .andExpect(jsonPath("$.id").value(1000L))
            .andExpect(jsonPath("$.content").value("통합테스트 루트 댓글"))

        verify(commentService, times(1)).create(any(), any())
    }

    @Test
    @DisplayName("POST /community/posts/{postId}/comments - 대댓글 생성 성공")
    fun createReplyComment_Success() {
        val parentId = 1000L

        val req = CommentCreateRequest(
            postId = postId,
            parentId = parentId,
            content = "통합테스트 대댓글"
        )

        val resp = CommentResponse(
            id = 2000L,
            content = req.content,
            authorId = user.userId,
            authorDisplay = "익명의 털실 1",
            createdAt = LocalDateTime.now(),
            mine = true
        )

        given(commentService.create(any(), any()))
            .willReturn(resp)

        val body = objectMapper.writeValueAsString(req)

        mockMvc.perform(
            post("/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", startsWith("/community/posts/$postId/comments/")))
            .andExpect(jsonPath("$.id").value(2000L))
            .andExpect(jsonPath("$.content").value("통합테스트 대댓글"))

        verify(commentService, times(1)).create(any(), any())
    }

    @Test
    @DisplayName("DELETE /community/comments/{commentId} - 댓글 삭제 성공")
    fun deleteComment_Success() {
        val commentId = 777L

        mockMvc.perform(
            delete("/community/comments/{commentId}", commentId)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isNoContent)

        verify(commentService, times(1)).delete(commentId, user)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> any(): T {
    org.mockito.Mockito.any<T>()
    return null as T
}
