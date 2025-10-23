package com.mysite.knitly.domain.community.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import org.apiguardian.api.API;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// 게시글 작성 API 입력값 검증 및 권한 테스트
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // 전역 테스트 필드
    private User author;
    private Long authorId;
    private User other;
    private Long otherId;

    @BeforeEach
    void setUp() {
        // 작성자
        author = User.builder()
                .socialId("s1")
                .email("author@test.com")
                .name("Author")
                .provider(Provider.GOOGLE)
                .build();
        authorId = userRepository.save(author).getUserId();

        // 다른 사용자
        other = User.builder()
                .socialId("s2")
                .email("other@test.com")
                .name("Other")
                .provider(Provider.KAKAO)
                .build();
        otherId = userRepository.save(other).getUserId();

        // 인증 유저를 SecurityContext에 주입 (MockMvc가 @AuthenticationPrincipal로 읽을 수 있게)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(author, null, List.of())
        );
    }


    // 제목이 100자 초과 시 validation 실패 테스트
    @Test
    void createPost_titleTooLong_returnsBadRequest() throws Exception {
        String longTitle = "a".repeat(101);
        Map<String, Object> request = Map.of(
                "category", PostCategory.FREE.name(),
                "title", longTitle,
                "content", "내용",
                "imageUrls", List.of("https://example.com/a.jpg")
        );

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("POST_TITLE_LENGTH_INVALID"));
    }


    // 이미지 6개 이상이면 validation 실패 테스트
    @Test
    void createPost_tooManyImages_returnsBadRequest() throws Exception {
        List<String> sixImages = List.of(
                "https://example.com/1.jpg",
                "https://example.com/2.jpg",
                "https://example.com/3.jpg",
                "https://example.com/4.jpg",
                "https://example.com/5.jpg",
                "https://example.com/6.jpg"
        );
        Map<String, Object> request = Map.of(
                "category", PostCategory.TIP.name(),
                "title", "제목",
                "content", "내용",
                "imageUrls", sixImages
        );

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("POST_IMAGE_COUNT_EXCEEDED"));
    }


     //필수값 누락 시 validation 실패 테스트
    @Test
    void createPost_missingFields_returnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "category", PostCategory.FREE.name(),
                "title", "",
                "content", ""
        );

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }


     // 정상 요청 시 201 Created 반환 테스트
    @Test
    void createPost_success_returnsCreated() throws Exception {
        Map<String, Object> request = Map.of(
                "category", PostCategory.QUESTION.name(),
                "title", "첫 글",
                "content", "내용",
                "imageUrls", List.of("https://example.com/ok.jpg")
        );

        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.authorId").value(authorId));

        assertThat(userRepository.findById(authorId)).isPresent();
    }
}
