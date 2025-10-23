'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

// 1. 폼 데이터 타입
export interface DesignSalesData {
  id: string; // 상품 ID (productId)
  name: string; // 상품 이름 (사용자가 입력/수정 가능)
  registeredAt?: string; // (수정) 등록일은 이제 필수가 아님 (수정 시에만 표시)
  images: string[]; // 기존 샘플 이미지 URL 목록
  category: '상의' | '하의' | '아우터' | '가방' | '기타' | ''; // (수정) '가방' 추가
  price: number;
  isFree: boolean;
  isLimited: boolean;
  stock: number;
  description: string;
  designType: string; // 구분
  size: string; // 사이즈
}

// (가정) '판매 등록' 시 받아올 *기본* 도안 정보 타입
// (수정) 등록일(registeredAt) 제거 - 이제 필요 없음
export interface BaseDesignData {
  id: string; // 도안 ID (designId)
  name: string; // 원본 도안 PDF 이름 (참고용, 수정 불가)
}

// 2. 컴포넌트 Props 정의
interface DesignFormProps {
  isEditMode: boolean; // true: 수정 모드, false: 등록 모드
  initialData?: Partial<DesignSalesData> | Partial<BaseDesignData>; // 타입은 그대로 유지
  entityId: string; // 등록 시: designId, 수정 시: productId
}

// 3. 컴포넌트 함수 이름
export default function DesignForm({
  isEditMode,
  initialData,
  entityId,
}: DesignFormProps) {
  const router = useRouter();

  // 4. 폼 상태 관리
  // (수정) name 상태 초기값을 '' 로 변경
  const [name, setName] = useState('');
  // (수정) registeredAt 상태 제거
  // const [registeredAt, setRegisteredAt] = useState('');
  const [originalDesignName, setOriginalDesignName] = useState(''); // (추가) 원본 PDF 이름 표시용
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [existingImages, setExistingImages] = useState<string[]>([]);
  const [category, setCategory] = useState<DesignSalesData['category']>('');
  const [price, setPrice] = useState(0);
  const [isFree, setIsFree] = useState(false);
  const [isLimited, setIsLimited] = useState(false);
  const [stock, setStock] = useState(0);
  const [description, setDescription] = useState('');
  const [designType, setDesignType] = useState('');
  const [size, setSize] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 5. 초기 데이터 설정 (수정)
  useEffect(() => {
    if (initialData) {
      // 등록 모드: 원본 PDF 이름만 참고용으로 저장
      if (!isEditMode) {
        setOriginalDesignName(initialData.name || '원본 이름 로드 실패');
        // 상품 이름은 빈 칸으로 시작
        setName('');
      }
      // 수정 모드: 모든 데이터 채우기 (상품 이름 포함)
      else if ('price' in initialData) {
        const data = initialData as DesignSalesData;
        setName(data.name || ''); // 수정 시에는 기존 상품 이름 로드
        setOriginalDesignName(data.name || ''); // 수정 시 참고용 이름도 일단 상품명으로
        setExistingImages(data.images || []);
        setCategory(data.category || '');
        setPrice(data.price || 0);
        setIsFree(data.isFree || false);
        setIsLimited(data.isLimited || false);
        setStock(data.stock || 0);
        setDescription(data.description || '');
        setDesignType(data.designType || '');
        setSize(data.size || '');
      }
    }
  }, [initialData, isEditMode]);

  // 6. 이미지 파일 핸들러 (이하 동일)
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      if (files.length + existingImages.length > 10) {
        alert('샘플 이미지는 최대 10개까지 등록할 수 있습니다.');
        return;
      }
      const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];
      const invalidFiles = files.filter(
        (file) => !allowedTypes.includes(file.type)
      );
      if (invalidFiles.length > 0) {
        alert('png, jpg, jpeg 파일 형식만 등록할 수 있습니다.');
        return;
      }
      setSelectedFiles(files);
      const previews = files.map((file) => URL.createObjectURL(file));
      setImagePreviews(previews);
    }
  };

  // 7. '무료' 체크박스 핸들러
  const handleFreeCheck = (e: React.ChangeEvent<HTMLInputElement>) => {
    const checked = e.target.checked;
    setIsFree(checked);
    if (checked) setPrice(0);
  };

  // 8. '한정' 체크박스 핸들러
  const handleLimitedCheck = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIsLimited(e.target.checked);
  };

  // 9. 폼 제출 핸들러 (백엔드 연동)
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // (추가) 이름 필드가 비어있는지 확인
    if (!name.trim()) {
      alert('상품 이름을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setError(null);

    const formData = new FormData();

    // (수정) 백엔드로 보낼 데이터에 'name' 추가
    const salesData = {
      name: name.trim(), // 사용자가 입력한 상품 이름
      category,
      price: isFree ? 0 : price,
      isFree,
      isLimited,
      stock: isLimited ? stock : 0,
      description,
      designType,
      size,
    };
    formData.append('data', JSON.stringify(salesData));

    selectedFiles.forEach((file) => {
      formData.append('images', file);
    });

    try {
      const url = isEditMode
        ? `/my/products/${entityId}/modify` // (수정) PATCH
        : `/my/products/${entityId}/sale`; // (등록) POST
      const method = isEditMode ? 'PATCH' : 'POST';

      const response = await fetch(url, {
        method: method,
        body: formData,
        // headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '요청 처리에 실패했습니다.');
      }

      alert(isEditMode ? '수정되었습니다.' : '등록되었습니다.');
      router.push('/mypage/design');
    } catch (err: any) {
      console.error(err);
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // 폼 UI 렌더링
  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white shadow-lg rounded-lg p-8 space-y-6"
    >
      {/* (수정) 도안 이름 -> 상품 이름으로 변경, 입력 가능하도록 수정 */}
      <FormRow label="상품 이름">
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="판매할 상품의 이름을 입력하세요"
          required // 이름은 필수 입력
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
        />
        {/* (추가) 원본 PDF 이름 참고용 표시 (등록 시에만) */}
        {!isEditMode && originalDesignName && (
           <p className="text-sm text-gray-500 mt-1">
             (원본 도안 파일명: {originalDesignName})
           </p>
        )}
      </FormRow>

      {/* (수정) 등록일 필드 제거 */}
      {/* <FormRow label="등록일"> ... </FormRow> */}

      {/* 샘플 이미지 등록 */}
      <FormRow label="샘플 이미지">
        <input
          type="file"
          multiple
          accept=".png,.jpg,.jpeg"
          onChange={handleImageChange}
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
        />
        <p className="text-sm text-gray-500 mt-1">
          최대 10개, png/jpg/jpeg 형식만 가능합니다.
        </p>
        <div className="flex flex-wrap gap-2 mt-2">
          {existingImages.map((imgUrl, index) => (
            <img
              key={`exist-${index}`}
              src={imgUrl}
              alt="기존 이미지"
              className="w-24 h-24 object-cover rounded"
            />
          ))}
          {imagePreviews.map((previewUrl, index) => (
            <img
              key={`new-${index}`}
              src={previewUrl}
              alt="새 이미지"
              className="w-24 h-24 object-cover rounded"
            />
          ))}
        </div>
      </FormRow>

      {/* 카테고리 (수정: '가방' 추가) */}
      <FormRow label="카테고리">
        <select
          value={category}
          onChange={(e) =>
            setCategory(e.target.value as DesignSalesData['category'])
          }
          required
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
        >
          <option value="">선택하세요</option>
          <option value="상의">상의</option>
          <option value="하의">하의</option>
          <option value="아우터">아우터</option>
          <option value="가방">가방</option> {/* 추가됨 */}
          <option value="기타">기타</option>
        </select>
      </FormRow>

      {/* 가격 */}
      <FormRow label="가격">
        <div className="flex items-center gap-4">
          <input
            type="number"
            value={price}
            onChange={(e) => setPrice(Number(e.target.value))}
            disabled={isEditMode || isFree} // 수정 모드이거나, 무료 체크 시 비활성화
            required={!isFree} // 무료가 아닐 시 필수
            min="0" // 가격은 0 이상
            className={
              isEditMode || isFree
                ? 'w-full p-2 border border-gray-200 rounded-md bg-gray-100 text-gray-500 cursor-not-allowed'
                : 'w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors'
            }
          />
          <label className="flex items-center gap-2 flex-shrink-0">
            <input
              type="checkbox"
              checked={isFree}
              onChange={handleFreeCheck}
              disabled={isEditMode} // 수정 모드 시 가격 관련 수정 불가
              className="w-5 h-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C]"
            />
            무료
          </label>
        </div>
        {isEditMode && (
          <p className="text-sm text-gray-500 mt-1">
            등록된 상품의 가격은 수정할 수 없습니다.
          </p>
        )}
      </FormRow>

      {/* 한정 여부 */}
      <FormRow label="한정 여부">
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={isLimited}
              onChange={handleLimitedCheck}
              className="w-5 h-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C]"
            />
            한정
          </label>
          {isLimited && (
            <input
              type="number"
              value={stock}
              onChange={(e) => setStock(Number(e.target.value))}
              placeholder="재고 입력"
              required={isLimited} // 한정일 시 필수
              min="0" // 재고는 0 이상
              className="w-32 p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
            />
          )}
        </div>
      </FormRow>

      {/* 도안 설명 */}
      <FormRow label="도안 설명">
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={5}
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
          placeholder="도안에 대해 설명해주세요."
        />
      </FormRow>

      {/* 구분 */}
      <FormRow label="구분">
        <input
          type="text"
          value={designType}
          onChange={(e) => setDesignType(e.target.value)}
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
          placeholder="예: 코바늘, 대바늘"
        />
      </FormRow>

      {/* 사이즈 */}
      <FormRow label="사이즈">
        <input
          type="text"
          value={size}
          onChange={(e) => setSize(e.target.value)}
          className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#925C4C] focus:border-transparent transition-colors"
          placeholder="예: S, M, L 또는 가슴단면 50cm"
        />
      </FormRow>

      {/* 에러 메시지 */}
      {error && <p className="text-red-600 text-sm">{error}</p>}

      {/* 제출 버튼 */}
      <div className="flex justify-end pt-4">
        <button
          type="submit"
          disabled={isLoading}
          className="bg-[#925C4C] text-white px-6 py-2 rounded-lg hover:bg-[#7a4c3e] transition-colors font-semibold disabled:bg-gray-400"
        >
          {isLoading
            ? '처리 중...'
            : isEditMode
            ? '수정하기'
            : '판매 등록'}
        </button>
      </div>
    </form>
  );
}

// 폼 레이아웃을 위한 공용 컴포넌트
const FormRow = ({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) => (
  <div>
    <label className="block text-lg font-semibold text-gray-800 mb-2">
      {label}
    </label>
    {children}
  </div>
);