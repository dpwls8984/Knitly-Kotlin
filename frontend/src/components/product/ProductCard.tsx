'use client';

import { useRouter } from 'next/navigation';
import { ProductListResponse } from '@/types/product.types';
import { useState, useEffect } from 'react';
import { addLike, removeFavorite } from '@/lib/api/like.api';

interface ProductCardProps {
  product: ProductListResponse;
}

export default function ProductCard({ product }: ProductCardProps) {
  const router = useRouter();

  const [isLiked, setIsLiked] = useState(product.isLikedByUser);
  const [likeCount, setLikeCount] = useState(product.likeCount);
  const [isLiking, setIsLiking] = useState(false);

  useEffect(() => {
    setIsLiked(product.isLikedByUser);
    setLikeCount(product.likeCount);
  }, [product.isLikedByUser, product.likeCount]);

  const handleCardClick = () => {
    router.push(`/product/${product.productId}`);
  };

  const handleAuthorClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // TODO: 판매자 스토어 페이지로 이동 (아직 구현 전)
    alert('판매자 스토어 페이지로 이동합니다. (구현 예정)');
  };

  const handleLikeClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (isLiking) return;
    setIsLiking(true);

    const originalIsLiked = isLiked;
    const originalLikeCount = likeCount;

    setIsLiked((prev) => !prev);
    setLikeCount((prevCount) => (originalIsLiked ? prevCount - 1 : prevCount + 1));
    
    try {
      // 6. 상태에 따라 API 호출
      if (originalIsLiked) {
        await removeFavorite(product.productId);
      } else {
        await addLike(product.productId);
      }
    } catch (error) {
      console.error('찜 처리 실패:', error);
      // 7. API 호출 실패 시 UI 롤백
      setIsLiked(originalIsLiked);
      setLikeCount(originalLikeCount);
      alert('찜 상태 변경에 실패했습니다.');
    } finally {
      setIsLiking(false); // 로딩 상태 해제
    }
  };

  return (
    <div 
      onClick={handleCardClick}
      className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
    >
      {/* 상품 이미지 */}
      <div className="aspect-square bg-gray-100 flex items-center justify-center">
        {product.thumbnailUrl ? (
          <img 
            src={product.thumbnailUrl} 
            alt={product.title}
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
            {product.title}
          </h3>
          <button
            onClick={handleAuthorClick}
            className="text-xs text-gray-600 hover:text-[#925C4C] transition-colors block mb-2"
          >
            {/* TODO: 작가명 정보가 백엔드에서 제공되지 않음 - 추후 수정 필요 */}
            작가명
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
            disabled={isLiking}
          >
            <svg
              className={`w-5 h-5 ${isLiked ? 'text-[#925C4C]' : 'text-gray-400'}`}
              fill={isLiked ? 'currentColor' : 'none'}
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
