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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    // 댓글 목록 - 전체 조회 후 서버에서 트리 구성(루트만 페이징)
    public Page<CommentTreeResponse> getComments(Long postId, String sort, int page, int size, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new ServiceException(ErrorCode.POST_NOT_FOUND);
        }

        // 1) 해당 게시글의 모든 댓글을 한 번에 조회(등록순 정렬)
        List<Comment> all = commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId);

        // 2) parentId -> children 매핑
        Map<Long, List<Comment>> byParent = new HashMap<>();
        List<Comment> roots = all.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        for (Comment c : all) {
            Long pid = (c.getParent() == null) ? null : c.getParent().getId();
            if (pid == null) continue;
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }

        // children 정렬(등록순 기본)
        byParent.values().forEach(list -> list.sort(Comparator.comparing(Comment::getCreatedAt)));

        // 루트 정렬
        roots.sort(Comparator.comparing(Comment::getCreatedAt));
        if ("desc".equalsIgnoreCase(sort)) {
            Collections.reverse(roots);
            byParent.values().forEach(Collections::reverse);
        }

        // 3) 익명 넘버링 맵(게시글 작성자 제외)
        Map<Long, Integer> authorNoMap = buildAuthorNoMap(postId, post.getAuthor().getUserId());

        // 4) 루트 레벨만 페이징
        Pageable pageable = PageRequest.of(page, size);
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), roots.size());
        int to = Math.min(from + pageable.getPageSize(), roots.size());
        List<Comment> rootSlice = (from < to) ? roots.subList(from, to) : Collections.emptyList();

        // 5) 재귀 변환
        List<CommentTreeResponse> content = rootSlice.stream()
                .map(r -> toTreeRecursive(r, currentUser, authorNoMap, byParent))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, roots.size());
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

        Map<Long, Integer> authorNoMap = buildAuthorNoMap(req.postId(), post.getAuthor().getUserId());
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

    // 게시글 작성자는 번호 매핑에서 제외 (번호 없이 "익명의 털실")
    private Map<Long, Integer> buildAuthorNoMap(Long postId, Long postAuthorId) {
        List<Long> order = commentRepository.findAuthorOrderForPost(postId);
        Map<Long, Integer> map = new HashMap<>();
        int n = 1;
        for (Long uid : order) {
            if (uid == null) continue;
            if (uid.equals(postAuthorId)) continue; // 게시글 작성자 제외
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
    // 재귀 트리 변환 (임의 깊이)
    private CommentTreeResponse toTreeRecursive(
            Comment node,
            User currentUser,
            Map<Long, Integer> authorNoMap,
            Map<Long, List<Comment>> byParent
    ) {
        List<Comment> children = byParent.getOrDefault(node.getId(), Collections.emptyList());

        List<CommentTreeResponse> childDtos = children.stream()
                .map(ch -> toTreeRecursive(ch, currentUser, authorNoMap, byParent))
                .collect(Collectors.toList());

        return new CommentTreeResponse(
                node.getId(),
                node.getContent(),
                node.getAuthor() == null ? null : node.getAuthor().getUserId(),
                displayName(node, authorNoMap),
                node.getCreatedAt(),
                isMine(node, currentUser),
                node.getParent() == null ? null : node.getParent().getId(),
                childDtos
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
