package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.dto.PostResponse;
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.Anonymizer;
import com.mysite.knitly.global.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

// 상세 기능:  mine(현재 사용자 = 작성자)
// 등록,수정 기능, 이미지 확장자 검증(png/jpg/jpeg) + 엔티티 저장/수정
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public Page<PostListItemResponse> getPostList(PostCategory category, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findListRows(category, query, pageable);

        // 댓글 수 일괄 조회로 N+1 제거
        List<Long> ids = posts.getContent().stream().map(Post::getId).toList();
        Map<Long, Long> countMap = postRepository.countCommentsByPostIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1], Long::sum));

        return posts.map(p -> {
            String exRaw = p.getContent() == null ? "" : p.getContent();

            // 공백/특수 공백 제거 후 요약 생성
            String cleaned = exRaw.replaceAll("[\\s\\u00A0\\u3000]+", "");
            String ex = cleaned.isEmpty()
                    ? ""
                    : (cleaned.length() > 10 ? cleaned.substring(0, 10) + "..." : cleaned);
            long commentCount = countMap.getOrDefault(p.getId(), 0L);

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

    public PostResponse getPost(Long id, User currentUserOrNull) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        long commentCount = postRepository.countCommentsByPostId(id);

        boolean mine = p.isAuthor(currentUserOrNull);

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
    public PostResponse create(PostCreateRequest req, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }
        List<String> urls = normalizeUrls(req.imageUrls());
        if (urls.size() > 5) {
            throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
        }
        for (String u : urls) {
            if (!ImageValidator.isAllowedImageUrl(u)) {
                throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
            }
        }

        // 요청 authorId는 무시, 인증된 사용자 객체를 그대로 사용
        User author = currentUser;

        Post post = Post.builder()
                .category(req.category())
                .title(req.title())
                .content(req.content())
                .author(author)
                .build();

        post.replaceImages(urls);

        Post saved = postRepository.save(post);
        return getPost(saved.getId(), currentUser);
    }

    @Transactional
    public PostResponse update(Long id, PostUpdateRequest req, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (!p.isAuthor(currentUser)) {
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

        return getPost(p.getId(), currentUser);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (!p.isAuthor(currentUser)) {
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