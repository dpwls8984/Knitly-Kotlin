package com.mysite.knitly.domain.design.entity;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "designs")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long designId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesignState designState;

    @Column(nullable = false, length = 30)
    private String designName;

    @Column(name = "grid_data", columnDefinition = "JSON", nullable = false)
    private String gridData;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 삭제 가능 여부 확인 - BEFORE_SALE 상태인 경우에만 삭제 가능
    public boolean isDeletable() {
        return this.designState == DesignState.BEFORE_SALE;
    }

    // 도안 작성자 확인 - userId 비교
    public boolean isOwnedBy(Long userId) {
        return this.user.getUserId().equals(userId);
    }

    // 최초 판매 시작 메서드
    // 오직 BEFORE_SALE 상태에서만 호출 가능
    // 판매 전 -> 판매 중으로 변경
    public void startSale() {
        if (this.designState != DesignState.BEFORE_SALE) {
            throw new ServiceException(ErrorCode.DESIGN_ALREADY_ON_SALE);
        }
        this.designState = DesignState.ON_SALE;
    }

    // 판매 중 -> 판매 중지 메서드
    // 오직 ON_SALE 상태에서만 호출 가능
    public void stopSale() {
        if( this.designState != DesignState.ON_SALE) {
            throw new ServiceException(ErrorCode.DESIGN_NOT_ON_SALE);
        }
        this.designState = DesignState.STOPPED;
    }

    // 판매 중지 -> 판매 재개 메서드
    // 오직 STOPPED 상태에서만 호출 가능
    public void relist() {
        if (this.designState != DesignState.STOPPED) {
            throw new ServiceException(ErrorCode.DESIGN_NOT_STOPPED);
        }
        this.designState = DesignState.ON_SALE;
    }
}


//CREATE TABLE `designs` (
//        `design_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `pdf_url`	VARCHAR(255)	NULL,
//	`design_state`	ENUM('ON_SALE', 'STOPPED', 'BEFORE_SALE')	NOT NULL	DEFAULT BEFORE_SALE,
//	`design_name`	VARCHAR(30)	NOT NULL
//);