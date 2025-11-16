package com.mysite.knitly.domain.product.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository;
import com.mysite.knitly.domain.product.product.dto.*;
import com.mysite.knitly.domain.product.product.entity.*;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.review.repository.ReviewRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DesignRepository designRepository;
    private final RedisProductService redisProductService;
    private final LocalFileStorage localFileStorage;
    private final ProductLikeRepository productLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "product:detail:";
    private static final String POPULAR_LIST_CACHE_PREFIX = "product:list:popular:";

    @Transactional
    public ProductRegisterResponse registerProduct(User seller, Long designId, ProductRegisterRequest request) {
        log.info("[Product] [Register] 상품 등록 시작 - sellerId={}, designId={}, title={}",
                seller.getUserId(), designId, request.title());

        try {
            Design design = designRepository.findById(designId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.DESIGN_NOT_FOUND));
            log.debug("[Product] [Register] 도안 조회 완료 - designId={}", designId);

            design.startSale();
            log.debug("[Product] [Register] design.startSale() 호출");

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
            log.debug("[Product] [Register] Product 엔티티 빌드 완료");

            List<ProductImage> productImages = saveProductImages(request.productImageUrls());
            product.addProductImages(productImages);

            Product savedProduct = productRepository.save(product);
            log.debug("[Product] [Register] DB 상품 저장 완료");

            List<String> imageUrls = savedProduct.getProductImages().stream()
                    .map(ProductImage::getProductImageUrl)
                    .collect(Collectors.toList());

            log.info("[Product] [Register] 상품 등록 성공 - new productId={}", savedProduct.getProductId());

            return ProductRegisterResponse.from(savedProduct, imageUrls);
        } catch (Exception e) {
            log.error("[Product] [Register] 상품 등록 실패 - sellerId={}, designId={}",
                    seller.getUserId(), designId, e);
            throw e;
        }
    }


    @Transactional
    public ProductModifyResponse modifyProduct(User currentUser, Long productId, ProductModifyRequest request) {
        log.info("[Product] [Modify] 상품 수정 시작 - userId={}, productId={}",
                currentUser.getUserId(), productId);

        try {
            Product product = findProductById(productId);
            log.debug("[Product] [Modify] 상품 조회 완료 - productId={}", productId);

            if (product.getIsDeleted()) {
                log.warn("[Product] [Modify] 실패: 이미 삭제된 상품 - productId={}", productId);
                throw new ServiceException(ErrorCode.PRODUCT_ALREADY_DELETED);
            }

            if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
                log.warn("[Product] [Modify] 실패: 권한 없음 - userId={}, sellerId={}",
                        currentUser.getUserId(), product.getUser().getUserId());
                throw new ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
            }

            product.update(
                    request.description(),
                    request.productCategory(),
                    request.sizeInfo(),
                    request.stockQuantity()
            );
            log.debug("[Product] [Modify] 상품 엔티티 필드 업데이트 완료");

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

            log.debug("[Product] [Modify] 이미지 계산 - Old: {}, Existing: {}, To Delete: {}",
                    oldImageUrls.size(), existingImageUrls.size(), deletedImageUrls.size());

            // 4. 새로운 이미지 파일을 저장
            List<ProductImage> newProductImages = saveProductImages(request.productImageUrls());
            log.debug("[Product] [Modify] 새 이미지 {}개 임시 저장 완료.", newProductImages.size());

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
            log.debug("[Product] [Modify] 병합된 이미지 리스트 크기: {}", mergedImages.size());

            // 6. 엔티티 반영 (기존 이미지 중 유지 대상은 그대로, 삭제 대상은 orphanRemoval로 DB에서 제거)
            product.addProductImages(mergedImages);
            log.debug("[Product] [Modify] product.addProductImages (orphanRemoval) 호출");

            // 7. 삭제할 이미지 파일 실제 삭제
            if (!deletedImageUrls.isEmpty()) {
                log.debug("[Product] [Modify] 스토리지에서 {}개의 이미지 파일 삭제 시작...", deletedImageUrls.size());
                deletedImageUrls.forEach(localFileStorage::deleteProductImage);
                log.debug("[Product] [Modify] 스토리지 이미지 삭제 완료");
            }

            List<String> currentImageUrls = product.getProductImages().stream()
                    .map(ProductImage::getProductImageUrl)
                    .collect(Collectors.toList());

            log.info("[Product] [Modify] 상품 수정 성공 - productId={}", product.getProductId());

            return ProductModifyResponse.from(product, currentImageUrls);
        } catch (Exception e) {
            log.error("[Product] [Modify] 상품 수정 실패 - userId={}, productId={}",
                    currentUser.getUserId(), productId, e);
            throw e;
        }
    }

    @Transactional
        public void deleteProduct(User currentUser, Long productId) {
        log.info("[Product] [Delete] 상품 삭제 시작 - userId={}, productId={}",
                currentUser.getUserId(), productId);

        try {
            Product product = findProductById(productId);

            if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
                log.warn("[Product] [Delete] 실패: 권한 없음 - userId={}, sellerId={}",
                        currentUser.getUserId(), product.getUser().getUserId());
                throw new ServiceException(ErrorCode.PRODUCT_DELETE_UNAUTHORIZED);
            }

            product.softDelete();
            log.debug("[Product] [Delete] 상품 softDelete() 호출");

            product.getDesign().stopSale();
            log.debug("[Product] [Delete] design.stopSale() 호출");

            log.info("[Product] [Delete] 상품 삭제(Soft) 성공 - productId={}", productId);

        } catch (Exception e) {
            log.error("[Product] [Delete] 상품 삭제 실패 - userId={}, productId={}",
                    currentUser.getUserId(), productId, e);
            throw e;
        }
    }

    @Transactional
    public void relistProduct(User currentUser, Long productId) {
        log.info("[Product] [Relist] 상품 재판매 시작 - userId={}, productId={}",
                currentUser.getUserId(), productId);

        try {
            Product product = findProductById(productId);

            if (!product.getUser().getUserId().equals(currentUser.getUserId())) {
                log.warn("[Product] [Relist] 실패: 권한 없음 - userId={}, sellerId={}",
                        currentUser.getUserId(), product.getUser().getUserId());
                throw new ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
            }

            product.relist();
            log.debug("[Product] [Relist] product.relist() 호출");
            product.getDesign().relist();
            log.debug("[Product] [Relist] design.relist() 호출");

            log.info("[Product] [Relist] 상품 재판매 성공 - productId={}", productId);

        } catch (Exception e) {
            log.error("[Product] [Relist] 상품 재판매 실패 - userId={}, productId={}",
                    currentUser.getUserId(), productId, e);
            throw e;
        }
    }

    private List<ProductImage> saveProductImages(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            log.debug("[Product] [ImageSave] 저장할 이미지 파일 없음.");
            return new ArrayList<>();
        }

        int fileCount = (int) imageFiles.stream().filter(f -> !f.isEmpty()).count();
        log.debug("[Product] [ImageSave] 이미지 저장 시작 - fileCount={}", fileCount);

        try {
            List<ProductImage> productImages = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                if (file.isEmpty()) continue;

                log.trace("[Product] [ImageSave] 파일 처리 중: {}", file.getOriginalFilename());
                String url = localFileStorage.saveProductImage(file);
                log.trace("[Product] [ImageSave] 스토리지 저장 완료 - URL: {}", url);

                ProductImage productImage = ProductImage.builder()
                        .productImageUrl(url)
                        .build();
                productImages.add(productImage);
            }
            log.debug("[Product] [ImageSave] 이미지 저장 완료 - savedCount={}", productImages.size());
            return productImages;
        } catch (Exception e) {
            log.error("[Product] [ImageSave] 이미지 저장 실패 - fileCount={}", fileCount, e);
            throw new ServiceException(ErrorCode.FILE_STORAGE_FAILED); // 예외 전환
        }
    }

    private Product findProductById(Long productId){
        return productRepository.findById(productId)
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

            // 캐시 대상 여부 (비로그인 + 인기순만 캐시)
            boolean cacheable = (user == null) && (sortType == ProductSortType.POPULAR);

            String popularCacheKey = null;
            if (cacheable) {
                popularCacheKey = buildPopularListCacheKey(effectiveCategory, effectiveFilter, pageable);

                try {
                    String cachedJson = redisTemplate.opsForValue().get(popularCacheKey);
                    if (cachedJson != null) {
                        ProductListPageCache cached =
                                objectMapper.readValue(cachedJson, ProductListPageCache.class);

                        log.info("[Product] [List] [Popular] 캐시 히트 - key={}", popularCacheKey);

                        return new PageImpl<>(
                                cached.getContent(),
                                pageable,
                                cached.getTotalElements()
                        );
                    }
                } catch (Exception e) {
                    log.error("[Product] [List] [Popular] 캐시 읽기 실패 - key={}, error={}",
                            popularCacheKey, e.getMessage(), e);
                }
            }

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
            Page<ProductListResponse> response = productPage.map(product ->
                    ProductListResponse.from(
                            product,
                            likedProductIds.contains(product.getProductId())
                    )
            );

            // 캐시 쓰기
            if (cacheable && popularCacheKey != null) {
                try {
                    ProductListPageCache cache = new ProductListPageCache(
                            response.getContent(),
                            response.getTotalElements()
                    );
                    String jsonData = objectMapper.writeValueAsString(cache);
                    redisTemplate
                            .opsForValue()
                            .set(popularCacheKey, jsonData, Duration.ofSeconds(60)); // 60초 캐시 유지

                    log.info("[Product] [List] [Popular] 캐시 쓰기 완료 - key={}", popularCacheKey);
                } catch (Exception e) {
                    log.error("[Product] [List] [Popular] 캐시 쓰기 실패 - key={}, error={}",
                            popularCacheKey, e.getMessage(), e);
                }
            }

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

                long sortFilterStartTime = System.currentTimeMillis();

                Map<Long, Product> productMap = new HashMap<>(unordered.size());
                for (Product product : unordered) {
                    productMap.put(product.getProductId(), product);
                }

                // 정렬 + 필터링을 한 번에 처리
                products = new ArrayList<>(topIds.size());
                int filteredCount = 0;

                for (Long id : topIds) {
                    Product product = productMap.get(id);
                    if (product != null && matchesCondition(product, category, filterType)) {
                        products.add(product);
                        filteredCount++;
                    }
                }

                long sortFilterDuration = System.currentTimeMillis() - sortFilterStartTime;

                log.debug("[Product] [Popular] 정렬+필터링 완료 - beforeCount={}, afterCount={}, duration={}ms",
                        topIds.size(), filteredCount, sortFilterDuration);
            }

            Page<Product> result = convertToPage(products, pageable);

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
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(User user, Long productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;
        Long userId = (user != null) ? user.getUserId() : null;
        log.info("[Product] [Detail] 상품 상세 조회 시작 - cacheKey={}, userId={}", cacheKey, userId);

        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("[Service] [Cache] 캐시 히트 - key={}", cacheKey);
                return objectMapper.readValue(cachedData, ProductDetailResponse.class);
            }
        } catch (Exception e) {
            log.error("[Service] [Cache] 캐시 읽기 실패 - key={}, error={}", cacheKey, e.getMessage(), e);
        }


        // 판매 중지된 상품은 조회 불가
        try{
            log.info("[Service] [DB] 캐시 미스(Miss) - DB 조회 - key={}", cacheKey);
            Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

            log.debug("[Product] [Detail] [DB] 상품 조회 완료");

            if (product.getIsDeleted()) {
                log.warn("[Product] [Detail] [DB] 실패: 삭제된 상품 - productId={}", productId);
                throw new ServiceException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            List<String> imageUrls = product.getProductImages().stream()
                    .map(ProductImage::getProductImageUrl)
                    .collect(Collectors.toList());

            boolean isLiked = false;
            if (user != null) {
                isLiked = productLikeRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId);
                log.debug("[Product] [Detail] [DB] '좋아요' 상태 확인 완료 - isLiked={}", isLiked);
            }

            long reviewCount = reviewRepository.countByProductAndIsDeletedFalse(product);
            product.setReviewCount((int) reviewCount);
            log.debug("[Product] [Detail] [DB] 리뷰 개수 카운트 완료 - count={}", reviewCount);
            ProductDetailResponse response = ProductDetailResponse.from(product, imageUrls, isLiked);

            try {
                String jsonData = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, jsonData, Duration.ofHours(1));
                log.info("[Service] [Cache] 캐시 쓰기(Write) 완료 - key={}", cacheKey);
            } catch (Exception e) {
                log.error("[Service] [Cache] 캐시 쓰기 실패 - key={}, error={}", cacheKey, e.getMessage(), e);
            }

            log.info("[Product] [Detail] [DB] 상품 상세 조회 완료 - productId={}", productId);
            return response;

        } catch (Exception e) {
            log.error("[Product] [Detail] 상품 상세 조회 실패 - productId={}", productId, e);
            throw e;
        }
    }

    private String buildPopularListCacheKey(
            ProductCategory category,
            ProductFilterType filterType,
            Pageable pageable
    ) {
        String categoryPart = (category != null) ? category.name() : "ALL";
        String filterPart = (filterType != null) ? filterType.name() : "ALL";

        return POPULAR_LIST_CACHE_PREFIX +
                "category=" + categoryPart +
                ":filter=" + filterPart +
                ":page=" + pageable.getPageNumber() +
                ":size=" + pageable.getPageSize();
    }

}
