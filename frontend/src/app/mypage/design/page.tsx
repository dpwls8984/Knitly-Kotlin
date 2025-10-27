'use client';

import { useRouter } from 'next/navigation';
import { useState, useRef, useEffect } from 'react';
import { 
  getMyDesigns, 
  uploadDesignPdf, 
  deleteDesign,
  stopDesignSale,
  relistDesign,
  viewDesignPdf,
  downloadDesignPdf
} from '@/lib/api/design.api';
import type { DesignListResponse, DesignState } from '@/lib/api/design.api';

// 도안 카드 컴포넌트
interface DesignCardProps {
  design: DesignListResponse;
  onStopSale: (id: number) => void;
  onDelete: (id: number) => void;
  onRegisterSale: (id: number) => void;
  onResumeSale: (id: number) => void;
}

function DesignCard({ design, onStopSale, onDelete, onRegisterSale, onResumeSale }: DesignCardProps) {
  const router = useRouter();

  // 상태에 따른 배지 렌더링
  const getStateBadge = () => {
    switch (design.designState) {
      case 'ON_SALE':
        return <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded">판매중</span>;
      case 'STOPPED':
        return <span className="px-2 py-1 bg-gray-100 text-gray-800 text-xs rounded">판매중지</span>;
      case 'BEFORE_SALE':
        return <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded">판매 전</span>;
      default:
        return null;
    }
  };

  // 하단 우측 버튼 렌더링
  const getBottomRightButton = () => {
    switch (design.designState) {
      case 'ON_SALE':
        return (
          <div className="flex gap-2">
            <button
              onClick={() => router.push(`/mypage/design/modify/${design.designId}`)}
              className="px-3 py-1 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d]"
            >
              수정
            </button>
            <button
              onClick={() => onStopSale(design.designId)}
              className="px-3 py-1 text-sm bg-gray-500 text-white rounded hover:bg-gray-600"
            >
              판매 중지
            </button>
          </div>
        );
      case 'STOPPED':
        return (
          <div className="flex gap-2">
            <button
              onClick={() => onResumeSale(design.designId)}
              className="px-3 py-1 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d]"
            >
              다시 판매하기
            </button>
            <button
              onClick={() => onDelete(design.designId)}
              className="px-3 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600"
            >
              삭제
            </button>
          </div>
        );
      case 'BEFORE_SALE':
        return (
          <div className="flex gap-2">
            <button
              onClick={() => onRegisterSale(design.designId)}
              className="px-3 py-1 text-sm bg-[#925C4C] text-white rounded hover:bg-[#7a4a3d]"
            >
              판매 등록
            </button>
            <button
              onClick={() => onDelete(design.designId)}
              className="px-3 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600"
            >
              삭제
            </button>
          </div>
        );
      default:
        return null;
    }
  };

  // PDF 보기 핸들러 (새 창)
  const handlePdfView = async () => {
    try {
      await viewDesignPdf(design.designId);
    } catch (error) {
      alert('PDF를 불러오는데 실패했습니다.');
    }
  };

  // PDF 다운로드 핸들러
  const handlePdfDownload = async () => {
    try {
      await downloadDesignPdf(design.designId, design.designName);
    } catch (error) {
      alert('PDF 다운로드에 실패했습니다.');
    }
  };

  return (
    <div className="border rounded-lg p-4 hover:shadow-md transition-shadow flex flex-col h-full">
      {/* 상단: 상태 배지 */}
      <div className="flex justify-between items-center mb-3">
        {getStateBadge()}
        <div className="flex gap-2">
          <button
            onClick={handlePdfView}
            className="text-sm text-[#925C4C] hover:underline"
          >
            PDF 보기
          </button>
          <button
            onClick={handlePdfDownload}
            className="text-sm text-gray-600 hover:underline"
          >
            다운로드
          </button>
        </div>
      </div>

      {/* 중단: 도안 정보 */}
      <div className="flex-1 mb-3">
        <div className="font-semibold text-lg mb-2">{design.designName}</div>
        <div className="text-xs text-gray-500">
          생성일: {new Date(design.createdAt).toLocaleDateString()}
        </div>
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
  
  // 도안 목록 상태
  const [designs, setDesigns] = useState<DesignListResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 업로드 관련 상태
  const [uploadDesignName, setUploadDesignName] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  // 도안 목록 조회
  const fetchDesigns = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    
    try {
      const data = await getMyDesigns();
      setDesigns(data);
    } catch (error: any) {
      console.error('도안 목록 조회 실패:', error);
      
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '도안 목록을 불러올 수 없습니다.';
      setErrorMessage(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  // 컴포넌트 마운트 시 도안 목록 조회
  useEffect(() => {
    fetchDesigns();
  }, []);

  // + 버튼 토글
  const toggleAddMenu = () => {
    setIsAddMenuOpen(!isAddMenuOpen);
  };

  // 새 도안 만들기
  const handleCreateNewDesign = () => {
    router.push('/mypage/design/create-design');
  };

  // 기존 도안 업로드 모달 열기
  const handleUploadExistingDesign = () => {
    setShowUploadModal(true);
    setIsAddMenuOpen(false);
  };

  // 파일 업로드 처리
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // PDF 파일 검증
    if (!file.type.includes('pdf')) {
      alert('PDF 파일만 업로드 가능합니다.');
      return;
    }

    setIsUploading(true);
    setErrorMessage(null);

    try {
      const result = await uploadDesignPdf(
        file, 
        uploadDesignName.trim() || undefined
      );
      
      console.log('업로드 성공:', result);

      // 목록 새로고침
      await fetchDesigns();
      
      // 모달 닫기
      setShowUploadModal(false);
      setUploadDesignName('');
      
      // 파일 입력 초기화
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

    } catch (error: any) {
      console.error('파일 업로드 실패:', error);
      
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '파일 업로드 중 오류가 발생했습니다.';
      setErrorMessage(errorMsg);
      
      // 3초 후 에러 메시지 제거
      setTimeout(() => setErrorMessage(null), 3000);
    } finally {
      setIsUploading(false);
    }
  };

  // 판매 중지
  const handleStopSale = (id: number) => {
    setSelectedDesignId(id);
    setShowStopSaleModal(true);
  };

  // 판매 중지 확인 - API 호출
  const confirmStopSale = async () => {
    if (!selectedDesignId) return;

    try {
      await stopDesignSale(selectedDesignId);
      
      // 로컬 상태 업데이트
      setDesigns(prev => prev.map(design => 
        design.designId === selectedDesignId 
          ? { ...design, designState: 'STOPPED' as DesignState }
          : design
      ));
      
      setShowStopSaleModal(false);
      setSelectedDesignId(null);
    } catch (error: any) {
      console.error('판매 중지 실패:', error);
      
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '판매 중지에 실패했습니다.';
      setErrorMessage(errorMsg);
      setTimeout(() => setErrorMessage(null), 3000);
    }
  };

  // 삭제
  const handleDelete = async (id: number) => {
    if (!confirm('정말로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      return;
    }

    try {
      await deleteDesign(id);
      
      // 목록에서 제거
      setDesigns(prev => prev.filter(design => design.designId !== id));
      
    } catch (error: any) {
      console.error('삭제 실패:', error);
      
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '도안 삭제 중 오류가 발생했습니다.';
      setErrorMessage(errorMsg);
      setTimeout(() => setErrorMessage(null), 3000);
    }
  };

  // 판매 등록
  const handleRegisterSale = (id: number) => {
    router.push(`/mypage/design/register/${id}`);
  };

  // 다시 판매하기 (판매중지 -> 판매중) - API 호출
  const handleResumeSale = async (id: number) => {
    try {
      await relistDesign(id);
      
      // 로컬 상태 업데이트
      setDesigns(prev => prev.map(design => 
        design.designId === id 
          ? { ...design, designState: 'ON_SALE' as DesignState }
          : design
      ));
    } catch (error: any) {
      console.error('재판매 실패:', error);
      
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '재판매 처리에 실패했습니다.';
      setErrorMessage(errorMsg);
      setTimeout(() => setErrorMessage(null), 3000);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">내 도안</h1>
        
        {/* + 버튼과 드롭다운 메뉴 */}
        <div className="relative">
          <button
            onClick={toggleAddMenu}
            className="w-12 h-12 bg-[#925C4C] text-white rounded-full flex items-center justify-center text-2xl hover:bg-[#7a4a3d] transition-colors"
          >
            +
          </button>
          
          {/* 드롭다운 메뉴 */}
          {isAddMenuOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
              <button
                onClick={handleCreateNewDesign}
                className="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-100"
              >
                새 도안 만들기
              </button>
              <button
                onClick={handleUploadExistingDesign}
                className="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors"
              >
                기존 도안 업로드
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 에러 메시지 */}
      {errorMessage && (
        <div className="mb-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {errorMessage}
        </div>
      )}

      {/* 로딩 상태 */}
      {isLoading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
        </div>
      ) : designs.length === 0 ? (
        /* 도안 없을 때 */
        <div className="text-center py-20">
          <p className="text-gray-500 mb-4">아직 등록된 도안이 없습니다.</p>
          <button
            onClick={handleCreateNewDesign}
            className="px-6 py-2 bg-[#925C4C] text-white rounded-md hover:bg-[#7a4a3d]"
          >
            첫 도안 만들기
          </button>
        </div>
      ) : (
        /* 도안 목록 그리드 */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {designs.map((design) => (
            <DesignCard
              key={design.designId}
              design={design}
              onStopSale={handleStopSale}
              onDelete={handleDelete}
              onRegisterSale={handleRegisterSale}
              onResumeSale={handleResumeSale}
            />
          ))}
        </div>
      )}

      {/* 파일 업로드 숨겨진 input */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf"
        onChange={handleFileUpload}
        className="hidden"
      />

      {/* 업로드 모달 */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">기존 도안 업로드</h2>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                도안 이름 (선택사항)
              </label>
              <input
                type="text"
                value={uploadDesignName}
                onChange={(e) => setUploadDesignName(e.target.value)}
                placeholder="입력하지 않으면 파일명이 사용됩니다"
                maxLength={30}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
                disabled={isUploading}
              />
            </div>

            <div className="mb-6">
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading}
                className="w-full px-4 py-2 border-2 border-dashed border-gray-300 rounded-md hover:border-[#925C4C] transition-colors disabled:opacity-50"
              >
                {isUploading ? '업로드 중...' : 'PDF 파일 선택'}
              </button>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowUploadModal(false);
                  setUploadDesignName('');
                }}
                disabled={isUploading}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 판매 중지 확인 모달 */}
      {showStopSaleModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">판매 중지</h2>
            <p className="text-gray-600 mb-6">
              정말로 이 도안의 판매를 중지하시겠습니까?
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowStopSaleModal(false);
                  setSelectedDesignId(null);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
              >
                취소
              </button>
              <button
                onClick={confirmStopSale}
                className="flex-1 px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600"
              >
                중지
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
