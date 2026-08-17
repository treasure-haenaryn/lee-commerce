package com.github.haenaryn.catalog.infrastructure.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

// 몇 번을 재시도해도 계속 실패하는 이벤트가 큐를 영원히 점유하지 않도록, 일정 횟수
// 이상 실패하면 PENDING으로 되돌리지 않고 FAILED로 고정한다(수동 개입이 필요한
// DLQ에 가까운 상태).
@Entity
@Table(name = "outbox_events", schema = "catalog")
@EntityListeners(AuditingEntityListener.class)
class CatalogOutboxEvent {

    private static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    // claimBatch가 반환한 이 엔티티 객체(와 그때의 version)를 폴러가 ES 호출이 끝날
    // 때까지 들고 있다가 그대로 save한다 — 그사이 다른 폴러가 리스 만료로 이 행을
    // 재선점해 커밋했다면 version이 이미 바뀐 상태라 이 save가
    // ObjectOptimisticLockingFailureException으로 실패한다. RefreshToken과 같은
    // 이유의 낙관적 락이지만, 여기서는 "재선점 후 결과 반영 경쟁"을 막는 데 쓰인다.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CatalogOutboxEvent() {
    }

    CatalogOutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    // 폴러가 이 행을 집어갔다는 표시다 — 이 상태로 바뀐 뒤 SELECT ... FOR UPDATE SKIP
    // LOCKED 트랜잭션이 즉시 커밋되므로, 실제 색인 작업(외부 ES 호출)은 DB 락을 잡지
    // 않은 채로 진행된다. 다른 폴러 인스턴스는 status = 'PENDING' 조건에 안 걸려 같은
    // 이벤트를 중복 처리하지 않는다. processingAt은 리스(lease) 시작 시각이다 — 폴러가
    // 이 행을 집어간 뒤 죽거나 배포로 종료되면 PROCESSING에 영원히 멈춰 있을 수 있어서,
    // 조회 쿼리가 일정 시간이 지난 PROCESSING 행도 다시 집어갈 수 있게 한다.
    void markProcessing(Instant processingAt) {
        this.status = OutboxEventStatus.PROCESSING;
        this.processingAt = processingAt;
    }

    void markPublished(Instant publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    void markFailed() {
        this.retryCount++;
        this.status = retryCount >= MAX_RETRY_COUNT ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING;
    }

    Long getId() {
        return id;
    }

    String getAggregateType() {
        return aggregateType;
    }

    Long getAggregateId() {
        return aggregateId;
    }

    String getEventType() {
        return eventType;
    }

    String getPayload() {
        return payload;
    }

    OutboxEventStatus getStatus() {
        return status;
    }

    int getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CatalogOutboxEvent other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
