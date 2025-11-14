package com.mysite.knitly.domain.product.like.dto;

import java.io.Serializable;

public record LikeEventRequest(Long userId, Long productId) implements Serializable {}
