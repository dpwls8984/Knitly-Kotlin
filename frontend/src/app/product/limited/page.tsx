import { ProductList } from '@/components/product';

export default function LimitedProductListPage() {
  return (
    <ProductList 
      title="한정판매 상품"
      filter="LIMITED"
      basePath="/product/limited"
    />
  );
}