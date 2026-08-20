package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.application.AddProductOptionCommand;
import com.github.haenaryn.catalog.application.AddProductOptionService;
import com.github.haenaryn.catalog.application.ChangeProductStatusCommand;
import com.github.haenaryn.catalog.application.ChangeProductStatusService;
import com.github.haenaryn.catalog.application.DeactivateProductOptionCommand;
import com.github.haenaryn.catalog.application.DeactivateProductOptionService;
import com.github.haenaryn.catalog.application.ProductOptionSpecCommand;
import com.github.haenaryn.catalog.application.RegisterProductCommand;
import com.github.haenaryn.catalog.application.RegisterProductResult;
import com.github.haenaryn.catalog.application.RegisterProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/products")
public class ProductAdminController {

    private final RegisterProductService registerProductService;
    private final AddProductOptionService addProductOptionService;
    private final DeactivateProductOptionService deactivateProductOptionService;
    private final ChangeProductStatusService changeProductStatusService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterProductResponse>> register(
            @Valid @RequestBody RegisterProductRequest request) {
        RegisterProductResult result = registerProductService.register(new RegisterProductCommand(
            request.categoryId(), request.name(), request.description(),
            request.basePriceAmount(), request.basePriceCurrency(),
            request.options().stream()
                .map(option -> new ProductOptionSpecCommand(
                    option.skuCode(), option.size(), option.color(),
                    option.priceOverrideAmount(), option.priceOverrideCurrency()))
                .toList()));

        RegisterProductResponse response = new RegisterProductResponse(
            result.productId(), result.name(), result.status(), result.skuCodes());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/{productId}/options")
    public ResponseEntity<Void> addOption(
            @PathVariable Long productId, @Valid @RequestBody AddProductOptionRequest request) {
        addProductOptionService.addOption(new AddProductOptionCommand(
            productId, request.skuCode(), request.size(), request.color(),
            request.priceOverrideAmount(), request.priceOverrideCurrency()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/options/{skuCode}/deactivate")
    public ResponseEntity<Void> deactivateOption(@PathVariable Long productId, @PathVariable String skuCode) {
        deactivateProductOptionService.deactivate(new DeactivateProductOptionCommand(productId, skuCode));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long productId, @Valid @RequestBody ChangeProductStatusRequest request) {
        changeProductStatusService.changeStatus(new ChangeProductStatusCommand(productId, request.status()));
        return ResponseEntity.noContent().build();
    }
}
