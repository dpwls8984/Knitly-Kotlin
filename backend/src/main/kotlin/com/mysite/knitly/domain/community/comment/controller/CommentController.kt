package com.mysite.knitly.domain.community.comment.controller

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest
import com.mysite.knitly.domain.community.comment.dto.CommentResponse
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest
import com.mysite.knitly.domain.community.comment.service.CommentService
import com.mysite.knitly.domain.mypage.dto.PageResponse
import com.mysite.knitly.domain.user.entity.User
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/community")
class CommentController(
    private val commentService: CommentService
) {

    private val log = LoggerFactory.getLogger(CommentController::class.java)

    @GetMapping("/posts/{postId}/comments")
    fun getComments(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "asc") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<PageResponse<CommentTreeResponse>> {
        log.info(
            "[CommentController] 댓글 목록 조회 요청 - postId={}, sort={}, page={}, size={}",
            postId, sort, page, size
        )
        val result: Page<CommentTreeResponse> = commentService.getComments(postId, sort, page, size, user)
        log.info(
            "[CommentController] 댓글 목록 조회 완료 - postId={}, returned={}",
            postId, result.numberOfElements
        )
        return ResponseEntity.ok(PageResponse.of(result))
    }

    @GetMapping("/posts/{postId}/comments/count")
    fun count(@PathVariable postId: Long): ResponseEntity<Long> {
        log.info("[CommentController] 댓글 개수 조회 요청 - postId={}", postId)
        return ResponseEntity.ok(commentService.count(postId))
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    fun create(
        @PathVariable postId: Long,
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CommentCreateRequest,
    ): ResponseEntity<CommentResponse> {

        log.info("[CommentController] 댓글 작성 요청 - postId={}, parentId={}", postId, request.parentId)

        // 🔥 1) PathVariable postId와 body postId 일치 여부 체크
        if (postId != request.postId) {
            log.warn(
                "[CommentController] 댓글 작성 실패 - 경로 postId와 본문 postId 불일치 path={}, body={}",
                postId, request.postId
            )
            return ResponseEntity.badRequest().build()
        }

        // 🔥 2) content 유효성 검증 (테스트 400 대응)
        val content = request.content?.trim() ?: ""
        if (content.isBlank() || content.length > 300) {
            log.warn("[CommentController] 댓글 작성 실패 - 유효성 오류 content='{}'", content)
            return ResponseEntity.badRequest().build()
        }

        // 🔥 3) 유효성 통과 → 서비스 호출
        val resp: CommentResponse = commentService.create(request, user)

        log.info("[CommentController] 댓글 작성 완료 - postId={}, commentId={}", postId, resp.id)

        val location = URI.create("/community/posts/$postId/comments/${resp.id}")
        return ResponseEntity.created(location).body(resp)
    }

    @PatchMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    fun update(
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        log.info("[CommentController] 댓글 수정 요청 - commentId={}", commentId)
        commentService.update(commentId, request, user)
        log.info("[CommentController] 댓글 수정 완료 - commentId={}", commentId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    fun delete(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        log.info("[CommentController] 댓글 삭제 요청 - commentId={}", commentId)
        commentService.delete(commentId, user)
        log.info("[CommentController] 댓글 삭제 완료 - commentId={}", commentId)
        return ResponseEntity.noContent().build()
    }
}
