"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import React from "react";

const categoryLinks = [
  { name: "내 정보", href: "/mypage" },
  { name: "주문 내역", href: "/mypage/order" },
  { name: "판매자 스토어", href: "/mypage/store" },
  { name: "도안 목록", href: "/mypage/design" },
  { name: "찜 목록", href: "/mypage/like" },
  { name: "리뷰 목록", href: "/mypage/review" },
  { name: "내 글", href: "/mypage/post" },
  { name: "내 댓글", href: "/mypage/comment" },
];
const renderLinks = (
  links: { name: string; href: string }[],
  currentPathname: string
) => {
  return links.map((link) => {
    const isActive = currentPathname === link.href;

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