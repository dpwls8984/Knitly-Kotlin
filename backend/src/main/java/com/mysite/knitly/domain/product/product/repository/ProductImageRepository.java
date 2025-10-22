package com.mysite.knitly.domain.product.product.repository;

import com.mysite.knitly.domain.product.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
