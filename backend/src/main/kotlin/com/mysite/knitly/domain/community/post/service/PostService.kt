package com.mysite.knitly.domain.community.post.service

import com.mysite.knitly.domain.community.post.dto.PostCreateRequest
import com.mysite.knitly.domain.community.post.dto.PostListItemResponse
import com.mysite.knitly.domain.community.post.dto.PostResponse
import com.mysite.knitly.domain.community.post.dto.PostUpdateRequest
import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.community.post.entity.PostCategory
import com.mysite.knitly.domain.community.post.repository.PostRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import com.mysite.knitly.global.util.ImageValidator
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository
) {

    private val log = LoggerFactory.getLogger(PostService::class.java)

    // 게시글 목록 조회
    fun getPostList(
        category: PostCategory?,
        query: String?,
        page: Int,
        size: Int
    ): Page<PostListItemResponse> {
        log.info(
            "[PostService] 게시글 목록 조회 요청 - category={}, query='{}', page={}, size={}",
            category, query, page, size
        )

        val pageable: Pageable = PageRequest.of(page, size)
        val posts: Page<Post> = postRepository.findListRows(category, query, pageable)

        val ids: List<Long> = posts.content.mapNotNull { it.id }

        val countMap: Map<Long, Long> =
            if (ids.isEmpty()) {
                emptyMap()
            } else {
                postRepository.countCommentsByPostIds(ids)
                    .associate { row ->
                        val postId = row[0] as Long
                        val cnt = row[1] as Long
                        postId to cnt
                    }
            }

        log.info("[PostService] 게시글 목록 조회 완료 - totalElements={}", posts.totalElements)

        return posts.map { p ->
            val exRaw = p.content ?: ""
            val cleaned = exRaw.replace(Regex("[\\s\\u00A0\\u3000]+"), "")
            val excerpt = when {
                cleaned.isEmpty() -> ""
                cleaned.length > 10 -> cleaned.substring(0, 10) + "..."
                else -> cleaned
            }

            val commentCount = countMap[p.id!!] ?: 0L
            val thumbnail = p.getThumbnail()

            PostListItemResponse(
                p.id!!,
                p.category,
                p.title,
                excerpt,
                "익명의 털실",
                p.createdAt,
                commentCount,
                thumbnail
            )
        }
    }

    // 게시글 상세 조회
    fun getPost(id: Long, currentUserOrNull: User?): PostResponse {
        log.info("[PostService] 게시글 상세 조회 - postId={}", id)

        val p: Post = postRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.POST_NOT_FOUND) }

        if (p.deleted) {
            log.warn("[PostService] 삭제된 게시글 접근 시도 - postId={}", id)
            throw ServiceException(ErrorCode.POST_NOT_FOUND)
        }

        val commentCount: Long = postRepository.countCommentsByPostId(id)
        val mine: Boolean = p.isAuthor(currentUserOrNull)

        log.info("[PostService] 게시글 조회 완료 - postId={}, commentCount={}", id, commentCount)

        return PostResponse(
            p.id!!,
            p.category,
            p.title,
            p.content,
            p.imageUrls,
            p.author.userId,
            "익명의 털실",
            p.createdAt,
            p.updatedAt,
            commentCount,
            mine
        )
    }

    // 게시글 생성
    @Transactional
    fun create(req: PostCreateRequest, currentUser: User?): PostResponse {
        log.info(
            "[PostService] 게시글 생성 요청 - userId={}, category={}, title='{}'",
            currentUser?.userId ?: "anonymous",
            req.category,
            req.title
        )

        if (currentUser == null) {
            log.warn("[PostService] 게시글 생성 실패 - 비인증 사용자 요청")
            throw ServiceException(ErrorCode.POST_UNAUTHORIZED)
        }

        // title 은 Java record 상 @NotBlank 이지만, 기존 검증 로직 유지
        if (req.title != null && req.title.length > 100) {
            log.warn(
                "[PostService] 게시글 생성 실패 - 제목 길이 초과 ({}자)",
                req.title.length
            )
            throw ServiceException(ErrorCode.POST_TITLE_LENGTH_INVALID)
        }

        val urls = normalizeUrls(req.imageUrls)
        if (urls.size > 5) {
            log.warn("[PostService] 게시글 생성 실패 - 이미지 개수 초과 ({})", urls.size)
            throw ServiceException(ErrorCode.POST_IMAGE_COUNT_EXCEEDED)
        }
        urls.forEach { u ->
            if (!ImageValidator.isAllowedImageUrl(u)) {
                log.warn("[PostService] 게시글 생성 실패 - 이미지 확장자 불허 ({})", u)
                throw ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID)
            }
        }

        val author: User = currentUser

        val post = Post(
            category = req.category,
            title = req.title,
            content = req.content,
            author = author
        )

        post.replaceImages(urls)

        val saved: Post = postRepository.save(post)
        log.info("[PostService] 게시글 생성 완료 - postId={}, images={}", saved.id, urls.size)

        return getPost(saved.id!!, currentUser)
    }

    // 게시글 수정
    @Transactional
    fun update(id: Long, req: PostUpdateRequest, currentUser: User?): PostResponse {
        log.info(
            "[PostService] 게시글 수정 요청 - userId={}, postId={}, title='{}'",
            currentUser?.userId ?: "anonymous",
            id,
            req.title
        )

        if (currentUser == null) {
            log.warn("[PostService] 게시글 수정 실패 - 비인증 사용자")
            throw ServiceException(ErrorCode.POST_UNAUTHORIZED)
        }

        if (req.title != null && req.title.length > 100) {
            log.warn(
                "[PostService] 게시글 수정 실패 - 제목 길이 초과 ({}자)",
                req.title.length
            )
            throw ServiceException(ErrorCode.POST_TITLE_LENGTH_INVALID)
        }

        val p: Post = postRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.POST_NOT_FOUND) }

        if (!p.isAuthor(currentUser)) {
            log.warn(
                "[PostService] 게시글 수정 실패 - 권한 없음 userId={}, authorId={}",
                currentUser.userId,
                p.author.userId
            )
            throw ServiceException(ErrorCode.POST_UPDATE_FORBIDDEN)
        }

        p.update(req.title, req.content, req.category)

        if (req.imageUrls != null) {
            val urls = normalizeUrls(req.imageUrls)
            if (urls.size > 5) {
                log.warn("[PostService] 게시글 수정 실패 - 이미지 개수 초과 ({})", urls.size)
                throw ServiceException(ErrorCode.POST_IMAGE_COUNT_EXCEEDED)
            }
            urls.forEach { u ->
                if (!ImageValidator.isAllowedImageUrl(u)) {
                    log.warn("[PostService] 게시글 수정 실패 - 이미지 확장자 불허 ({})", u)
                    throw ServiceException(ErrorCode.POST_IMAGE_EXTENSION_INVALID)
                }
            }
            p.replaceImages(urls)
        }

        log.info("[PostService] 게시글 수정 완료 - postId={}", p.id)
        return getPost(p.id!!, currentUser)
    }

    // 게시글 삭제
    @Transactional
    fun delete(id: Long, currentUser: User?) {
        log.info(
            "[PostService] 게시글 삭제 요청 - userId={}, postId={}",
            currentUser?.userId ?: "anonymous",
            id
        )

        if (currentUser == null) {
            log.warn("[PostService] 게시글 삭제 실패 - 비인증 사용자")
            throw ServiceException(ErrorCode.POST_UNAUTHORIZED)
        }

        val p: Post = postRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.POST_NOT_FOUND) }

        if (!p.isAuthor(currentUser)) {
            log.warn(
                "[PostService] 게시글 삭제 실패 - 권한 없음 userId={}, authorId={}",
                currentUser.userId,
                p.author.userId
            )
            throw ServiceException(ErrorCode.POST_DELETE_FORBIDDEN)
        }

        p.softDelete()
        log.info("[PostService] 게시글 삭제 완료 - postId={}", id)
    }

    private fun normalizeUrls(raw: List<String>?): List<String> {
        return raw
            ?.mapNotNull { it?.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
}
