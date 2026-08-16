package com.github.haenaryn.catalog.domain;

import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final Currency KRW = Currency.getInstance("KRW");
    private static final Money BASE_PRICE = new Money(new BigDecimal("10000"), KRW);

    private ProductOptionSpec spec(String skuCode) {
        return new ProductOptionSpec(skuCode, null, null, null);
    }

    @Test
    void 옵션이_없으면_등록에_실패한다() {
        assertThatThrownBy(() -> Product.register(1L, "티셔츠", null, BASE_PRICE, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이름이_비어있으면_등록에_실패한다() {
        assertThatThrownBy(() -> Product.register(1L, " ", null, BASE_PRICE, List.of(spec("SKU-1"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 카테고리가_없으면_등록에_실패한다() {
        assertThatThrownBy(() -> Product.register(null, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기본가가_음수면_등록에_실패한다() {
        Money negative = new Money(new BigDecimal("-1"), KRW);

        assertThatThrownBy(() -> Product.register(1L, "티셔츠", null, negative, List.of(spec("SKU-1"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 옵션_스펙만큼_옵션이_생성된다() {
        Product product = Product.register(
            1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1"), spec("SKU-2")));

        assertThat(product.getOptions()).hasSize(2);
        assertThat(product.getOptions()).extracting(ProductOption::getSkuCode)
            .containsExactlyInAnyOrder("SKU-1", "SKU-2");
    }

    @Test
    void 옵션을_추가할_수_있다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1")));

        product.addOption(spec("SKU-2"));

        assertThat(product.getOptions()).hasSize(2);
    }

    @Test
    void 등록_시점에_SKU가_중복되면_실패한다() {
        assertThatThrownBy(() ->
            Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1"), spec("SKU-1"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이미_존재하는_SKU를_추가하면_실패한다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1")));

        assertThatThrownBy(() -> product.addOption(spec("SKU-1")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상태를_null로_변경할_수_없다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1")));

        assertThatThrownBy(() -> product.changeStatus(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 마지막_활성_옵션은_비활성화할_수_없다() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1")));

        assertThatThrownBy(() -> product.deactivateOption("SKU-1"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 활성_옵션이_둘_이상이면_하나를_비활성화할_수_있다() {
        Product product = Product.register(
            1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1"), spec("SKU-2")));

        product.deactivateOption("SKU-1");

        assertThat(product.getOptions())
            .filteredOn(option -> option.getSkuCode().equals("SKU-1"))
            .allMatch(option -> !option.isActive());
    }

    @Test
    void 존재하지_않는_옵션을_비활성화하면_예외() {
        Product product = Product.register(1L, "티셔츠", null, BASE_PRICE, List.of(spec("SKU-1")));

        assertThatThrownBy(() -> product.deactivateOption("NOT-EXIST"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
