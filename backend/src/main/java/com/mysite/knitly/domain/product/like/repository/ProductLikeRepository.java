package com.mysite.knitly.domain.product.like.repository;

import com.mysite.knitly.domain.product.like.entity.ProductLike;
import com.mysite.knitly.domain.product.like.entity.ProductLikeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLike, ProductLikeId> {
    Page<ProductLike> findByUser_UserId(Long userId, Pageable pageable);

    default void deleteByUserIdAndProductId(Long userId, Long productId) {
        ProductLikeId id = new ProductLikeId(userId, productId);
        deleteById(id);
    }
}
