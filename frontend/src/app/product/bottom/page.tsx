import { ProductList } from '@/components/product';

export default function BottomProductListPage() {
  return (
    <ProductList 
      title="하의 상품"
      category="BOTTOM"
      basePath="/product/bottom"
    />
  );
}