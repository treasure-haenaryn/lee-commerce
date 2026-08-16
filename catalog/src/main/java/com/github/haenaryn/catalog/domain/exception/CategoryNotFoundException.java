package com.github.haenaryn.catalog.domain.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long categoryId) {
        super("존재하지 않는 카테고리다: " + categoryId);
    }
}
