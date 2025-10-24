import { ProductList } from '@/components/product';

export default function OutwearProductListPage() {
  return (
    <ProductList 
      title="아우터 상품"
      category="OUTER"
      basePath="/product/outwear"
    />
  );
}