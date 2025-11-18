package com.mysite.knitly.domain.community.comment.service

import com.mysite.knitly.domain.community.comment.dto.CommentCreateRequest
import com.mysite.knitly.domain.community.comment.dto.CommentResponse
import com.mysite.knitly.domain.community.comment.dto.CommentTreeResponse
import com.mysite.knitly.domain.community.comment.dto.CommentUpdateRequest
import com.mysite.knitly.domain.community.comment.entity.Comment
import com.mysite.knitly.domain.community.comment.repository.CommentRepository
import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.community.post.repository.PostRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository
) {

    private val logger = LoggerFactory.getLogger(CommentService::class.java)

    fun getComments(postId: Long, sort: String, page: Int, size: Int, currentUser: User?): Page<CommentTreeResponse> {
        logger.info("[CommentService] 댓글 목록 조회 요청 - postId={}, sort={}, page={}, size={}", postId, sort, page, size)

        val post: Post = postRepository.findById(postId)
            .orElseThrow { ServiceException(ErrorCode.POST_NOT_FOUND) }

        if (post.deleted) {
            logger.warn("[CommentService] 삭제된 게시글의 댓글 조회 시도 - postId={}", postId)
            throw ServiceException(ErrorCode.POST_NOT_FOUND)
        }

        val all: List<Comment> = commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId)

        val byParent: MutableMap<Long, MutableList<Comment>> = HashMap()
        val roots: MutableList<Comment> = all
            .filter { it.parent == null }
            .toMutableList()

        for (c in all) {
            val pid = c.parent?.id
            if (pid == null) continue
            byParent.computeIfAbsent(pid) { mutableListOf() }.add(c)
        }

        byParent.values.forEach { list ->
            list.sortBy { it.createdAt }
        }

        roots.sortBy { it.createdAt }
        if (sort.equals("desc", ignoreCase = true)) {
            roots.reverse()
            byParent.values.forEach { it.reverse() }
        }

        val authorNoMap = buildAuthorNoMap(postId, post.author.userId)

        val pageable: Pageable = PageRequest.of(page, size)
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(roots.size)
        val to = (from + pageable.pageSize).coerceAtMost(roots.size)
        val rootSlice: List<Comment> =
            if (from < to) roots.subList(from, to) else emptyList()

        val content: List<CommentTreeResponse> = rootSlice
            .map { r -> toTreeRecursive(r, currentUser, authorNoMap, byParent) }

        logger.info(
            "[CommentService] 댓글 목록 조회 완료 - postId={}, roots={}, returned={}",
            postId,
            roots.size,
            content.size
        )

        return PageImpl(content, pageable, roots.size.toLong())
    }

    fun count(postId: Long): Long {
        val cnt = commentRepository.countByPostIdAndDeletedFalse(postId)
        logger.info("[CommentService] 댓글 개수 조회 - postId={}, count={}", postId, cnt)
        return cnt
    }

    @Transactional
    fun create(req: CommentCreateRequest, currentUser: User?): CommentResponse {
        logger.info("[CommentService] 댓글 작성 요청 - postId={}, parentId={}", req.postId, req.parentId)

        if (currentUser == null) {
            logger.warn("[CommentService] 댓글 작성 실패 - 비인증 사용자")
            throw ServiceException(ErrorCode.COMMENT_UNAUTHORIZED)
        }

        val post: Post = postRepository.findById(req.postId)
            .orElseThrow { ServiceException(ErrorCode.POST_NOT_FOUND) }

        var parent: Comment? = null
        val parentId = req.parentId
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                .orElseThrow { ServiceException(ErrorCode.COMMENT_NOT_FOUND) }

            if (parent.post.id != req.postId) {
                logger.warn(
                    "[CommentService] 댓글 작성 실패 - 서로 다른 게시글(parent/postId 불일치) parentId={}, postId={}",
                    parentId,
                    req.postId
                )
                throw ServiceException(ErrorCode.BAD_REQUEST)
            }
        }

        val trimmed = req.content.trim()
        if (trimmed.isBlank()) {
            logger.warn("[CommentService] 댓글 작성 실패 - 공백 또는 null 내용")
            throw ServiceException(ErrorCode.BAD_REQUEST)
        }

        val saved = commentRepository.save(
            Comment(
                content = trimmed,
                author = currentUser,
                post = post,
                parent = parent
            )
        )

        val authorNoMap = buildAuthorNoMap(req.postId, post.author.userId)
        val resp = toFlatResponse(saved, currentUser, authorNoMap)

        logger.info("[CommentService] 댓글 작성 완료 - postId={}, commentId={}", req.postId, saved.id)
        return resp
    }

    @Transactional
    fun update(commentId: Long, req: CommentUpdateRequest, currentUser: User?) {
        logger.info("[CommentService] 댓글 수정 요청 - commentId={}", commentId)

        if (currentUser == null) {
            logger.warn("[CommentService] 댓글 수정 실패 - 비인증 사용자")
            throw ServiceException(ErrorCode.COMMENT_UNAUTHORIZED)
        }

        val c: Comment = commentRepository.findById(commentId)
            .orElseThrow { ServiceException(ErrorCode.COMMENT_NOT_FOUND) }

        if (c.deleted) {
            logger.warn("[CommentService] 댓글 수정 실패 - 이미 삭제된 댓글 commentId={}", commentId)
            throw ServiceException(ErrorCode.COMMENT_ALREADY_DELETED)
        }

        if (!c.isAuthor(currentUser)) {
            logger.warn(
                "[CommentService] 댓글 수정 실패 - 권한 없음 userId={}, authorId={}",
                currentUser.userId,
                c.author.userId
            )
            throw ServiceException(ErrorCode.COMMENT_UPDATE_FORBIDDEN)
        }

        val trimmed = req.content.trim()
        if (trimmed.isBlank()) {
            logger.warn("[CommentService] 댓글 수정 실패 - 공백 또는 null 내용 commentId={}", commentId)
            throw ServiceException(ErrorCode.BAD_REQUEST)
        }

        c.update(trimmed)
        logger.info("[CommentService] 댓글 수정 완료 - commentId={}", commentId)
    }

    @Transactional
    fun delete(commentId: Long, currentUser: User?) {
        logger.info("[CommentService] 댓글 삭제 요청 - commentId={}", commentId)

        if (currentUser == null) {
            logger.warn("[CommentService] 댓글 삭제 실패 - 비인증 사용자")
            throw ServiceException(ErrorCode.COMMENT_UNAUTHORIZED)
        }

        val c: Comment = commentRepository.findById(commentId)
            .orElseThrow { ServiceException(ErrorCode.COMMENT_NOT_FOUND) }

        if (c.deleted) {
            logger.warn("[CommentService] 댓글 삭제 실패 - 이미 삭제된 댓글 commentId={}", commentId)
            throw ServiceException(ErrorCode.COMMENT_ALREADY_DELETED)
        }

        if (!c.isAuthor(currentUser)) {
            logger.warn(
                "[CommentService] 댓글 삭제 실패 - 권한 없음 userId={}, authorId={}",
                currentUser.userId,
                c.author.userId
            )
            throw ServiceException(ErrorCode.COMMENT_DELETE_FORBIDDEN)
        }

        c.softDelete()
        logger.info("[CommentService] 댓글 삭제 완료 - commentId={}", commentId)
    }

    private fun buildAuthorNoMap(postId: Long, postAuthorId: Long): Map<Long, Int> {
        val order: List<Long> = commentRepository.findAuthorOrderForPost(postId)
        val map: MutableMap<Long, Int> = HashMap()
        var n = 1
        for (uid in order) {
            if (uid == null) continue
            if (uid == postAuthorId) continue
            map[uid] = n++
        }
        return map
    }

    private fun toFlatResponse(
        c: Comment,
        currentUser: User?,
        authorNoMap: Map<Long, Int>
    ): CommentResponse {
        val commentId = c.id ?: throw IllegalStateException("Comment id is null")
        val uid = c.author.userId
        val no = authorNoMap[uid] ?: 0
        val display = if (no > 0) "익명의 털실 $no" else "익명의 털실"
        val mine = c.isAuthor(currentUser)

        return CommentResponse(
            commentId,
            c.content,
            uid,
            display,
            c.createdAt,
            mine
        )
    }

    private fun toTreeRecursive(
        node: Comment,
        currentUser: User?,
        authorNoMap: Map<Long, Int>,
        byParent: Map<Long, List<Comment>>
    ): CommentTreeResponse {
        val nodeId = node.id ?: throw IllegalStateException("Comment id is null")
        val children: List<Comment> = byParent[nodeId] ?: emptyList()

        val childDtos: List<CommentTreeResponse> = children
            .map { ch -> toTreeRecursive(ch, currentUser, authorNoMap, byParent) }

        return CommentTreeResponse(
            nodeId,
            node.content,
            node.author.userId,
            displayName(node, authorNoMap),
            node.createdAt,
            isMine(node, currentUser),
            node.parent?.id,
            childDtos
        )
    }

    private fun isMine(c: Comment, currentUser: User?): Boolean {
        return c.isAuthor(currentUser)
    }

    private fun displayName(c: Comment, authorNoMap: Map<Long, Int>): String {
        val uid = c.author.userId
        val no = authorNoMap[uid] ?: 0
        return if (no > 0) "익명의 털실 $no" else "익명의 털실"
    }
}
