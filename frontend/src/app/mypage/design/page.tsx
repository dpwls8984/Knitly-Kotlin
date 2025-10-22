import Link from "next/link";

export default function ProductListPage() {
    return (
      <div>
        <h1>마이페이지 - 도안 목록</h1>
        <h2>src/app/mypage/design/page.tsx</h2>
        <Link href="/mypage/design/create-design">도안 제작 링크</Link>
      </div>
    );
  }