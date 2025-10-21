package com.mysite.knitly.domain.product.like.repository;

import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductLikeRepository extends CrudRepository<ProductLike, ProductLikeId> {
}
