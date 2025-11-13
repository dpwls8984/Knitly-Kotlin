package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest;
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse;
import com.mysite.knitly.domain.community.post.dto.PostResponse;
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    // 게시글 목록 조회
    public Page<PostListItemResponse> getPostList(PostCategory category, String query, int page, int size) {
        log.info("[PostService] 게시글 목록 조회 요청 - category={}, query='{}', page={}, size={}", category, query, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findListRows(category, query, pageable);

        // 댓글 수 일괄 조회로 N+1 제거
        List<Long> ids = posts.getContent().stream().map(Post::getId).toList();
        Map<Long, Long> countMap = postRepository.countCommentsByPostIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1], Long::sum));

        log.info("[PostService] 게시글 목록 조회 완료 - totalElements={}", posts.getTotalElements());

        return posts.map(p -> {
            String exRaw = p.getContent() == null ? "" : p.getContent();

            // 공백/특수 공백 제거 후 요약 생성
            String cleaned = exRaw.replaceAll("[\\s\\u00A0\\u3000]+", "");
            String ex = cleaned.isEmpty()
                    ? ""
                    : (cleaned.length() > 10 ? cleaned.substring(0, 10) + "..." : cleaned);
            long commentCount = countMap.getOrDefault(p.getId(), 0L);

            String thumbnail = p.getThumbnail();

            return new PostListItemResponse(
                    p.getId(),
                    p.getCategory(),
                    p.getTitle(),
                    ex,
                    "익명의 털실", // 모든 게시글·댓글 작성자는 익명 처리
                    p.getCreatedAt(),
                    commentCount,
                    thumbnail
            );
        });
    }

    // 게시글 상세 조회
    public PostResponse getPost(Long id, User currentUserOrNull) {
        log.info("[PostService] 게시글 상세 조회 - postId={}", id);

        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        // soft-delete 된 글은 404로 차단
        if (p.isDeleted()) {
            log.warn("[PostService] 삭제된 게시글 접근 시도 - postId={}", id);
            throw new ServiceException(ErrorCode.POST_NOT_FOUND);
        }

        long commentCount = postRepository.countCommentsByPostId(id);
        boolean mine = p.isAuthor(currentUserOrNull);

        log.info("[PostService] 게시글 조회 완료 - postId={}, commentCount={}", id, commentCount);

        return new PostResponse(
                p.getId(),
                p.getCategory(),
                p.getTitle(),
                p.getContent(),
                p.getImageUrls(),
                p.getAuthor().getUserId(),
                "익명의 털실", // 모든 게시글·댓글 작성자는 익명 처리
                p.getCreatedAt(),
                p.getUpdatedAt(),
                commentCount,
                mine
        );
    }

    // 게시글 생성
    @Transactional
    public PostResponse create(PostCreateRequest req, User currentUser) {
        log.info("[PostService] 게시글 생성 요청 - userId={}, category={}, title='{}'",
                (currentUser != null ? currentUser.getUserId() : "anonymous"), req.category(), req.title());

        if (currentUser == null) {
            log.warn("[PostService] 게시글 생성 실패 - 비인증 사용자 요청");
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }
        if (req.title() != null && req.title().length() > 100) {
            log.warn("[PostService] 게시글 생성 실패 - 제목 길이 초과 ({}자)", req.title().length());
            throw new ServiceException(ErrorCode.POST_TITLE_LENGTH_INVALID);
        }

        List<String> urls = normalizeUrls(req.imageUrls());
        if (urls.size() > 5) {
            log.warn("[PostService] 게시글 생성 실패 - 이미지 개수 초과 ({})", urls.size());
            throw new ServiceException(ErrorCode.POST_IMAGE_COUNT_EXCEEDED);
        }
        for (String u : urls) {
            if (!ImageValidator.isAllowedImageUrl(u)) {
                log.warn("[PostService] 게시글 생성 실패 - 이미지 확장자 불허 ({})", u);
                throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
            }
        }

        User author = currentUser;

        Post post = Post.builder()
                .category(req.category())
                .title(req.title())
                .content(req.content())
                .author(author)
                .build();

        post.replaceImages(urls);

        Post saved = postRepository.save(post);
        log.info("[PostService] 게시글 생성 완료 - postId={}, images={}", saved.getId(), urls.size());

        return getPost(saved.getId(), currentUser);
    }

    // 게시글 수정
    @Transactional
    public PostResponse update(Long id, PostUpdateRequest req, User currentUser) {
        log.info("[PostService] 게시글 수정 요청 - userId={}, postId={}, title='{}'",
                (currentUser != null ? currentUser.getUserId() : "anonymous"), id, req.title());

        if (currentUser == null) {
            log.warn("[PostService] 게시글 수정 실패 - 비인증 사용자");
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }
        if (req.title() != null && req.title().length() > 100) {
            log.warn("[PostService] 게시글 수정 실패 - 제목 길이 초과 ({}자)", req.title().length());
            throw new ServiceException(ErrorCode.POST_TITLE_LENGTH_INVALID);
        }

        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (!p.isAuthor(currentUser)) {
            log.warn("[PostService] 게시글 수정 실패 - 권한 없음 userId={}, authorId={}",
                    currentUser.getUserId(), p.getAuthor().getUserId());
            throw new ServiceException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        p.update(req.title(), req.content(), req.category());

        if (req.imageUrls() != null) {
            List<String> urls = normalizeUrls(req.imageUrls());
            if (urls.size() > 5) {
                log.warn("[PostService] 게시글 수정 실패 - 이미지 개수 초과 ({})", urls.size());
                throw new ServiceException(ErrorCode.POST_IMAGE_COUNT_EXCEEDED);
            }
            for (String u : urls) {
                if (!ImageValidator.isAllowedImageUrl(u)) {
                    log.warn("[PostService] 게시글 수정 실패 - 이미지 확장자 불허 ({})", u);
                    throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
                }
            }
            p.replaceImages(urls);
        }

        log.info("[PostService] 게시글 수정 완료 - postId={}", p.getId());
        return getPost(p.getId(), currentUser);
    }

    // 게시글 삭제
    @Transactional
    public void delete(Long id, User currentUser) {
        log.info("[PostService] 게시글 삭제 요청 - userId={}, postId={}",
                (currentUser != null ? currentUser.getUserId() : "anonymous"), id);

        if (currentUser == null) {
            log.warn("[PostService] 게시글 삭제 실패 - 비인증 사용자");
            throw new ServiceException(ErrorCode.POST_UNAUTHORIZED);
        }

        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (!p.isAuthor(currentUser)) {
            log.warn("[PostService] 게시글 삭제 실패 - 권한 없음 userId={}, authorId={}",
                    currentUser.getUserId(), p.getAuthor().getUserId());
            throw new ServiceException(ErrorCode.POST_DELETE_FORBIDDEN);
        }

        p.softDelete();
        log.info("[PostService] 게시글 삭제 완료 - postId={}", id);
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
