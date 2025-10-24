import { useState, useEffect } from 'react';
import api from '@/lib/api/axios';

interface PdfViewerModalProps {
  designId: number;
  designName: string;
  onClose: () => void;
}

export function PdfViewerModal({ designId, designName, onClose }: PdfViewerModalProps) {
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPdf = async () => {
      try {
        const response = await api.get(`/designs/${designId}/pdf`, {
          responseType: 'blob',
        });

        const blob = new Blob([response.data], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        setPdfUrl(url);
        setIsLoading(false);
      } catch (err: any) {
        console.error('PDF 로드 실패:', err);
        setError('PDF를 불러오는데 실패했습니다.');
        setIsLoading(false);
      }
    };

    fetchPdf();

    // cleanup
    return () => {
      if (pdfUrl) {
        window.URL.revokeObjectURL(pdfUrl);
      }
    };
  }, [designId]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-11/12 h-5/6 flex flex-col">
        {/* 헤더 */}
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-xl font-bold">{designName}</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl"
          >
            ×
          </button>
        </div>

        {/* PDF 뷰어 */}
        <div className="flex-1 p-4">
          {isLoading ? (
            <div className="flex justify-center items-center h-full">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
          ) : error ? (
            <div className="flex justify-center items-center h-full">
              <p className="text-red-500">{error}</p>
            </div>
          ) : (
            <iframe
              src={pdfUrl || ''}
              className="w-full h-full border-0"
              title={designName}
            />
          )}
        </div>
      </div>
    </div>
  );
}
