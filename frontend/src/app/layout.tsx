'use client';

import "./globals.css";
import Image from "next/image";
import Link from "next/link";
import { useAuthStore } from "@/lib/store/authStore";
import { useEffect, useState } from "react";
import LoginModal from "@/components/common/LoginModal/LoginModal";

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    const { user, logout, initAuth, isLoading } = useAuthStore();
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isClient, setIsClient] = useState(false);

    // 클라이언트 사이드에서만 실행
    useEffect(() => {
        setIsClient(true);
        initAuth(); // 초기 로그인 상태 확인
    }, [initAuth]);

    const handleLogout = async () => {
        if (confirm('로그아웃 하시겠습니까?')) {
            await logout();
            window.location.href = '/'; // 홈으로 리다이렉트
        }
    };

    // 서버 사이드 렌더링 중에는 로그인 상태를 표시하지 않음
    const userStatus = isClient && user ? 1 : 0;

    return (
        <html lang="ko">
        <body className="antialiased">
        {/* Header */}
        <header className="flex items-center justify-between px-8 h-16 border-b border-gray-200">
            {/* 왼쪽 로고 */}
            <div className="flex-shrink-0">
                <Link href="/">
                    <Image src="/logo.png" alt="Logo" width={120} height={40} />
                </Link>
            </div>

            {/* 가운데 메뉴 */}
            <nav className="flex-1 flex justify-between max-w-lg mx-auto">
                <Link href="/product" className="hover:text-[#925C4C] transition-colors">
                    도안구매
                </Link>
                <Link href="/mypage/design/create-design" className="hover:text-[#925C4C] transition-colors">
                    도안제작
                </Link>
                <Link href="/community" className="hover:text-[#925C4C] transition-colors">
                    커뮤니티
                </Link>
                <Link href="/event" className="hover:text-[#925C4C] transition-colors">
                    이벤트
                </Link>
            </nav>

            {/* 오른쪽 로그인/로그아웃 버튼 */}
            <nav className="flex items-center gap-4">
                {userStatus === 1 && (
                    <Link
                        href="/cart"
                        className="text-[#925C4C] hover:text-[#7a4a3d] transition-colors"
                    >
                        장바구니
                    </Link>
                )}

                <div className="flex items-center gap-2">
                    {userStatus === 1 ? (
                        <>
                            <Link
                                href="/mypage"
                                className="px-4 py-2 text-[#925C4C] hover:text-[#7a4a3d] transition-colors"
                            >
                                마이페이지
                            </Link>
                            <button
                                onClick={handleLogout}
                                className="px-4 py-2 text-gray-600 hover:text-gray-800 transition-colors"
                            >
                                로그아웃
                            </button>
                        </>
                    ) : (
                        <button
                            onClick={() => setIsLoginModalOpen(true)}
                            className="px-4 py-2 text-[#925C4C] hover:text-[#7a4a3d] transition-colors font-medium"
                        >
                            로그인/회원가입
                        </button>
                    )}
                </div>
            </nav>
        </header>

        <main className="p-10 px-20 py-20">{children}</main>

        {/* 로그인 모달 */}
        <LoginModal
            isOpen={isLoginModalOpen}
            onClose={() => setIsLoginModalOpen(false)}
        />
        </body>
        </html>
    );
}