package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.application.CreateProductDiscountCommand;
import com.github.haenaryn.catalog.application.CreateProductDiscountResult;
import com.github.haenaryn.catalog.application.CreateProductDiscountService;
import com.github.haenaryn.catalog.application.DeactivateProductDiscountCommand;
import com.github.haenaryn.catalog.application.DeactivateProductDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductDiscountAdminController {

    private final CreateProductDiscountService createProductDiscountService;
    private final DeactivateProductDiscountService deactivateProductDiscountService;

    @PostMapping("/internal/products/{productId}/discounts")
    public ResponseEntity<ApiResponse<CreateProductDiscountResponse>> create(
            @PathVariable Long productId, @Valid @RequestBody CreateProductDiscountRequest request) {
        CreateProductDiscountResult result = createProductDiscountService.create(new CreateProductDiscountCommand(
            productId, request.discountType(), request.discountValue(), request.startsAt(), request.endsAt()));

        CreateProductDiscountResponse response = new CreateProductDiscountResponse(
            result.discountId(), result.productId(), result.discountType(), result.discountValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/internal/discounts/{discountId}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long discountId) {
        deactivateProductDiscountService.deactivate(new DeactivateProductDiscountCommand(discountId));
        return ResponseEntity.noContent().build();
    }
}
