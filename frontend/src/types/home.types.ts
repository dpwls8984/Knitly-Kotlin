// 홈 화면용 타입 정의

/**
 * 최신 리뷰 아이템
 */
export interface LatestReviewItem {
    reviewId: number;
    productId: number;
    productTitle: string;
    productThumbnailUrl: string | null;
    rating: number;
    content: string;
    createdDate: string; // LocalDate
  }
  
  /**
   * 최신 커뮤니티 글 아이템
   */
  export interface LatestPostItem {
    postId: number;
    title: string;
    category: string;
    thumbnailUrl: string | null;
    createdAt: string; // LocalDateTime
  }
  
  /**
   * 홈 요약 응답 (한번에 모든 데이터)
   */
  export interface HomeSummaryResponse {
    popularProducts: any[]; // ProductListResponse[]
    latestReviews: LatestReviewItem[];
    latestPosts: LatestPostItem[];
  }
  