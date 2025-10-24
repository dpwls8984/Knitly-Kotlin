export interface FavoriteProductItem {
    productId: number;
    productTitle: string;
    sellerName: string;
    thumbnailUrl: string;
  }
  
  export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }