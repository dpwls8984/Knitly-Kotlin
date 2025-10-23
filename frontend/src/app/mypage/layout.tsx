"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import React from "react";
import { useAuthStore } from "@/lib/store/authStore";  // 🔥 추가

const renderLinks = (
    links: { name: string; href: string; disabled?: boolean }[],
    currentPathname: string
) => {
    return links.map((link) => {
        const isActive = currentPathname === link.href;

        // 🔥 비활성화된 링크 처리
        if (link.disabled) {
            return (
                <li key={link.name}>
          <span className="text-gray-400 cursor-not-allowed">
            {link.name}
          </span>
                </li>
            );
        }

        return (
            <li key={link.name}>
                <Link
                    href={link.href}
                    className={
                        isActive
                            ? "text-[#925C4C] font-bold"
                            : "text-black hover:text-[#925C4C]"
                    }
                >
                    {link.name}
                </Link>
            </li>
        );
    });
};

export default function ProductLayout({
                                          children,
                                      }: {
    children: React.ReactNode;
}) {
    const pathname = usePathname();
    const { user } = useAuthStore();  // 🔥 현재 로그인 유저

    // 🔥 categoryLinks를 컴포넌트 내부로 이동 (userId 사용)
    const categoryLinks = [
        { name: "내 정보", href: "/mypage" },
        { name: "주문 내역", href: "/mypage/order" },
        {
            name: "판매자 스토어",
            href: user?.userId ? `/mypage/store/${user.userId}` : '/mypage',  // 🔥 수정!
            disabled: !user?.userId  // 🔥 비로그인 시 비활성화
        },
        { name: "도안 목록", href: "/mypage/design" },
        { name: "찜 목록", href: "/mypage/like" },
        { name: "리뷰 목록", href: "/mypage/review" },
        { name: "내 글", href: "/mypage/post" },
        { name: "내 댓글", href: "/mypage/comment" },
    ];

    return (
        <div style={{ display: "flex" }}>
            <aside style={{ width: "250px", padding: "20px" }}>
                <nav>
                    <ul className="space-y-4">
                        {renderLinks(categoryLinks, pathname)}
                    </ul>
                </nav>
            </aside>

            <main style={{ flex: 1, padding: "20px" }}>
                {children}
            </main>
        </div>
    );
}