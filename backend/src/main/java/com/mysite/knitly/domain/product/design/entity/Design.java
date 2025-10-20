package com.mysite.knitly.domain.product.design.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "designs")
@AllArgsConstructor
@Builder
public class Design {

    @Id
    private Long designId;

    @Column
    private String pdfUrl;

    @Column(nullable = false, columnDefinition = "ENUM('ON_SALE', 'STOPPED', 'BEFORE_SALE')")
    private String designState; // 'ON_SALE', 'STOPPED', 'BEFORE_SALE'

    @Column(nullable = false, length = 30)
    private String designName;
}

//CREATE TABLE `designs` (
//        `design__id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `pdf_url`	VARCHAR(255)	NULL,
//	`design_state`	ENUM('ON_SALE', 'STOPPED', 'BEFORE_SALE')	NOT NULL	DEFAULT BEFORE_SALE,
//	`design_name`	VARCHAR(30)	NOT NULL
//);