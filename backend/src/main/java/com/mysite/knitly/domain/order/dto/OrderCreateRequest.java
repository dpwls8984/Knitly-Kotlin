package com.mysite.knitly.domain.order.dto;

import java.util.List;

public record OrderCreateRequest(
   List<Long> productIds
) {
}
