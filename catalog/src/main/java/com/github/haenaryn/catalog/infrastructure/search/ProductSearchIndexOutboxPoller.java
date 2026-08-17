package com.github.haenaryn.catalog.infrastructure.search;

import com.github.haenaryn.catalog.domain.Product;
import com.github.haenaryn.catalog.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class ProductSearchIndexOutboxPoller {

    private static final int BATCH_SIZE = 50;

    private final CatalogOutboxEventGateway outboxEventGateway;
    private final ProductRepository productRepository;
    private final ProductSearchDocumentRepository productSearchDocumentRepository;

    // claim(짧은 트랜잭션)과 색인(트랜잭션 없음), 결과 반영(짧은 트랜잭션)을 분리한다 —
    // 배치 전체를 하나의 트랜잭션으로 묶으면 ES 호출이 느려질 때마다 DB 커넥션과 행
    // 잠금을 오래 쥐게 된다.
    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<CatalogOutboxEvent> claimed = outboxEventGateway.claimBatch(BATCH_SIZE);

        for (CatalogOutboxEvent event : claimed) {
            processOne(event);
        }
    }

    // OptimisticLockingFailureException은 CatalogOutboxEventGateway 트랜잭션 안에서
    // 잡지 않고 여기까지 전파시킨다(Gateway의 클래스 주석 참고) — 트랜잭션 밖인 여기서만
    // 안전하게 잡을 수 있다. 리스가 이미 만료돼 다른 폴러가 재선점한 것이므로 이 결과는
    // 버리고 넘어간다.
    private void processOne(CatalogOutboxEvent event) {
        try {
            index(event.getAggregateId());
            outboxEventGateway.markPublished(event, Instant.now());
        } catch (OptimisticLockingFailureException e) {
            log.warn("리스가 만료돼 처리 결과를 반영하지 않았다. eventId={}", event.getId());
        } catch (Exception e) {
            log.warn("상품 색인 동기화에 실패했다. productId={}", event.getAggregateId(), e);
            markFailedSafely(event);
        }
    }

    private void markFailedSafely(CatalogOutboxEvent event) {
        try {
            outboxEventGateway.markFailed(event);
        } catch (OptimisticLockingFailureException e) {
            log.warn("리스가 만료돼 실패 결과를 반영하지 않았다. eventId={}", event.getId());
        }
    }

    // 알려진 한계: 리스가 만료돼 다른 폴러가 같은 이벤트를 재선점한 뒤, 먼저 처리하던
    // 폴러의 이 save가 나중에 도착하면 더 최신 색인 결과를 오래된 값으로 덮어쓸 수 있다.
    // @Version은 outbox 행의 DB 상태 갱신만 보호하고 이 외부 ES 쓰기 자체는 보호하지
    // 않는다. 여러 인스턴스 배포 + 리스 타임아웃 초과 + 네트워크 순서 역전이 동시에
    // 일어나야 하는 낮은 확률의 시나리오라 지금은 받아들이기로 했다 — 막으려면
    // ProductSearchDocument에 단조증가 버전을 넣고 Elasticsearch의 external versioning을
    // 써야 한다.
    private void index(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalStateException("색인 대상 상품을 찾지 못했다: " + productId));
        productSearchDocumentRepository.save(toDocument(product));
    }

    private ProductSearchDocument toDocument(Product product) {
        return new ProductSearchDocument(
            String.valueOf(product.getId()),
            product.getName(),
            product.getCategoryId(),
            product.getBasePrice().amount(),
            product.getBasePrice().currency().getCurrencyCode(),
            product.getStatus().name());
    }
}
