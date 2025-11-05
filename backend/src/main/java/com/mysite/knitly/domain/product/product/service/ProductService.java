package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.dto.*;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final DesignRepository designRepository;
    private final RedisProductService redisProductService;
    private final FileStorageService fileStorageService;
    private final ProductLikeRepository productLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

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

// 1. 기존 이미지 URL 전체
        List<String> oldImageUrls = product.getProductImages().stream()
                .map(ProductImage::getProductImageUrl)
                .collect(Collectors.toList());

// 2. 유지할 기존 이미지 URL 목록 (프론트에서 전달된 값)
        List<String> existingImageUrls = request.existingImageUrls() != null
                ? request.existingImageUrls()
                : new ArrayList<>();

// 3. 삭제할 이미지 = oldImageUrls - existingImageUrls
        List<String> deletedImageUrls = oldImageUrls.stream()
                .filter(url -> !existingImageUrls.contains(url))
                .collect(Collectors.toList());

// 4. 새로운 이미지 파일을 저장
        List<ProductImage> newProductImages = saveProductImages(request.productImageUrls());

// 5. 유지할 기존 이미지 + 새 이미지 합치기
        List<ProductImage> mergedImages = new ArrayList<>();

// 기존 이미지 중 유지 대상만 다시 추가
        for (ProductImage oldImg : product.getProductImages()) {
            if (existingImageUrls.contains(oldImg.getProductImageUrl())) {
                mergedImages.add(oldImg);
            }
        }

// 새 이미지 추가
        mergedImages.addAll(newProductImages);

// 6. 엔티티 반영 (기존 이미지 중 유지 대상은 그대로, 삭제 대상은 orphanRemoval로 DB에서 제거)
        product.addProductImages(mergedImages);

// 7. 삭제할 이미지 파일 실제 삭제 (S3, 로컬 등)
        deletedImageUrls.forEach(fileStorageService::deleteFile);


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
            User user,
            ProductCategory category,
            ProductFilterType filterType,
            ProductSortType sortType,
            Pageable pageable) {

        long startTime = System.currentTimeMillis();
        Long userId = user != null ? user.getUserId() : null;

        log.info("[Product] [List] 상품 목록 조회 시작 - userId={}, category={}, filter={}, sort={}, page={}, size={}",
                userId, category, filterType, sortType, pageable.getPageNumber(), pageable.getPageSize());

        try{
            ProductFilterType effectiveFilter = (filterType == null) ? ProductFilterType.ALL : filterType;
            ProductCategory effectiveCategory =
                    (effectiveFilter == ProductFilterType.ALL) ? category : null;

            Page<Product> productPage;
            long dbStartTime = System.currentTimeMillis();

            if (sortType == ProductSortType.POPULAR) {
                log.debug("[Product] [List] 인기순 조회 시작 - effectiveCategory={}, effectiveFilter={}",
                        effectiveCategory, effectiveFilter);
                productPage = getProductsByPopular(effectiveCategory, effectiveFilter, pageable);
            } else {
                log.debug("[Product] [List] 일반 조회 시작 - effectiveCategory={}, effectiveFilter={}, sort={}",
                        effectiveCategory, effectiveFilter, sortType);
                Pageable sortedPageable = createPageable(pageable, sortType);
                productPage = getFilteredProducts(effectiveCategory, effectiveFilter, sortedPageable);
            }

            long dbDuration = System.currentTimeMillis() - dbStartTime;
            log.debug("[Product] [List] DB 조회 완료 - resultCount={}, dbDuration={}ms",
                    productPage.getTotalElements(), dbDuration);

            // '좋아요' 누른 상품 ID 목록을 한 번에 조회
            long likeStartTime = System.currentTimeMillis();
            Set<Long> likedProductIds = getLikedProductIds(user, productPage.getContent());
            long likeDuration = System.currentTimeMillis() - likeStartTime;

            log.debug("[Product] [List] 좋아요 정보 조회 완료 - likedCount={}, likeDuration={}ms",
                    likedProductIds.size(), likeDuration);

            // DTO 변환
            Page response = productPage.map(product ->
                    ProductListResponse.from(
                            product,
                            likedProductIds.contains(product.getProductId())
                    )
            );

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Product] [List] 상품 목록 조회 완료 - userId={}, totalCount={}, returnedCount={}, totalDuration={}ms",
                    userId, response.getTotalElements(), response.getNumberOfElements(), totalDuration);

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Product] [List] 상품 목록 조회 실패 - userId={}, category={}, duration={}ms",
                    userId, category, duration, e);
            throw e;
        }
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


    // 인기순 상품 조회 - Redis 활용
    private Page<Product> getProductsByPopular(
            ProductCategory category,
            ProductFilterType filterType,
            Pageable pageable) {

        long startTime = System.currentTimeMillis();
        log.debug("[Product] [Popular] 인기순 조회 시작 - category={}, filter={}",
                category, filterType);

        try{
            // Redis에서 인기 상품 ID 목록 가져오기
            long redisStartTime = System.currentTimeMillis();
            List<Long> topIds = redisProductService.getTopNPopularProducts(100);
            long redisDuration = System.currentTimeMillis() - redisStartTime;
            log.debug("[Product] [Popular] Redis 조회 완료 - count={}, redisDuration={}ms",
                    topIds.size(), redisDuration);

            List<Product> products;

            if (topIds.isEmpty()) {
                // Redis에 데이터가 없으면 DB에서 직접 조회 (이미지 포함)
                log.warn("[Product] [Popular] Redis 데이터 없음, DB에서 직접 조회");

                long dbStartTime = System.currentTimeMillis();
                Pageable top100 = PageRequest.of(0, 100, Sort.by("purchaseCount").descending());
                products = productRepository.findAllWithImagesAndNotDeleted(top100).getContent();
                long dbDuration = System.currentTimeMillis() - dbStartTime;

                log.debug("[Product] [Popular] DB 직접 조회 완료 - count={}, dbDuration={}ms",
                        products.size(), dbDuration);
            } else {
                // Redis에서 가져온 ID로 상품 조회 (이미지 포함)
                long dbStartTime = System.currentTimeMillis();
                List<Product> unordered = productRepository.findByProductIdInWithImagesAndNotDeleted(topIds);
                long dbDuration = System.currentTimeMillis() - dbStartTime;

                log.debug("[Product] [Popular] DB 상품 정보 조회 완료 - requestedCount={}, foundCount={}, dbDuration={}ms",
                        topIds.size(), unordered.size(), dbDuration);

                // Redis의 순서대로 정렬
                Map<Long, Product> productMap = unordered.stream()
                        .collect(Collectors.toMap(Product::getProductId, p -> p));

                products = topIds.stream()
                        .map(productMap::get)
                        .filter(Objects::nonNull)
                        .toList();

                log.debug("[Product] [Popular] Redis 순서 정렬 완료 - finalCount={}", products.size());
            }

            // 필터링 적용
            long filterStartTime = System.currentTimeMillis();
            int beforeFilterCount = products.size();
            products = products.stream()
                    .filter(p -> matchesCondition(p, category, filterType))
                    .toList();
            long filterDuration = System.currentTimeMillis() - filterStartTime;

            log.debug("[Product] [Popular] 필터링 완료 - beforeCount={}, afterCount={}, filterDuration={}ms",
                    beforeFilterCount, products.size(), filterDuration);


            Page result = convertToPage(products, pageable);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Product] [Popular] 인기순 조회 완료 - totalCount={}, pageSize={}, totalDuration={}ms",
                    result.getTotalElements(), result.getNumberOfElements(), totalDuration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Product] [Popular] 인기순 조회 실패 - category={}, duration={}ms",
                    category, duration, e);
            throw e;
        }
    }


    /**
     * 조건별 상품 조회 (이미지 포함)
     *
     * 1. 카테고리 조회: 특정 카테고리의 상품들
     * 2. 무료 상품 조회: 모든 카테고리의 무료 상품
     * 3. 한정판매 조회: 모든 카테고리의 한정판매 상품
     * 4. 전체 조회: 모든 상품
     */
    private Page<Product> getFilteredProducts(
            ProductCategory category,
            ProductFilterType filterType,
            Pageable pageable
    ) {

        long startTime = System.currentTimeMillis();
        log.debug("[Product] [Filter] 조건별 조회 시작 - category={}, filter={}",
                category, filterType);

        try{
            Page<Product> result;
            // 1. 카테고리 조회 (ALL)
            if (category != null) {
                result = productRepository.findByCategoryWithImagesAndNotDeleted(category, pageable);
                log.debug("[Product] [Filter] 카테고리 조회 - category={}, count={}",
                        category, result.getTotalElements());
            }
            // 2. 무료 상품 조회
            else if (filterType == ProductFilterType.FREE) {
                result = productRepository.findByPriceWithImagesAndNotDeleted(0.0, pageable);
                log.debug("[Product] [Filter] 무료 상품 조회 - count={}", result.getTotalElements());
            }
            // 3. 한정판매 조회
            else if (filterType == ProductFilterType.LIMITED) {
                result = productRepository.findLimitedWithImagesAndNotDeleted(pageable);
                log.debug("[Product] [Filter] 한정판매 조회 - count={}", result.getTotalElements());
            }
            // 4. 전체 조회
            else {
                result = productRepository.findAllWithImagesAndNotDeleted(pageable);
                log.debug("[Product] [Filter] 전체 조회 - count={}", result.getTotalElements());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Product] [Filter] 조건별 조회 완료 - category={}, filter={}, count={}, duration={}ms",
                    category, filterType, result.getTotalElements(), duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Product] [Filter] 조건별 조회 실패 - category={}, filter={}, duration={}ms",
                    category, filterType, duration, e);
            throw e;
        }
    }

    /**
     * 특정 유저의 판매 상품 목록 조회 (대표 이미지 포함)
     *
     * @param userId 판매자 ID
     * @param pageable 페이지네이션 정보
     * @return 상품 목록 (대표 이미지 포함)
     */
    public Page<ProductListResponse> findProductsByUserId(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        String sellerName= user.getName();
        // Repository에서 DTO로 조회
        Page<ProductWithThumbnailDto> dtoPage = productRepository.findByUserIdWithThumbnail(userId, pageable);

        // DTO -> Response 변환
        Page<ProductListResponse> responsePage = dtoPage.map(
                dto -> dto.toResponse(true, sellerName) // 찜한 목록이므로 isLikedByUser = true
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

        long reviewCount = reviewRepository.countByProductAndIsDeletedFalse(product);
        product.setReviewCount((int) reviewCount);

        return ProductDetailResponse.from(product, imageUrls, isLiked);
    }
}
