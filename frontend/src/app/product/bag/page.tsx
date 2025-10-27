import { ProductList } from '@/components/product';

export default function BagProductListPage() {
  return (
    <ProductList 
      title="가방 상품"
      category="BAG"
      basePath="/product/bag"
    />
  );
}