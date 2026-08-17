package com.github.haenaryn.catalog.infrastructure.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

// domain/catalog.md의 검색 기준(상품명/카테고리/가격대)만 담는다 — 할인 적용가는
// GetEffectivePriceService가 별도로 책임지는 관심사라 여기 포함하지 않는다.
@Document(indexName = "products")
class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Long, name = "category_id")
    private Long categoryId;

    // 통화 금액을 이진 부동소수(FieldType.Double)로 저장하면 반올림 오차가 생길 수 있어
    // scaled_float를 쓴다 — 정수로 스케일링해 저장하고 조회 시 다시 나눈다.
    @Field(type = FieldType.Scaled_Float, name = "base_price_amount", scalingFactor = 100)
    private BigDecimal basePriceAmount;

    @Field(type = FieldType.Keyword, name = "base_price_currency")
    private String basePriceCurrency;

    @Field(type = FieldType.Keyword)
    private String status;

    protected ProductSearchDocument() {
    }

    ProductSearchDocument(
            String id, String name, Long categoryId, BigDecimal basePriceAmount,
            String basePriceCurrency, String status) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.basePriceAmount = basePriceAmount;
        this.basePriceCurrency = basePriceCurrency;
        this.status = status;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    Long getCategoryId() {
        return categoryId;
    }

    BigDecimal getBasePriceAmount() {
        return basePriceAmount;
    }

    String getBasePriceCurrency() {
        return basePriceCurrency;
    }

    String getStatus() {
        return status;
    }
}
