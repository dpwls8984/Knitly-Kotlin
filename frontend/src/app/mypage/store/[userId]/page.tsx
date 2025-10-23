'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { getSellerProducts, getStoreDescription, updateStoreDescription } from '@/lib/api/product.api';
import { ProductListResponse } from '@/types/product.types';

export default function SellerStorePage() {
    // 🔥 모든 Hook을 최상단에 선언 (early return 이전)
    const params = useParams<{ userId: string }>();
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();

    // State
    const [products, setProducts] = useState<ProductListResponse[]>([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(true);
    const [storeDescription, setStoreDescription] = useState('');
    const [isEditingDescription, setIsEditingDescription] = useState(false);
    const [tempDescription, setTempDescription] = useState('');
    const [descriptionLoading, setDescriptionLoading] = useState(false);

    // URL에서 판매자 ID 가져오기
    const storeOwnerId = params?.userId;

    // 본인 스토어 여부 판단
    const isMyStore = isAuthenticated && user?.userId === storeOwnerId;

    // 🔥 디버깅 (개발 중에만 사용)
    useEffect(() => {
        console.log('📍 Current params:', params);
        console.log('📍 Store Owner ID:', storeOwnerId);
        console.log('📍 Current User ID:', user?.userId);
    }, [params, storeOwnerId, user]);

    // 🔥 storeOwnerId 없으면 에러 처리 (useEffect로)
    useEffect(() => {
        if (!storeOwnerId) {
            alert('잘못된 접근입니다.');
            router.push('/mypage');
        }
    }, [storeOwnerId, router]);

    // 🔥 스토어 설명 조회
    useEffect(() => {
        const fetchDescription = async () => {
            if (!storeOwnerId) return;

            try {
                const description = await getStoreDescription(storeOwnerId);
                setStoreDescription(description);
            } catch (error) {
                console.error('스토어 설명 조회 실패:', error);
                setStoreDescription('안녕하세요! 제 스토어에 오신 것을 환영합니다.');
            }
        };

        fetchDescription();
    }, [storeOwnerId]);

    // 상품 목록 조회
    useEffect(() => {
        const fetchProducts = async () => {
            if (!storeOwnerId) return;

            try {
                setLoading(true);
                const response = await getSellerProducts(storeOwnerId, page, 20);
                setProducts(response.content);
                setTotalPages(response.totalPages);
                setTotalElements(response.totalElements);
            } catch (error) {
                console.error('상품 조회 실패:', error);
                alert('상품을 불러오는데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };

        fetchProducts();
    }, [storeOwnerId, page]);

    // 설명 수정 시작
    const handleEditDescription = () => {
        setTempDescription(storeDescription);
        setIsEditingDescription(true);
    };

    // 🔥 설명 저장 (API 호출)
    const handleSaveDescription = async () => {
        if (!storeOwnerId) return;

        // 빈 내용 체크
        if (!tempDescription.trim()) {
            alert('스토어 설명을 입력해주세요.');
            return;
        }

        try {
            setDescriptionLoading(true);

            // 🔥 백엔드 API 호출
            await updateStoreDescription(storeOwnerId, tempDescription);

            // 성공 시 상태 업데이트
            setStoreDescription(tempDescription);
            setIsEditingDescription(false);
            alert('스토어 설명이 저장되었습니다.');

        } catch (error: any) {
            console.error('스토어 설명 저장 실패:', error);

            // 에러 처리
            if (error.response?.status === 403) {
                alert('본인의 스토어만 수정할 수 있습니다.');
            } else if (error.response?.status === 404) {
                alert('스토어를 찾을 수 없습니다.');
            } else {
                alert('저장에 실패했습니다. 다시 시도해주세요.');
            }
        } finally {
            setDescriptionLoading(false);
        }
    };

    // 설명 취소
    const handleCancelEdit = () => {
        setIsEditingDescription(false);
        setTempDescription('');
    };

    // 로딩 중 (storeOwnerId 없으면 표시 안함)
    if (!storeOwnerId) {
        return null; // 또는 로딩 표시
    }

    if (loading && page === 0) {
        return (
            <div className="flex justify-center items-center min-h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-4 py-8">
            {/* 🔥 스토어 헤더 (설명 포함) */}
            <div className="bg-white rounded-lg shadow-md p-6 mb-8">
                <div className="flex justify-between items-start mb-4">
                    <div>
                        <h1 className="text-3xl font-bold mb-2">
                            {isMyStore ? '내 스토어' : '판매자 스토어'}
                        </h1>
                        <p className="text-gray-600">
                            총 {totalElements}개의 도안
                        </p>
                    </div>
                </div>

                {/* 스토어 설명 */}
                <div className="mt-4">
                    {isEditingDescription ? (
                        // 🔥 수정 모드 (본인만)
                        <div>
              <textarea
                  value={tempDescription}
                  onChange={(e) => setTempDescription(e.target.value)}
                  className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-[#925C4C] focus:border-transparent"
                  rows={4}
                  placeholder="스토어 설명을 입력하세요..."
              />
                            <div className="flex gap-2 mt-2">
                                <button
                                    onClick={handleSaveDescription}
                                    disabled={descriptionLoading}
                                    className="bg-[#925C4C] text-white px-4 py-2 rounded hover:bg-[#7a4a3d] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {descriptionLoading ? '저장 중...' : '저장'}
                                </button>
                                <button
                                    onClick={handleCancelEdit}
                                    disabled={descriptionLoading}
                                    className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400 transition-colors disabled:opacity-50"
                                >
                                    취소
                                </button>
                            </div>
                        </div>
                    ) : (
                        // 읽기 모드
                        <div className="flex justify-between items-start">
                            <p className="text-gray-700 whitespace-pre-wrap flex-1">
                                {storeDescription}
                            </p>

                            {/* 🔥 본인만 수정 버튼 표시 */}
                            {isMyStore && (
                                <button
                                    onClick={handleEditDescription}
                                    className="ml-4 text-[#925C4C] hover:text-[#7a4a3d] transition-colors"
                                >
                                    ✏️ 수정
                                </button>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* 상품 목록 */}
            {products.length === 0 ? (
                <div className="text-center py-20 bg-white rounded-lg shadow-md">
                    <p className="text-gray-500 text-lg">등록된 상품이 없습니다.</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {products.map((product) => (
                        <ProductCard
                            key={product.productId}
                            product={product}
                            isMyStore={isMyStore}  // 🔥 본인 스토어 여부 전달
                        />
                    ))}
                </div>
            )}

            {/* 페이지네이션 */}
            {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-8">
                    <button
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="px-4 py-2 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        이전
                    </button>

                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                        const pageNum = page < 3 ? i : page - 2 + i;
                        if (pageNum >= totalPages) return null;

                        return (
                            <button
                                key={pageNum}
                                onClick={() => setPage(pageNum)}
                                className={`px-4 py-2 rounded ${
                                    page === pageNum
                                        ? 'bg-[#925C4C] text-white'
                                        : 'bg-gray-200 hover:bg-gray-300'
                                }`}
                            >
                                {pageNum + 1}
                            </button>
                        );
                    })}

                    <button
                        onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        disabled={page === totalPages - 1}
                        className="px-4 py-2 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        다음
                    </button>
                </div>
            )}
        </div>
    );
}

// 🔥 상품 카드 컴포넌트
interface ProductCardProps {
    product: ProductListResponse;
    isMyStore: boolean;
}

function ProductCard({ product, isMyStore }: ProductCardProps) {
    const router = useRouter();

    const handleEdit = () => {
        // 🔥 상품 수정 페이지로 이동
        router.push(`/mypage/product/edit/${product.productId}`);
    };

    const handleViewDetail = () => {
        // 상품 상세 페이지로 이동
        router.push(`/product/${product.productId}`);
    };

    return (
        <div
            className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
            onClick={handleViewDetail}
        >
            {/* 이미지 */}
            <div className="relative h-48 bg-gray-200">
                {product.thumbnailUrl ? (
                    <img
                        src={product.thumbnailUrl}
                        alt={product.title}
                        className="w-full h-full object-cover"
                    />
                ) : (
                    <div className="flex items-center justify-center h-full text-gray-400">
                        No Image
                    </div>
                )}

                {/* 뱃지 */}
                <div className="absolute top-2 left-2 flex gap-2">
                    {product.isFree && (
                        <span className="bg-green-500 text-white px-2 py-1 text-xs rounded">
              무료
            </span>
                    )}
                    {product.isSoldOut && (
                        <span className="bg-red-500 text-white px-2 py-1 text-xs rounded">
              품절
            </span>
                    )}
                    {product.isLimited && !product.isSoldOut && (
                        <span className="bg-orange-500 text-white px-2 py-1 text-xs rounded">
              한정수량
            </span>
                    )}
                </div>
            </div>

            {/* 정보 */}
            <div className="p-4">
                <h3 className="font-semibold text-lg mb-2 truncate">
                    {product.title}
                </h3>

                <div className="flex justify-between items-center mb-3">
          <span className="text-xl font-bold text-[#925C4C]">
            {product.isFree ? '무료' : `₩${product.price.toLocaleString()}`}
          </span>
                    <div className="text-sm text-gray-600">
                        ⭐ {product.avgReviewRating.toFixed(1)}
                    </div>
                </div>

                <div className="flex gap-4 text-sm text-gray-600 mb-4">
                    <span>❤️ {product.likeCount}</span>
                    <span>🛒 {product.purchaseCount}</span>
                </div>

                {/* 🔥 본인 스토어일 때만 수정 버튼 표시 */}
                {isMyStore && (
                    <button
                        onClick={(e) => {
                            e.stopPropagation(); // 카드 클릭 이벤트 방지
                            handleEdit();
                        }}
                        className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600 transition-colors"
                    >
                        ✏️ 수정
                    </button>
                )}
            </div>
        </div>
    );
}