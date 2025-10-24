import { ProductList } from '@/components/product';

export default function FreeProductListPage() {
  return (
    <ProductList 
      title="무료 상품"
      filter="FREE"
      basePath="/product/free"
    />
  );
}