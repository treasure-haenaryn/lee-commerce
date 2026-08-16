package com.github.haenaryn.catalog.application;

public record RegisterCategoryResult(Long categoryId, Long parentId, String name, int depth, int displayOrder) {
}
