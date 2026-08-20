package com.github.haenaryn.catalog.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegisterCategoryRequest(Long parentId, @NotBlank String name, int displayOrder) {
}
