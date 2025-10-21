package com.mysite.knitly.domain.community.comment.controller;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mysite.knitly.domain.user.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 목록
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentTreeResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ) {
        Long userIdFromToken = (user == null) ? null : user.getUserId();
        return ResponseEntity.ok(commentService.getComments(postId, sort, page, size, userIdFromToken));
    }

    // 댓글 개수
    @GetMapping("/posts/{postId}/comments/count")
    public ResponseEntity<Long> count(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.count(postId));
    }

    // 댓글 작성 (parentId 있으면 대댓글)
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        if (!postId.equals(request.postId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(commentService.create(request, user.getUserId()));
    }

    // 댓글 수정
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        commentService.update(commentId, request, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId
    ) {
        commentService.delete(commentId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
