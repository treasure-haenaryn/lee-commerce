package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.catalog.domain.exception.OverlappingDiscountException;
import com.github.haenaryn.catalog.domain.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductDiscountService {

    private final ProductRepository productRepository;
    private final ProductDiscountRepository productDiscountRepository;

    // 상품 행을 먼저 잠가서 "기존 할인이 겹치지 않는지 조회 → 저장" 사이의 경쟁 조건을
    // 막는다. ProductDiscount 행 자체를 잠그는 방식은 기존 할인이 0개인 상태에서 동시에
    // 2개가 생성되는 phantom 케이스를 못 막는다 — 잠글 대상 행이 아직 없기 때문이다.
    // 항상 존재가 보장된 부모 Product 행을 잠그면 이 케이스까지 막을 수 있다.
    @Transactional
    public CreateProductDiscountResult create(CreateProductDiscountCommand command) {
        productRepository.findByIdForUpdate(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        ProductDiscount candidate = ProductDiscount.create(
            command.productId(), command.discountType(), command.discountValue(),
            command.startsAt(), command.endsAt());

        List<ProductDiscount> existing = productDiscountRepository.findAllByProductId(command.productId());
        boolean overlaps = existing.stream().anyMatch(discount -> discount.overlapsWith(candidate));
        if (overlaps) {
            throw new OverlappingDiscountException(command.productId());
        }

        ProductDiscount saved = productDiscountRepository.save(candidate);
        return new CreateProductDiscountResult(
            saved.getId(), saved.getProductId(), saved.getDiscountType(), saved.getDiscountValue());
    }
}
