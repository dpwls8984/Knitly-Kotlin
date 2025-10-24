'use client';
import Link from 'next/link';

import { useAuthStore } from '@/lib/store/authStore';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function MyPage() {
    const router = useRouter();
    const { user, isAuthenticated, isLoading } = useAuthStore();

    // 비로그인 상태면 로그인 페이지로 리다이렉트
    useEffect(() => {
        if (!isLoading && !isAuthenticated) {
            alert('로그인이 필요합니다.');
            router.push('/');
        }
    }, [isAuthenticated, isLoading, router]);

    // 로딩 중
    if (isLoading) {
        return (
            <div className="flex justify-center items-center min-h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
        );
    }

    // 비로그인 상태 (리다이렉트 전)
    if (!user) {
        return null;
    }

    // 마이페이지 메인
    return (
        <div>
            <h1 className="text-3xl font-bold mb-6">마이페이지</h1>

            {/* 사용자 정보 카드 */}
            <div className="bg-white shadow-lg rounded-lg p-6 mb-6">
                <div className="flex items-center gap-4 mb-4">
                    <div className="w-16 h-16 bg-[#925C4C] rounded-full flex items-center justify-center text-white text-2xl font-bold">
                        {user.name.charAt(0)}
                    </div>
                    <div>
                        <h2 className="text-2xl font-semibold">{user.name}</h2>
                        <p className="text-gray-600">{user.email}</p>
                    </div>
                </div>
            </div>

                    {/* ▼▼▼ 임시 테스트용 링크 (나중에 삭제) ▼▼▼ */}
            <div className="mt-8 p-4 border-2 border-dashed border-red-400">
                <h3 className="font-bold text-red-600">🧪 임시 테스트 링크</h3>
                <ul className="list-disc list-inside mt-2 space-y-2">
                <li>
                    <Link href="/mypage/design/register/test-design-123" className="text-blue-600 hover:underline">
                    '판매 등록' 페이지로 이동 (test-design-123)
                    </Link>
                </li>
                <li>
                    <Link href="/mypage/design/modify/product-abc-456" className="text-blue-600 hover:underline">
                    '판매 수정' 페이지로 이동 (product-abc-456)
                    </Link>
                </li>
                </ul>
            </div>

        </div>
    );
}