export type FlatComment = {
  commentId: number | string;
  parentId: number | string | null;
  content: string;
  authorId: number | null;
  authorName: string;
  createdAt: string;
};

export type TreeComment = {
  id: number;
  parentId: number | null;
  content: string;
  authorId: number | null;
  authorName: string;
  createdAt: string;
  children: TreeComment[];
};

// 어떤 형태가 와도 안전하게 배열로 정규화
export function normalizeList<T>(input: any): T[] {
  if (Array.isArray(input)) return input as T[];
  if (input && Array.isArray(input.content)) return input.content as T[];
  if (input && Array.isArray(input.items)) return input.items as T[];
  return [];
}

// 평면 -> 트리 (parentId: null/0/'' 는 루트)
export function toTree(raw: any): TreeComment[] {
  const items = normalizeList<FlatComment>(raw);

  const byId = new Map<number, TreeComment>();
  const roots: TreeComment[] = [];

  // 노드 사전
  for (const it of items) {
    const id = Number(it.commentId);
    const pidRaw = it.parentId;
    const pid =
      pidRaw === null || pidRaw === '' || Number(pidRaw) === 0 ? null : Number(pidRaw);

    byId.set(id, {
      id,
      parentId: pid,
      content: it.content,
      authorId: it.authorId,
      authorName: it.authorName,
      createdAt: it.createdAt,
      children: [],
    });
  }

  // 링크
  for (const node of byId.values()) {
    if (node.parentId && byId.has(node.parentId)) {
      byId.get(node.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  }

  // 원본 순서 유지(필요시)
  roots.sort((a, b) => a.id - b.id);
  roots.forEach(dfsSort);
  return roots;
}

function dfsSort(n: TreeComment) {
  n.children.sort((a, b) => a.id - b.id);
  n.children.forEach(dfsSort);
}
