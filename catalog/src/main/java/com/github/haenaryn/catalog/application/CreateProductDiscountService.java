package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.exception.OverlappingDiscountException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductDiscountService {

    private final ProductDiscountRepository productDiscountRepository;

    // 조회 후 저장(findAllByProductId → save) 방식이라 동시에 두 요청이 들어오면 서로의
    // 변경을 못 보고 둘 다 통과할 수 있다 — 진짜 동시성 안전성은 JPA 어댑터가 생기는
    // Infrastructure 슬라이스에서 락/DB 제약으로 보강해야 한다(현재는 Mock Repository라
    // 여기서 락을 걸 대상이 없다).
    @Transactional
    public CreateProductDiscountResult create(CreateProductDiscountCommand command) {
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
