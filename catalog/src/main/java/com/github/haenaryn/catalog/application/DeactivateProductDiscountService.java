package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.ProductDiscount;
import com.github.haenaryn.catalog.domain.ProductDiscountRepository;
import com.github.haenaryn.catalog.domain.exception.ProductDiscountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateProductDiscountService {

    private final ProductDiscountRepository productDiscountRepository;

    @Transactional
    public void deactivate(DeactivateProductDiscountCommand command) {
        ProductDiscount discount = productDiscountRepository.findById(command.discountId())
            .orElseThrow(() -> new ProductDiscountNotFoundException(command.discountId()));

        discount.deactivate();

        productDiscountRepository.save(discount);
    }
}
