package com.mysite.knitly.domain.community.post.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.community.config.CurrentUserResolver
import com.mysite.knitly.domain.community.post.dto.PostCreateRequest
import com.mysite.knitly.domain.community.post.dto.PostResponse
import com.mysite.knitly.domain.community.post.entity.PostCategory
import com.mysite.knitly.domain.community.post.service.CommunityImageStorage
import com.mysite.knitly.domain.community.post.service.PostService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostControllerValidationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @MockBean
    lateinit var postService: PostService

    @MockBean
    lateinit var imageStorage: CommunityImageStorage

    @MockBean
    lateinit var currentUserResolver: CurrentUserResolver

    private lateinit var author: User
    private val authorId: Long
        get() = author.userId!!

    @BeforeEach
    fun setUp() {
        // 유저 저장
        author = User.builder()
            .socialId("s1")
            .email("author@test.com")
            .name("Author")
            .provider(Provider.GOOGLE)
            .build()

        author = userRepository.save(author)

        // Security Mock
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(author, null, listOf())

        given(currentUserResolver.getCurrentUserOrNull()).willReturn(author)
    }

    @Test
    fun createPost_success_returnsCreated() {
        val img = MockMultipartFile(
            "images", "ok.jpg", "image/jpeg", "bin".toByteArray()
        )

        given(imageStorage.saveImages(any())).willReturn(
            listOf("/uploads/communitys/2025/11/09/ok.jpg")
        )

        given(postService.create(any(), any())).willReturn(
            PostResponse(
                1001L, PostCategory.QUESTION, "첫 글", "내용",
                listOf("/uploads/communitys/2025/11/09/ok.jpg"),
                authorId, "익명의 털실",
                LocalDateTime.now(), LocalDateTime.now(),
                0L, true
            )
        )

        mockMvc.perform(
            multipart("/community/posts")
                .file(img)
                .param("title", "첫 글")
                .param("content", "내용")
                .param("category", "QUESTION")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("첫 글"))
            .andExpect(jsonPath("$.authorId").value(authorId))
    }

    @Test
    fun createPost_missingCategory_returnsBadRequest() {
        // category 누락 시 스프링 기본 MissingServletRequestParameterException 발생
        // 기본 에러 응답은 JSON 구조가 아니므로 JSONPath 검증 제거
        mockMvc.perform(
            multipart("/community/posts")
                .param("title", "제목")
                .param("content", "내용")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun createPost_titleTooLong_returnsBadRequest() {
        given(postService.create(any(), any()))
            .willThrow(
                com.mysite.knitly.global.exception.ServiceException(
                    com.mysite.knitly.global.exception.ErrorCode.POST_TITLE_LENGTH_INVALID
                )
            )

        val longTitle = "a".repeat(101)

        mockMvc.perform(
            multipart("/community/posts")
                .param("title", longTitle)
                .param("content", "내용")
                .param("category", "FREE")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("POST_TITLE_LENGTH_INVALID"))
    }

    @Test
    fun createPost_tooManyImages_returnsBadRequest() {
        given(postService.create(any(), any()))
            .willThrow(
                com.mysite.knitly.global.exception.ServiceException(
                    com.mysite.knitly.global.exception.ErrorCode.POST_IMAGE_COUNT_EXCEEDED
                )
            )

        mockMvc.perform(
            multipart("/community/posts")
                .param("title", "제목")
                .param("content", "내용")
                .param("category", "TIP")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("POST_IMAGE_COUNT_EXCEEDED"))
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> any(): T {
    org.mockito.Mockito.any<T>()
    return null as T
}
