package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.application.EffectivePriceResult;
import com.github.haenaryn.catalog.application.GetEffectivePriceQuery;
import com.github.haenaryn.catalog.application.GetEffectivePriceService;
import com.github.haenaryn.catalog.application.ProductSearchResultItem;
import com.github.haenaryn.catalog.application.SearchProductsCommand;
import com.github.haenaryn.catalog.application.SearchProductsResult;
import com.github.haenaryn.catalog.application.SearchProductsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// 회원 인증 체계와 분리된 공개 조회/검색 API다 — API Key 인터셉터는 /internal/**
// 에만 적용돼 있어 이 컨트롤러는 인증 없이 접근 가능하다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final SearchProductsService searchProductsService;
    private final GetEffectivePriceService getEffectivePriceService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchProductsResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPriceAmount,
            @RequestParam(required = false) BigDecimal maxPriceAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchProductsResult result = searchProductsService.search(
            new SearchProductsCommand(name, categoryId, minPriceAmount, maxPriceAmount, page, size));

        return ResponseEntity.ok(ApiResponse.of(toResponse(result)));
    }

    @GetMapping("/{productId}/options/{skuCode}/price")
    public ResponseEntity<ApiResponse<EffectivePriceResponse>> effectivePrice(
            @PathVariable Long productId, @PathVariable String skuCode) {
        EffectivePriceResult result =
            getEffectivePriceService.getEffectivePrice(new GetEffectivePriceQuery(productId, skuCode));

        EffectivePriceResponse response = new EffectivePriceResponse(
            result.productId(), result.skuCode(), result.amount(), result.currency(), result.discountApplied());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    private SearchProductsResponse toResponse(SearchProductsResult result) {
        return new SearchProductsResponse(
            result.items().stream().map(this::toItem).toList(),
            result.totalCount(), result.page(), result.size());
    }

    private SearchProductItemResponse toItem(ProductSearchResultItem item) {
        return new SearchProductItemResponse(
            item.productId(), item.name(), item.categoryId(), item.basePriceAmount(),
            item.basePriceCurrency(), item.status());
    }
}
