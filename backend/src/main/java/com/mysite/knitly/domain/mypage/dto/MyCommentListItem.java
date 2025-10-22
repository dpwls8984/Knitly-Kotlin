package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDate;

public record MyCommentListItem(
        Long commentId,
        Long postId,
        LocalDate createdDate,
        String preview
) {}
