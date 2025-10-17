package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.product.design.entity.Design;
import com.mysite.knitly.domain.product.design.repository.DesignRepositoryTmp;
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepositoryTmp userRepository;
    private final DesignRepositoryTmp designRepository;

    public ProductRegisterResponse registerProduct(UUID userId, Long designId, ProductRegisterRequest request) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new ServiceException(ErrorCode.DESIGN_NOT_FOUND));
        Product newProduct = Product.builder()
                .title(request.title())
                .description(request.description())
                .productCategory(request.productCategory())
                .sizeInfo(request.sizeInfo())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .user(seller) // 판매자 정보 연결
                .design(design) // 도안 정보 연결
                .isDeleted(false) // 초기 상태: 판매 중
                .purchaseCount(0) // 초기값 설정
                .likeCount(0) // 초기값 설정
                .build();

        Product savedProduct = productRepository.save(newProduct);
        return ProductRegisterResponse.from(savedProduct);
    }

    public ProductModifyResponse modifyProduct(UUID userId, Long productId, ProductModifyRequest request) {
        // 1. 상품 정보 조회
        Product product = findProductById(productId);

        // 2. (핵심) 상품 소유자의 ID와 현재 로그인한 사용자의 userId가 같은지 비교
        if (!product.getUser().getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
        }

        // 3. 엔티티 내부의 update 메서드를 호출하여 값 변경 (Dirty Checking 활용)
        product.update(
                request.description(),
                request.productCategory(),
                request.sizeInfo(),
                request.stockQuantity()
        );

        return ProductModifyResponse.from(product);
    }

    private Product findProductById(Long productId){
        return productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
