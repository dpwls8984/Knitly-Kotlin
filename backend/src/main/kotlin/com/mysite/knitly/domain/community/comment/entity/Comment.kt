package com.mysite.knitly.domain.community.comment.entity

import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.jpa.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.Where

@Entity
@Table(name = "comments")
@Where(clause = "is_deleted = false")
class Comment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val id: Long? = null,

    @Lob
    @Column(nullable = false)
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val author: User,  // 작성자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post, // 어떤 게시글의 댓글인지 생성 시 결정

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: Comment? = null, // parent도 생성 시점에만 설정

    @OneToMany(
        mappedBy = "parent",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    val children: MutableList<Comment> = mutableListOf(),

    @Column(name = "is_deleted", nullable = false)
    var deleted: Boolean = false

) : BaseTimeEntity() {

    fun isRoot(): Boolean {
        return parent == null
    }

    fun softDelete() {
        deleted = true
    }

    fun update(newContent: String) {
        this.content = newContent
    }

    fun isAuthor(user: User?): Boolean {
        return user != null && author.userId == user.userId
    }
}
