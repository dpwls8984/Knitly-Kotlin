"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import React from "react";

const categoryLinks = [
  { name: "전체", href: "/product" },
  { name: "상의", href: "/product/top" },
  { name: "하의", href: "/product/bottom" },
  { name: "아우터", href: "/product/outwear" },
  { name: "가방", href: "/product/bag" },
  { name: "기타", href: "/product/etc" },
];

const specialLinks = [
  { name: "무료", href: "/product/free" },
  { name: "한정판매", href: "/product/limited" },
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
          <ul className="space-y-4 mt-8">
            {renderLinks(specialLinks, pathname)}
          </ul>
        </nav>
      </aside>
      
      <main style={{ flex: 1, padding: "20px" }}>
        {children}
      </main>
    </div>
  );
}