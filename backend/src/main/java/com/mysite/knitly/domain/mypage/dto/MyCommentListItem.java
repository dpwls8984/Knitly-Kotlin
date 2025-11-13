package com.mysite.knitly.domain.mypage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record MyCommentListItem(
        Long commentId,
        Long postId,

        // 문자열(ISO)로 내려주고 KST 적용
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss",
                timezone = "Asia/Seoul")
        LocalDateTime createdAt,

        String preview
) {}
