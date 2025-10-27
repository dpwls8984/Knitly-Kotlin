// src/app/mypage/design/register/[designId]/page.tsx

'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import DesignForm, { DesignSalesData } from '@/app/components/DesignForm';

export default function RegisterDesignPage() {
  const params = useParams();
  const designId = params.designId as string;
  const [initialData, setInitialData] = useState<DesignSalesData>();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!designId) return;

    const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
    if (!token) {
      setError('로그인이 필요합니다.');
      setIsLoading(false);
      return;
    }

    fetch('http://localhost:8080/designs/my', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error('도안 목록 조회 실패');
        return res.json();
      })
      .then((data) => {
        const target = data.find((d: any) => String(d.designId) === String(designId));
        if (!target) throw new Error('도안을 찾을 수 없습니다.');

        const mapped: DesignSalesData = {
          id: String(target.designId),
          name: target.designName,
          images: [],
          category: '상의',
          price: 0,
          isFree: false,
          isLimited: false,
          stock: 0,
          description: '',
          designType: '',
          size: 'Free',
        };
        setInitialData(mapped);
      })
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, [designId]);

  if (isLoading) return <div>로딩중...</div>;
  if (error) return <div>에러: {error}</div>;

  return (
    <DesignForm
      isEditMode={false}
      initialData={initialData}
      entityId={designId} // ✅ 중요
    />
  );
}
