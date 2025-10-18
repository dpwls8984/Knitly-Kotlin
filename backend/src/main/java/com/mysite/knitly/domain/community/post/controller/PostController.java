package com.mysite.knitly.domain.community.post.controller;

import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostListItemResponse>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getPostList(category, query, page, size));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable("postId") Long postId,
            @RequestParam(required = false) UUID currentUserId
    ) {
        return ResponseEntity.ok(postService.getPost(postId, currentUserId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostCreateRequest request
    ) {
        if (request.imageUrls() != null && request.imageUrls().size() > 5) {
            throw new IllegalArgumentException("이미지는 최대 5개까지 업로드할 수 있습니다.");
        }
        return ResponseEntity.ok(postService.create(request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @RequestParam UUID currentUserId
    ) {
        if (request.imageUrls() != null && request.imageUrls().size() > 5) {
            throw new IllegalArgumentException("이미지는 최대 5개까지 업로드할 수 있습니다.");
        }
        return ResponseEntity.ok(postService.update(postId, request, currentUserId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable("postId") Long postId,
            @RequestParam UUID currentUserId
    ) {
        postService.delete(postId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
