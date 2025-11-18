package com.mysite.knitly.domain.product.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.dto.*
import com.mysite.knitly.domain.product.product.entity.*

import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.collections.contains

@Service
@Transactional
class ProductService (
    private val productRepository: ProductRepository,
    private val designRepository: DesignRepository,
    private val redisProductService: RedisProductService,
    private val localFileStorage: LocalFileStorage,
    private val productLikeRepository: ProductLikeRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ProductService::class.java)

    companion object {
        private const val CACHE_KEY_PREFIX = "product:detail:"
        private const val POPULAR_LIST_CACHE_PREFIX = "product:list:popular:"
    }

    @Transactional
    fun registerProduct(seller: User, designId: Long, request: ProductRegisterRequest): ProductRegisterResponse {
        log.info(
            "[Product] [Register] 상품 등록 시작 - sellerId={}, designId={}, title={}",
            seller.userId, designId, request.title
        )

        try {
            val design = designRepository.findById(designId)
                .orElseThrow { ServiceException(ErrorCode.DESIGN_NOT_FOUND) }
            log.debug("[Product] [Register] 도안 조회 완료 - designId={}", designId)

            design.startSale()
            log.debug("[Product] [Register] design.startSale() 호출")

            val product = Product(
                title = request.title,
                description = request.description,
                productCategory = request.productCategory,
                sizeInfo = request.sizeInfo,
                price = request.price,
                stockQuantity = request.stockQuantity,
                user = seller,
                design = design,
                isDeleted = false,
                purchaseCount = 0,
                likeCount = 0
            )
            log.debug("[Product] [Register] Product 엔티티 빌드 완료")

            val productImages = saveProductImages(request.productImageUrls)
            product.addProductImages(productImages)

            val savedProduct = productRepository.save(product)
            log.debug("[Product] [Register] DB 상품 저장 완료")

            val imageUrls = savedProduct.productImages.map { it.productImageUrl }

            log.info("[Product] [Register] 상품 등록 성공 - new productId={}", savedProduct.productId)

            return ProductRegisterResponse.from(savedProduct, imageUrls)
        } catch (e: Exception) {
            log.error(
                "[Product] [Register] 상품 등록 실패 - sellerId={}, designId={}",
                seller.userId, designId, e
            )
            throw e
        }
    }

    @Transactional
    fun modifyProduct(currentUser: User, productId: Long, request: ProductModifyRequest): ProductModifyResponse {
        log.info(
            "[Product] [Modify] 상품 수정 시작 - userId={}, productId={}",
            currentUser.userId, productId
        )

        try {
            val product = findProductById(productId)
            log.debug("[Product] [Modify] 상품 조회 완료 - productId={}", productId)

            if (product.isDeleted) {
                log.warn("[Product] [Modify] 실패: 이미 삭제된 상품 - productId={}", productId)
                throw ServiceException(ErrorCode.PRODUCT_ALREADY_DELETED)
            }

            if (product.user.userId != currentUser.userId) {
                log.warn(
                    "[Product] [Modify] 실패: 권한 없음 - userId={}, sellerId={}",
                    currentUser.userId, product.user.userId
                )
                throw ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED)
            }

            product.update(
                request.description,
                request.productCategory,
                request.sizeInfo,
                request.stockQuantity
            )
            log.debug("[Product] [Modify] 상품 엔티티 필드 업데이트 완료")

            // 1. 기존 이미지 URL 전체
            val oldImageUrls = product.productImages.map { it.productImageUrl }

            // 2. 유지할 기존 이미지 URL 목록 (프론트에서 전달된 값)
            val existingImageUrls = request.existingImageUrls ?: emptyList()

            // 3. 삭제할 이미지 = oldImageUrls - existingImageUrls
            val deletedImageUrls = oldImageUrls.filter { it !in existingImageUrls }

            log.debug(
                "[Product] [Modify] 이미지 계산 - Old: {}, Existing: {}, To Delete: {}",
                oldImageUrls.size, existingImageUrls.size, deletedImageUrls.size
            )

            // 4. 새로운 이미지 파일을 저장
            val newProductImages = saveProductImages(request.productImageUrls)
            log.debug("[Product] [Modify] 새 이미지 {}개 임시 저장 완료.", newProductImages.size)

            // 5. 유지할 기존 이미지 + 새 이미지 합치기
            val mergedImages = product.productImages
                .filter { it.productImageUrl in existingImageUrls }
                .toMutableList()
            mergedImages.addAll(newProductImages)

            log.debug("[Product] [Modify] 병합된 이미지 리스트 크기: {}", mergedImages.size)

            // 6. 엔티티 반영 (orphanRemoval)
            product.addProductImages(mergedImages)
            log.debug("[Product] [Modify] product.addProductImages (orphanRemoval) 호출")

            // 7. 삭제할 이미지 파일 실제 삭제
            if (deletedImageUrls.isNotEmpty()) {
                log.debug("[Product] [Modify] 스토리지에서 {}개의 이미지 파일 삭제 시작...", deletedImageUrls.size)
                deletedImageUrls.forEach { fileUrl -> localFileStorage.deleteProductImage(fileUrl) }
                log.debug("[Product] [Modify] 스토리지 이미지 삭제 완료")
            }

            val currentImageUrls = product.productImages.map { it.productImageUrl }

            log.info("[Product] [Modify] 상품 수정 성공 - productId={}", product.productId)

            return ProductModifyResponse.from(product, currentImageUrls)
        } catch (e: Exception) {
            log.error(
                "[Product] [Modify] 상품 수정 실패 - userId={}, productId={}",
                currentUser.userId, productId, e
            )
            throw e
        }
    }

    @Transactional
    fun deleteProduct(currentUser: User, productId: Long) {
        log.info(
            "[Product] [Delete] 상품 삭제 시작 - userId={}, productId={}",
            currentUser.userId, productId
        )

        try {
            val product = findProductById(productId)

            if (product.user.userId != currentUser.userId) {
                log.warn(
                    "[Product] [Delete] 실패: 권한 없음 - userId={}, sellerId={}",
                    currentUser.userId, product.user.userId
                )
                throw ServiceException(ErrorCode.PRODUCT_DELETE_UNAUTHORIZED)
            }

            product.softDelete()
            log.debug("[Product] [Delete] 상품 softDelete() 호출")

            product.design.stopSale()
            log.debug("[Product] [Delete] design.stopSale() 호출")

            log.info("[Product] [Delete] 상품 삭제(Soft) 성공 - productId={}", productId)
        } catch (e: Exception) {
            log.error(
                "[Product] [Delete] 상품 삭제 실패 - userId={}, productId={}",
                currentUser.userId, productId, e
            )
            throw e
        }
    }

    @Transactional
    fun relistProduct(currentUser: User, productId: Long) {
        log.info(
            "[Product] [Relist] 상품 재판매 시작 - userId={}, productId={}",
            currentUser.userId, productId
        )

        try {
            val product = findProductById(productId)

            if (product.user.userId != currentUser.userId) {
                log.warn(
                    "[Product] [Relist] 실패: 권한 없음 - userId={}, sellerId={}",
                    currentUser.userId, product.user.userId
                )
                throw ServiceException(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED)
            }

            product.relist()
            log.debug("[Product] [Relist] product.relist() 호출")
            product.design.relist()
            log.debug("[Product] [Relist] design.relist() 호출")

            log.info("[Product] [Relist] 상품 재판매 성공 - productId={}", productId)
        } catch (e: Exception) {
            log.error(
                "[Product] [Relist] 상품 재판매 실패 - userId={}, productId={}",
                currentUser.userId, productId, e
            )
            throw e
        }
    }

    private fun saveProductImages(imageFiles: List<MultipartFile>?): List<ProductImage> {
        if (imageFiles.isNullOrEmpty()) {
            log.debug("[Product] [ImageSave] 저장할 이미지 파일 없음.")
            return emptyList()
        }

        val validFiles = imageFiles.filter { !it.isEmpty }
        val fileCount = validFiles.size
        log.debug("[Product] [ImageSave] 이미지 저장 시작 - fileCount={}", fileCount)

        try {
            val productImages = validFiles.map { file ->
                log.trace("[Product] [ImageSave] 파일 처리 중: {}", file.originalFilename)
                val url = localFileStorage.saveProductImage(file)
                log.trace("[Product] [ImageSave] 스토리지 저장 완료 - URL: {}", url)

                ProductImage(productImageUrl = url)
            }
            log.debug("[Product] [ImageSave] 이미지 저장 완료 - savedCount={}", productImages.size)
            return productImages
        } catch (e: Exception) {
            log.error("[Product] [ImageSave] 이미지 저장 실패 - fileCount={}", fileCount, e)
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }
    }

    private fun findProductById(productId: Long): Product {
        return productRepository.findById(productId)
            .orElseThrow { ServiceException(ErrorCode.PRODUCT_NOT_FOUND) }
    }


    // 상품 목록 조회
    // 상품 목록 조회
    @Transactional(readOnly = true)
    fun getProducts(
        user: User?,
        category: ProductCategory?,
        filterType: ProductFilterType?,
        sortType: ProductSortType?,
        pageable: Pageable
    ): Page<ProductListResponse> {
        val startTime = System.currentTimeMillis()
        val userId = user?.userId

        log.info(
            "[Product] [List] 상품 목록 조회 시작 - userId={}, category={}, filter={}, sort={}, page={}, size={}",
            userId, category, filterType, sortType, pageable.pageNumber, pageable.pageSize
        )

        return try {
            val effectiveFilter = filterType ?: ProductFilterType.ALL
            val effectiveCategory = if (effectiveFilter == ProductFilterType.ALL) category else null

            // 캐시 대상 여부 (비로그인 + 인기순만 캐시)
            val cacheable = (user == null) && (sortType == ProductSortType.POPULAR)

            var popularCacheKey: String? = null
            if (cacheable) {
                popularCacheKey = buildPopularListCacheKey(effectiveCategory, effectiveFilter, pageable)

                try {
                    val cachedJson = redisTemplate.opsForValue().get(popularCacheKey)
                    if (cachedJson != null) {
                        val cached = objectMapper.readValue(cachedJson, ProductListPageCache::class.java)

                        log.info("[Product] [List] [Popular] 캐시 히트 - key={}", popularCacheKey)

                        return PageImpl(cached.content, pageable, cached.totalElements)
                    }
                } catch (e: Exception) {
                    log.error(
                        "[Product] [List] [Popular] 캐시 읽기 실패 - key={}, error={}",
                        popularCacheKey, e.message, e
                    )
                }
            }

            val dbStartTime = System.currentTimeMillis()

            val productPage = when (sortType) {
                ProductSortType.POPULAR -> {
                    log.debug(
                        "[Product] [List] 인기순 조회 시작 - effectiveCategory={}, effectiveFilter={}",
                        effectiveCategory, effectiveFilter
                    )
                    getProductsByPopular(effectiveCategory, effectiveFilter, pageable)
                }
                else -> {
                    log.debug(
                        "[Product] [List] 일반 조회 시작 - effectiveCategory={}, effectiveFilter={}, sort={}",
                        effectiveCategory, effectiveFilter, sortType
                    )
                    val sortedPageable = createPageable(pageable, sortType)
                    getFilteredProducts(effectiveCategory, effectiveFilter, sortedPageable)
                }
            }

            val dbDuration = System.currentTimeMillis() - dbStartTime
            log.debug(
                "[Product] [List] DB 조회 완료 - resultCount={}, dbDuration={}ms",
                productPage.totalElements, dbDuration
            )

            // TODO: 시현
            // '좋아요' 누른 상품 ID 목록을 한 번에 조회
            val likeStartTime = System.currentTimeMillis()
            val likedProductIds = getLikedProductIds(user, productPage.content)
            val likeDuration = System.currentTimeMillis() - likeStartTime

            log.debug(
                "[Product] [List] 좋아요 정보 조회 완료 - likedCount={}, likeDuration={}ms",
                likedProductIds.size, likeDuration
            )

            // DTO 변환
            val response = productPage.map { product ->
                ProductListResponse.from(
                    product,
                    product.productId in likedProductIds
                )
            }

            // 캐시 쓰기
            if (cacheable && popularCacheKey != null) {
                try {
                    val cache = ProductListPageCache(response.content, response.totalElements)
                    val jsonData = objectMapper.writeValueAsString(cache)
                    redisTemplate.opsForValue().set(popularCacheKey, jsonData, Duration.ofSeconds(60))

                    log.info("[Product] [List] [Popular] 캐시 쓰기 완료 - key={}", popularCacheKey)
                } catch (e: Exception) {
                    log.error(
                        "[Product] [List] [Popular] 캐시 쓰기 실패 - key={}, error={}",
                        popularCacheKey, e.message, e
                    )
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            log.info(
                "[Product] [List] 상품 목록 조회 완료 - userId={}, totalCount={}, returnedCount={}, totalDuration={}ms",
                userId, response.totalElements, response.numberOfElements, totalDuration
            )

            response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Product] [List] 상품 목록 조회 실패 - userId={}, category={}, duration={}ms",
                userId, category, duration, e
            )
            throw e
        }
    }

    // TODO: 시현
    private fun getLikedProductIds(user: User?, products: List<Product>): Set<Long> {
        // 1. 비로그인 사용자이거나 상품 목록이 비어있으면 빈 Set 반환
        if (user == null || products.isEmpty()) {
            return emptySet()
        }

        // 2. 상품 ID 목록 추출
        val productIds = products.mapNotNull { it.productId }

        // 3. 좋아요한 상품 ID 조회
        return productLikeRepository.findLikedProductIdsByUserId(user.userId, productIds)
    }


    // 인기순 상품 조회 - Redis 활용
    private fun getProductsByPopular(
        category: ProductCategory?,
        filterType: ProductFilterType?,
        pageable: Pageable
    ): Page<Product> {
        val startTime = System.currentTimeMillis()
        log.debug("[Product] [Popular] 인기순 조회 시작 - category={}, filter={}", category, filterType)

        return try {
            // Redis에서 인기 상품 ID 목록 가져오기
            val redisStartTime = System.currentTimeMillis()
            val topIds = redisProductService.getTopNPopularProducts(100)
            val redisDuration = System.currentTimeMillis() - redisStartTime
            log.debug(
                "[Product] [Popular] Redis 조회 완료 - count={}, redisDuration={}ms",
                topIds.size, redisDuration
            )

            val products = if (topIds.isEmpty()) {
                // Redis에 데이터가 없으면 DB에서 직접 조회
                log.warn("[Product] [Popular] Redis 데이터 없음, DB에서 직접 조회")

                val dbStartTime = System.currentTimeMillis()
                val top100 = PageRequest.of(0, 100, Sort.by("purchaseCount").descending())
                val result = productRepository.findAllWithImagesAndNotDeleted(top100).content
                val dbDuration = System.currentTimeMillis() - dbStartTime

                log.debug(
                    "[Product] [Popular] DB 직접 조회 완료 - count={}, dbDuration={}ms",
                    result.size, dbDuration
                )
                result
            } else {
                // Redis에서 가져온 ID로 상품 조회
                val dbStartTime = System.currentTimeMillis()
                val unordered = productRepository.findByProductIdInWithImagesAndNotDeleted(topIds)
                val dbDuration = System.currentTimeMillis() - dbStartTime

                log.debug(
                    "[Product] [Popular] DB 상품 정보 조회 완료 - requestedCount={}, foundCount={}, dbDuration={}ms",
                    topIds.size, unordered.size, dbDuration
                )

                val sortFilterStartTime = System.currentTimeMillis()

                // ID를 키로 하는 Map 생성
                val productMap = unordered.associateBy { it.productId }

                // 정렬 + 필터링을 한 번에 처리
                val result = topIds.mapNotNull { id ->
                    productMap[id]?.takeIf { matchesCondition(it, category, filterType) }
                }

                val sortFilterDuration = System.currentTimeMillis() - sortFilterStartTime

                log.debug(
                    "[Product] [Popular] 정렬+필터링 완료 - beforeCount={}, afterCount={}, duration={}ms",
                    topIds.size, result.size, sortFilterDuration
                )
                result
            }

            val result = convertToPage(products, pageable)

            val totalDuration = System.currentTimeMillis() - startTime
            log.info(
                "[Product] [Popular] 인기순 조회 완료 - totalCount={}, pageSize={}, totalDuration={}ms",
                result.totalElements, result.numberOfElements, totalDuration
            )

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Product] [Popular] 인기순 조회 실패 - category={}, duration={}ms", category, duration, e)
            throw e
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
    private fun getFilteredProducts(
        category: ProductCategory?,
        filterType: ProductFilterType?,
        pageable: Pageable
    ): Page<Product> {
        val startTime = System.currentTimeMillis()
        log.debug("[Product] [Filter] 조건별 조회 시작 - category={}, filter={}", category, filterType)

        return try {
            val result = when {
                // 1. 카테고리 조회 (ALL)
                category != null -> {
                    productRepository.findByCategoryWithImagesAndNotDeleted(category, pageable).also {
                        log.debug(
                            "[Product] [Filter] 카테고리 조회 - category={}, count={}",
                            category, it.totalElements
                        )
                    }
                }
                // 2. 무료 상품 조회
                filterType == ProductFilterType.FREE -> {
                    productRepository.findByPriceWithImagesAndNotDeleted(0.0, pageable).also {
                        log.debug("[Product] [Filter] 무료 상품 조회 - count={}", it.totalElements)
                    }
                }
                // 3. 한정판매 조회
                filterType == ProductFilterType.LIMITED -> {
                    productRepository.findLimitedWithImagesAndNotDeleted(pageable).also {
                        log.debug("[Product] [Filter] 한정판매 조회 - count={}", it.totalElements)
                    }
                }
                // 4. 전체 조회
                else -> {
                    productRepository.findAllWithImagesAndNotDeleted(pageable).also {
                        log.debug("[Product] [Filter] 전체 조회 - count={}", it.totalElements)
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Product] [Filter] 조건별 조회 완료 - category={}, filter={}, count={}, duration={}ms",
                category, filterType, result.totalElements, duration
            )

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Product] [Filter] 조건별 조회 실패 - category={}, filter={}, duration={}ms",
                category, filterType, duration, e
            )
            throw e
        }
    }

    /**
     * 특정 유저의 판매 상품 목록 조회 (대표 이미지 포함)
     *
     * @param userId 판매자 ID
     * @param pageable 페이지네이션 정보
     * @return 상품 목록 (대표 이미지 포함)
     */
    // TODO: 웅철 - 특정 유저의 판매 상품 목록 조회
    fun findProductsByUserId(userId: Long, pageable: Pageable): Page<ProductListResponse> {
        val user = userRepository!!.findById(userId).orElseThrow<ServiceException?>(Supplier {
            ServiceException(
                ErrorCode.USER_NOT_FOUND
            )
        })
        val sellerName = user.name
        // Repository에서 DTO로 조회
        val dtoPage = productRepository.findByUserIdWithThumbnail(userId, pageable)

        // DTO -> Response 변환
        val responsePage = dtoPage.map<ProductListResponse?>(
            Function { dto: ProductWithThumbnailDto ->
                dto.toResponse(
                    true,
                    sellerName
                )
            } // 찜한 목록이므로 isLikedByUser = true
        )

        return responsePage
    }

    // 정렬 조건 생성
    private fun createPageable(pageable: Pageable, sortType: ProductSortType?): Pageable {
        val sort = when (sortType) {
            ProductSortType.LATEST -> Sort.by("createdAt").descending()
            ProductSortType.PRICE_ASC -> Sort.by("price").ascending()
            ProductSortType.PRICE_DESC -> Sort.by("price").descending()
            else -> Sort.unsorted()
        }

        return PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
    }

    // 상품이 조회 조건이 맞는지 확인
    private fun matchesCondition(
        product: Product,
        category: ProductCategory?,
        filterType: ProductFilterType?
    ): Boolean = when {
        category != null ->
            product.productCategory == category

        filterType == ProductFilterType.FREE ->
            product.price == 0.0

        filterType == ProductFilterType.LIMITED ->
            product.stockQuantity != null

        else -> true // 전체 조회
    }

    // 페이징 처리
    private fun convertToPage(products: List<Product>, pageable: Pageable): Page<Product> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, products.size)

        if (start >= products.size) {
            return PageImpl(emptyList(), pageable, products.size.toLong())
        }

        val pageContent = products.subList(start, end)

        return PageImpl(pageContent, pageable, products.size.toLong())
    }

    @Transactional(readOnly = true)
    fun getProductDetail(user: User?, productId: Long): ProductDetailResponse? {
        val cacheKey: String = CACHE_KEY_PREFIX + productId
        val userId = user?.userId
        log.info("[Product] [Detail] 상품 상세 조회 시작 - cacheKey={}, userId={}", cacheKey, userId)

        try {
            val cachedData = redisTemplate.opsForValue().get(cacheKey)
            if (cachedData != null) {
                log.info("[Service] [Cache] 캐시 히트 - key={}", cacheKey)
                return objectMapper.readValue(cachedData, ProductDetailResponse::class.java)
            }
        } catch (e: Exception) {
            log.error("[Service] [Cache] 캐시 읽기 실패 - key={}, error={}", cacheKey, e.message, e)
        }

        // 판매 중지된 상품은 조회 불가
        try {
            log.info("[Service] [DB] 캐시 미스(Miss) - DB 조회 - key={}", cacheKey)
            val product = productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                ?: throw ServiceException(ErrorCode.PRODUCT_NOT_FOUND)

            log.debug("[Product] [Detail] [DB] 상품 조회 완료")

            if (product.isDeleted) {
                log.warn("[Product] [Detail] [DB] 실패: 삭제된 상품 - productId={}", productId)
                throw ServiceException(ErrorCode.PRODUCT_NOT_FOUND)
            }

            val imageUrls = product.productImages.map { it.productImageUrl }

            // TODO: 시현
            var isLiked = false
            if (user != null) {
                // [개선] !! 제거
                isLiked = productLikeRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId)
                log.debug("[Product] [Detail] [DB] '좋아요' 상태 확인 완료 - isLiked={}", isLiked)
            }

            val reviewCount = reviewRepository.countByProductAndIsDeletedFalse(product)
            product.reviewCount = reviewCount?.toInt()
            log.debug("[Product] [Detail] [DB] 리뷰 개수 카운트 완료 - count={}", reviewCount)
            val response = ProductDetailResponse.from(product, imageUrls, isLiked)

            try {
                val jsonData = objectMapper.writeValueAsString(response)
                redisTemplate.opsForValue().set(cacheKey, jsonData, Duration.ofHours(1))
                log.info("[Service] [Cache] 캐시 쓰기(Write) 완료 - key={}", cacheKey)
            } catch (e: Exception) {
                log.error("[Service] [Cache] 캐시 쓰기 실패 - key={}, error={}", cacheKey, e.message, e)
            }

            log.info("[Product] [Detail] [DB] 상품 상세 조회 완료 - productId={}", productId)
            return response
        } catch (e: Exception) {
            log.error("[Product] [Detail] 상품 상세 조회 실패 - productId={}", productId, e)
            throw e
        }
    }

    private fun buildPopularListCacheKey(
        category: ProductCategory?,
        filterType: ProductFilterType?,
        pageable: Pageable
    ): String {
        val categoryPart = category?.name ?: "ALL"
        val filterPart = filterType?.name ?: "ALL"

        return POPULAR_LIST_CACHE_PREFIX +
                "category=$categoryPart" +
                ":filter=$filterPart" +
                ":page=${pageable.pageNumber}" +
                ":size=${pageable.pageSize}"
    }
}
