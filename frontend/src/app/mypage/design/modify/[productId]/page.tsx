'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
// 공통 폼 컴포넌트 임포트 (경로와 이름 확인)
import DesignForm, {
  DesignSalesData, // '판매 수정'이므로 전체 데이터 타입인 DesignSalesData를 임포트
} from '@/app/components/DesignForm';

// --- (추가) Mock 데이터 ---
// '판매 수정' 시 필요한 '기존 판매 정보'의 Mock 데이터
const mockSalesData: DesignSalesData = {
  id: 'mock-product-id-456',
  name: '아가일 패턴 양말 (Mock)',
  registeredAt: '2025.10.01',
  images: [
    // (가정) 이미지는 placeholder 이미지를 사용합니다.
    'https://placehold.co/100x100/925C4C/white?text=Img1',
  ],
  category: '기타',
  price: 7000,
  isFree: false,
  isLimited: true,
  stock: 50,
  description: '따뜻한 울 소재의 양말 도안입니다. (Mock)',
  designType: '대바늘',
  size: '230-250mm',
};
// ---

export default function ModifyDesignPage() { // 함수 이름 변경 (선택 사항)
  const params = useParams();
  const productId = params.productId as string;

  // '판매 수정'이므로 DesignSalesData 타입을 사용
  const [initialData, setInitialData] =
    useState<DesignSalesData | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (productId) {
      setIsLoading(true);
      setError(null);

      // --- Mock 데이터를 사용하도록 설정 ---
      const timer = setTimeout(() => {
        setInitialData(mockSalesData); // Mock 데이터로 상태 설정
        setIsLoading(false);
      }, 500); // 0.5초 로딩 흉내

      return () => clearTimeout(timer);
      // ---

      /*
      // [실제 API 연동 시 주석 해제]
      fetch(`/my/products/${productId}`)
        .then((res) => {
          if (!res.ok) {
            throw new Error('기존 판매 정보를 불러오는데 실패했습니다.');
          }
          return res.json();
        })
        .then((data) => {
          setInitialData(data.product);
        })
        .catch((err: any) => {
          console.error(err);
          setError(err.message);
        })
        .finally(() => setIsLoading(false));
      */
    }
  }, [productId]);

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

  if (!initialData) {
    return (
      <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
        판매 정보를 찾을 수 없습니다.
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">도안 판매 수정</h1>
      {/*
        공통 폼 컴포넌트 사용
        - isEditMode={true} : '판매 수정' 모드
        - initialData : API로 불러온 *기존 판매 정보* 전체
        - entityId : 현재 '상품 ID' (productId) 전달
      */}
      <DesignForm
        isEditMode={true}
        initialData={initialData}
        entityId={productId}
      />
    </div>
  );
}

// 'use client';

// import { useParams } from 'next/navigation';
// import { useEffect, useState } from 'react';
// // (수정) 컴포넌트 임포트 경로, 이름, 타입 변경
// import DesignForm, {
//   DesignSalesData, // BaseDesignData가 아닌 DesignSalesData를 임포트
// } from '@/app/components/DesignForm';

// export default function EditDesignPage() {
//   const params = useParams();
//   const productId = params.productId as string;

//   // (가정) GET /my/products/{productId}
//   // (수정) useState 타입을 DesignSalesData로 변경
//   const [initialData, setInitialData] =
//     useState<DesignSalesData | undefined>(undefined);
//   const [isLoading, setIsLoading] = useState(true);
//   const [error, setError] = useState<string | null>(null);

//   useEffect(() => {
//     if (productId) {
//       setIsLoading(true);
//       setError(null);

//       fetch(`/my/products/${productId}`) // (필요시 인증 헤더 추가)
//         .then((res) => {
//           if (!res.ok) {
//             throw new Error('기존 판매 정보를 불러오는데 실패했습니다.');
//           }
//           return res.json();
//         })
//         .then((data) => {
//           setInitialData(data.product);
//         })
//         .catch((err: any) => {
//           console.error(err);
//           setError(err.message);
//         })
//         .finally(() => setIsLoading(false));
//     }
//   }, [productId]);

//   if (isLoading) {
//     return (
//       <div className="flex justify-center items-center min-h-[60vh]">
//         <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
//       </div>
//     );
//   }

//   if (error) {
//     return (
//       <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
//         <p>오류: {error}</p>
//       </div>
//     );
//   }

//   if (!initialData) {
//     return (
//       <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
//         판매 정보를 찾을 수 없습니다.
//       </div>
//     );
//   }

//   return (
//     <div>
//       <h1 className="text-3xl font-bold mb-6">도안 판매 수정</h1>
//       {/* (수정) DesignForm 컴포넌트 사용 */}
//       <DesignForm
//         isEditMode={true}
//         initialData={initialData}
//         entityId={productId}
//       />
//     </div>
//   );
// }