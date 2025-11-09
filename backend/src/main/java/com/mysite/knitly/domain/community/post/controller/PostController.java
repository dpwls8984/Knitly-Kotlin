package com.mysite.knitly.domain.community.post.controller;

import com.mysite.knitly.domain.community.config.CurrentUserResolver;
import com.mysite.knitly.domain.community.post.dto.PostCreateRequest;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.dto.PostResponse;
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.service.CommunityImageStorage;
import com.mysite.knitly.domain.community.post.service.PostService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommunityImageStorage imageStorage;
    private final CurrentUserResolver currentUserResolver;

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<Page<PostListItemResponse>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getPostList(category, query, page, size));
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable("postId") Long postId) {
        User me = currentUserResolver.getCurrentUserOrNull();
        return ResponseEntity.ok(postService.getPost(postId, me));
    }

    // 게시글 작성 (multipart/form-data)
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") PostCategory category,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        // 현재 사용자
        User me = currentUserResolver.getCurrentUserOrNull();

        // 이미지 저장
        List<String> imageUrls = imageStorage.saveImages(images);

        // DTO 조립
        PostCreateRequest req = new PostCreateRequest(category, title, content, imageUrls);

        // 저장
        PostResponse res = postService.create(req, me);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 게시글 수정 (multipart/form-data) - 텍스트+이미지
    @PutMapping(
            value = "/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> updateWithImages(
            @PathVariable("postId") Long postId,
            @RequestPart("data") @Valid PostUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        User me = currentUserResolver.getCurrentUserOrNull();

        // 1) 새로 업로드된 파일을 저장해 URL 목록 확보
        List<String> newUrls = imageStorage.saveImages(images); // null/empty 안전

        // 2) 기존 요청의 imageUrls와 병합 (null이면 빈 리스트 간주)
        List<String> base = (request.imageUrls() == null) ? List.of() : request.imageUrls();
        List<String> merged = new ArrayList<>(base);
        if (newUrls != null && !newUrls.isEmpty()) merged.addAll(newUrls);

        // 3) 병합된 URL로 새 DTO 만들어 기존 서비스 update 재사용
        PostUpdateRequest mergedReq = new PostUpdateRequest(
                request.category(),
                request.title(),
                request.content(),
                merged
        );

        return ResponseEntity.ok(postService.update(postId, mergedReq, me));
    }


    // 게시글 삭제
    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long postId) {
        User me = currentUserResolver.getCurrentUserOrNull();
        postService.delete(postId, me);
        return ResponseEntity.noContent().build();
    }
}
