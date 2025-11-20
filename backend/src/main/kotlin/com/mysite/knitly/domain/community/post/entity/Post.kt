package com.mysite.knitly.domain.community.post.entity

import com.mysite.knitly.domain.community.comment.entity.Comment
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.jpa.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.Where

@Entity
@Table(name = "posts")
@Where(clause = "is_deleted = false")
class Post(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    val id: Long? = null, // PK는 생성 후 변경되지 않음

    @Column(nullable = false, length = 100)
    var title: String,

    @Lob
    @Column(nullable = false)
    var content: String,

    @ElementCollection
    @CollectionTable(
        name = "post_images",
        joinColumns = [JoinColumn(name = "post_id")]
    )
    @Column(name = "url", nullable = false, length = 512)
    @OrderColumn(name = "sort_order")
    val imageUrls: MutableList<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(
        name = "post_category",
        nullable = false,
        columnDefinition = "ENUM('FREE','QUESTION','TIP')"
    )
    var category: PostCategory,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val author: User, // 작성자는 변경되지 않음

    @Column(name = "is_deleted", nullable = false)
    var deleted: Boolean = false,

    @OneToMany(
        mappedBy = "post",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val comments: MutableList<Comment> = mutableListOf()

) : BaseTimeEntity() {

    fun getThumbnail(): String? {
        return if (imageUrls.isEmpty()) null else imageUrls[0]
    }

    fun promoteThumbnail(index: Int) {
        if (index <= 0 || index >= imageUrls.size) return
        val picked = imageUrls.removeAt(index)
        imageUrls.add(0, picked)
    }

    fun promoteThumbnailByUrl(url: String?) {
        if (url == null) return
        val idx = imageUrls.indexOf(url)
        if (idx > 0) promoteThumbnail(idx)
    }

    fun addComment(comment: Comment) {
        comments.add(comment)
    }

    fun softDelete() {
        deleted = true
    }

    fun update(title: String, content: String, category: PostCategory) {
        this.title = title
        this.content = content
        this.category = category
    }

    fun replaceImages(newUrls: List<String>?) {
        imageUrls.clear()
        if (newUrls != null) {
            imageUrls.addAll(newUrls)
        }
    }

    fun isAuthor(user: User?): Boolean {
        return user != null && author.userId == user.userId
    }
}
