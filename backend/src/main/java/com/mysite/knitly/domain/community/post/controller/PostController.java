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
@RequestMapping("/api/community/posts")
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

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long id,
            @RequestParam(required = false) UUID currentUserId
    ) {
        return ResponseEntity.ok(postService.getPost(id, currentUserId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            @RequestParam UUID currentUserId
    ) {
        return ResponseEntity.ok(postService.update(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam UUID currentUserId
    ) {
        postService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
