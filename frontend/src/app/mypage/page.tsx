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

                <div className="grid grid-cols-2 gap-4 mt-6">
                    <div className="bg-gray-50 p-4 rounded-lg">
                        <p className="text-sm text-gray-600">User ID</p>
                        <p className="text-lg font-semibold">{user.userId}</p>
                    </div>
                    <div className="bg-gray-50 p-4 rounded-lg">
                        <p className="text-sm text-gray-600">로그인 방식</p>
                        <p className="text-lg font-semibold">{user.provider}</p>
                    </div>
                </div>
            </div>

            {/* 메뉴 */}
            <div className="grid grid-cols-2 gap-4">
                <button className="bg-white shadow-md rounded-lg p-6 hover:shadow-lg transition-shadow">
                    <h3 className="font-semibold text-lg mb-2">구매 내역</h3>
                    <p className="text-sm text-gray-600">구매한 도안을 확인하세요</p>
                </button>
                <button className="bg-white shadow-md rounded-lg p-6 hover:shadow-lg transition-shadow">
                    <h3 className="font-semibold text-lg mb-2">내 도안</h3>
                    <p className="text-sm text-gray-600">제작한 도안을 관리하세요</p>
                </button>
            </div>
        </div>
    );
}