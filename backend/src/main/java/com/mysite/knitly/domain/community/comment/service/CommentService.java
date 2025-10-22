package com.mysite.knitly.domain.community.comment.service;

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest;
import com.mysite.knitly.domain.community.comment.dto.CommentResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse;
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest;
import com.mysite.knitly.domain.community.comment.entity.Comment;
import com.mysite.knitly.domain.community.comment.repository.CommentRepository;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.repository.PostRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    // 댓글 목록
    public Page<CommentTreeResponse> getComments(Long postId, String sort, int page, int size, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Page<Comment> roots = ("desc".equalsIgnoreCase(sort))
                ? commentRepository.findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtDesc(post, pageable)
                : commentRepository.findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtAsc(post, pageable);

        Map<Long, Integer> authorNoMap = buildAuthorNoMap(postId);
        // N+1 제거
        List<Long> parentIds = roots.getContent().stream()
                .map(Comment::getId)
                .toList();
        Map<Long, List<Comment>> childrenMap = parentIds.isEmpty()
                ? Map.of()
                : commentRepository.findByParentIdInAndDeletedFalseOrderByCreatedAtAsc(parentIds)
                .stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return roots.map(root ->
                toTreeResponseWithGroupedChildren(root, currentUser, authorNoMap, childrenMap)
        );

    }

    // 댓글 개수
    public long count(Long postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }

    // 댓글 작성
    @Transactional
    public CommentResponse create(CommentCreateRequest req, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.COMMENT_UNAUTHORIZED);
        }
        Post post = postRepository.findById(req.postId())
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));
        // 인증 사용자 그대로 사용
        User author = currentUser;

        // parentId가 있으면 동일 게시글 소속인지 검증
        Comment parent = null;
        if (req.parentId() != null) {
            parent = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));
            if (!parent.getPost().getId().equals(req.postId())) {
                throw new ServiceException(ErrorCode.BAD_REQUEST);
            }
        }

        // content trim & 공백만 입력 금지
        String trimmed = req.content() == null ? null : req.content().trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        Comment saved = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(author)
                        .content(trimmed)
                        .parent(parent)
                        .build()
        );

        Map<Long, Integer> authorNoMap = buildAuthorNoMap(req.postId());
        return toFlatResponse(saved, currentUser, authorNoMap);
    }

    // 댓글 수정
    @Transactional
    public void update(Long commentId, CommentUpdateRequest req, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.COMMENT_UNAUTHORIZED);
        }
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new ServiceException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (!c.isAuthor(currentUser)) {
            throw new ServiceException(ErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }

        // content trim & 공백만 입력 금지
        String trimmed = req.content() == null ? null : req.content().trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }
        c.update(trimmed);
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long commentId, User currentUser) {
        if (currentUser == null) {
            throw new ServiceException(ErrorCode.COMMENT_UNAUTHORIZED);
        }
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new ServiceException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (!c.isAuthor(currentUser)) {
            throw new ServiceException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
        }
        c.softDelete();
    }

    // 작성자 첫 댓글 시간 기준으로
    private Map<Long, Integer> buildAuthorNoMap(Long postId) {
        List<Long> order = commentRepository.findAuthorOrderForPost(postId);
        Map<Long, Integer> map = new HashMap<>();
        int n = 1;
        for (Long uid : order) {
            map.put(uid, n++);
        }
        return map;
    }

    // create 응답
    private CommentResponse toFlatResponse(Comment c, User currentUser, Map<Long, Integer> authorNoMap) {
        Long uid = (c.getAuthor() == null) ? null : c.getAuthor().getUserId();
        int no = (uid != null && authorNoMap.containsKey(uid)) ? authorNoMap.get(uid) : 0;
        String display = (no > 0) ? "익명의 털실 " + no : "익명의 털실";

        boolean mine = c.isAuthor(currentUser);

        return new CommentResponse(
                c.getId(),
                c.getContent(),
                uid,
                display,
                c.getCreatedAt(),
                mine
        );
    }
    // 트리 응답 변환
    private CommentTreeResponse toTreeResponseWithGroupedChildren(
            Comment root,
            User currentUser,
            Map<Long, Integer> authorNoMap,
            Map<Long, List<Comment>> childrenMap
    ) {
        List<Comment> children = childrenMap.getOrDefault(root.getId(), List.of());
        return new CommentTreeResponse(
                root.getId(),
                root.getContent(),
                root.getAuthor() == null ? null : root.getAuthor().getUserId(),
                displayName(root, authorNoMap),
                root.getCreatedAt(),
                isMine(root, currentUser),
                root.getParent() == null ? null : root.getParent().getId(),
                children.stream()
                        .map(ch -> new CommentTreeResponse(
                                ch.getId(),
                                ch.getContent(),
                                ch.getAuthor() == null ? null : ch.getAuthor().getUserId(),
                                displayName(ch, authorNoMap),
                                ch.getCreatedAt(),
                                isMine(ch, currentUser),
                                ch.getParent() == null ? null : ch.getParent().getId(),
                                List.of()
                        ))
                        .collect(Collectors.toList())
        );
    }

    private boolean isMine(Comment c, User currentUser) {
        return c.isAuthor(currentUser);
    }

    private String displayName(Comment c, Map<Long, Integer> authorNoMap) {
        Long uid = (c.getAuthor() == null) ? null : c.getAuthor().getUserId();
        int no = (uid != null && authorNoMap.containsKey(uid)) ? authorNoMap.get(uid) : 0;
        return (no > 0) ? "익명의 털실 " + no : "익명의 털실";
    }
}
