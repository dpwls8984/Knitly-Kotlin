package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PostUpdateRequest {

    @NotNull(message = "카테고리를 선택해 주세요.")
    private PostCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private String imageUrl;
}
