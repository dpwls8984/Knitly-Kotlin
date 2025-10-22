import "./globals.css";
import Image from "next/image";
import Link from "next/link";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const userStatus = 1; //1 로그인, 0 로그아웃
  
  return (
    <html lang="en">
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
            <Link href="/product">도안구매</Link>
            <Link href="/mypage/design/create-design">도안제작</Link>
            <Link href="/community">커뮤니티</Link>
            <Link href="/event">이벤트</Link>
            
          </nav>

          {/* 오른쪽 로그인 버튼*/}
          <nav className="flex">
            {userStatus === 1 && (
              <Link 
                href="/cart"
                className="text-[#925C4C] rounded"
              >
                  장바구니
              </Link>
            )}
          <div>
            {userStatus === 1 ? (
              <Link
                href="/mypage"
                className="px-4 py-2 text-[#925C4C] rounded"
              >
                마이페이지
              </Link>
            ) : (
              <Link
                href="/login"
                className="px-4 py-2 text-[#925C4C] rounded"
              >
                로그인/회원가입
              </Link>
            )}
            {userStatus === 1 && (
              <Link href="/">로그아웃</Link>
            )}
          </div>
          </nav>
        </header>

        <main className="p-10 px-20 py-20">
          {children}
        </main>
      </body>
    </html>
  );
}