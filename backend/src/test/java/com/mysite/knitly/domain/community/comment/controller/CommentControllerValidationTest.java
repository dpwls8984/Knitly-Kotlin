package com.mysite.knitly.domain.community.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터는 필요하면 나중에, SecurityContext는 수동 주입
@ActiveProfiles("test")
class CommentControllerValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired
    UserRepository userRepository;
    @Autowired PostRepository postRepository;

    private Long postId;
    private Long authorId;
    private com.mysite.knitly.domain.user.entity.User author;

    @BeforeEach
    void setUp() {

        // 테스트 사용자 (email 필수)
        author = com.mysite.knitly.domain.user.entity.User.builder()
                .socialId("cval-auth-" + java.util.UUID.randomUUID())
                .email("author@test.com")
                .name("Author")
                .provider(com.mysite.knitly.domain.user.entity.Provider.GOOGLE)
                .build();
        authorId = userRepository.save(author).getUserId();

        // 게시글 생성
        var post = com.mysite.knitly.domain.community.post.entity.Post.builder()
                .category(com.mysite.knitly.domain.community.post.entity.PostCategory.FREE)
                .title("댓글 검증용 글")
                .content("본문")
                .imageUrls(java.util.List.of())
                .author(author)
                .build();
        postId = postRepository.save(post).getId();
    }

        // SecurityContext에 @AuthenticationPrincipal(User) 세팅
        private RequestPostProcessor withAuth(User u) {
            return request -> {
                var auth = new UsernamePasswordAuthenticationToken(u, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return request;
            };
    }

    @Test
    void create_comment_blank_or_too_long_returns_400() throws Exception {
        // 빈 댓글 -> 400
        var bodyBlank = om.writeValueAsString(Map.of(
                "postId", postId, "content", "   "
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBlank)
                        .with(withAuth(author)))
                .andExpect(status().isBadRequest());

        // 301자 -> 400
        var longText = "가".repeat(301);
        var bodyLong = om.writeValueAsString(Map.of(
                "postId", postId, "content", longText
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyLong)
                        .with(withAuth(author)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_comment_ok_returns_201() throws Exception {
        var body = om.writeValueAsString(Map.of(
                "postId", postId, "content", "정상 댓글"
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/comments/")))
                .andExpect(jsonPath("$.content").value("정상 댓글"));
    }

    @Test
    void create_reply_ok_returns_201() throws Exception {
        // 루트 댓글 생성
        var root = new HashMap<String, Object>();
        root.put("postId", postId);
        root.put("content", "루트");
        var rootRes = mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(root))
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/comments/")))
                .andReturn();

        long rootId = om.readTree(rootRes.getResponse().getContentAsString()).get("id").asLong();

        // 대댓글 생성 parentId 포함 됨.
        var reply = new HashMap<String, Object>();
        reply.put("postId", postId);
        reply.put("parentId", rootId);
        reply.put("content", "대댓글");
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reply))
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/comments/")))
                .andExpect(jsonPath("$.content").value("대댓글"));
    }
}
