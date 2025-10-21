package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.product.dto.*;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileNameUtils;
import com.mysite.knitly.global.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DesignRepository designRepository;

    String uploadDir = "resources/static/product/";
    String urlPrefix = "/resources/static/product/";

    //@Transactional
    public ProductRegisterResponse registerProduct(User seller, Long designId, ProductRegisterRequest request) {

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

        List<ProductImage> productImages = saveProductImages(request.productImageUrls());
        product.addProductImages(productImages);

        Product savedProduct = productRepository.save(product);

        List<String> imageUrls = savedProduct.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

        return ProductRegisterResponse.from(savedProduct, imageUrls);
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

        // 2. 이미지 업데이트 로직 추가 (기존 이미지 삭제 후 새로 저장)
        // TODO: 기존 파일 시스템의 이미지 파일 삭제 로직 추가 필요
        List<ProductImage> productImages = saveProductImages(request.productImageUrls());
        product.addProductImages(productImages);

        // 변경 감지(Dirty Checking)에 의해 product가 자동으로 업데이트됨

        List<String> imageUrls = product.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

        return ProductModifyResponse.from(product, imageUrls);
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

    @Transactional
    public void relistProduct(User currentUser, Long productId) {
        Product product = findProductById(productId);

        if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED); // 수정 권한 에러 재사용
        }

        // 3. Product와 Design 상태를 '판매 중'으로 원복
        product.relist(); // Product의 isDeleted를 false로 변경
        product.getDesign().relist(); // Design의 designState를 ON_SALE으로 변경
    }

    private List<ProductImage> saveProductImages(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return new ArrayList<>();
        }

        new File(uploadDir).mkdirs(); // 업로드 디렉토리 생성
        List<ProductImage> productImages = new ArrayList<>();

        for (int i = 0; i < imageFiles.size(); i++) {
            MultipartFile file = imageFiles.get(i);
            if (file.isEmpty()) continue;

            // 확장자 검증 (팀원의 유틸리티 클래스 활용)
            String originalFilename = file.getOriginalFilename();
            if (!ImageValidator.isAllowedImageUrl(originalFilename)) {
                throw new ServiceException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
            }

            try {
                // 고유한 파일명 생성 및 저장
                String filename = UUID.randomUUID() + "_" + FileNameUtils.sanitize(originalFilename);
                Path path = Paths.get(uploadDir, filename);
                Files.write(path, file.getBytes());

                String url = urlPrefix + filename;

                ProductImage productImage = ProductImage.builder()
                        .productImageUrl(url)
                        // .sortOrder(i) // ProductImage에 sortOrder 필드가 있다면 추가
                        .build();
                productImages.add(productImage);

            } catch (IOException e) {
                throw new ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED); // TODO: Product용 에러 코드로 변경 추천
            }
        }
        return productImages;
    }

    private Product findProductById(Long productId){
        return productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
