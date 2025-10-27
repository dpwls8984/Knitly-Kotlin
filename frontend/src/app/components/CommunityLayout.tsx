'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ReactNode } from 'react';

interface MyPageLayoutProps {
  children: ReactNode;
}

const menuItems = [
  { label: '전체', href: '/community', exact: true },
  { label: '자유', href: '/community?category=FREE', category: 'FREE' },
  { label: '질문', href: '/community?category=QUESTION', category: 'QUESTION' },
  { label: '팁', href: '/community?category=TIP', category: 'TIP' },
];

export default function MyPageLayout({ children }: MyPageLayoutProps) {
  const pathname = usePathname();

  const isActive = (href: string, exact?: boolean) => {
    if (exact) {
      return pathname === href;
    }
    return pathname.startsWith(href);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex gap-8">
        {/* 왼쪽 사이드바 */}
        <aside className="w-48 flex-shrink-0">
          <nav className="sticky top-8">
            <ul className="space-y-1">
              {menuItems.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={`block px-4 py-2 rounded-lg transition-colors ${
                      isActive(item.href, item.exact)
                        ? 'bg-[#925C4C] text-white font-semibold'
                        : 'text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </aside>

        {/* 메인 컨텐츠 */}
        <main className="flex-1 min-w-0">
          {children}
        </main>
      </div>
    </div>
  );
}