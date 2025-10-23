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
  
  export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number; // 현재 페이지
  }
  