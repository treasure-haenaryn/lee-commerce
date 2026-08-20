package com.github.haenaryn.catalog.interfaces;

import com.github.haenaryn.catalog.application.RegisterCategoryCommand;
import com.github.haenaryn.catalog.application.RegisterCategoryResult;
import com.github.haenaryn.catalog.application.RegisterCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/categories")
public class CategoryAdminController {

    private final RegisterCategoryService registerCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterCategoryResponse>> register(
            @Valid @RequestBody RegisterCategoryRequest request) {
        RegisterCategoryResult result = registerCategoryService.register(
            new RegisterCategoryCommand(request.parentId(), request.name(), request.displayOrder()));

        RegisterCategoryResponse response = new RegisterCategoryResponse(
            result.categoryId(), result.parentId(), result.name(), result.depth(), result.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
