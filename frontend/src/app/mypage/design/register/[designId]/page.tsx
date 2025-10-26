'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import DesignForm, { BaseDesignData } from '@/app/components/DesignForm';
// (추가) api 인스턴스를 import 합니다.
import api from '@/lib/api/axios';

// (추가) GET /designs/my API의 응답 DTO 타입을 정의합니다.
//
interface DesignListResponse {
  designId: number; //
  designName: string; //
  designState: 'ON_SALE' | 'STOPPED' | 'BEFORE_SALE'; //
  // ... (DesignListResponse DTO에 다른 필드가 있다면 추가)
}

export default function RegisterDesignPage() {
  const params = useParams();
  const designId = params.designId as string; // URL에서 넘어온 designId

  const [initialData, setInitialData] = useState<
    Partial<BaseDesignData> | undefined
  >(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (designId) {
      setIsLoading(true);
      setError(null);

      // --- (수정) Mock 데이터 대신 실제 API 호출 ---
      const fetchDesignInfo = async () => {
        try {
          // 1. /designs/my API를 호출해 사용자의 모든 도안 목록을 가져옵니다.
          //
          const response = await api.get<DesignListResponse[]>('/designs/my');
          const allDesigns = response.data;

          // 2. 전체 목록에서 현재 URL의 designId와 일치하는 도안을 찾습니다.
          const matchingDesign = allDesigns.find(
            (design) => design.designId.toString() === designId
          );

          if (matchingDesign) {
            // 3. 찾은 도안의 정보를 DesignForm이 필요로 하는 BaseDesignData 형식으로 매핑합니다.
            //
            setInitialData({
              id: matchingDesign.designId.toString(),
              name: matchingDesign.designName,
            });
          } else {
            throw new Error('해당 도안을 찾을 수 없거나 접근 권한이 없습니다.');
          }
        } catch (err: any) {
          console.error(err);
          setError(err.message || '도안 정보를 불러오는데 실패했습니다.');
        } finally {
          setIsLoading(false);
        }
      };

      fetchDesignInfo();
      // ---
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
      {/* DesignForm은 받은 props를 기반으로 동작합니다:
        - isEditMode={false}: 등록 모드로 동작
        - initialData: 위에서 찾은 도안의 이름(name)을 표시
        - entityId={designId}: "판매 등록" 버튼 클릭 시 이 designId를 사용해
                              POST /my/products/{designId}/sale API를 호출
       
      */}
      <DesignForm
        isEditMode={false}
        initialData={initialData}
        entityId={designId}
      />
    </div>
  );
}