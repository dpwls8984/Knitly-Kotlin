package com.mysite.knitly.domain.community.comment.controller;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.service.CommentService;
import com.mysite.knitly.domain.mypage.dto.PageResponse;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
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
        log.info("[CommentController] 댓글 목록 조회 요청 - postId={}, sort={}, page={}, size={}", postId, sort, page, size);
        Page<CommentTreeResponse> result = commentService.getComments(postId, sort, page, size, user);
        log.info("[CommentController] 댓글 목록 조회 완료 - postId={}, returned={}", postId, result.getNumberOfElements());
        return ResponseEntity.ok(PageResponse.of(result));
    }

    // 댓글 개수
    @GetMapping("/posts/{postId}/comments/count")
    public ResponseEntity<Long> count(@PathVariable Long postId) {
        log.info("[CommentController] 댓글 개수 조회 요청 - postId={}", postId);
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
        log.info("[CommentController] 댓글 작성 요청 - postId={}, parentId={}", postId, request.parentId());
        if (!postId.equals(request.postId())) {
            log.warn("[CommentController] 댓글 작성 실패 - 경로 postId와 본문 postId 불일치 path={}, body={}", postId, request.postId());
            return ResponseEntity.badRequest().build();
        }
        CommentResponse resp = commentService.create(request, user);
        log.info("[CommentController] 댓글 작성 완료 - postId={}, commentId={}", postId, resp.id());
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
        log.info("[CommentController] 댓글 수정 요청 - commentId={}", commentId);
        commentService.update(commentId, request, user);
        log.info("[CommentController] 댓글 수정 완료 - commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user
    ) {
        log.info("[CommentController] 댓글 삭제 요청 - commentId={}", commentId);
        commentService.delete(commentId, user);
        log.info("[CommentController] 댓글 삭제 완료 - commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }
}
