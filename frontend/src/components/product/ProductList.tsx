'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ProductListResponse, PageResponse } from '@/types/product.types';
import { getProductList, ProductListParams } from '@/lib/api/product.api';
import { addLike, removeFavorite } from '@/lib/api/like.api';
import ProductCard from './ProductCard';

interface ProductListProps {
  title: string;
  category?: 'TOP' | 'BOTTOM' | 'OUTER' | 'BAG' | 'ETC';
  filter?: 'FREE' | 'LIMITED';
  basePath: string;
}

export default function ProductList({ title, category, filter, basePath }: ProductListProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  
  // 상태 관리
  const [products, setProducts] = useState<ProductListResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // 정렬 상태
  const [selectedSort, setSelectedSort] = useState<'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC'>('LATEST');

  // URL 파라미터에서 초기값 설정
  useEffect(() => {
    const sort = searchParams.get('sort') as 'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC' || 'LATEST';
    const page = parseInt(searchParams.get('page') || '0');

    setSelectedSort(sort);
    setCurrentPage(page);
  }, [searchParams]);

  // 상품 목록 조회
  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params: ProductListParams = {
        page: currentPage,
        size: 20,
        sort: selectedSort,
      };

      if (category) {
        params.category = category;
      }
      
      if (filter) {
        params.filter = filter;
      }

      const response: PageResponse<ProductListResponse> = await getProductList(params);
      
      setProducts(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('상품 목록 조회 실패:', error);
      // 에러 발생 시 빈 배열로 설정
      setProducts([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [currentPage, selectedSort, category, filter]);

  // 정렬 변경
  const handleSortChange = (sort: 'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC') => {
    setSelectedSort(sort);
    setCurrentPage(0);
    updateURL({ sort, page: 0 });
  };

  // 페이지 변경
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    updateURL({ page });
  };

  // URL 업데이트
  const updateURL = (params: { sort?: string; page?: number }) => {
    const newSearchParams = new URLSearchParams(searchParams);
    
    if (params.sort !== undefined) {
      newSearchParams.set('sort', params.sort);
    }
    
    if (params.page !== undefined) {
      if (params.page === 0) {
        newSearchParams.delete('page');
      } else {
        newSearchParams.set('page', params.page.toString());
      }
    }

    router.push(`${basePath}?${newSearchParams.toString()}`);
  };

  // 찜 토글
  const handleLikeToggle = async (productId: number) => {
    // 🔥 [수정] 전체 로직 변경

    // 1. 현재 상태 찾기
    const product = products.find(p => p.productId === productId);
    if (!product) return;

    const currentIsLiked = product.isLikedByUser;

    // 2. 낙관적 업데이트 (UI 먼저 변경)
    setProducts(prev => prev.map(p => 
      p.productId === productId 
        ? { 
            ...p, 
            // 찜 상태와 카운트를 모두 토글
            isLikedByUser: !currentIsLiked, 
            likeCount: currentIsLiked ? p.likeCount - 1 : p.likeCount + 1
          }
        : p
    ));

    try {
      // 3. 상태에 따라 다른 API 호출
      if (currentIsLiked) {
        await removeFavorite(productId); // 찜 취소
      } else {
        await addLike(productId); // 찜 등록
      }
    } catch (error) {
      console.error('찜 토글 실패:', error);
      // 4. 실패 시 롤백
      setProducts(prev => prev.map(p => 
        p.productId === productId 
          ? { 
              ...p, 
              // 원래 상태로 되돌리기
              isLikedByUser: currentIsLiked, 
              likeCount: product.likeCount // 원래 카운트로
            }
          : p
      ));
    }
  };

  // 정렬 옵션
  const sortOptions = [
    { value: 'POPULAR', label: '인기순' },
    { value: 'LATEST', label: '최신순' },
    { value: 'PRICE_DESC', label: '가격 높은순' },
    { value: 'PRICE_ASC', label: '가격 낮은순' },
  ];

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-7xl mx-auto px-4 py-4">
        {/* 메인 콘텐츠 */}
        <div>
          {/* 정렬 옵션 */}
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold">{title}</h2>
            
            <div className="flex items-center gap-2">
              <select
                value={selectedSort}
                onChange={(e) => handleSortChange(e.target.value as 'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC')}
                className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 상품 그리드 */}
          {loading ? (
            <div className="flex justify-center items-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
                {products.map((product) => (
                  <ProductCard
                    key={product.productId}
                    product={product}
                    onLikeToggle={handleLikeToggle}
                  />
                ))}
              </div>

              {/* 페이징 */}
              {totalPages > 1 && (
                <div className="flex justify-center items-center gap-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="px-3 py-2 border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    이전
                  </button>
                  
                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                    const pageNum = Math.max(0, Math.min(totalPages - 5, currentPage - 2)) + i;
                    return (
                      <button
                        key={pageNum}
                        onClick={() => handlePageChange(pageNum)}
                        className={`px-3 py-2 border rounded-md ${
                          currentPage === pageNum
                            ? 'bg-[#925C4C] text-white border-[#925C4C]'
                            : 'border-gray-300 hover:bg-gray-50'
                        }`}
                      >
                        {pageNum + 1}
                      </button>
                    );
                  })}
                  
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages - 1}
                    className="px-3 py-2 border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    다음
                  </button>
                </div>
              )}

              {/* 결과 정보 */}
              <div className="text-center text-sm text-gray-600 mt-4">
                총 {totalElements.toLocaleString()}개의 상품
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
