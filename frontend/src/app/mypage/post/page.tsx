'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { getMyPosts, deletePost } from '@/lib/api/mypage.api';
import { MyPostListItemResponse } from '@/types/mypage.types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export default function MyPostPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuthStore();

  const [posts, setPosts] = useState<MyPostListItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');
  
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);

  const [selectedPosts, setSelectedPosts] = useState<Set<number>>(new Set());

  // 비로그인 상태 체크
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      alert('로그인이 필요합니다.');
      router.push('/');
    }
  }, [isAuthenticated, authLoading, router]);

  // 글 목록 조회
  const fetchPosts = async (page: number = 0, query?: string) => {
    if (!user || !isAuthenticated) return;

    try {
      setIsLoading(true);
      setError(null);

      const response = await getMyPosts(query, page, 10);
      
      setPosts(response.content);
      setCurrentPage(response.page);
      setTotalPages(response.totalPages);
      setIsLastPage(response.last);
    } catch (err: any) {
      console.error('글 목록 조회 실패:', err);
      setError(err.response?.data?.message || '글 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (user && isAuthenticated && !authLoading) {
      fetchPosts(0, searchQuery);
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
  const handleCheckboxToggle = (postId: number) => {
    setSelectedPosts(prev => {
      const newSet = new Set(prev);
      if (newSet.has(postId)) {
        newSet.delete(postId);
      } else {
        newSet.add(postId);
      }
      return newSet;
    });
  };

  // 전체 선택/해제
  const handleSelectAll = () => {
    if (selectedPosts.size === posts.length) {
      setSelectedPosts(new Set());
    } else {
      setSelectedPosts(new Set(posts.map(p => p.id)));
    }
  };

  // 선택 항목 삭제
  const handleBatchDelete = async () => {
    if (selectedPosts.size === 0) {
      alert('삭제할 글을 선택해주세요.');
      return;
    }

    if (!confirm(`선택한 ${selectedPosts.size}개의 글을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      // 선택된 글들을 순차적으로 삭제
      await Promise.all(
        Array.from(selectedPosts).map(id => deletePost(id))
      );

      alert('선택한 글이 삭제되었습니다.');
      setSelectedPosts(new Set());
      fetchPosts(currentPage, searchQuery);
    } catch (err: any) {
      console.error('글 삭제 실패:', err);
      alert(err.response?.data?.message || '글 삭제에 실패했습니다.');
    }
  };

  // 페이지네이션
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    fetchPosts(page, searchQuery);
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
          <h1 className="text-3xl font-bold text-gray-900">내가 작성한 글 조회</h1>
        </div>

        {/* 검색 바 */}
        <div className="p-6 bg-gray-50 border-b">
          <div className="flex gap-2 max-w-2xl mx-auto">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="제목으로 검색"
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

        {/* 글 목록 */}
        <div className="p-6">
          {isLoading && posts.length === 0 ? (
            <div className="flex justify-center items-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
          ) : posts.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <p className="text-lg">작성한 글이 없습니다.</p>
            </div>
          ) : (
            <>
              {/* 전체 선택 및 일괄 삭제 */}
              <div className="flex justify-between items-center mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedPosts.size === posts.length && posts.length > 0}
                    onChange={handleSelectAll}
                    className="w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                  />
                  <span className="text-sm font-medium text-gray-700">전체 선택</span>
                </label>
                <button
                  onClick={handleBatchDelete}
                  disabled={selectedPosts.size === 0}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    selectedPosts.size === 0
                      ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                      : 'bg-red-500 text-white hover:bg-red-600'
                  }`}
                >
                  선택 항목 삭제
                </button>
              </div>

              {/* 글 아이템 */}
              <div className="space-y-3">
                {posts.map((post) => (
                  <div
                    key={post.id}
                    className="flex items-start gap-4 p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    {/* 체크박스 */}
                    <input
                      type="checkbox"
                      checked={selectedPosts.has(post.id)}
                      onChange={() => handleCheckboxToggle(post.id)}
                      className="mt-1 w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                    />

                    {/* 글 정보 */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 mb-2">
                        <span className="text-sm font-medium text-gray-500">자유</span>
                      </div>
                      <h3 className="font-semibold text-lg text-gray-900 mb-2">{post.title}</h3>
                      <div className="mb-3">
                        <p className="text-gray-600">{post.content || '내용없음'}</p>
                      </div>
                      <div className="text-sm text-gray-500">
                        작성시간: {new Date(post.createdAt).toLocaleDateString('ko-KR')}
                      </div>
                    </div>

                    {/* 이미지 */}
                    {post.thumbnailUrl && (
                      <div className="flex-shrink-0 w-24 h-24 bg-gray-200 rounded-lg overflow-hidden">
                        <img
                          src={`${API_URL}${post.thumbnailUrl}`}
                          alt="썸네일"
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = 'https://placehold.co/200x200/CCCCCC/FFFFFF?text=No+Image';
                          }}
                        />
                      </div>
                    )}
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