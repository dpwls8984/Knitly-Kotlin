package com.mysite.knitly.domain.product.like.dto;

import java.util.UUID;

public record LikeEventRequest(
        Long userId,
        Long productId
) {}
