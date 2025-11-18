package com.mysite.knitly.domain.user.entity

import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.userstore.entity.UserStore
import com.mysite.knitly.global.jpa.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "users")
open class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Long = 0,  // knitly 서비스 내에서의 키값

    @Column(nullable = false, unique = true)
    val socialId: String,  // 구글의 고유 ID (sub)

    @Column(nullable = false)
    val email: String,  // 구글 이메일

    @Column(nullable = false, length = 50)
    val name: String,  // 구글에서 받아온 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val provider: Provider  // GOOGLE
) : BaseTimeEntity() {

    // 🔥 연관 관계에 CascadeType.REMOVE 추가
    @OneToMany(mappedBy = "user", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    lateinit var designs: MutableList<Design>

    @OneToMany(mappedBy = "user", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    lateinit var products: MutableList<Product>

    @OneToMany(mappedBy = "author", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    lateinit var posts: MutableList<Post>

    @OneToMany(mappedBy = "user", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    lateinit var reviews: MutableList<Review>

    @OneToOne(
        mappedBy = "user",
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true
    )
    var userStore: UserStore? = null

    // UserStore 초기화 메서드
    @PostPersist
    fun initializeUserStore() {
        if (userStore == null) {
            userStore = UserStore(
                user = this,
                storeDetail = "안녕하세요! 제 스토어에 오신 것을 환영합니다."
            )
        }
    }

    companion object {
        // 정적 팩토리 메서드
        @JvmStatic
        fun createGoogleUser(socialId: String, email: String, name: String): User {
            return User(
                socialId = socialId,
                email = email,
                name = name,
                provider = Provider.GOOGLE
            )
        }

        // Builder 패턴
        @JvmStatic
        fun builder(): UserBuilder {
            return UserBuilder()
        }
    }

    // Builder 클래스
    class UserBuilder {
        private var userId: Long = 0
        private var socialId: String = ""
        private var email: String = ""
        private var name: String = ""
        private var provider: Provider? = null

        fun userId(userId: Long) = apply { this.userId = userId }
        fun socialId(socialId: String) = apply { this.socialId = socialId }
        fun email(email: String) = apply { this.email = email }
        fun name(name: String) = apply { this.name = name }
        fun provider(provider: Provider) = apply { this.provider = provider }

        fun build(): User {
            require(provider != null) { "Provider must not be null" }
            return User(
                userId = userId,
                socialId = socialId,
                email = email,
                name = name,
                provider = provider!!
            )
        }
    }
}