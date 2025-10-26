package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.dto.*;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DesignRepository designRepository;
    private final RedisProductService redisProductService;
    private final FileStorageService fileStorageService;
    private final ProductLikeRepository productLikeRepository;

    @Transactional
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

    @Transactional
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

        // 1. 삭제할 기존 이미지 파일들의 URL을 미리 확보
        List<String> oldImageUrls = product.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

        // 2. 새로운 이미지 파일들을 저장하고 ProductImage 엔티티 리스트를 생성
        List<ProductImage> newProductImages = saveProductImages(request.productImageUrls());

        // 3. Product 엔티티의 이미지 리스트를 교체 (orphanRemoval=true에 의해 기존 DB 레코드는 자동 삭제됨), 엔티티에 반영
        product.addProductImages(newProductImages);

        // 4. 기존 이미지 파일들을 파일 시스템에서 삭제
        oldImageUrls.forEach(fileStorageService::deleteFile);

        List<String> currentImageUrls = product.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

        return ProductModifyResponse.from(product, currentImageUrls);
    }

    @Transactional
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

        List<ProductImage> productImages = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            if (file.isEmpty()) continue;

            // FileStorageService에게 "product" 도메인의 파일 저장을 위임하고 URL만 받음
            String url = fileStorageService.storeFile(file, "product");

            ProductImage productImage = ProductImage.builder()
                    .productImageUrl(url)
                    .build();
            productImages.add(productImage);
        }
        return productImages;
    }

    private Product findProductById(Long productId){
        return productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 상품 목록 조회
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(
            User user, // 컨트롤러에서 받은 User 객체
            ProductCategory category,
            ProductFilterType filterType,
            ProductSortType sortType,
            Pageable pageable) {

        ProductFilterType effectiveFilter = (filterType == null) ? ProductFilterType.ALL : filterType;

        ProductCategory effectiveCategory =
                (effectiveFilter == ProductFilterType.ALL) ? category : null;

        Page<Product> productPage;

        if (sortType == ProductSortType.POPULAR) {
            productPage = getProductsByPopular(effectiveCategory, effectiveFilter, pageable);
        } else {
            Pageable sortedPageable = createPageable(pageable, sortType);
            productPage = getFilteredProducts(effectiveCategory, effectiveFilter, sortedPageable);
        }

        // '좋아요' 누른 상품 ID 목록을 한 번에 조회
        Set<Long> likedProductIds = getLikedProductIds(user, productPage.getContent());

        // DTO의 from(Product, boolean) 메서드를 사용하여 변환
        return productPage.map(product ->
                ProductListResponse.from(
                        product,
                        likedProductIds.contains(product.getProductId())
                )
        );
    }

    private Set<Long> getLikedProductIds(User user, List<Product> products) {
        // 1. 비로그인 사용자이거나 상품 목록이 비어있으면 빈 Set 반환
        if (user == null || products.isEmpty()) {
            return Collections.emptySet();
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .toList();

        // 3. [수정] 제공된 레포지토리의 메서드명과 User의 ID 필드명(userId)을 사용
        //    (user.getId() -> user.getUserId()로 가정)
        return productLikeRepository.findLikedProductIdsByUserId(
                user.getUserId(), // user.userId 필드에 접근
                productIds
        );
    }


    // 인기순 - Redis 활용
    private Page<Product> getProductsByPopular(
            ProductCategory category,
            ProductFilterType filterType,
            Pageable pageable) {

        // Redis에서 인기순 목록 가져오기
        List<Long> popularIds = redisProductService.getTopNPopularProducts(1000);

        if (popularIds.isEmpty()) {
            Pageable popularPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("purchaseCount").descending()
            );
            return getFilteredProducts(category, filterType, popularPageable);
        }

        // Redis에서 가져온 ID로 DB 조회
        List<Product> allProducts = productRepository.findByProductIdInAndIsDeletedFalse(popularIds);

        // FREE/LIMITED가 오면 카테고리 무시
        boolean filterFree = (filterType == ProductFilterType.FREE);
        boolean filterLimited = (filterType == ProductFilterType.LIMITED);

        // 필터 적용
        List<Product> filtered = allProducts.stream().filter(p -> {
            if (filterFree) return Double.compare(p.getPrice(), 0.0) == 0;
            if (filterLimited) return p.getStockQuantity() != null;
            if (category != null) return p.getProductCategory() == category;
            return true;
        }).toList();

        // Redis 순서 유지하며 정렬
        Map<Long, Product> productMap = filtered.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        List<Product> sorted = popularIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 페이징 처리
        return convertToPage(sorted, pageable);
    }

    /**
     * 조회 조건에 따른 상품 조회
     * 1. 카테고리 조회: 해당 카테고리만
     * 2. 무료 조회: 모든 카테고리의 무료 상품
     * 3. 한정판매 조회: 모든 카테고리의 한정판매 상품
     * 4. 전체 조회: 모든 상품
     */
    private Page<Product> getFilteredProducts(
            ProductCategory category,
            ProductFilterType filterType,
            Pageable pageable
    ) {

        // 1. 카테고리 조회 (ALL)
        if (category != null) {
            return productRepository.findByProductCategoryAndIsDeletedFalse(category, pageable);
        }

        // 2. 무료 상품 조회 (카테고리 무관)
        if (filterType == ProductFilterType.FREE) {
            return productRepository.findByPriceAndIsDeletedFalse(0.0, pageable);
        }

        // 3. 한정판매 조회 (카테고리 무관)
        if (filterType == ProductFilterType.LIMITED) {
            return productRepository.findByStockQuantityIsNotNullAndIsDeletedFalse(pageable);
        }

        // 4. 전체 조회
        return productRepository.findByIsDeletedFalse(pageable);
    }

    /**
     * 특정 유저의 판매 상품 목록 조회 (대표 이미지 포함)
     *
     * @param userId 판매자 ID
     * @param pageable 페이지네이션 정보
     * @return 상품 목록 (대표 이미지 포함)
     */
    public Page<ProductListResponse> findProductsByUserId(Long userId, Pageable pageable) {

        // Repository에서 DTO로 조회
        Page<ProductWithThumbnailDto> dtoPage = productRepository.findByUserIdWithThumbnail(userId, pageable);

        // DTO -> Response 변환
        Page<ProductListResponse> responsePage = dtoPage.map(
                dto -> dto.toResponse(true) // 찜한 목록이므로 isLikedByUser = true
        );

        return responsePage;
    }

    // 정렬 조건 생성
    private Pageable createPageable(Pageable pageable, ProductSortType sortType) {
        Sort sort = switch (sortType) {
            case LATEST -> Sort.by("createdAt").descending();
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            default -> Sort.unsorted();
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    // 상품이 조회 조건이 맞는지 확인
    private boolean matchesCondition(Product product, ProductCategory category, ProductFilterType filterType) {
        // 카테고리 조회
        if (category != null) {
            return product.getProductCategory().equals(category);
        }

        // 무료 상품 조회
        if (filterType == ProductFilterType.FREE) {
            return product.getPrice() == 0.0;
        }

        // 한정판매 조회
        if (filterType == ProductFilterType.LIMITED) {
            return product.getStockQuantity() != null;
        }

        // 전체 조회
        return true;
    }

    // 페이징 처리
    private Page<Product> convertToPage(List<Product> products, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), products.size());

        if (start > products.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, products.size());
        }

        List<Product> pageContent = products.subList(start, end);

        return new PageImpl<>(pageContent, pageable, products.size());
    }

    // 상품 상세 조회 로직 추가
    @Transactional(readOnly = true) // 데이터를 읽기만 하므로 readOnly=true로 성능 최적화
    public ProductDetailResponse getProductDetail(User user, Long productId) {
        // N+1 방지를 위해 User, Design 등 연관 정보를 함께 가져오는 것이 좋음
        Product product = productRepository.findProductDetailById(productId) // 예시 메서드
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        // 판매 중지된 상품은 조회 불가
        if (product.getIsDeleted()) {
            throw new ServiceException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<String> imageUrls = product.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

        boolean isLiked = false;
        if (user != null) {
            Long userId = user.getUserId();

            isLiked = productLikeRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId);
        }

        return ProductDetailResponse.from(product, imageUrls, isLiked);
    }
}
