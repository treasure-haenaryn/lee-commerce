package com.github.haenaryn.catalog.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void 루트_카테고리는_depth가_0이다() {
        Category root = Category.createRoot("의류", 1);

        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.getParentId()).isNull();
    }

    @Test
    void 부모가_저장되지_않았으면_자식_생성에_실패한다() {
        Category parent = Category.createRoot("의류", 1);

        assertThatThrownBy(() -> Category.createChild(parent, "상의", 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 자식_카테고리는_부모보다_depth가_1_크다() {
        Category parent = Category.createRoot("의류", 1);
        ReflectionTestUtils.setField(parent, "id", 1L);

        Category child = Category.createChild(parent, "상의", 1);

        assertThat(child.getParentId()).isEqualTo(1L);
        assertThat(child.getDepth()).isEqualTo(1);
    }

    @Test
    void id가_같으면_동등하다() {
        Category a = Category.createRoot("의류", 1);
        ReflectionTestUtils.setField(a, "id", 1L);
        Category b = Category.createRoot("잡화", 2);
        ReflectionTestUtils.setField(b, "id", 1L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void id가_없으면_같은_참조가_아닌_한_동등하지_않다() {
        Category a = Category.createRoot("의류", 1);
        Category b = Category.createRoot("의류", 1);

        assertThat(a).isNotEqualTo(b);
    }
}
