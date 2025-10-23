'use client';

// Next.js 및 React에서 필요한 훅들을 임포트합니다.
import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';

// --- 백엔드 주니어님께 ---
// 1. (가정) 제품 상세 정보 타입
// ... (이전과 동일) ...
interface ProductDetails {
  id: string;
  name: string;
  category: string;
  price: number;
  images: string[];
  isLimited: boolean;
  stock: number;
  description: string;
  designType: string;
  registeredAt: string;
  size: string;
  initialWishCount: number;
  isWishedByUser: boolean;
}

// 2. Mock 데이터
const mockProduct: ProductDetails = {
  id: 'prod-abc-123',
  name: '따뜻한 겨울 스웨터 도안',
  category: '상의',
  price: 15000,
  images: [
    'https://placehold.co/600x600/925C4C/white?text=Image+1',
    'https://placehold.co/600x600/EAD9D5/white?text=Image+2',
    'https://placehold.co/600x600/D5E0EA/white?text=Image+3',
  ],
  isLimited: true,
  stock: 20,
  description: '부드러운 울 소재를 사용한 따뜻한 스웨터입니다.\n초보자도 쉽게 따라 할 수 있는 상세한 도안이 포함되어 있습니다.',
  designType: '대바늘',
  registeredAt: '2023.10.01',
  size: '가슴둘레 : 86 (92) 97 (100) 106 (114) 125 cm\n옷길이 : 50 (50) 52 (52) 54 (54) 56 cm (뒷목 중심부터 쟀을 때)',
  initialWishCount: 999,
  isWishedByUser: false,
};
// ---

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
  const productId = params.productId as string; // URL에서 상품 ID 추출

  // --- 3. 상태 관리 ---
  const [product, setProduct] = useState<ProductDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // (기능 1) 이미지 캐러셀 상태
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  
  // (기능 4) 찜(Wistlist) 상태
  const [isWished, setIsWished] = useState(false);
  const [wishCount, setWishCount] = useState(0);

  // (기능 7) 탭 상태
  const [activeTab, setActiveTab] = useState<'info' | 'size' | 'review'>('info');
  // ---

  // --- 4. 데이터 페칭 (Mock 사용) ---
  useEffect(() => {
    if (productId) {
      setIsLoading(true);
      setError(null);
      
      // [Mock 데이터 로직]
      const timer = setTimeout(() => {
        setProduct(mockProduct);
        setIsWished(mockProduct.isWishedByUser);
        setWishCount(mockProduct.initialWishCount);
        setIsLoading(false);
      }, 500);

      return () => clearTimeout(timer);
      
      /*
      // [실제 API 연동 시 주석 해제]
      fetch(`/products/${productId}`)
        .then(res => {
          if (!res.ok) throw new Error('상품 정보를 불러오는데 실패했습니다.');
          return res.json();
        })
        .then(data => {
          setProduct(data.product);
          setIsWished(data.product.isWishedByUser);
          setWishCount(data.product.initialWishCount);
        })
        .catch(err => setError(err.message))
        .finally(() => setIsLoading(false));
      */
    }
  }, [productId]);

  // --- 5. 이벤트 핸들러 ---

  // (기능 1) 이미지 캐러셀 핸들러
  const handleNextImage = () => {
    if (!product) return;
    setCurrentImageIndex((prevIndex) => (prevIndex + 1) % product.images.length);
  };
  const handlePrevImage = () => {
    if (!product) return;
    setCurrentImageIndex((prevIndex) => (prevIndex - 1 + product.images.length) % product.images.length);
  };

  // (기능 4) 찜 버튼 핸들러
  const handleWishClick = () => {
    // (가정) 로그인 상태 확인 로직 추가 필요
    
    // 즉각적인 UI 반응
    setIsWished((prev) => !prev);
    setWishCount((prevCount) => (isWished ? prevCount - 1 : prevCount + 1));

    // (가정) 백엔드 API 호출 (Toggling) - 실제 구현 시 주석 해제
    // fetch(`/products/${productId}/wish`, { method: isWished ? 'DELETE' : 'POST', ... });
    console.log(isWished ? '찜 취소 API 호출' : '찜 등록 API 호출');
  };
  
  // (기능 5) 장바구니 / 구매하기 핸들러
  const handleAddToCart = () => {
    // (가정) POST /cart { productId, quantity: 1 }
    console.log('장바구니 담기 API 호출');
    alert('장바구니에 담겼습니다. (Mock)');
  };
  
  // ▼▼▼ [수정된 부분] ▼▼▼
  const handleBuyNow = () => {
    // '/purchase' 대신 올바른 경로인 '/cart'로 이동합니다.
    router.push('/cart'); 
  };
  // ▲▲▲ [수정된 부분] ▲▲▲


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
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
        <p>오류: {error}</p>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
        상품 정보를 찾을 수 없습니다.
      </div>
    );
  }

  // --- 메인 UI 렌더링 ---
  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8">
      {/* 상단 섹션: 이미지 + 정보 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-12">
        
        {/* 1. 이미지 캐러셀 */}
        <div className="w-full">
          {/* (기능 2) 카테고리 (이미지 위로 이동) */}
          <div className="mb-2">
            <span className="bg-[#925C4C] text-white text-xs font-semibold px-3 py-1 rounded-full">
              {product.category}
            </span>
          </div>

          {/* 이미지 캐러셀 컨테이너 */}
          <div className="relative aspect-square bg-gray-100 rounded-lg overflow-hidden shadow-lg">
            {/* 대표 이미지 */}
            <img
              src={product.images[currentImageIndex]}
              alt={`${product.name} 이미지 ${currentImageIndex + 1}`}
              className="w-full h-full object-cover transition-opacity duration-300"
            />
            
            {/* 좌/우 화살표 */}
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
          </div>
        </div>

        {/* 2. 상품 정보 및 버튼 (기능 3, 4, 5) */}
        <div className="flex flex-col justify-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-3">{product.name}</h1>
          
          <p className="text-3xl font-semibold text-gray-800 mb-4">
            {product.price.toLocaleString()}원
          </p>
          
          {/* (기능 3) 한정 상품일 경우 재고 표시 */}
          {product.isLimited && (
            <p className="text-lg text-red-600 font-medium mb-4">
              [한정 수량] 남은 재고: {product.stock}개
            </p>
          )}

          <div className="border-t border-b border-gray-200 py-6 my-4 space-y-4">
            {/* (기능 4) 장바구니 / 찜 버튼 */}
            <div className="flex items-center space-x-4">
              <button
                onClick={handleAddToCart}
                className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold py-3 px-6 rounded-lg transition-colors"
              >
                장바구니
              </button>
              
              <div className="flex flex-col items-center">
                <button
                  onClick={handleWishClick}
                  className="p-3 rounded-full hover:bg-gray-100 transition-colors"
                  aria-label="찜하기"
                >
                  <HeartIcon filled={isWished} />
                </button>
                <span className="text-sm text-gray-600">{wishCount.toLocaleString()}</span>
              </div>
            </div>

            {/* (기능 5) 구매하기 버튼 */}
            <button
              onClick={handleBuyNow}
              className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg"
            >
              구매하기
            </button>
          </div>
        </div>
      </div>

      {/* (기능 6) 고지 사항 */}
      <div className="text-center text-sm text-gray-600 bg-gray-50 p-4 rounded-lg my-8 shadow-sm">
        PDF 상품 특성상 교환/환불 불가, Knitly는 중복 구매에 대한 책임을 지지 않습니다.
      </div>
      
      {/* (기능 7, 8) 탭 섹션 */}
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
              <InfoRow label="상품명" value={product.name} />
              <InfoRow label="카테고리" value={product.category} />
              <InfoRow label="가격" value={`${product.price.toLocaleString()}원`} />
              <InfoRow label="구분" value={product.designType} />
              <InfoRow label="등록일" value={product.registeredAt} />
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
                    <p className="whitespace-pre-wrap">{product.size}</p>
                  }
                />
              </dl>
            </div>
          )}
          
          {/* 리뷰 탭 */}
          {activeTab === 'review' && (
            <div className="text-center py-10 text-gray-500">
              {/* 요청하신 대로, 리뷰 탭은 비워둡니다. */}
              <p>리뷰 섹션이 여기에 표시됩니다.</p>
              <p>(현재는 구현 범위가 아닙니다.)</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}