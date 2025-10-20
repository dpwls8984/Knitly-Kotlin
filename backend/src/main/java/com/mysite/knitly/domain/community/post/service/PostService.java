package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.util.Anonymizer;
import com.mysite.knitly.domain.util.ImageValidator;
import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.community.post.repository.UserRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// 상세 기능:  mine(현재 사용자 = 작성자)
// 등록,수정 기능, 이미지 확장자 검증(png/jpg/jpeg) + 엔티티 저장/수정
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public Page<PostListItemResponse> getPostList(PostCategory category, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findListRows(category, query, pageable);

        return posts.map(p -> {
            String exRaw = p.getContent() == null ? "" : p.getContent();
            String ex = exRaw.length() > 10 ? exRaw.substring(0, 10) + "..." : exRaw;
            Long commentCount = postRepository.countCommentsByPostId(p.getId());
            String thumbnail = (p.getImageUrls() == null || p.getImageUrls().isEmpty())
                    ? null : p.getImageUrls().get(0);

            return new PostListItemResponse(
                    p.getId(),
                    p.getCategory(),
                    p.getTitle(),
                    ex,
                    Anonymizer.yarn(p.getAuthor().getUserId()),
                    p.getCreatedAt(),
                    commentCount,
                    thumbnail
            );
        });
    }

    public PostResponse getPost(Long id, Long currentUserIdOrNull) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        long commentCount = postRepository.countCommentsByPostId(id);

        boolean mine = currentUserIdOrNull != null
                && p.getAuthor() != null
                && p.getAuthor().getUserId().equals(currentUserIdOrNull);

        return new PostResponse(
                p.getId(),
                p.getCategory(),
                p.getTitle(),
                p.getContent(),
                p.getImageUrls(),
                p.getAuthor().getUserId(),
                Anonymizer.yarn(p.getAuthor().getUserId()),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                commentCount,
                mine
        );
    }

    @Transactional
    public PostResponse create(PostCreateRequest req) {
        List<String> urls = normalizeUrls(req.imageUrls());
        if (urls.size() > 5) {
            throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
        }
        for (String u : urls) {
            if (!ImageValidator.isAllowedImageUrl(u)) {
                throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
            }
        }

        User author = userRepository.findById(req.authorId())
                .orElseThrow(() -> new ServiceException(ErrorCode.BAD_REQUEST));

        Post post = Post.builder()
                .category(req.category())
                .title(req.title())
                .content(req.content())
                .author(author)
                .build();

        post.replaceImages(urls);

        Post saved = postRepository.save(post);
        return getPost(saved.getId(), author.getUserId());
    }

    @Transactional
    public PostResponse update(Long id, PostUpdateRequest req, Long currentUserId) {

        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (p.getAuthor() == null || !p.getAuthor().getUserId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        p.update(
                req.title(),
                req.content(),
                req.category()
        );

        if (req.imageUrls() != null) {
            List<String> urls = normalizeUrls(req.imageUrls());
            if (urls.size() > 5) {
                throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
            }
            for (String u : urls) {
                if (!ImageValidator.isAllowedImageUrl(u)) {
                    throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
                }
            }
            p.replaceImages(urls);
        }

        return getPost(p.getId(), currentUserId);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (p.getAuthor() == null || !p.getAuthor().getUserId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.POST_DELETE_FORBIDDEN);
        }

        p.softDelete();
    }

    private List<String> normalizeUrls(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}