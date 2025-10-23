'use client';

import { useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

// 도안 상태 타입 정의
type DesignState = 'BEFORE_SALE' | 'ON_SALE' | 'STOPPED';

// 도안 데이터 타입 정의
interface Design {
  id: number;
  name: string;
  fileName: string;
  designState: DesignState;
  createdAt: string;
}

// 도안 카드 컴포넌트
interface DesignCardProps {
  design: Design;
  onStopSale: (id: number) => void;
  onDelete: (id: number) => void;
  onRegisterSale: (id: number) => void;
  onModifyProduct: (id: number) => void;
  onResumeSale: (id: number) => void;
}

function DesignCard({ design, onStopSale, onDelete, onRegisterSale, onModifyProduct, onResumeSale }: DesignCardProps) {
  const getStateText = (state: DesignState) => {
    switch (state) {
      case 'BEFORE_SALE': return '판매전';
      case 'ON_SALE': return '판매중';
      case 'STOPPED': return '판매중지';
      default: return state;
    }
  };

  const getStateColor = (state: DesignState) => {
    switch (state) {
      case 'BEFORE_SALE': return 'bg-gray-500';
      case 'ON_SALE': return 'bg-green-500';
      case 'STOPPED': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  };

  const getTopRightButton = () => {
    if (design.designState === 'ON_SALE') {
      return (
        <button
          onClick={() => onStopSale(design.id)}
          className="px-3 py-1 text-xs bg-red-500 text-white rounded hover:bg-red-600 transition-colors"
        >
          판매중지
        </button>
      );
    } else if (design.designState === 'BEFORE_SALE') {
      return (
        <button
          onClick={() => onDelete(design.id)}
          className="px-3 py-1 text-xs bg-gray-500 text-white rounded hover:bg-gray-600 transition-colors"
        >
          삭제
        </button>
      );
    }
    return null;
  };

  const getBottomRightButton = () => {
    if (design.designState === 'BEFORE_SALE') {
      return (
        <button
          onClick={() => onRegisterSale(design.id)}
          className="px-4 py-2 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d] transition-colors"
        >
          판매등록
        </button>
      );
    } else if (design.designState === 'STOPPED') {
      return (
        <button
          onClick={() => onResumeSale(design.id)}
          className="px-4 py-2 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d] transition-colors"
        >
          다시 판매하기
        </button>
      );
    } else if (design.designState === 'ON_SALE') {
      return (
        <button
          onClick={() => onModifyProduct(design.id)}
          className="px-4 py-2 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d] transition-colors"
        >
          상품수정
        </button>
      );
    }
    return null;
  };

  return (
    <div className="border border-gray-200 rounded-lg p-4 relative flex flex-col">
      {/* 상단 상태 및 버튼 */}
      <div className="flex justify-between items-start mb-4">
        <span className={`px-2 py-1 text-xs text-white rounded ${getStateColor(design.designState)}`}>
          {getStateText(design.designState)}
        </span>
        {getTopRightButton()}
      </div>

      {/* 도안 정보 */}
      <div className="text-center mb-4 flex-1">
        <div className="text-sm text-gray-600 mb-2">{design.fileName}</div>
        <div className="text-xs text-gray-500">{design.name}</div>
      </div>

      {/* 하단 버튼 */}
      <div className="flex justify-end mt-auto">
        {getBottomRightButton()}
      </div>
    </div>
  );
}

export default function DesignListPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showStopSaleModal, setShowStopSaleModal] = useState(false);
  const [selectedDesignId, setSelectedDesignId] = useState<number | null>(null);

  // 임시 도안 데이터 (실제로는 API에서 가져와야 함)
  const [designs, setDesigns] = useState<Design[]>([
    {
      id: 1,
      name: '나만의 겨울 스웨터',
      fileName: 'winter_sweater.pdf',
      designState: 'ON_SALE',
      createdAt: '2024-01-15'
    },
    {
      id: 2,
      name: '봄 가디건',
      fileName: 'spring_cardigan.pdf',
      designState: 'STOPPED',
      createdAt: '2024-01-10'
    },
    {
      id: 3,
      name: '여름 베스트',
      fileName: 'summer_vest.pdf',
      designState: 'BEFORE_SALE',
      createdAt: '2024-01-05'
    }
  ]);

  // + 버튼 토글
  const toggleAddMenu = () => {
    setIsAddMenuOpen(!isAddMenuOpen);
  };

  // 새 도안 만들기
  const handleCreateNewDesign = () => {
    router.push('/mypage/design/create-design');
  };

  // 기존 도안 업로드
  const handleUploadExistingDesign = () => {
    setShowUploadModal(true);
    fileInputRef.current?.click();
  };

  // 파일 업로드 처리
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      // TODO: 실제 파일 업로드 API 호출
      console.log('업로드할 파일:', file.name);
      // 임시로 목록에 추가
      const newDesign: Design = {
        id: Date.now(),
        name: file.name.replace('.pdf', ''),
        fileName: file.name,
        designState: 'BEFORE_SALE',
        createdAt: new Date().toISOString().split('T')[0]
      };
      setDesigns(prev => [newDesign, ...prev]);
      setShowUploadModal(false);
    }
  };

  // 판매 중지
  const handleStopSale = (id: number) => {
    setSelectedDesignId(id);
    setShowStopSaleModal(true);
  };

  // 판매 중지 확인
  const confirmStopSale = () => {
    if (selectedDesignId) {
      setDesigns(prev => prev.map(design => 
        design.id === selectedDesignId 
          ? { ...design, designState: 'STOPPED' as DesignState }
          : design
      ));
    }
    setShowStopSaleModal(false);
    setSelectedDesignId(null);
  };

  // 삭제
  const handleDelete = (id: number) => {
    if (confirm('정말로 삭제하시겠습니까?')) {
      setDesigns(prev => prev.filter(design => design.id !== id));
    }
  };

  // 판매 등록
  const handleRegisterSale = (id: number) => {
    // TODO: 상품 등록 페이지로 이동 (아직 경로가 없음)
    alert('상품 등록 페이지로 이동합니다. (구현 예정)');
  };

  // 다시 판매하기 (판매중지 -> 판매중)
  const handleResumeSale = (id: number) => {
    setDesigns(prev => prev.map(design => 
      design.id === id 
        ? { ...design, designState: 'ON_SALE' as DesignState }
        : design
    ));
  };

  // 상품 수정
  const handleModifyProduct = (id: number) => {
    // TODO: 상품 수정 페이지로 이동 (아직 경로가 없음)
    alert('상품 수정 페이지로 이동합니다. (구현 예정)');
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-7xl mx-auto px-4 py-4">
        {/* 메인 콘텐츠 - 사이드바 제거 */}
        <div className="w-full">
          <h2 className="text-2xl font-bold mb-6">내 도안 목록</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* 새 도안 추가 카드 */}
            <div className="border border-gray-200 rounded-lg p-6 flex flex-col items-center justify-center min-h-[200px]">
              <button
                onClick={toggleAddMenu}
                className="text-4xl text-gray-400 hover:text-[#925C4C] transition-colors mb-4"
              >
                {isAddMenuOpen ? '×' : '+'}
              </button>
              
              {isAddMenuOpen && (
                <div className="space-y-3 w-full">
                  <button
                    onClick={handleCreateNewDesign}
                    className="w-full py-2 px-4 bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d] transition-colors"
                  >
                    새 도안 만들기
                  </button>
                  <button
                    onClick={handleUploadExistingDesign}
                    className="w-full py-2 px-4 bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d] transition-colors"
                  >
                    기존 도안 업로드
                  </button>
                </div>
              )}
            </div>

            {/* 도안 목록 */}
            {designs.map((design) => (
              <DesignCard
                key={design.id}
                design={design}
                onStopSale={handleStopSale}
                onDelete={handleDelete}
                onRegisterSale={handleRegisterSale}
                onModifyProduct={handleModifyProduct}
                onResumeSale={handleResumeSale}
              />
            ))}
          </div>
        </div>
      </div>

      {/* 파일 업로드 input (숨김) */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf"
        onChange={handleFileUpload}
        className="hidden"
      />

      {/* 판매 중지 확인 모달 */}
      {showStopSaleModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">판매 중지</h3>
            <p className="text-gray-600 mb-6">판매를 중지하시겠습니까?</p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowStopSaleModal(false)}
                className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
              >
                취소
              </button>
              <button
                onClick={confirmStopSale}
                className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition-colors"
              >
                예
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
