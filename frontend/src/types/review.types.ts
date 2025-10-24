// types/review.types.ts
export interface ReviewListItem {
    reviewId: number;
    productId: number;
    productTitle: string;
    productThumbnailUrl: string;
    rating: number;
    content: string;
    reviewImageUrls?: string[];
    createdDate: string;
    purchasedDate?: string;
  }

  export interface ReviewCreateRequest {
    rating: number;
    content: string;
    images?: File[];
  }

  // 리뷰 작성 폼용 상품 정보
export interface ReviewCreateResponse {
  productTitle: string;
  productThumbnailUrl: string;
}
  
  export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }
  