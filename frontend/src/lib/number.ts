// 숫자 안전 변환 (NaN 방지)
export const toInt = (v: unknown, fallback = 0) => {
    const n = Number(v);
    return Number.isFinite(n) ? n : fallback;
  };
  