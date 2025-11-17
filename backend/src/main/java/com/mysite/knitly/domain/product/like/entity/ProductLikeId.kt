package com.mysite.knitly.domain.product.like.entity

import lombok.AllArgsConstructor
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.NoArgsConstructor
import java.io.Serializable

data class ProductLikeId(
    val user: Long = 0L,
    val product: Long = 0L
) : Serializable