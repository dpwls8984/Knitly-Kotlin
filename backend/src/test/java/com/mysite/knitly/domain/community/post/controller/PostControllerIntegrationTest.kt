package com.mysite.knitly.domain.community.post.controller

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest
import com.mysite.knitly.domain.community.post.dto.PostResponse
import com.mysite.knitly.domain.community.post.entity.PostCategory
import com.mysite.knitly.domain.community.post.service.CommunityImageStorage
import com.mysite.knitly.domain.community.post.service.PostService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.TestSecurityUtil
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIntegrationTest(

    @Autowired
    private val mockMvc: MockMvc

) {

    // Redis 연동 막기 (팀원 패턴 맞춤)
    @MockBean
    lateinit var redissonClient: RedissonClient

    // Service는 MockBean 으로 주입 (DB는 안 타고, 호출 여부만 검증)
    @MockBean
    lateinit var postService: PostService

    // 실제 파일 IO 막기 위해 이미지 스토리지도 Mock (지금은 stub 없이 사용)
    @MockBean
    lateinit var communityImageStorage: CommunityImageStorage

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User.builder()
            .userId(1L)
            .socialId("post-int-test-social")
            .email("post-int@test.com")
            .name("PostTester")
            .provider(Provider.GOOGLE)
            .build()
    }

    @Test
    @DisplayName("POST /community/posts - 게시글 생성 성공 (로그인 사용자)")
    fun createPost_Success() {
        val category = PostCategory.FREE
        val title = "통합테스트 제목"
        val content = "통합테스트 내용"

        val imageUrls = listOf("/uploads/communitys/2025/11/19/test1.jpg")

        // Service 레벨에서 반환될 응답 가짜 정의
        val response = PostResponse(
            id = 100L,
            category = category,
            title = title,
            content = content,
            imageUrls = imageUrls,
            authorId = user.userId,
            authorDisplay = "익명의 털실",
            createdAt = LocalDateTime.now(),
            updatedAt = null,
            commentCount = 0L,
            mine = true
        )

        // postService.create(어떤 요청이 들어오더라도) 위 response 반환
        given(postService.create(any(), any()))
            .willReturn(response)

        // PostController.create 는 multipart/form-data 사용
        val requestBuilder = multipart("/community/posts")
            .param("category", category.name)
            .param("title", title)
            .param("content", content)
            .with(TestSecurityUtil.createPrincipal(user)!!) // 로그인 사용자 세팅

        mockMvc.perform(requestBuilder)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(100L))
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.category").value(category.name))
            .andExpect(jsonPath("$.authorId").value(user.userId))

        // Service가 정확히 한 번 호출되었는지 검증
        verify(postService, times(1)).create(any(), any())
    }

    @Test
    @DisplayName("DELETE /community/posts/{postId} - 게시글 삭제 성공 (로그인 사용자)")
    fun deletePost_Success() {
        val postId = 200L

        mockMvc.perform(
            delete("/community/posts/{postId}", postId)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isNoContent)

        // delete(postId, user) 호출 확인
        verify(postService, times(1)).delete(postId, user)
    }

    @Test
    @DisplayName("GET /community/posts/{postId} - 게시글 상세 조회 성공")
    fun getPost_Success() {
        val postId = 300L

        val response = PostResponse(
            id = postId,
            category = PostCategory.TIP,
            title = "상세조회 제목",
            content = "상세조회 내용",
            imageUrls = emptyList(),
            authorId = user.userId,
            authorDisplay = "익명의 털실",
            createdAt = LocalDateTime.now(),
            updatedAt = null,
            commentCount = 3L,
            mine = true
        )

        given(postService.getPost(postId, user))
            .willReturn(response)

        mockMvc.perform(
            get("/community/posts/{postId}", postId)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(postId))
            .andExpect(jsonPath("$.title").value("상세조회 제목"))
            .andExpect(jsonPath("$.commentCount").value(3))

        verify(postService, times(1)).getPost(postId, user)
    }
}

// Mockito any() 헬퍼 (Kotlin 제네릭 대응)
@Suppress("UNCHECKED_CAST")
private fun <T> any(): T {
    org.mockito.Mockito.any<T>()
    return null as T
}
