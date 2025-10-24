import { ProductList } from '@/components/product';

export default function ProductListPage() {
  return (
    <ProductList 
      title="전체 상품"
      basePath="/product"
    />
  );
}