package com.github.haenaryn.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories", schema = "catalog")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Category() {
    }

    private Category(Long parentId, String name, int depth, int displayOrder) {
        this.parentId = parentId;
        this.name = name;
        this.depth = depth;
        this.displayOrder = displayOrder;
    }

    public static Category createRoot(String name, int displayOrder) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 비어 있을 수 없다");
        }
        return new Category(null, name, 0, displayOrder);
    }

    public static Category createChild(Category parent, String name, int displayOrder) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 비어 있을 수 없다");
        }
        if (parent.getId() == null) {
            throw new IllegalArgumentException("부모 카테고리가 아직 저장되지 않았다");
        }
        return new Category(parent.getId(), name, parent.depth + 1, displayOrder);
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
