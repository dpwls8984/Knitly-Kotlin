'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { getMyComments, deleteComment } from '@/lib/api/mypage.api';
import { MyCommentListItem } from '@/types/mypage.types';

export default function MyCommentPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuthStore();

  const [comments, setComments] = useState<MyCommentListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');
  
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);

  const [selectedComments, setSelectedComments] = useState<Set<number>>(new Set());

  // 비로그인 상태 체크
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      alert('로그인이 필요합니다.');
      router.push('/');
    }
  }, [isAuthenticated, authLoading, router]);

  // 댓글 목록 조회
  const fetchComments = async (page: number = 0, query?: string) => {
    if (!user || !isAuthenticated) return;

    try {
      setIsLoading(true);
      setError(null);

      const response = await getMyComments(query, page, 10);
      
      setComments(response.content);
      setCurrentPage(response.page);
      setTotalPages(response.totalPages);
      setIsLastPage(response.last);
    } catch (err: any) {
      console.error('댓글 목록 조회 실패:', err);
      setError(err.response?.data?.message || '댓글 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (user && isAuthenticated && !authLoading) {
      fetchComments(0, searchQuery);
    }
  }, [user, isAuthenticated, authLoading, searchQuery]);

  // 검색
  const handleSearch = () => {
    setSearchQuery(searchInput);
    setCurrentPage(0);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  // 체크박스 토글
  const handleCheckboxToggle = (commentId: number) => {
    setSelectedComments(prev => {
      const newSet = new Set(prev);
      if (newSet.has(commentId)) {
        newSet.delete(commentId);
      } else {
        newSet.add(commentId);
      }
      return newSet;
    });
  };

  // 전체 선택/해제
  const handleSelectAll = () => {
    if (selectedComments.size === comments.length) {
      setSelectedComments(new Set());
    } else {
      setSelectedComments(new Set(comments.map(c => c.id)));
    }
  };

  // 선택 항목 삭제
  const handleBatchDelete = async () => {
    if (selectedComments.size === 0) {
      alert('삭제할 댓글을 선택해주세요.');
      return;
    }

    if (!confirm(`선택한 ${selectedComments.size}개의 댓글을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      // 선택된 댓글들을 순차적으로 삭제
      await Promise.all(
        Array.from(selectedComments).map(id => deleteComment(id))
      );

      alert('선택한 댓글이 삭제되었습니다.');
      setSelectedComments(new Set());
      fetchComments(currentPage, searchQuery);
    } catch (err: any) {
      console.error('댓글 삭제 실패:', err);
      alert(err.response?.data?.message || '댓글 삭제에 실패했습니다.');
    }
  };

  // 페이지네이션
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    fetchComments(page, searchQuery);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // 로딩 중
  if (authLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex justify-center items-center min-h-[60vh]">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
        </div>
      </div>
    );
  }

  // 비로그인 상태
  if (!user) {
    return null;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="bg-white rounded-lg shadow-sm">
        {/* 헤더 */}
        <div className="bg-gray-100 py-8 px-6 text-center rounded-t-lg">
          <h1 className="text-3xl font-bold text-gray-900">내가 작성한 댓글 조회</h1>
        </div>

        {/* 검색 바 */}
        <div className="p-6 bg-gray-50 border-b">
          <div className="flex gap-2 max-w-2xl mx-auto">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="댓글 내용으로 검색"
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
            />
            <button
              onClick={handleSearch}
              className="px-6 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d] transition-colors"
            >
              검색
            </button>
          </div>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="m-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        {/* 댓글 목록 */}
        <div className="p-6">
          {isLoading && comments.length === 0 ? (
            <div className="flex justify-center items-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
          ) : comments.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <p className="text-lg">작성한 댓글이 없습니다.</p>
            </div>
          ) : (
            <>
              {/* 전체 선택 및 일괄 삭제 */}
              <div className="flex justify-between items-center mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedComments.size === comments.length && comments.length > 0}
                    onChange={handleSelectAll}
                    className="w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                  />
                  <span className="text-sm font-medium text-gray-700">전체 선택</span>
                </label>
                <button
                  onClick={handleBatchDelete}
                  disabled={selectedComments.size === 0}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    selectedComments.size === 0
                      ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                      : 'bg-red-500 text-white hover:bg-red-600'
                  }`}
                >
                  선택 항목 삭제
                </button>
              </div>

              {/* 댓글 아이템 */}
              <div className="space-y-3">
                {comments.map((comment) => (
                  <div
                    key={comment.id}
                    className="flex items-start gap-4 p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    {/* 체크박스 */}
                    <input
                      type="checkbox"
                      checked={selectedComments.has(comment.id)}
                      onChange={() => handleCheckboxToggle(comment.id)}
                      className="mt-1 w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                    />

                    {/* 댓글 정보 */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 mb-2">
                        <span className="text-sm font-medium text-gray-500">작성일</span>
                        <span className="text-sm text-gray-700">{comment.createdAt}</span>
                      </div>
                      <div className="mb-3">
                        <h3 className="font-semibold text-gray-900 mb-1">제목</h3>
                        <p className="text-gray-700">{comment.content}</p>
                      </div>
                    </div>

                    {/* 게시글로 이동 버튼 */}
                    <button
                      onClick={() => router.push(`/community/tip/${comment.postId}`)}
                      className="px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors whitespace-nowrap"
                    >
                      게시글 보기
                    </button>
                  </div>
                ))}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="mt-8 flex justify-center items-center gap-2">
                  {Array.from({ length: totalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => handlePageChange(i)}
                      className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                        currentPage === i
                          ? 'bg-[#925C4C] text-white'
                          : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}