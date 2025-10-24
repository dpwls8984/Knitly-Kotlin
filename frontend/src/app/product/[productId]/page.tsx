'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
// (가정) 로그인 상태 및 토큰 관리를 위한 store 또는 context
// import { useAuthStore } from '@/lib/store/authStore';

import { addLike, removeFavorite } from '@/lib/api/like.api';
import { useCartStore } from '@/lib/store/cartStore';

// --- 백엔드 연동 ---
// 1. 수정된 ProductDetails 타입 (ProductDetailResponse.java 기반)
interface ProductDetails {
  productId: number;
  title: string;
  description: string;
  productCategory: string; // 백엔드 Enum ('TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC')
  sizeInfo: string;
  price: number;
  createdAt: string; // 백엔드에서 String으로 변환됨
  stockQuantity: number | null; // null이면 상시 판매
  likeCount: number;
  avgReviewRating: number | null;
  productImageUrls: string[];
  isWishedByUser: boolean;
}

/**
 * 찜(하트) 버튼 아이콘 SVG 컴포넌트
 */
const HeartIcon = ({ filled }: { filled: boolean }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill={filled ? '#925C4C' : 'none'}
    stroke={filled ? '#925C4C' : 'currentColor'}
    strokeWidth="2"
    className="w-6 h-6"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
    />
  </svg>
);

/**
 * 이미지 캐러셀 화살표 아이콘 (왼쪽)
 */
const ChevronLeftIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
  </svg>
);

/**
 * 이미지 캐러셀 화살표 아이콘 (오른쪽)
 */
const ChevronRightIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
    <path strokeLinecap="round" strokeLinejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
  </svg>
);

const StarIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="#FFC107" // 노란색 채우기
    className="w-5 h-5" // 크기 조절 (찜 아이콘보다 약간 작게)
  >
    <path
      fillRule="evenodd"
      d="M10.788 3.21c.448-1.077 1.976-1.077 2.424 0l2.082 5.006 5.404.434c1.164.093 1.636 1.545.749 2.305l-4.117 3.527 1.257 5.273c.271 1.136-.964 2.033-1.96 1.425L12 18.354l-4.597 2.917c-.996.608-2.231-.29-1.96-1.425l1.257-5.273-4.117-3.527c-.887-.76-.415-2.212.749-2.305l5.404-.434L10.788 3.21z"
      clipRule="evenodd"
    />
  </svg>
);
/**
 * 탭 버튼 컴포넌트
 */
const TabButton = ({
  children,
  active,
  onClick,
}: {
  children: React.ReactNode;
  active: boolean;
  onClick: () => void;
}) => (
  <button
    onClick={onClick}
    className={`flex-1 py-3 px-4 font-semibold text-center transition-colors ${
      active
        ? 'border-b-2 border-[#925C4C] text-[#925C4C]'
        : 'text-gray-500 hover:text-gray-800'
    }`}
  >
    {children}
  </button>
);

/**
 * '정보' 탭 내부의 행 컴포넌트
 */
const InfoRow = ({ label, value }: { label: string; value: React.ReactNode }) => (
  <div className="flex border-b border-gray-200 py-3">
    <dt className="w-1/3 text-gray-500 font-medium">{label}</dt>
    <dd className="w-2/3 text-gray-900">{value}</dd>
  </div>
);


// --- 제품 상세 페이지 메인 컴포넌트 ---
export default function ProductDetailPage() {
  const router = useRouter();
  const params = useParams();
  const productId = params.productId as string; // URL에서 상품 ID 추출 (string)

  // --- 3. 상태 관리 ---
  const [product, setProduct] = useState<ProductDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 이미지 캐러셀 상태
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  
  // 찜(Wistlist) 상태
  // (수정) 초기값은 Mock 또는 API 응답으로 설정
  const [isWished, setIsWished] = useState(false);
  const [wishCount, setWishCount] = useState(0);

  // 탭 상태
  const [activeTab, setActiveTab] = useState<'info' | 'size' | 'review'>('info');
  // ---
  // ▼▼▼ [추가] 리뷰 탭 DOM 요소에 접근하기 위한 ref ▼▼▼
  const reviewTabRef = useRef<HTMLDivElement>(null);

  // ▼▼▼ [추가] 찜 처리 로딩 상태 ▼▼▼
  const [isWishLoading, setIsWishLoading] = useState(false);
  
  // ▼▼▼ [수정] Zustand 훅 사용 방식 변경 ▼▼▼
  // 필요한 함수/상태를 개별적으로 가져옵니다.
  const addToCart = useCartStore((state) => state.addToCart);
  // isInCart 함수는 스토어 내부에 정의되어 있으므로 스토어 자체에서 가져올 필요는 없습니다.
  // 대신 장바구니 아이템 목록 전체를 가져옵니다.
  const cartItems = useCartStore((state) => state.items);
  // ▲▲▲ [수정] Zustand 훅 사용 방식 변경 ▲▲▲

  // --- 4. 데이터 페칭 (실제 API 호출) ---
  useEffect(() => {
    if (productId) {
      setIsLoading(true);
      setError(null);

      // (가정) Access Token 가져오기 (실제 구현 필요)
      const accessToken = localStorage.getItem('accessToken'); // 예시

      // 실제 API 호출 (GET /products/{productId})
      fetch(`http://localhost:8080/products/${productId}`, {
        method: 'GET',
        headers: {
          // 로그인이 필요한 API이므로 Authorization 헤더 추가
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        }
      })
      .then(async res => { // async 추가
        if (!res.ok) {
          // 백엔드에서 JSON 에러 응답을 줄 경우를 대비
          let errorMsg = '상품 정보를 불러오는데 실패했습니다.';
          try {
            const errorData = await res.json();
            errorMsg = errorData.message || errorMsg;
          } catch (e) {
            // JSON 파싱 실패 시 기본 메시지 사용
          }
          throw new Error(errorMsg);
        }
        return res.json();
      })
      .then((data: ProductDetails) => { // 받아온 데이터 타입 명시
        setProduct(data);
        setIsWished(data.isWishedByUser); // 백엔드 응답 사용
        setWishCount(data.likeCount); // 백엔드의 likeCount 사용
      })
      .catch((err: any) => {
        console.error(err);
        setError(err.message);
      })
      .finally(() => setIsLoading(false));
    }
  }, [productId]); // productId가 변경될 때마다 재호출

  // ▼▼▼ [수정] isAlreadyInCart 계산 위치 및 방식 변경 ▼▼▼
  // 컴포넌트 렌더링 로직 내에서 계산합니다.
  const isAlreadyInCart = product
    ? cartItems.some(item => item.productId === product.productId)
    : false;
  // ▲▲▲ [수정] isAlreadyInCart 계산 위치 및 방식 변경 ▲▲▲

  // --- 5. 이벤트 핸들러 ---

  // 이미지 캐러셀 핸들러
  const handleNextImage = () => {
    if (!product) return;
    setCurrentImageIndex((prevIndex) => (prevIndex + 1) % product.productImageUrls.length); // 필드명 변경
  };
  const handlePrevImage = () => {
    if (!product) return;
    setCurrentImageIndex((prevIndex) => (prevIndex - 1 + product.productImageUrls.length) % product.productImageUrls.length); // 필드명 변경
  };

  // 찜 버튼 핸들러
  const handleWishClick = async () => {
    // (가정) 로그인 상태 확인
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) { alert("로그인이 필요합니다."); return; }
    if (!product || isWishLoading) return; // 상품 정보 없거나 로딩 중이면 중단

    setIsWishLoading(true); // 찜 처리 시작 (버튼 비활성화)
    const originalIsWished = isWished; // 롤백을 위한 원래 상태 저장
    const productIdNum = product.productId; // API 호출에 사용할 숫자 ID

    // 낙관적 업데이트: UI 먼저 변경
    setIsWished((prev) => !prev);
    setWishCount((prevCount) => (originalIsWished ? prevCount - 1 : prevCount + 1));

    try {
      if (originalIsWished) {
        // 찜 취소 API 호출
        await removeFavorite(productIdNum); // 팀원이 만든 API 함수 사용
      } else {
        // 찜 등록 API 호출
        await addLike(productIdNum); // 팀원이 만든 API 함수 사용
      }
      // 성공 시 별도 처리 없음 (이미 UI는 업데이트됨)
      // 필요하다면 여기서 최신 찜 카운트를 다시 받아오는 API 호출 가능
    } catch (error) {
      console.error('찜 처리 실패:', error);
      alert('찜 상태 변경에 실패했습니다. 다시 시도해 주세요.');

      // 실패 시 UI 롤백
      setIsWished(originalIsWished);
      setWishCount((prevCount) => (originalIsWished ? prevCount + 1 : prevCount - 1));
    } finally {
      setIsWishLoading(false); // 찜 처리 완료 (버튼 활성화)
    }
  };
  
// ▼▼▼ [수정] 장바구니 버튼 핸들러 (내부 로직 변경 없음) ▼▼▼
const handleAddToCart = () => {
  if (!product) return;

  // isAlreadyInCart 값은 위에서 계산된 것을 사용합니다.
  if (isAlreadyInCart) {
    alert('이미 장바구니에 담긴 상품입니다.');
    return;
  }

  addToCart({
    productId: product.productId,
    title: product.title,
    price: product.price,
    imageUrl: product.productImageUrls?.[0] || undefined,
  });

  alert(`${product.title} 상품이 장바구니에 담겼습니다.`);
};
// ▲▲▲ [수정] 장바구니 버튼 핸들러 ▲▲▲
  
  const handleBuyNow = () => {
    // 장바구니 페이지로 이동
    router.push('/cart');
  };

  const handleReviewClick = () => {
    setActiveTab('review'); // 탭을 '리뷰'로 활성화
    if (reviewTabRef.current) {
      // '리뷰' 탭 섹션으로 부드럽게 스크롤
      reviewTabRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };


  // --- 6. 렌더링 로직 ---

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-6xl mx-auto p-4 md:p-8">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
          <p>오류: {error}</p>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="max-w-6xl mx-auto p-4 md:p-8">
        <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
          상품 정보를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  // 한정 판매 여부 판단 (isLimited 대신 사용)
  const isProductLimited = product.stockQuantity !== null;

  // --- 메인 UI 렌더링 ---
  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8">
      {/* 상단 섹션: 이미지 + 정보 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-12">
        
        {/* 1. 이미지 캐러셀 */}
        <div className="w-full">
          {/* 카테고리 (이미지 위로 이동) */}
          <div className="mb-2">
            <span className="bg-[#925C4C] text-white text-xs font-semibold px-3 py-1 rounded-full">
              {product.productCategory} {/* 필드명 변경 */}
            </span>
          </div>

          {/* 이미지 캐러셀 컨테이너 */}
          <div className="relative aspect-square bg-gray-100 rounded-lg overflow-hidden shadow-lg">
            {/* 대표 이미지 */}
            {product.productImageUrls.length > 0 ? (
              <img
                src={product.productImageUrls[currentImageIndex]}
                alt={`${product.title} 이미지 ${currentImageIndex + 1}`} // 필드명 변경
                className="w-full h-full object-cover transition-opacity duration-300"
                onError={(e) => { // 이미지 로드 실패 처리
                  (e.target as HTMLImageElement).src = 'https://placehold.co/600x600/CCCCCC/FFFFFF?text=Load+Error';
                }}
              />
            ) : (
              <div className="flex justify-center items-center h-full">
                <p className="text-gray-500">이미지가 없습니다.</p>
              </div>
            )}
            
            {/* 좌/우 화살표 (이미지가 2개 이상일 때만 표시) */}
            {product.productImageUrls.length > 1 && (
              <>
                <button
                  onClick={handlePrevImage}
                  className="absolute top-1/2 left-2 md:left-4 transform -translate-y-1/2 bg-white/70 hover:bg-white rounded-full p-2 transition-colors shadow-md"
                  aria-label="이전 이미지"
                >
                  <ChevronLeftIcon />
                </button>
                <button
                  onClick={handleNextImage}
                  className="absolute top-1/2 right-2 md:right-4 transform -translate-y-1/2 bg-white/70 hover:bg-white rounded-full p-2 transition-colors shadow-md"
                  aria-label="다음 이미지"
                >
                  <ChevronRightIcon />
                </button>
              </>
            )}
          </div>
        </div>

        {/* 2. 상품 정보 및 버튼 */}
        <div className="flex flex-col justify-center">

          {/* ▼▼▼ [수정] 별점 영역에 flex items-center 추가 ▼▼▼ */}
          {product.avgReviewRating !== null && (
            <button
              onClick={handleReviewClick}
              // items-center 클래스를 추가하여 수직 중앙 정렬
              className="flex items-center space-x-1 mb-2 self-start p-1 rounded-md hover:bg-gray-100 transition-colors cursor-pointer"
              aria-label={`평균 별점 ${product.avgReviewRating.toFixed(1)}점`}
            >
              <StarIcon />
              <span className="font-semibold text-gray-700 text-base"> {/* pt-0.5 제거 */}
                {product.avgReviewRating.toFixed(1)}
              </span>
              <span className="text-gray-500 text-sm"> {/* pt-0.5 제거 */}
                리뷰 {product.likeCount}개 {/* 임시: likeCount 대신 리뷰 개수 API필요 */}
              </span>
            </button>
          )}
          {/* ▲▲▲ [수정] 별점 영역 ▲▲▲ */}

          <h1 className="text-3xl font-bold text-gray-900 mb-3">{product.title}</h1>
          
          <p className="text-3xl font-semibold text-gray-800 mb-4">
            {product.price.toLocaleString()}원
          </p>
          
          {/* 한정 상품일 경우 재고 표시 */}
          {isProductLimited && (
            <p className="text-lg text-red-600 font-medium mb-4">
              [한정 수량] 남은 재고: {product.stockQuantity}개
            </p>
          )}

          <div className="border-t border-b border-gray-200 py-6 my-4 space-y-4">
            {/* 장바구니 / 찜 버튼 */}
            <div className="flex items-center space-x-4">
              {/* ▼▼▼ [수정] 장바구니 버튼 상태 반영 (isAlreadyInCart 사용) ▼▼▼ */}
              <button
                onClick={handleAddToCart}
                // isAlreadyInCart 값에 따라 버튼 비활성화/스타일 변경
                disabled={isAlreadyInCart}
                className={`flex-1 font-bold py-3 px-6 rounded-lg transition-colors ${
                  isAlreadyInCart
                    ? 'bg-gray-400 text-gray-700 cursor-not-allowed'
                    : 'bg-gray-200 hover:bg-gray-300 text-gray-800'
                }`}
              >
                {isAlreadyInCart ? '장바구니에 담김' : '장바구니'}
              </button>
              {/* ▲▲▲ [수정] 장바구니 버튼 상태 반영 ▲▲▲ */}

              {/* 찜 버튼 영역 (별점은 위로 이동) */}
              <div className="flex flex-col items-center">
                {/* ▼▼▼ [수정] 찜 버튼에 disabled 속성 추가 ▼▼▼ */}
                <button
                  onClick={handleWishClick}
                  className="p-3 rounded-full hover:bg-gray-100 transition-colors"
                  aria-label="찜하기"
                  disabled={isWishLoading}
                >
                  <HeartIcon filled={isWished} />
                </button>
                {/* ▲▲▲ [수정] 찜 버튼 ▲▲▲ */}
                <span className="text-sm text-gray-600">{wishCount.toLocaleString()}</span>
              </div>
            </div>
            {/* 구매하기 버튼 */}
            <button
              onClick={handleBuyNow}
              className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg"
            >
              구매하기
            </button>
          </div>
        </div>
      </div>

      {/* 고지 사항 */}
      <div className="text-center text-sm text-gray-600 bg-gray-50 p-4 rounded-lg my-8 shadow-sm">
        PDF 상품 특성상 교환/환불 불가, Knitly는 중복 구매에 대한 책임을 지지 않습니다.
      </div>
      
      {/* 탭 섹션 */}
      <div className="w-full mt-12">
        {/* 탭 헤더 */}
        <div className="flex border-b border-gray-300">
          <TabButton active={activeTab === 'info'} onClick={() => setActiveTab('info')}>
            정보
          </TabButton>
          <TabButton active={activeTab === 'size'} onClick={() => setActiveTab('size')}>
            사이즈
          </TabButton>
          <TabButton active={activeTab === 'review'} onClick={() => setActiveTab('review')}>
            리뷰
          </TabButton>
        </div>
        
        {/* 탭 컨텐츠 */}
        <div className="py-8 px-2">
          {/* 정보 탭 */}
          {activeTab === 'info' && (
            <dl className="divide-y divide-gray-200">
              <InfoRow label="상품명" value={product.title} />
              <InfoRow label="카테고리" value={product.productCategory} />
              <InfoRow label="가격" value={`${product.price.toLocaleString()}원`} />
              <InfoRow label="등록일" value={new Date(product.createdAt).toLocaleDateString()} />
              <InfoRow
                label="상품 설명"
                value={
                  <p className="whitespace-pre-wrap">{product.description}</p>
                }
              />
            </dl>
          )}
          
          {/* 사이즈 탭 */}
          {activeTab === 'size' && (
            <div>
              <dl>
                <InfoRow
                  label="사이즈 정보"
                  value={
                    <p className="whitespace-pre-wrap">{product.sizeInfo}</p>
                  }
                />
              </dl>
            </div>
          )}
          
          {/* ▼▼▼ [수정] 리뷰 탭에 ref 연결 ▼▼▼ */}
          {activeTab === 'review' && (
            <div ref={reviewTabRef} className="text-center py-10 text-gray-500"> {/* ref 연결 */}
              {/* 별점 표시는 상단으로 이동했으므로 여기서는 제거 */}
              {/* 리뷰 개수도 표시하고 싶다면 백엔드에서 reviewCount 필드 추가 필요 */}
              <p>리뷰 섹션이 여기에 표시됩니다.</p>
              <p>(현재는 구현 범위가 아닙니다.)</p>
            </div>
          )}
          {/* ▲▲▲ [수정] 리뷰 탭 ▲▲▲ */}
        </div>
      </div>
    </div>
  );
} 