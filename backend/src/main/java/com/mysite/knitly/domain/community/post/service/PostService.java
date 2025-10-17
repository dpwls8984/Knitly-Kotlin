package com.mysite.knitly.domain.community.post.service;

import com.mysite.knitly.domain.community.common.util.Anonymizer;
import com.mysite.knitly.domain.community.common.util.ImageValidator;
import com.mysite.knitly.domain.community.post.dto.*;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.community.post.repository.UserRepositoryTmp;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 상세 기능:  mine(현재 사용자 = 작성자)
// 등록,수정 기능, 이미지 확장자 검증(png/jpg/jpeg) + 엔티티 저장/수정
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepositoryTmp userRepository;

    public Page<PostListItem> getPostList(PostCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostListRow> rows = postRepository.findListRows(category, pageable);

        return rows.map(row -> {
            String ex = (row.excerpt == null) ? "" :
                    (row.excerpt.length() > 100 ? row.excerpt.substring(0, 100) : row.excerpt);

            return PostListItem.builder()
                    .id(row.id)
                    .category(row.category)
                    .title(row.title)
                    .excerpt(ex)
                    .authorDisplay(Anonymizer.yarn(row.authorId))
                    .createdAt(row.createdAt)
                    .commentCount(row.commentCount)
                    .thumbnailUrl(row.imageUrl)
                    .build();
        });
    }

    public PostResponse getPost(Long id, UUID currentUserIdOrNull) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        long commentCount = (p.getComments() == null) ? 0 : p.getComments().size();

        boolean mine = currentUserIdOrNull != null
                && p.getAuthor() != null
                && p.getAuthor().getUserId().equals(currentUserIdOrNull);

        return PostResponse.builder()
                .id(p.getId())
                .category(p.getCategory())
                .title(p.getTitle())
                .content(p.getContent())
                .imageUrl(p.getImageUrl())
                .authorId(p.getAuthor().getUserId())
                .authorDisplay(Anonymizer.yarn(p.getAuthor().getUserId()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .commentCount(commentCount)
                .mine(mine)
                .build();
    }

    @Transactional
    public PostResponse create(PostCreateRequest req) {
        if (!ImageValidator.isAllowedImageUrl(req.getImageUrl())) {
            throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
        }

        User author = userRepository.findByUserId(req.getAuthorId())
                .orElseThrow(() -> new ServiceException(ErrorCode.BAD_REQUEST));

        Post post = Post.builder()
                .category(req.getCategory())
                .title(req.getTitle())
                .content(req.getContent())
                .imageUrl(req.getImageUrl())
                .author(author)
                .build();

        Post saved = postRepository.save(post);
        return getPost(saved.getId(), author.getUserId());
    }

    @Transactional
    public PostResponse update(Long id, PostUpdateRequest req, UUID currentUserId) {
        if (!ImageValidator.isAllowedImageUrl(req.getImageUrl())) {
            throw new ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID);
        }

        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (p.getAuthor() == null || !p.getAuthor().getUserId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        p.update(req.getTitle(), req.getContent(), req.getImageUrl(), req.getCategory());
        return getPost(p.getId(), currentUserId);
    }

    @Transactional
    public void delete(Long id, UUID currentUserId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        if (p.getAuthor() == null || !p.getAuthor().getUserId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.POST_DELETE_FORBIDDEN);
        }

        p.softDelete();
    }
}
