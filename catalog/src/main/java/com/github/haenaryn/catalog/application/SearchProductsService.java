package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchProductsService {

    // Elasticsearch의 기본 index.max_result_window(10,000)를 넘는 offset(page*size)
    // 요청은 검색 자체가 실패한다 — size 상한뿐 아니라 offset 상한도 함께 걸어야
    // page를 깊게 넘겨도 제어된 예외로 막을 수 있다.
    private static final int MAX_SIZE = 100;
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final ProductSearchPort productSearchPort;

    // 검색 결과는 판매중(ON_SALE) 상품만 노출한다 — SearchProductsCommand에는 status
    // 필드가 없어서 호출자가 이 값을 바꿀 방법이 없다.
    public SearchProductsResult search(SearchProductsCommand command) {
        validate(command);

        SearchProductsQuery query = new SearchProductsQuery(
            command.name(), command.categoryId(), command.minPriceAmount(), command.maxPriceAmount(),
            ProductStatus.ON_SALE.name(), command.page(), command.size());
        return productSearchPort.search(query);
    }

    private void validate(SearchProductsCommand command) {
        if (command.page() < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 한다");
        }
        if (command.size() < 1 || command.size() > MAX_SIZE) {
            throw new IllegalArgumentException("size는 1 이상 " + MAX_SIZE + " 이하여야 한다");
        }
        long offset = (long) command.page() * command.size() + command.size();
        if (offset > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException(
                "page/size 조합의 조회 범위가 너무 깊다(최대 " + MAX_RESULT_WINDOW + "): " + offset);
        }
        if (command.minPriceAmount() != null && command.minPriceAmount().signum() < 0) {
            throw new IllegalArgumentException("minPriceAmount는 0 이상이어야 한다");
        }
        if (command.maxPriceAmount() != null && command.maxPriceAmount().signum() < 0) {
            throw new IllegalArgumentException("maxPriceAmount는 0 이상이어야 한다");
        }
        if (command.minPriceAmount() != null && command.maxPriceAmount() != null
                && command.minPriceAmount().compareTo(command.maxPriceAmount()) > 0) {
            throw new IllegalArgumentException("minPriceAmount는 maxPriceAmount보다 클 수 없다");
        }
    }
}
