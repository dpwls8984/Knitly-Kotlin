package com.mysite.knitly.domain.mypage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record MyPostListItemResponse(
        Long id,
        String title,
        String excerpt,
        String thumbnailUrl,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss",
                timezone = "Asia/Seoul"
        )
        LocalDateTime createdAt
) {}
