'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
// (수정) 컴포넌트 임포트 경로 및 이름 변경
import DesignForm, {
  BaseDesignData,
} from '@/app/components/DesignForm';

// --- (추가) Mock 데이터 ---
// '판매 등록' 시 필요한 '도안 기본 정보'의 Mock 데이터
const mockBaseData: Partial<BaseDesignData> = {
  id: 'mock-design-id-123',
  name: '따뜻한 겨울 스웨터 (Mock)'
};
// ---

export default function RegisterDesignPage() {
  const params = useParams();
  const designId = params.designId as string;

  const [initialData, setInitialData] =
    useState<Partial<BaseDesignData> | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (designId) {
      setIsLoading(true);
      setError(null);

      // --- (수정) Mock 데이터를 사용하도록 변경 ---
      // 0.5초 지연을 줘서 로딩 스피너도 테스트합니다.
      const timer = setTimeout(() => {
        setInitialData(mockBaseData); // Mock 데이터로 상태 설정
        setIsLoading(false);
      }, 500);

      return () => clearTimeout(timer);
      // ---

      /*
      // [실제 API 연동 시 주석 해제]
      fetch(`/my/designs/${designId}/base-info`)
        .then((res) => {
          if (!res.ok) {
            throw new Error('도안 기본 정보를 불러오는데 실패했습니다.');
          }
          return res.json();
        })
        .then((data) => {
          setInitialData(data.design);
        })
        .catch((err: any) => {
          console.error(err);
          setError(err.message);
        })
        .finally(() => setIsLoading(false));
      */
    }
  }, [designId]);

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

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">도안 판매 등록</h1>
      {/* (수정) DesignForm 컴포넌트 사용 */}
      <DesignForm
        isEditMode={false}
        initialData={initialData}
        entityId={designId}
      />
    </div>
  );
}

// 'use client';

// import { useParams } from 'next/navigation';
// import { useEffect, useState } from 'react';
// // (수정) 컴포넌트 임포트 경로 및 이름 변경
// import DesignForm, {
//   BaseDesignData,
// } from '@/app/components/DesignForm';

// export default function RegisterDesignPage() {
//   const params = useParams();
//   const designId = params.designId as string;

//   // (가정) GET /my/designs/{designId}/base-info
//   const [initialData, setInitialData] =
//     useState<Partial<BaseDesignData> | undefined>(undefined);
//   const [isLoading, setIsLoading] = useState(true);
//   const [error, setError] = useState<string | null>(null);

//   useEffect(() => {
//     if (designId) {
//       setIsLoading(true);
//       setError(null);

//       fetch(`/my/designs/${designId}/base-info`) // (필요시 인증 헤더 추가)
//         .then((res) => {
//           if (!res.ok) {
//             throw new Error('도안 기본 정보를 불러오는데 실패했습니다.');
//           }
//           return res.json();
//         })
//         .then((data) => {
//           setInitialData(data.design);
//         })
//         .catch((err: any) => {
//           console.error(err);
//           setError(err.message);
//         })
//         .finally(() => setIsLoading(false));
//     }
//   }, [designId]);

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

//   return (
//     <div>
//       <h1 className="text-3xl font-bold mb-6">도안 판매 등록</h1>
//       {/* (수정) DesignForm 컴포넌트 사용 */}
//       <DesignForm
//         isEditMode={false}
//         initialData={initialData}
//         entityId={designId}
//       />
//     </div>
//   );
// }