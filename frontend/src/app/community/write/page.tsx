'use client';

import { useRef, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { createPost } from '@/lib/api/community.api';
import type { PostCategory } from '@/types/community.types';

const THEME = '#925C4C';
const MAX_MB = 3;
const ACCEPT = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

type LocalImage = { file: File; url: string };

export default function CommunityWritePage() {
  const { setAccessToken } = useAuthStore();
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement | null>(null);

  const [category, setCategory] = useState<PostCategory>('FREE');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [images, setImages] = useState<LocalImage[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const at = localStorage.getItem('accessToken');
    if (at) {
      console.log('[INIT] restoring accessToken from localStorage');
      setAccessToken(at);
    }
  }, [setAccessToken]);

  const openPicker = () => fileRef.current?.click();

  const onFilesSelected = (files: FileList | null) => {
    if (!files || !files.length) return;
    const next: LocalImage[] = [];

    for (const f of Array.from(files)) {
      if (!ACCEPT.includes(f.type)) {
        alert('이미지 파일만 업로드할 수 있어요 (jpg/png/webp/gif).');
        continue;
      }
      if (f.size > MAX_MB * 1024 * 1024) {
        alert(`이미지는 파일당 최대 ${MAX_MB}MB까지 업로드 가능해요.`);
        continue;
      }
      next.push({ file: f, url: URL.createObjectURL(f) });
    }
    if (next.length) setImages(prev => [...prev, ...next]);
  };

  const removeAt = (idx: number) => {
    setImages(prev => {
      const copy = [...prev];
      const [removed] = copy.splice(idx, 1);
      if (removed) URL.revokeObjectURL(removed.url);
      return copy;
    });
  };

  const validate = () => {
    if (!title.trim()) { alert('제목을 입력해주세요.'); return false; }
    if (!content.trim()) { alert('내용을 입력해주세요.'); return false; }
    return true;
  };

  const submit = async () => {
    if (!validate()) return;
    try {
      setSubmitting(true);
      const form = new FormData();
      form.append('title', title.trim());
      form.append('content', content.trim());
      form.append('category', category);
      images.forEach(img => form.append('images', img.file));

      await createPost(form);
      alert('게시글이 등록되었습니다.');
      router.replace('/community');
    } catch (e: any) {
      console.error('❌ 등록 실패:', e);
      alert(e?.response?.data?.message ?? '등록 중 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">새 글쓰기</h1>

      <div className="flex flex-wrap items-center gap-3 mb-4">
        <label className="text-gray-700 mr-2">카테고리</label>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as PostCategory)}
          className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2"
          style={{ ['--tw-ring-color' as any]: THEME }}
        >
          <option value="FREE">자유</option>
          <option value="QUESTION">질문</option>
          <option value="TIP">TIP</option>
        </select>

        <button
          type="button"
          onClick={openPicker}
          className="inline-flex items-center px-4 py-2 rounded-full text-white shadow-sm hover:opacity-90"
          style={{ backgroundColor: THEME }}
        >
          이미지 첨부
        </button>
        <input
          ref={fileRef}
          type="file"
          className="hidden"
          accept={ACCEPT.join(',')}
          multiple
          onChange={(e) => onFilesSelected(e.target.files)}
        />
        <span className="text-sm text-gray-500">(파일당 최대 {MAX_MB}MB)</span>
      </div>

      <input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="글제목"
        className="w-full px-4 py-3 mb-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2"
        style={{ ['--tw-ring-color' as any]: THEME }}
      />

      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="글 내용"
        rows={12}
        className="w-full px-4 py-3 mb-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2"
        style={{ ['--tw-ring-color' as any]: THEME }}
      />

      {images.length > 0 && (
        <div className="mb-6">
          <div className="text-sm text-gray-600 mb-2">첨부 이미지 (맨 왼쪽이 썸네일)</div>
          <div className="flex flex-wrap gap-3">
            {images.map((img, idx) => (
              <button
                key={img.url}
                type="button"
                onClick={() => removeAt(idx)}
                title="클릭하여 제거"
                className={`relative w-28 h-28 rounded-lg overflow-hidden border hover:opacity-90 ${
                  idx === 0 ? 'ring-2' : ''
                }`}
                style={{ borderColor: '#e5e7eb', ['--tw-ring-color' as any]: THEME }}
              >
                {idx === 0 && (
                  <span
                    className="absolute top-1 left-1 text-[10px] px-2 py-0.5 rounded-full text-white"
                    style={{ backgroundColor: THEME }}
                  >
                    썸네일
                  </span>
                )}
                <img src={img.url} alt="" className="object-cover w-full h-full" />
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={() => router.back()}
          className="px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
        >
          취소
        </button>
        <button
          type="button"
          disabled={submitting}
          onClick={submit}
          className="px-5 py-2 rounded-lg text-white disabled:opacity-60"
          style={{ backgroundColor: THEME }}
        >
          {submitting ? '등록 중…' : '글 등록하기'}
        </button>
      </div>
    </div>
  );
}
