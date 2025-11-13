package com.mysite.knitly.domain.community.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.service.CommentService;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 OFF, SecurityContext 수동 주입
@ActiveProfiles("test")
class CommentControllerValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean CommentService commentService;

    private Long postId;
    private User author;

    @BeforeEach
    void setUp() {
        // 컨트롤러 유효성만 검증 → DB 저장 불필요. 더미 객체/ID만 준비.
        author = User.builder()
                .socialId("cval-auth-" + UUID.randomUUID())
                .email("author@test.com")
                .name("Author")
                .provider(Provider.GOOGLE)
                .build();
        postId = 123L;
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
        // 서비스 목: 정상 생성 응답
        given(commentService.create(any(), any())).willReturn(
                new CommentResponse(
                        1000L, "정상 댓글",
                        999L, "익명의 털실",
                        LocalDateTime.now(), true
                )
        );

        var body = om.writeValueAsString(Map.of(
                "postId", postId, "content", "정상 댓글"
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/posts/" + postId + "/comments/")))
                .andExpect(jsonPath("$.content").value("정상 댓글"));
    }

    @Test
    void create_reply_ok_returns_201() throws Exception {
        // 서비스 목: 첫 호출(루트) → id=2000, 두 번째(대댓글) → id=2001
        given(commentService.create(any(), any()))
                .willReturn(new CommentResponse(
                        2000L, "루트", 999L, "익명의 털실", LocalDateTime.now(), true
                ))
                .willReturn(new CommentResponse(
                        2001L, "대댓글", 999L, "익명의 털실 1", LocalDateTime.now(), true
                ));

        // 1) 루트
        var rootBody = om.writeValueAsString(Map.of("postId", postId, "content", "루트"));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rootBody)
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/posts/" + postId + "/comments/")))
                .andExpect(jsonPath("$.content").value("루트"));

        // 2) 대댓글 (parentId는 경로 검증만 필요)
        var replyBody = om.writeValueAsString(Map.of(
                "postId", postId, "parentId", 2000L, "content", "대댓글"
        ));
        mvc.perform(post("/community/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyBody)
                        .with(withAuth(author)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/community/posts/" + postId + "/comments/")))
                .andExpect(jsonPath("$.content").value("대댓글"));
    }
}
