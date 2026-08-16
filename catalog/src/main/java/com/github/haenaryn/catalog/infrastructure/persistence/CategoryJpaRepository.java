package com.github.haenaryn.catalog.infrastructure.persistence;

import com.github.haenaryn.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByParentId(Long parentId);
}
