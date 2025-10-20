package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import jakarta.validation.constraints.*;
import java.util.UUID;
import java.util.List;

public record PostCreateRequest(

    @NotNull(message = "카테고리를 선택해 주세요.")
    PostCategory category,

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    String content,

    @Size(max = 5, message = "이미지는 최대 5개까지 업로드할 수 있습니다.")
    List<String> imageUrls,

    @NotNull(message = "작성자 ID는 필수입니다.")
    UUID authorId
) {}