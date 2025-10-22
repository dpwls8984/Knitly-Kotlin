"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import React from "react";

const categoryLinks = [
  { name: "전체", href: "/community" },
  { name: "자유", href: "/community/free" },
  { name: "질문", href: "/community/question" },
  { name: "팁", href: "/community/tip" },
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