// 상품 카테고리
export enum ProductCategory {
    PATTERN = 'PATTERN',
    KIT = 'KIT',
    MATERIAL = 'MATERIAL',
}

// 상품 목록 응답
export interface ProductListResponse {
    productId: number;
    title: string;
    productCategory: ProductCategory;
    price: number;
    purchaseCount: number;
    likeCount: number;
    stockQuantity: number | null;
    avgReviewRating: number;
    createdAt: string;
    thumbnailUrl: string | null;

    // 상태 플래그
    isFree: boolean;
    isLimited: boolean;
    isSoldOut: boolean;
}

// 페이지네이션 응답
export interface PageResponse<T> {
    content: T[];
    pageable: {
        pageNumber: number;
        pageSize: number;
        sort: {
            empty: boolean;
            sorted: boolean;
            unsorted: boolean;
        };
        offset: number;
        paged: boolean;
        unpaged: boolean;
    };
    totalPages: number;
    totalElements: number;
    last: boolean;
    first: boolean;
    size: number;
    number: number;
    numberOfElements: number;
    empty: boolean;
}