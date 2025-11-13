// 게시글 카테고리
export type PostCategory = 'FREE' | 'QUESTION' | 'TIP';

// 게시글 목록 아이템
export interface PostListItem {
  id: number;
  category: PostCategory;
  title: string;
  excerpt: string;
  authorDisplay: string;
  createdAt: string;
  commentCount: number;
  thumbnailUrl: string | null;
}

// 페이지 응답
export interface PostListResponse {
  content: PostListItem[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
}

// 카테고리 라벨
export const CATEGORY_LABELS: Record<PostCategory, string> = {
  FREE: '자유',
  QUESTION: '질문',
  TIP: '팁'
};






