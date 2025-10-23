'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';

// 타입 정의
type ProductCategory = 'ALL' | 'TOP' | 'BOTTOM' | 'OUTERWEAR' | 'BAG' | 'ETC';
type ProductFilterType = 'ALL' | 'FREE' | 'LIMITED';
type ProductSortType = 'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC';

interface Product {
  id: number;
  name: string;
  price: number;
  thumbnailImage: string;
  authorName: string;
  authorId: number;
  likeCount: number;
  isLiked: boolean;
  category: ProductCategory;
  isFree: boolean;
  isLimited: boolean;
}

interface ProductListResponse {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 상품 카드 컴포넌트
interface ProductCardProps {
  product: Product;
  onLikeToggle: (productId: number) => void;
}

function ProductCard({ product, onLikeToggle }: ProductCardProps) {
  const router = useRouter();

  const handleCardClick = () => {
    router.push(`/product/${product.id}`);
  };

  const handleAuthorClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // TODO: 판매자 스토어 페이지로 이동 (아직 구현 전)
    alert('판매자 스토어 페이지로 이동합니다. (구현 예정)');
  };

  const handleLikeClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onLikeToggle(product.id);
  };

  return (
    <div 
      onClick={handleCardClick}
      className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
    >
      {/* 상품 이미지 */}
      <div className="aspect-square bg-gray-100 flex items-center justify-center">
        {product.thumbnailImage ? (
          <img 
            src={product.thumbnailImage} 
            alt={product.name}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="text-gray-400 text-sm">이미지</div>
        )}
      </div>
      
      {/* 상품 정보 */}
      <div className="p-4 flex justify-between items-end">
        {/* 좌측: 상품명, 작가명, 가격 */}
        <div className="flex-1">
          <h3 className="font-medium text-gray-900 text-sm line-clamp-2 mb-1">
            {product.name}
          </h3>
          <button
            onClick={handleAuthorClick}
            className="text-xs text-gray-600 hover:text-[#925C4C] transition-colors block mb-2"
          >
            {product.authorName}
          </button>
          <div className="text-sm font-medium text-gray-900">
            {product.isFree ? '무료' : `${product.price.toLocaleString()}원`}
          </div>
        </div>
        
        {/* 우측: 찜 버튼 + 찜 개수 (세로 배치) */}
        <div className="flex flex-col items-center gap-1 ml-4">
          <button
            onClick={handleLikeClick}
            className="flex-shrink-0"
          >
            <svg
              className={`w-5 h-5 ${product.isLiked ? 'text-[#925C4C] fill-current' : 'text-gray-400'}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
              />
            </svg>
          </button>
          <div className="text-xs text-gray-500">
            {product.likeCount}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ProductListPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  
  // 상태 관리
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // 정렬 상태
  const [selectedSort, setSelectedSort] = useState<ProductSortType>('LATEST');

  // URL 파라미터에서 초기값 설정
  useEffect(() => {
    const sort = searchParams.get('sort') as ProductSortType || 'LATEST';
    const page = parseInt(searchParams.get('page') || '0');

    setSelectedSort(sort);
    setCurrentPage(page);
  }, [searchParams]);

  // 상품 목록 조회
  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: '20',
        sort: selectedSort,
      });

      // TODO: 실제 API 호출로 교체
      // const response = await fetch(`/api/products?${params}`);
      // const data: ProductListResponse = await response.json();
      
      // 임시 데이터
      const mockProducts: Product[] = Array.from({ length: 20 }, (_, i) => ({
        id: i + 1,
        name: `상품 ${i + 1}`,
        price: Math.floor(Math.random() * 50000) + 5000,
        thumbnailImage: '',
        authorName: `작가 ${i + 1}`,
        authorId: i + 1,
        likeCount: Math.floor(Math.random() * 1000),
        isLiked: Math.random() > 0.5,
        category: ['TOP', 'BOTTOM', 'OUTERWEAR', 'BAG', 'ETC'][Math.floor(Math.random() * 5)] as ProductCategory,
        isFree: Math.random() > 0.8,
        isLimited: Math.random() > 0.9,
      }));

      const mockData: ProductListResponse = {
        content: mockProducts,
        totalElements: 100,
        totalPages: 5,
        size: 20,
        number: currentPage,
        first: currentPage === 0,
        last: currentPage === 4,
      };

      setProducts(mockData.content);
      setTotalPages(mockData.totalPages);
      setTotalElements(mockData.totalElements);
    } catch (error) {
      console.error('상품 목록 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [currentPage, selectedSort]);

  // 정렬 변경
  const handleSortChange = (sort: ProductSortType) => {
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
  const updateURL = (params: { sort?: ProductSortType; page?: number }) => {
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

    router.push(`/product?${newSearchParams.toString()}`);
  };

  // 찜 토글
  const handleLikeToggle = async (productId: number) => {
    try {
      // TODO: 실제 API 호출로 교체
      // await fetch(`/api/products/${productId}/like`, { method: 'POST' });
      
      // 임시로 로컬 상태 업데이트
      setProducts(prev => prev.map(product => 
        product.id === productId 
          ? { 
              ...product, 
              isLiked: !product.isLiked,
              likeCount: product.isLiked ? product.likeCount - 1 : product.likeCount + 1
            }
          : product
      ));
    } catch (error) {
      console.error('찜 토글 실패:', error);
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
              <h2 className="text-2xl font-bold">전체 상품</h2>
              
              <div className="flex items-center gap-2">
                <select
                  value={selectedSort}
                  onChange={(e) => handleSortChange(e.target.value as ProductSortType)}
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
                      key={product.id}
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