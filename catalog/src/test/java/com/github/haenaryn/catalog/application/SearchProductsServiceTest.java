package com.github.haenaryn.catalog.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchProductsServiceTest {

    @Mock
    private ProductSearchPort productSearchPort;

    private SearchProductsService service;

    @BeforeEach
    void setUp() {
        service = new SearchProductsService(productSearchPort);
    }

    @Test
    void 검색_조건에_상관없이_ON_SALE_상태만_요청한다() {
        SearchProductsResult expected = new SearchProductsResult(List.of(), 0, 0, 20);
        when(productSearchPort.search(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        SearchProductsResult result = service.search(
            new SearchProductsCommand("티셔츠", 1L, new BigDecimal("1000"), new BigDecimal("50000"), 0, 20));

        ArgumentCaptor<SearchProductsQuery> captor = ArgumentCaptor.forClass(SearchProductsQuery.class);
        verify(productSearchPort).search(captor.capture());

        SearchProductsQuery query = captor.getValue();
        assertThat(query.status()).isEqualTo("ON_SALE");
        assertThat(query.name()).isEqualTo("티셔츠");
        assertThat(query.categoryId()).isEqualTo(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void page가_음수면_예외() {
        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(null, null, null, null, -1, 20)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(productSearchPort, never()).search(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void size가_범위를_벗어나면_예외() {
        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(null, null, null, null, 0, 0)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(null, null, null, null, 0, 101)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 가격이_음수면_예외() {
        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(null, null, new BigDecimal("-1"), null, 0, 20)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 최소가격이_최대가격보다_크면_예외() {
        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(
                null, null, new BigDecimal("50000"), new BigDecimal("1000"), 0, 20)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 조회_범위가_result_window를_넘으면_예외() {
        assertThatThrownBy(() -> service.search(
            new SearchProductsCommand(null, null, null, null, 100, 100)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(productSearchPort, never()).search(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 조회_범위가_result_window_경계면_허용한다() {
        when(productSearchPort.search(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new SearchProductsResult(List.of(), 0, 99, 100));

        service.search(new SearchProductsCommand(null, null, null, null, 99, 100));

        verify(productSearchPort).search(org.mockito.ArgumentMatchers.any());
    }
}
