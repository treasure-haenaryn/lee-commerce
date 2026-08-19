package com.github.haenaryn.catalog.infrastructure.search;

import com.github.haenaryn.catalog.application.SearchProductsQuery;
import com.github.haenaryn.catalog.application.SearchProductsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchPortImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<ProductSearchDocument> searchHits;

    @Mock
    private SearchHit<ProductSearchDocument> searchHit;

    private ProductSearchPortImpl port;

    @BeforeEach
    void setUp() {
        port = new ProductSearchPortImpl(elasticsearchOperations);
    }

    @Test
    void 검색_결과를_애플리케이션_DTO로_변환한다() {
        ProductSearchDocument document = new ProductSearchDocument(
            "1", 1L, "티셔츠", 10L, new BigDecimal("15000.00"), "KRW", "ON_SALE");
        when(searchHit.getContent()).thenReturn(document);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(elasticsearchOperations.search(any(Query.class), eq(ProductSearchDocument.class)))
            .thenReturn(searchHits);

        SearchProductsResult result = port.search(
            new SearchProductsQuery("티셔츠", 10L, null, null, "ON_SALE", 0, 20));

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productId()).isEqualTo(1L);
        assertThat(result.items().get(0).name()).isEqualTo("티셔츠");
        assertThat(result.items().get(0).categoryId()).isEqualTo(10L);
        assertThat(result.items().get(0).basePriceAmount()).isEqualByComparingTo("15000.00");
        assertThat(result.items().get(0).basePriceCurrency()).isEqualTo("KRW");
        assertThat(result.items().get(0).status()).isEqualTo("ON_SALE");
    }

    @Test
    void 조건이_없으면_빈_결과를_그대로_반환한다() {
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(elasticsearchOperations.search(any(Query.class), eq(ProductSearchDocument.class)))
            .thenReturn(searchHits);

        SearchProductsResult result = port.search(
            new SearchProductsQuery(null, null, null, null, "ON_SALE", 0, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }
}
