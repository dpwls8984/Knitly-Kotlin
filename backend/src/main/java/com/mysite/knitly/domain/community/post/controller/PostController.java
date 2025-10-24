package com.mysite.knitly.domain.community.post.controller;

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.dto.PostResponse;
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.service.PostService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostCreateRequest request
            , @AuthenticationPrincipal User user

    ) {
        PostResponse res = postService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{postId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> update(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(postService.update(postId, request, user));
    }

    @DeleteMapping("/{postId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user
    ) {
        postService.delete(postId, user);
        return ResponseEntity.noContent().build();
    }
}
