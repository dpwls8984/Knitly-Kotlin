import { ProductList } from '@/components/product';

export default function EtcProductListPage() {
  return (
    <ProductList 
      title="기타 상품"
      category="ETC"
      basePath="/product/etc"
    />
  );
}