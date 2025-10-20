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
import com.mysite.knitly.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 댓글 목록
    public Page<CommentTreeResponse> getComments(Long postId, String sort, int page, int size, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Page<Comment> roots = ("desc".equalsIgnoreCase(sort))
                ? commentRepository.findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtDesc(post, pageable)
                : commentRepository.findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtAsc(post, pageable);

        Map<Long, Integer> authorNoMap = buildAuthorNoMap(postId);

        return roots.map(root -> toTreeResponse(root, currentUserId, authorNoMap));
    }

    // 댓글 개수
    public long count(Long postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }

    // 댓글 작성
    @Transactional
    public CommentResponse create(CommentCreateRequest req) {
        Post post = postRepository.findById(req.postId())
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));
        User author = userRepository.findById(req.authorId())
                .orElseThrow(() -> new ServiceException(ErrorCode.BAD_REQUEST));

        // parentId가 있으면 동일 게시글 소속인지 검증
        Comment parent = null;
        if (req.parentId() != null) {
            parent = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));
            if (!parent.getPost().getId().equals(req.postId())) {
                throw new ServiceException(ErrorCode.BAD_REQUEST);
            }
        }

        Comment saved = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(author)
                        .content(req.content())
                        .parent(parent)
                        .build()
        );

        Map<Long, Integer> authorNoMap = buildAuthorNoMap(req.postId());
        return toFlatResponse(saved, author.getUserId(), authorNoMap);
    }

    // 댓글 수정
    @Transactional
    public void update(Long commentId, CommentUpdateRequest req, Long currentUserId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new ServiceException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (c.getAuthor() == null || !c.getAuthor().getUserId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }
        c.update(req.content());
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long commentId, Long currentUserId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new ServiceException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (c.getAuthor() == null || !c.getAuthor().getUserId().equals(currentUserId)) {
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
    private CommentResponse toFlatResponse(Comment c, Long currentUserId, Map<Long, Integer> authorNoMap) {
        Long uid = (c.getAuthor() == null) ? null : c.getAuthor().getUserId();
        int no = (uid != null && authorNoMap.containsKey(uid)) ? authorNoMap.get(uid) : 0;
        String display = (no > 0) ? "익명의 털실 " + no : "익명의 털실";

        boolean mine = (currentUserId != null && uid != null && uid.equals(currentUserId));

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
    private CommentTreeResponse toTreeResponse(Comment root, Long currentUserId, Map<Long, Integer> authorNoMap) {
        // 자식(대댓글) 조회
        List<Comment> children = commentRepository.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(root.getId());

        return new CommentTreeResponse(
                root.getId(),
                root.getContent(),
                root.getAuthor() == null ? null : root.getAuthor().getUserId(),
                displayName(root, authorNoMap),
                root.getCreatedAt(),
                isMine(root, currentUserId),
                root.getParent() == null ? null : root.getParent().getId(),
                children.stream()
                        .map(ch -> new CommentTreeResponse(
                                ch.getId(),
                                ch.getContent(),
                                ch.getAuthor() == null ? null : ch.getAuthor().getUserId(),
                                displayName(ch, authorNoMap),
                                ch.getCreatedAt(),
                                isMine(ch, currentUserId),
                                ch.getParent() == null ? null : ch.getParent().getId(),
                                List.of() // 2-depth까지만 응답
                        ))
                        .collect(Collectors.toList())
        );
    }

    private boolean isMine(Comment c, Long currentUserId) {
        Long uid = (c.getAuthor() == null) ? null : c.getAuthor().getUserId();
        return currentUserId != null && uid != null && uid.equals(currentUserId);
    }

    private String displayName(Comment c, Map<Long, Integer> authorNoMap) {
        Long uid = (c.getAuthor() == null) ? null : c.getAuthor().getUserId();
        int no = (uid != null && authorNoMap.containsKey(uid)) ? authorNoMap.get(uid) : 0;
        return (no > 0) ? "익명의 털실 " + no : "익명의 털실";
    }
}
