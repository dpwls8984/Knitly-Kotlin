// 커뮤니티 공통 타입

export type PostCategory = 'FREE' | 'QUESTION' | 'TIP';

export const CATEGORY_LABELS: Record<PostCategory, string> = {
  FREE: '자유',
  QUESTION: '질문',
  TIP: 'TIP',
};

/** 목록 아이템 */
export interface PostListItem {
  id: number;
  category: PostCategory;
  title: string;
  excerpt: string;
  authorDisplay: string;
  authorId?: number | null;
  createdAt: string;
  commentCount: number;
  thumbnailUrl?: string;
  thumbnail?: string; // 백엔드가 thumbnail 로 내려올 수도 있어 방어
}

/** 페이지네이션 공통 응답 */
export interface PageResponse<T> {
  content: T[];
  page: number;          // 현재 페이지(0-base)
  totalPages: number;
  totalElements: number;
  last: boolean;
}

/** 목록 API 응답 */
export type PostListResponse = PageResponse<PostListItem>;

/** 상세 */
export interface PostDetail {
  id: number;
  category: PostCategory;
  title: string;
  content: string;
  authorDisplay: string;
  authorId?: number | null;
  createdAt: string;
  images: string[]; // 상대경로(/uploads/...) 또는 절대경로
}

/** 댓글(플랫) */
export interface CommentItem {
  id: number;
  postId: number;
  parentId?: number | null;
  content: string;
  authorDisplay: string;
  authorId?: number | null;
  createdAt: string;
}

/** 트리 변환용 */
export interface CommentNode extends CommentItem {
  children: CommentNode[];
}
