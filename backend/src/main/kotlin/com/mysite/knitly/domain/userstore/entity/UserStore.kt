package com.mysite.knitly.domain.userstore.entity

import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "user_stores")
class UserStore(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false)
    val storeId: Long = 0,

    // store_detail - TEXT, NULL
    @Lob
    @Column(name = "store_detail")
    var storeDetail: String? = null,

    // 1:1 관계 매핑: user_id 컬럼을 통해 User 엔티티와 연결
    // UserStore가 관계의 주인(Owning Side)이 됩니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null
) {
    // 편의 생성자 - User.kt의 @PostPersist에서 사용
    constructor(user: User, storeDetail: String) : this(
        storeId = 0,
        user = user,
        storeDetail = storeDetail
    )

    // 업데이트 메서드
    fun updateStoreDetail(storeDetail: String) {
        this.storeDetail = storeDetail
    }
}
