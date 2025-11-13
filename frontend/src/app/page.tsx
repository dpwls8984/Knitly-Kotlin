'use client';

import { useEffect, useState } from 'react';
import { getPopularTop5 } from '@/lib/api/home';
import { ProductListResponse } from '@/types/product.types';
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
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchPopularProducts();
      }, []);

    const fetchPopularProducts = async () => {
        try {
          setIsLoading(true);
          setError(null);
          const data = await getPopularTop5();
          setPopularProducts(data);
        } catch (err: any) {
          console.error('인기 상품 조회 실패:', err);
          setError('인기 상품을 불러오는데 실패했습니다.');
        } finally {
          setIsLoading(false);
        }
    };
    
    const handleLikeToggle = async (productId: number) => {
        // TODO: 찜하기 API 연동
        console.log('찜하기:', productId);
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

    // 메인 페이지
    return (
        <div>
            <h1 className="text-3xl font-bold mb-4">메인페이지</h1>
            <section className="mb-12">
                <h2 className="text-2xl font-bold mb-6 text-gray-900">인기 상품 랭킹 TOP 5</h2>

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

            <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="font-semibold mb-2">개발 정보:</h3>
                <ul className="list-disc list-inside text-sm text-gray-600 space-y-1">
                    <li>경로: src/app/page.tsx</li>
                    <li>레이아웃: src/app/layout.tsx</li>
                    <li>
                        {user ? '현재 로그인 상태' : '현재 비로그인 상태'}
                    </li>
                    <li>
                        {user
                            ? '헤더에서 "로그아웃" 버튼으로 로그아웃 가능'
                            : '헤더에서 "로그인/회원가입" 버튼으로 로그인 모달 오픈'}
                    </li>
                </ul>
            </div>

        </div>
    );
}