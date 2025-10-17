package com.mysite.knitly.domain.community.post.controller;

import com.mysite.knitly.domain.community.post.dto.*;
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
    public ResponseEntity<Page<PostListItem>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getPostList(category, page, size));
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
        return ResponseEntity.ok(postService.create(request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @RequestParam UUID currentUserId
    ) {
        return ResponseEntity.ok(postService.update(postId, request, currentUserId));
    }

    @DeleteMapping("/{postid}")
    public ResponseEntity<Void> delete(
            @PathVariable("postId") Long postId,
            @RequestParam UUID currentUserId
    ) {
        postService.delete(postId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
