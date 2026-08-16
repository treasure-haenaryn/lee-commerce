package com.github.haenaryn.catalog.infrastructure.persistence;

import com.github.haenaryn.catalog.domain.Category;
import com.github.haenaryn.catalog.domain.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    @Override
    public Category save(Category category) {
        return jpaRepository.save(category);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Category> findAllByParentId(Long parentId) {
        return jpaRepository.findAllByParentId(parentId);
    }
}
