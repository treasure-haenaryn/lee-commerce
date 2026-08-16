package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Category;
import com.github.haenaryn.catalog.domain.CategoryRepository;
import com.github.haenaryn.catalog.domain.exception.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterCategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public RegisterCategoryResult register(RegisterCategoryCommand command) {
        Category category = command.parentId() == null
            ? Category.createRoot(command.name(), command.displayOrder())
            : Category.createChild(findParent(command.parentId()), command.name(), command.displayOrder());

        Category saved = categoryRepository.save(category);
        return new RegisterCategoryResult(
            saved.getId(), saved.getParentId(), saved.getName(), saved.getDepth(), saved.getDisplayOrder());
    }

    private Category findParent(Long parentId) {
        return categoryRepository.findById(parentId)
            .orElseThrow(() -> new CategoryNotFoundException(parentId));
    }
}
