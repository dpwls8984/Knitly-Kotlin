// 마이페이지 - 내가 쓴 댓글
export interface MyCommentListItem {
    id: number; // 댓글 ID
    postId: number; // 게시글 ID
    createdAt: string; // 작성일 (DATE 형식)
    content: string; // 댓글 내용 요약 (최대 30자)
  }
  
  // 마이페이지 - 내가 쓴 글
  export interface MyPostListItemResponse {
    id: number; // 게시글 ID
    title: string; // 제목
    content: string; // 내용 요약 (최대 10자)
    thumbnailUrl: string | null; // 대표 이미지 URL
    createdAt: string; // 작성일
  }
  
  // 페이지 응답 공통 타입
  export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
  }
  
  
  
  
  
  
  