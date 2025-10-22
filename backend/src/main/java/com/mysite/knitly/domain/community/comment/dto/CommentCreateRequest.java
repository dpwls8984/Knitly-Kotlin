package com.mysite.knitly.domain.community.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record CommentCreateRequest(

        @NotNull(message = "게시글 ID는 필수입니다.")
        Long postId,

        @Positive(message = "parentId는 1 이상의 값이어야 합니다.")
        Long parentId,

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(min = 1, max = 300, message = "댓글은 1자 이상 300자 이하로 입력해 주세요.")
        String content
) {}
