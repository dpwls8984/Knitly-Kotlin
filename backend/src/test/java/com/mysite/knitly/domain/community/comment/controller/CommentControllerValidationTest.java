package com.mysite.knitly.domain.community.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.community.post.repository.UserRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.UserProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터는 필요하면 나중에
@ActiveProfiles("test")
class CommentControllerValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired
    UserRepository userRepository;
    @Autowired PostRepository postRepository;

    private Long postId;
    private UUID authorId;

    @BeforeEach
    void setUp() {

        User author = User.builder()
                .socialId("cval-auth-" + UUID.randomUUID())
                .name("Author")
                .provider(UserProvider.GOOGLE)
                .build();
        authorId = userRepository.save(author).getUserId();

        Post post = Post.builder()
                .category(PostCategory.FREE)
                .title("댓글 검증용 글")
                .content("본문")
                .imageUrls(List.of())
                .author(author)
                .build();
        postId = postRepository.save(post).getId();
    }

    @Test
    void create_comment_blank_or_too_long_returns_400() throws Exception {
        // 빈 댓글 -> 400
        var bodyBlank = om.writeValueAsString(Map.of(
                "postId", postId, "authorId", authorId, "content", "   "
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBlank))
                .andExpect(status().isBadRequest());

        // 301자 -> 400
        var longText = "가".repeat(301);
        var bodyLong = om.writeValueAsString(Map.of(
                "postId", postId, "authorId", authorId, "content", longText
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyLong))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_comment_ok_returns_200() throws Exception {
        var body = om.writeValueAsString(Map.of(
                "postId", postId, "authorId", authorId, "content", "정상 댓글"
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("정상 댓글"));
    }

    @Test
    void create_reply_ok_returns_200() throws Exception {
        // 루트 댓글 생성
        var root = new HashMap<String, Object>();
        root.put("postId", postId);
        root.put("authorId", authorId);
        root.put("content", "루트");
        var rootRes = mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(root)))
                .andExpect(status().isOk())
                .andReturn();

        long rootId = om.readTree(rootRes.getResponse().getContentAsString()).get("id").asLong();

        // 대댓글 생성 parentId 포함 됨.
        var reply = new HashMap<String, Object>();
        reply.put("postId", postId);
        reply.put("authorId", authorId);
        reply.put("parentId", rootId);
        reply.put("content", "대댓글");
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reply)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("대댓글"));
    }
}
