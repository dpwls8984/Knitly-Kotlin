package com.mysite.knitly.domain.community.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.UserRepositoryTmp;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.UserProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PostControllerValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepositoryTmp userRepository;

    private UUID authorId;

    @BeforeEach
    void setUp() {
        // 고유 socialId 로 저장
        User author = User.builder()
                .socialId("pval-auth-" + UUID.randomUUID())
                .name("Author")
                .provider(UserProvider.GOOGLE)
                .build();
        authorId = userRepository.save(author).getUserId();
    }

    @Test
    void create_post_blank_title_or_content_returns_400() throws Exception {
        // 빈 제목 -> 400
        var reqBlankTitle = Map.of(
                "category", PostCategory.FREE.name(),
                "title", "   ",
                "content", "내용",
                "imageUrls", List.of("https://example.com/a.jpg"),
                "authorId", authorId.toString()
        );
        mvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reqBlankTitle)))
                .andExpect(status().isBadRequest());

        // 빈 내용 -> 400
        var reqBlankContent = Map.of(
                "category", PostCategory.FREE.name(),
                "title", "제목",
                "content", "   ",
                "imageUrls", List.of("https://example.com/a.jpg"),
                "authorId", authorId.toString()
        );
        mvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reqBlankContent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_post_title_too_long_returns_400() throws Exception {
        var longTitle = "가".repeat(101); // @Size(max=100)
        var req = Map.of(
                "category", PostCategory.FREE.name(),
                "title", longTitle,
                "content", "본문",
                "imageUrls", List.of("https://example.com/a.jpg"),
                "authorId", authorId.toString()
        );
        mvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_post_ok_returns_200() throws Exception {
        var req = Map.of(
                "category", PostCategory.FREE.name(),
                "title", "정상 제목",
                "content", "정상 본문",
                "imageUrls", List.of("https://example.com/a.jpg"),
                "authorId", authorId.toString()
        );
        mvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("정상 제목"))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/a.jpg"));
    }
}
