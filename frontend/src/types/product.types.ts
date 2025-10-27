/**
 * 상품 목록 조회 응답 (백엔드 ProductListResponse와 매핑)
 */
export interface ProductListResponse {
    productId: number;
    title: string;
    productCategory: 'TOP' | 'BOTTOM' | 'OUTER' | 'BAG' | 'ETC';
    price: number;
    purchaseCount: number;
    likeCount: number;
    isLikedByUser: boolean;
    stockQuantity: number | null;
    avgReviewRating: number | null;
    createdAt: string;
    productImageUrls: string[];
    isFree: boolean;
    isLimited: boolean;
    isSoldOut: boolean;
}

/**
 * 페이지네이션 응답
 */
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
    size: number;
    number: number;
    sort: {
        empty: boolean;
        sorted: boolean;
        unsorted: boolean;
    };
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

export interface ProductRegisterResponse {
    productId: number;
    title: string;
    description: string;
    productCategory: 'TOP' | 'BOTTOM' | 'OUTER' | 'BAG' | 'ETC';
    sizeInfo: string;
    price: number;
    createdAt: string;
    stockQuantity: number | null; // Java의 Integer는 null일 수 있습니다.
    designId: number;
    productImageUrls: string[];
  }

  export interface ProductModifyResponse {
    productId: number;
    title: string;
    description: string;
    productCategory: 'TOP' | 'BOTTOM' | 'OUTER' | 'BAG' | 'ETC';
    sizeInfo: string;
    stockQuantity: number | null;
    productImageUrls: string[];
  }