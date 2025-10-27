'use client';

import { useEffect, useState } from 'react';
import { getHomeSummary } from '@/lib/api/home.api';
import { ProductListResponse } from '@/types/product.types';
import { LatestReviewItem, LatestPostItem } from '@/types/home.types';
import ProductCard from '@/components/product/ProductCard';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { User } from '@/types/auth.types';


export default function HomePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, login } = useAuthStore();
  const [isProcessingLogin, setIsProcessingLogin] = useState(false);
  const [popularProducts, setPopularProducts] = useState<ProductListResponse[]>([]);
  const [latestReviews, setLatestReviews] = useState<LatestReviewItem[]>([]);
  const [latestPosts, setLatestPosts] = useState<LatestPostItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHomeData();
  }, []);

  const fetchHomeData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getHomeSummary();
      setPopularProducts(data.popularProducts);
      setLatestReviews(data.latestReviews);
      setLatestPosts(data.latestPosts);
    } catch (err: any) {
      console.error('홈 화면 데이터 조회 실패:', err);
      setError('데이터를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLikeToggle = async (productId: number) => {
    // TODO: 찜하기 API 연동
    console.log('찜하기:', productId);
  };

  // 카테고리 한글 변환
  const getCategoryKorean = (category: string) => {
    const categoryMap: { [key: string]: string } = {
      'QUESTION': '질문',
      'SHARE': '공유',
      'FREE': '자유',
      'REVIEW': '후기',
    };
    return categoryMap[category] || category;
  };
// OAuth2 로그인 성공 처리
useEffect(() => {
  const accessToken = searchParams.get('accessToken');
  const userId = searchParams.get('userId');
  const email = searchParams.get('email');
  const name = searchParams.get('name');
  const loginError = searchParams.get('loginError');

  if (loginError) {
      alert('로그인에 실패했습니다. 다시 시도해주세요.');
      router.replace('/'); // URL 파라미터 제거
      return;
  }

  // 로그인 성공 처리
  if (accessToken && userId && email && name) {
      setIsProcessingLogin(true);

      const userData: User = {
          userId,
          email: decodeURIComponent(email),
          name: decodeURIComponent(name),
          provider: 'GOOGLE',
      };

      // 토큰 및 사용자 정보 저장
      login(accessToken, userData);

      // URL에서 파라미터 제거 (깔끔하게)
      router.replace('/');

      // 로그인 성공 알림
      setTimeout(() => {
          setIsProcessingLogin(false);
          alert(`환영합니다, ${userData.name}님!`);
      }, 300);
  }
}, [searchParams, router, login]);

// 로그인 처리 중 로딩
if (isProcessingLogin) {
  return (
      <div className="flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C] mx-auto mb-4"></div>
              <h2 className="text-xl font-semibold text-gray-700">
                  로그인 처리 중...
              </h2>
          </div>
      </div>
  );
}
  // 로딩 중
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  // 에러 발생
  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* 인기 상품 섹션 */}
      <section className="mb-16">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">인기 상품 랭킹 TOP 5</h2>
          <button
            onClick={() => router.push('/product')}
            className="text-sm text-gray-600 hover:text-[#925C4C] transition-colors"
          >
            더보기 →
          </button>
        </div>
        
        {popularProducts.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <p>인기 상품이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-5 gap-4">
            {popularProducts.map((product, index) => (
              <div key={product.productId} className="relative">
                {/* 랭킹 배지 */}
                <div className="absolute top-2 left-2 z-10">
                  <span className="bg-[#925C4C] text-white text-sm font-bold px-3 py-1 rounded-full shadow-md">
                    #{index + 1}
                  </span>
                </div>
                
                {/* 기존 ProductCard 컴포넌트 재사용 */}
                <ProductCard 
                  product={product} 
                  onLikeToggle={handleLikeToggle}
                />
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 최신 리뷰 섹션 */}
      <section className="mb-16">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">최신 리뷰</h2>
        </div>
        
        {latestReviews.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <p>최신 리뷰가 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {latestReviews.map((review) => (
              <div
                key={review.reviewId}
                className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow"
              >
                {/* 별점 */}
                <div className="flex items-center mb-3">
                  <div className="flex text-yellow-400">
                    {[...Array(5)].map((_, i) => (
                      <svg
                        key={i}
                        className={`w-5 h-5 ${i < review.rating ? 'fill-current' : 'fill-gray-200'}`}
                        viewBox="0 0 20 20"
                      >
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                    ))}
                  </div>
                  <span className="ml-2 text-sm text-gray-600">{review.rating}.0</span>
                </div>

                {/* 리뷰 내용 */}
                <p className="text-gray-700 mb-3 line-clamp-3">
                  {review.content}
                </p>

                {/* 상품 정보 */}
                <div className="pt-3 border-t border-gray-100">
                  <p className="text-sm text-gray-600 line-clamp-1">
                    {review.productTitle}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {new Date(review.createdDate).toLocaleDateString('ko-KR')}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 커뮤니티 글 섹션 */}
      <section className="mb-16">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">커뮤니티 최신 글</h2>
          <button
            onClick={() => router.push('/community/posts')}
            className="text-sm text-gray-600 hover:text-[#925C4C] transition-colors"
          >
            더보기 →
          </button>
        </div>
        
        {latestPosts.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <p>최신 글이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {latestPosts.map((post) => (
              <div
                key={post.postId}
                className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow"
              >
                {/* 카테고리 배지 */}
                <div className="mb-3">
                  <span className="inline-block bg-[#925C4C] text-white text-xs font-medium px-3 py-1 rounded-full">
                    {getCategoryKorean(post.category)}
                  </span>
                </div>

                {/* 제목 */}
                <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                  {post.title}
                </h3>

                {/* 작성일 */}
                <p className="text-sm text-gray-500">
                  {new Date(post.createdAt).toLocaleDateString('ko-KR')}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
