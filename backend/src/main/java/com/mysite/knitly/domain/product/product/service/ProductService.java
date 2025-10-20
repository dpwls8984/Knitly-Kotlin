package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
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
    //private final UserRepository userRepository;
    private final DesignRepository designRepository;

    public ProductRegisterResponse registerProduct(User seller, Long designId, ProductRegisterRequest request) {
//        User seller = userRepository.findById(userId)
//                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new ServiceException(ErrorCode.DESIGN_NOT_FOUND));

        // 도안 등록시 [판매 중] 상태로 변경
        design.startSale();

        Product product = Product.builder()
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

        Product savedProduct = productRepository.save(product);
        return ProductRegisterResponse.from(savedProduct);
    }

    public ProductModifyResponse modifyProduct(User currentUser, Long productId, ProductModifyRequest request) {
        Product product = findProductById(productId);

        if (product.getIsDeleted()) {
            throw new ServiceException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
        }

        product.update(
                request.description(),
                request.productCategory(),
                request.sizeInfo(),
                request.stockQuantity()
        );

        return ProductModifyResponse.from(product);
    }

    public void deleteProduct(User currentUser, Long productId) {
        Product product = findProductById(productId);

        if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ServiceException(ErrorCode.PRODUCT_DELETE_UNAUTHORIZED);
        }

        // 소프트 딜리트 처리 (isDeleted = true)
        product.softDelete();

        // [판매중] 도안을 [판매 중지]로 변경
        product.getDesign().stopSale();
    }

    private Product findProductById(Long productId){
        return productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
