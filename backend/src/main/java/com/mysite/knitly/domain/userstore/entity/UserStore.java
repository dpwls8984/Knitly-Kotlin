package com.mysite.knitly.domain.userstore.entity;

import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    // store_detail - TEXT, NULL
    @Lob
    @Column(name = "store_detail")
    private String storeDetail;

    // 1:1 관계 매핑: user_id 컬럼을 통해 User 엔티티와 연결
    // UserStore가 관계의 주인(Owning Side)이 됩니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 업데이트 메서드 추가
    public void updateStoreDetail(String storeDetail) {
        this.storeDetail = storeDetail;
    }
}
