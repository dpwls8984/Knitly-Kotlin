package com.mysite.knitly.domain.community.post.controller;

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.dto.PostResponse;
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mysite.knitly.domain.user.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
            @AuthenticationPrincipal User user,
            @PathVariable("postId") Long postId
    ) {
        return ResponseEntity.ok(postService.getPost(postId, user));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.create(request, user));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable("postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(postService.update(postId, request, user));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable("postId") Long postId
    ) {
        postService.delete(postId, user);
        return ResponseEntity.noContent().build();
    }
}
