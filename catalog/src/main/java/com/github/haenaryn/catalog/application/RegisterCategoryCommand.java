package com.github.haenaryn.catalog.application;

public record RegisterCategoryCommand(Long parentId, String name, int displayOrder) {
}
