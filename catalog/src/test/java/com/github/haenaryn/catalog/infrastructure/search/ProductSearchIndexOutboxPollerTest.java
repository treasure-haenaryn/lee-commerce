package com.github.haenaryn.catalog.infrastructure.search;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductOptionSpec;
import com.github.haenaryn.catalog.domain.ProductRepository;
import com.github.haenaryn.common.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchIndexOutboxPollerTest {

    @Mock
    private CatalogOutboxEventGateway outboxEventGateway;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchDocumentRepository productSearchDocumentRepository;

    private ProductSearchIndexOutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new ProductSearchIndexOutboxPoller(outboxEventGateway, productRepository, productSearchDocumentRepository);
    }

    @Test
    void 상품을_찾으면_색인하고_발행_완료로_표시한다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventGateway.claimBatch(anyInt())).thenReturn(List.of(event));
        Product product = Product.register(1L, "티셔츠", null,
            new Money(new BigDecimal("10000"), Currency.getInstance("KRW")),
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        poller.poll();

        verify(productSearchDocumentRepository).save(any());
        verify(outboxEventGateway).markPublished(eq(event), any());
        verify(outboxEventGateway, never()).markFailed(any());
    }

    @Test
    void 상품을_찾지_못하면_실패로_표시한다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventGateway.claimBatch(anyInt())).thenReturn(List.of(event));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        poller.poll();

        verify(productSearchDocumentRepository, never()).save(any());
        verify(outboxEventGateway).markFailed(event);
        verify(outboxEventGateway, never()).markPublished(any(), any());
    }

    @Test
    void 발행_반영_중_리스가_만료되면_실패_처리를_다시_시도하지_않는다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventGateway.claimBatch(anyInt())).thenReturn(List.of(event));
        Product product = Product.register(1L, "티셔츠", null,
            new Money(new BigDecimal("10000"), Currency.getInstance("KRW")),
            List.of(new ProductOptionSpec("SKU-1", null, null, null)));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doThrow(new OptimisticLockingFailureException("stale"))
            .when(outboxEventGateway).markPublished(eq(event), any());

        poller.poll();

        verify(outboxEventGateway, never()).markFailed(any());
    }

    @Test
    void 실패_반영_중_리스가_만료돼도_예외가_전파되지_않는다() {
        CatalogOutboxEvent event = new CatalogOutboxEvent("Product", 1L, "ProductChanged", "{}");
        when(outboxEventGateway.claimBatch(anyInt())).thenReturn(List.of(event));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        doThrow(new OptimisticLockingFailureException("stale")).when(outboxEventGateway).markFailed(event);

        poller.poll();
    }
}
