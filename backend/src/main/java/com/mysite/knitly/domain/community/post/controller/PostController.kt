package com.mysite.knitly.domain.community.post.controller

import com.mysite.knitly.domain.community.config.CurrentUserResolver
import com.mysite.knitly.domain.community.post.dto.PostCreateRequest
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse
import com.mysite.knitly.domain.community.post.dto.PostResponse
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest
import com.mysite.knitly.domain.community.post.entity.PostCategory
import com.mysite.knitly.domain.community.post.service.CommunityImageStorage
import com.mysite.knitly.domain.community.post.service.PostService
import com.mysite.knitly.domain.user.entity.User
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/community/posts")
class PostController(
    private val postService: PostService,
    private val imageStorage: CommunityImageStorage,
    private val currentUserResolver: CurrentUserResolver
) {

    private val log = LoggerFactory.getLogger(PostController::class.java)

    @GetMapping
    fun getPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<PostListItemResponse>> {
        log.info(
            "[PostController] 게시글 목록 조회 요청 - category={}, query='{}', page={}, size={}",
            category, query, page, size
        )
        return ResponseEntity.ok(postService.getPostList(category, query, page, size))
    }

    @GetMapping("/{postId}")
    fun getPost(@PathVariable("postId") postId: Long): ResponseEntity<PostResponse> {
        log.info("[PostController] 게시글 상세 조회 요청 - postId={}", postId)
        val me: User? = currentUserResolver.getCurrentUserOrNull()
        return ResponseEntity.ok(postService.getPost(postId, me))
    }

    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    fun create(
        @RequestParam("title") title: String,
        @RequestParam("content") content: String,
        @RequestParam("category") category: PostCategory,
        @RequestParam(value = "images", required = false) images: List<MultipartFile>?
    ): ResponseEntity<PostResponse> {

        val me: User? = currentUserResolver.getCurrentUserOrNull()

        val imageUrls: List<String> = imageStorage.saveImages(images)

        val req = PostCreateRequest(
            category,
            title,
            content,
            imageUrls
        )

        val res = postService.create(req, me)
        log.info("[PostController] 게시글 작성 요청 완료 - category={}, title='{}'", category, title)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @PutMapping(
        value = ["/{postId}"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    fun updateWithImages(
        @PathVariable("postId") postId: Long,
        @RequestPart("data") @Valid request: PostUpdateRequest,
        @RequestPart(value = "images", required = false) images: List<MultipartFile>?
    ): ResponseEntity<PostResponse> {
        log.info(
            "[PostController] 게시글 수정 요청 - postId={}, category={}, title='{}'",
            postId, request.category, request.title
        )

        val me: User? = currentUserResolver.getCurrentUserOrNull()

        val newUrls: List<String> = imageStorage.saveImages(images)
        val base: List<String> = request.imageUrls ?: emptyList()

        val merged: MutableList<String> = base.toMutableList()
        if (newUrls.isNotEmpty()) {
            merged.addAll(newUrls)
        }

        val mergedReq = PostUpdateRequest(
            request.category,
            request.title,
            request.content,
            merged
        )

        val res = postService.update(postId, mergedReq, me)
        log.info("[PostController] 게시글 수정 완료 - postId={}", postId)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    fun delete(@PathVariable postId: Long): ResponseEntity<Void> {
        log.info("[PostController] 게시글 삭제 요청 - postId={}", postId)
        val me: User? = currentUserResolver.getCurrentUserOrNull()
        postService.delete(postId, me)
        return ResponseEntity.noContent().build()
    }
}
