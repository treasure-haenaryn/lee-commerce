package com.github.haenaryn.catalog.application;

import com.github.haenaryn.catalog.domain.Category;
import com.github.haenaryn.catalog.domain.CategoryRepository;
import com.github.haenaryn.catalog.domain.exception.CategoryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private RegisterCategoryService service;

    @BeforeEach
    void setUp() {
        service = new RegisterCategoryService(categoryRepository);
    }

    @Test
    void 부모가_없으면_루트_카테고리를_생성한다() {
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterCategoryResult result = service.register(new RegisterCategoryCommand(null, "의류", 1));

        assertThat(result.parentId()).isNull();
        assertThat(result.depth()).isZero();
    }

    @Test
    void 부모가_있으면_자식_카테고리를_생성한다() {
        Category parent = Category.createRoot("의류", 1);
        ReflectionTestUtils.setField(parent, "id", 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterCategoryResult result = service.register(new RegisterCategoryCommand(1L, "상의", 1));

        assertThat(result.parentId()).isEqualTo(1L);
        assertThat(result.depth()).isEqualTo(1);
    }

    @Test
    void 부모가_존재하지_않으면_예외() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(new RegisterCategoryCommand(1L, "상의", 1)))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }
}
