package com.github.haenaryn.catalog.interfaces;

public record RegisterCategoryResponse(Long categoryId, Long parentId, String name, int depth, int displayOrder) {
}
