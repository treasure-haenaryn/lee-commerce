package com.github.haenaryn.catalog.infrastructure.search;

import com.github.haenaryn.catalog.application.ProductSearchPort;
import com.github.haenaryn.catalog.application.ProductSearchResultItem;
import com.github.haenaryn.catalog.application.SearchProductsQuery;
import com.github.haenaryn.catalog.application.SearchProductsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.List;

// 이름/카테고리/가격대 조건이 전부 선택적이고 조합 가능해야 해서, 파생 쿼리 메서드 대신
// Criteria를 동적으로 조립한다.
@Component
@RequiredArgsConstructor
class ProductSearchPortImpl implements ProductSearchPort {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchProductsResult search(SearchProductsQuery query) {
        Criteria criteria = Criteria.where("status").is(query.status());
        if (query.name() != null && !query.name().isBlank()) {
            criteria = criteria.and(Criteria.where("name").matches(query.name()));
        }
        if (query.categoryId() != null) {
            criteria = criteria.and(Criteria.where("category_id").is(query.categoryId()));
        }
        if (query.minPriceAmount() != null) {
            criteria = criteria.and(Criteria.where("base_price_amount").greaterThanEqual(query.minPriceAmount()));
        }
        if (query.maxPriceAmount() != null) {
            criteria = criteria.and(Criteria.where("base_price_amount").lessThanEqual(query.maxPriceAmount()));
        }

        // productId(문서별로 유일)로 정렬하지 않으면 정렬 기준이 ES 내부 순서에 의존하게
        // 되어, 세그먼트 병합/리프레시 사이에 같은 페이지를 다시 조회했을 때 상품이
        // 중복되거나 누락될 수 있다. @Id 필드(id)는 ES의 _id 메타 필드로 매핑돼 일반
        // 필드로 정렬할 수 없어서, 정렬 전용으로 둔 product_id 필드를 쓴다.
        CriteriaQuery searchQuery = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "productId")));

        SearchHits<ProductSearchDocument> hits =
            elasticsearchOperations.search(searchQuery, ProductSearchDocument.class);

        List<ProductSearchResultItem> items = hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(this::toItem)
            .toList();

        return new SearchProductsResult(items, hits.getTotalHits(), query.page(), query.size());
    }

    private ProductSearchResultItem toItem(ProductSearchDocument document) {
        return new ProductSearchResultItem(
            document.getProductId(), document.getName(), document.getCategoryId(),
            document.getBasePriceAmount(), document.getBasePriceCurrency(), document.getStatus());
    }
}
