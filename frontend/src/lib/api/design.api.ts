import api from './axios';

/**
 * 도안 상태 타입
 */
export type DesignState = 'BEFORE_SALE' | 'ON_SALE' | 'STOPPED';

/**
 * 뜨개질 기호 타입
 */
export type KnittingSymbol = 'empty' | 'filled' | 'x' | 'v' | 't' | 'plus' | 'a';

/**
 * 도안 생성 요청
 */
export interface DesignCreateRequest {
    designName: string;        // 필수, 최대 30자
    gridData: string[][];      // 필수, 10x10 배열
    fileName?: string;         // 선택, 최대 80자
}

/**
 * 도안 응답
 */
export interface DesignResponse {
    designId: number;
    designName: string;
    pdfUrl: string;
    designState: DesignState;
    createdAt: string;
}

/**
 * 도안 목록 응답
 */
export interface DesignListResponse {
    designId: number;
    designName: string;
    pdfUrl: string;
    designState: DesignState;
    createdAt: string;
}

/**
 * 도안 생성 (격자 기반)
 * POST /designs
 * 
 * @description 10x10 격자 데이터를 전송하면 백엔드에서 PDF를 생성합니다.
 * @param request - 도안명과 gridData
 * @returns DesignResponse
 */
export const createDesign = async (
    request: DesignCreateRequest
): Promise<DesignResponse> => {
    const response = await api.post('/designs', request);
    return response.data;
};

/**
 * 기존 PDF 업로드
 * POST /designs/upload
 * 
 * @description PDF 파일을 업로드하여 도안을 등록합니다.
 * @param file - PDF 파일
 * @param designName - 도안명 (선택)
 * @returns DesignResponse
 */
export const uploadDesignPdf = async (
    file: File,
    designName?: string
): Promise<DesignResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    
    if (designName) {
        formData.append('designName', designName);
    }

    const response = await api.post('/designs/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    
    return response.data;
};

/**
 * 내 도안 목록 조회
 * GET /designs/my
 * 
 * @description 로그인한 사용자의 도안 목록을 조회합니다.
 * @returns DesignListResponse[]
 */
export const getMyDesigns = async (): Promise<DesignListResponse[]> => {
    const response = await api.get('/designs/my');
    return response.data;
};

/**
 * 도안 삭제
 * DELETE /designs/{designId}
 * 
 * @description BEFORE_SALE 상태의 도안만 삭제 가능합니다.
 * @param designId - 도안 ID
 */
export const deleteDesign = async (designId: number): Promise<void> => {
    await api.delete(`/designs/${designId}`);
};

/**
 * 판매 중지
 * PATCH /designs/{designId}/stop
 * 
 * @description ON_SALE 상태의 도안을 STOPPED 상태로 변경합니다.
 * @param designId - 도안 ID
 */
export const stopDesignSale = async (designId: number): Promise<void> => {
    await api.patch(`/designs/${designId}/stop`);
};

/**
 * 판매 재개
 * PATCH /designs/{designId}/relist
 * 
 * @description STOPPED 상태의 도안을 ON_SALE 상태로 변경합니다.
 * @param designId - 도안 ID
 */
export const relistDesign = async (designId: number): Promise<void> => {
    await api.patch(`/designs/${designId}/relist`);
};

/**
 * PDF 보기 (Blob 방식 - 인증 유지)
 * GET /designs/{designId}/pdf
 * 
 * @description axios가 자동으로 Authorization 헤더를 추가하여 인증된 상태로 PDF를 가져옵니다.
 * @param designId - 도안 ID
 */
export const viewDesignPdf = async (designId: number): Promise<void> => {
    try {
        const response = await api.get(`/designs/${designId}/pdf`, {
            responseType: 'blob', // 중요! PDF를 Blob으로 받기
        });

        // Blob URL 생성
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const blobUrl = window.URL.createObjectURL(blob);

        // 새 창에서 열기
        window.open(blobUrl, '_blank');

        // 메모리 정리 (5초 후)
        setTimeout(() => {
            window.URL.revokeObjectURL(blobUrl);
        }, 5000);
    } catch (error: any) {
        console.error('PDF 로드 실패:', error);
        throw error;
    }
};

/**
 * PDF 다운로드
 * GET /designs/{designId}/pdf
 * 
 * @description PDF 파일을 다운로드합니다.
 * @param designId - 도안 ID
 * @param designName - 도안명 (파일명으로 사용)
 */
export const downloadDesignPdf = async (
    designId: number, 
    designName: string
): Promise<void> => {
    try {
        const response = await api.get(`/designs/${designId}/pdf`, {
            responseType: 'blob',
        });

        // Blob 생성
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);

        // 다운로드 링크 생성 및 클릭
        const link = document.createElement('a');
        link.href = url;
        link.download = `${designName}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // 메모리 정리
        window.URL.revokeObjectURL(url);
    } catch (error: any) {
        console.error('PDF 다운로드 실패:', error);
        throw error;
    }
};
