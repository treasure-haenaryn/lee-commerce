package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.ProductOption;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.ProductStatus;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import com.github.haenaryn.common.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetEffectivePriceService {

    private final ProductRepository productRepository;
    private final ProductDiscountRepository productDiscountRepository;

    @Transactional(readOnly = true)
    public EffectivePriceResult getEffectivePrice(GetEffectivePriceQuery query) {
        // 판매중이 아닌 상품의 존재 여부를 응답 차이로 흘리지 않기 위해 "찾지 못함"과
        // 같은 예외로 통일한다 — 이 API는 인증 없이 노출되는 공개 API라, 상품이
        // HIDDEN/DISCONTINUED/SOLD_OUT인지 여부까지 외부에 드러낼 이유가 없다.
        Product product = productRepository.findById(query.productId())
            .filter(found -> found.getStatus() == ProductStatus.ON_SALE)
            .orElseThrow(() -> new ProductNotFoundException(query.productId()));

        // 비활성화된 옵션도 같은 이유로 "존재하지 않는 옵션"과 동일하게 취급한다.
        ProductOption option = product.getOptions().stream()
            .filter(candidate -> candidate.getSkuCode().equals(query.skuCode()) && candidate.isActive())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션이다: " + query.skuCode()));

        Money optionPrice = option.effectivePrice(product.getBasePrice());

        // 정책상 한 상품에 동시에 유효한 할인은 최대 1개라 "가장 유리한 것 선택" 로직이
        // 필요 없다 — CreateProductDiscountService가 생성 시점에 기간 겹침을 막는다.
        List<ProductDiscount> discounts = productDiscountRepository.findAllByProductId(query.productId());
        Instant now = Instant.now();
        Optional<ProductDiscount> effectiveDiscount = discounts.stream()
            .filter(discount -> discount.isEffective(now))
            .findFirst();

        Money finalPrice = effectiveDiscount.map(discount -> discount.applyTo(optionPrice)).orElse(optionPrice);

        return new EffectivePriceResult(
            product.getId(), option.getSkuCode(), finalPrice.amount(), finalPrice.currency().getCurrencyCode(),
            effectiveDiscount.isPresent());
    }
}
