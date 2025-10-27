'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { createDesign } from '@/lib/api/design.api';
import type { KnittingSymbol } from '@/lib/api/design.api';

// 격자 셀 컴포넌트
interface GridCellProps {
  symbol: KnittingSymbol | null;
  onClick: () => void;
  isSelected: boolean;
}

function GridCell({ symbol, onClick, isSelected }: GridCellProps) {
  const renderSymbol = () => {
    switch (symbol) {
      case 'empty':
        return <div className="w-4 h-4 border border-gray-400 rounded-full"></div>;
      case 'filled':
        return <div className="w-4 h-4 bg-black rounded-full"></div>;
      case 'x':
        return <span className="text-lg font-bold">×</span>;
      case 'v':
        return <span className="text-sm font-bold">(V)</span>;
      case 't':
        return <span className="text-lg font-bold">T</span>;
      case 'plus':
        return <span className="text-lg font-bold">+</span>;
      case 'a':
        return <span className="text-sm font-bold">(A)</span>;
      default:
        return null;
    }
  };

  return (
    <button
      onClick={onClick}
      className={`
        w-8 h-8 border border-gray-300 flex items-center justify-center
        hover:bg-gray-100 transition-colors
        ${isSelected ? 'bg-[#925C4C] bg-opacity-10 border-[#925C4C]' : ''}
      `}
    >
      {renderSymbol()}
    </button>
  );
}

// 뜨개질 기호 버튼 컴포넌트
interface SymbolButtonProps {
  symbol: KnittingSymbol;
  label: string;
  isSelected: boolean;
  onClick: () => void;
}

function SymbolButton({ symbol, label, isSelected, onClick }: SymbolButtonProps) {
  const renderSymbol = () => {
    switch (symbol) {
      case 'empty':
        return <div className="w-6 h-6 border border-gray-400 rounded-full"></div>;
      case 'filled':
        return <div className="w-6 h-6 bg-black rounded-full"></div>;
      case 'x':
        return <span className="text-xl font-bold">×</span>;
      case 'v':
        return <span className="text-sm font-bold">(V)</span>;
      case 't':
        return <span className="text-xl font-bold">T</span>;
      case 'plus':
        return <span className="text-xl font-bold">+</span>;
      case 'a':
        return <span className="text-sm font-bold">(A)</span>;
      default:
        return null;
    }
  };

  return (
    <button
      onClick={onClick}
      className={`
        w-12 h-12 border border-gray-300 flex items-center justify-center
        hover:bg-gray-100 transition-colors rounded
        ${isSelected ? 'bg-[#925C4C] bg-opacity-10 border-[#925C4C]' : ''}
      `}
      title={label}
    >
      {renderSymbol()}
    </button>
  );
}

export default function CreateDesignPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuthStore(); // 인증 상태 가져오기
  
  const [grid, setGrid] = useState<(KnittingSymbol | null)[][]>(
    Array(10).fill(null).map(() => Array(10).fill(null))
  );
  const [selectedSymbol, setSelectedSymbol] = useState<KnittingSymbol>('empty');
  const [designName, setDesignName] = useState('');
  const [showNotification, setShowNotification] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 🔥 인증 체크 - 비로그인 시 홈으로 리다이렉트
  useEffect(() => {
    if (isLoading) {
      return; // 로딩 중이면 기다림
    }

    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.replace('/');
    }
  }, [isLoading, isAuthenticated, router]);

  // 로딩 중일 때
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  // 비인증 상태일 때 (리다이렉트 처리 중)
  if (!isAuthenticated) {
    return null;
  }

  // 격자 셀 클릭 핸들러
  const handleCellClick = (row: number, col: number) => {
    const newGrid = [...grid];
    newGrid[row][col] = selectedSymbol;
    setGrid(newGrid);
  };

  // gridData를 백엔드 형식으로 변환 (null -> 빈 문자열)
  const convertGridDataForBackend = (): string[][] => {
    return grid.map(row => 
      row.map(cell => cell === null ? '' : cell)
    );
  };

  // PDF 저장 핸들러 - axios API 호출
  const handleSaveAsPDF = async () => {
    if (!designName.trim()) {
      alert('도안명을 입력해주세요.');
      return;
    }

    setIsSaving(true);
    setErrorMessage(null);

    try {
      const gridDataForBackend = convertGridDataForBackend();

      const result = await createDesign({
        designName: designName.trim(),
        gridData: gridDataForBackend,
        fileName: designName.trim(),
      });

      console.log('도안 저장 성공:', result);

      // 저장 성공 알림 표시
      setShowNotification(true);
      
      // 2초 후 알림 숨기고 페이지 이동
      setTimeout(() => {
        setShowNotification(false);
        router.push('/mypage/design');
      }, 2000);

    } catch (error: any) {
      console.error('도안 저장 실패:', error);
      
      // axios 에러 처리
      const errorMsg = error.response?.data?.message || 
                       error.message || 
                       '도안 저장 중 오류가 발생했습니다.';
      setErrorMessage(errorMsg);
      
      // 3초 후 에러 메시지 제거
      setTimeout(() => setErrorMessage(null), 3000);
    } finally {
      setIsSaving(false);
    }
  };

  // 뜨개질 기호 목록
  const symbols: { symbol: KnittingSymbol; label: string }[] = [
    { symbol: 'empty', label: '빈 원' },
    { symbol: 'filled', label: '채워진 원' },
    { symbol: 'x', label: 'X' },
    { symbol: 'v', label: '(V)' },
    { symbol: 't', label: 'T' },
    { symbol: 'plus', label: '+' },
    { symbol: 'a', label: '(A)' },
  ];

  return (
    <div className="min-h-screen bg-white">
      {/* 성공 알림창 */}
      {showNotification && (
        <div className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50">
          <div className="bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg">
            도안이 성공적으로 저장되었습니다!
          </div>
        </div>
      )}

      {/* 에러 메시지 */}
      {errorMessage && (
        <div className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50">
          <div className="bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg">
            {errorMessage}
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 py-4">
        {/* 메인 콘텐츠 */}
        <div className="flex gap-6">
          {/* 도안 제작 영역 */}
          <div className="flex-1">
            <h2 className="text-2xl font-bold mb-6">나만의 도안 제작하기</h2>
            
            {/* 10x10 격자 */}
            <div className="p-6 rounded-lg border border-gray-200">
              <div className="relative">
                {/* 격자 */}
                <div className="grid grid-cols-10 gap-0 border border-gray-300 w-fit">
                  {grid.map((row, rowIndex) =>
                    row.map((cell, colIndex) => (
                      <GridCell
                        key={`${rowIndex}-${colIndex}`}
                        symbol={cell}
                        onClick={() => handleCellClick(rowIndex, colIndex)}
                        isSelected={false}
                      />
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* 오른쪽 도구 영역 */}
          <div className="w-80">
            <div className="p-6 rounded-lg border border-gray-200">
              {/* 사이즈 정보 */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold mb-2">사이즈</h3>
                <div className="text-gray-600">10 x 10 (고정)</div>
              </div>

              {/* 뜨개질 기호들 */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold mb-4">뜨개질 기호</h3>
                <div className="grid grid-cols-3 gap-3">
                  {symbols.map(({ symbol, label }) => (
                    <SymbolButton
                      key={symbol}
                      symbol={symbol}
                      label={label}
                      isSelected={selectedSymbol === symbol}
                      onClick={() => setSelectedSymbol(symbol)}
                    />
                  ))}
                </div>
              </div>

              {/* 도안명 입력 */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  도안 이름 입력 창(필수)
                </label>
                <input
                  type="text"
                  value={designName}
                  onChange={(e) => setDesignName(e.target.value)}
                  placeholder="도안명을 입력하세요"
                  maxLength={30}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
                />
                <p className="text-xs text-gray-500 mt-1">
                  {designName.length}/30
                </p>
              </div>

              {/* PDF 저장 버튼 */}
              <button
                onClick={handleSaveAsPDF}
                disabled={isSaving}
                className="w-full bg-[#925C4C] text-white py-3 px-4 rounded-md hover:bg-[#7a4a3d] transition-colors font-medium disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                {isSaving ? '저장 중...' : 'PDF로 저장하기'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}