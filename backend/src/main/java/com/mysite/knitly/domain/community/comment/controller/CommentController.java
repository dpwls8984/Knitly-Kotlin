package com.mysite.knitly.domain.community.comment.controller;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.service.CommentService;
import com.mysite.knitly.domain.mypage.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mysite.knitly.domain.user.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import java.net.URI;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 목록
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<PageResponse<CommentTreeResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ) {
        Page<CommentTreeResponse> result = commentService.getComments(postId, sort, page, size, user);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    // 댓글 개수
    @GetMapping("/posts/{postId}/comments/count")
    public ResponseEntity<Long> count(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.count(postId));
    }

    // 댓글 작성 (parentId 있으면 대댓글)
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        if (!postId.equals(request.postId())) {
            return ResponseEntity.badRequest().build();
        }
        CommentResponse resp = commentService.create(request, user);
        // 리소스 구조 일관성: /community/posts/{postId}/comments/{commentId}
        return ResponseEntity
                .created(URI.create(String.format("/community/posts/%d/comments/%d", postId, resp.id())))
                .body(resp);
    }

    // 댓글 수정
    @PatchMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        commentService.update(commentId, request, user);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user
    ) {
        commentService.delete(commentId, user);
        return ResponseEntity.noContent().build();
    }
}
