'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
// 공통 폼 컴포넌트 임포트 (경로와 이름 확인)
import DesignForm, {
  DesignSalesData, // '판매 수정'이므로 전체 데이터 타입인 DesignSalesData를 임포트
} from '@/app/components/DesignForm';
//import { ProductCategory } from '@/types/product.types'; // (가정) ProductCategory 타입이 정의된 경로
import api from '@/lib/api/axios';


export default function ModifyDesignPage() { // 함수 이름 변경 (선택 사항)
  const params = useParams();
  const productId = params.productId as string;

  // '판매 수정'이므로 DesignSalesData 타입을 사용
  const [initialData, setInitialData] =
    useState<DesignSalesData | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // ModifyDesignPage.tsx 내부 useEffect 수정
  useEffect(() => {
    if (productId) {
      setIsLoading(true);
      setError(null);

      fetch(`http://localhost:8080/products/${productId}`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
        },
      })
        .then((res) => {
          if (!res.ok) throw new Error('기존 판매 정보를 불러오는데 실패했습니다.');
          return res.json();
        })
        .then((data) => {
          // ✅ ProductDetailResponse를 DesignSalesData로 매핑
          const mappedData: DesignSalesData = {
            id: String(data.productId),
            name: data.title,
            images: data.productImageUrls || [],
            category:
              data.productCategory === 'TOP'
                ? '상의'
                : data.productCategory === 'BOTTOM'
                ? '하의'
                : data.productCategory === 'OUTER'
                ? '아우터'
                : data.productCategory === 'BAG'
                ? '가방'
                : '기타',
            price: data.price,
            isFree: data.price === 0,
            isLimited: data.stockQuantity !== null,
            stock: data.stockQuantity ?? 0,
            description: data.description,
            designType: '',
            size: data.sizeInfo,
          };
          setInitialData(mappedData);
        })
        .catch((err) => setError(err.message))
        .finally(() => setIsLoading(false));
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
