import { ProductList } from '@/components/product';

export default function TopProductListPage() {
  return (
    <ProductList 
      title="상의 상품"
      category="TOP"
      basePath="/product/top"
    />
  );
}