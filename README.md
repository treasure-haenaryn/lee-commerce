# lee-commerce

주문 생성부터 결제 승인, 취소/환불까지 — 재고 동시성과 외부 PG 연동의 정합성을 직접 다루는
단일 판매자 커머스 백엔드. DDD 기반 모듈러 모놀리스로 Bounded Context 경계를 Gradle 모듈과
ArchUnit으로 강제한다. 프론트엔드는 다루지 않는다(API + Swagger로 대체).

## 무엇을 다루는가

- 회원가입/로그인 → 장바구니 → 주문 → 결제(토스페이먼츠) → 취소/환불로 이어지는 전체 흐름
- 동시 주문 상황에서 재고가 오버셀되지 않도록 낙관적 락 / 비관적 락 / Redis 분산락 세 가지 전략 비교
- 결제 승인과 재고 예약 만료가 동시에 발생하는 레이스컨디션 처리
- Outbox 패턴 기반 비동기 이벤트 발행(Kafka), 검색 인덱스(Elasticsearch) 동기화

## 기술 스택

- Java 25 (LTS)
- Spring Boot 4.1
- PostgreSQL 18 (Bounded Context별 스키마 분리)
- Kafka 4.1 (Outbox 이벤트 발행)
- Redis + Redisson 4.7 (재고 분산락 + 디바이스별 세션 관리)
- Elasticsearch + Spring Data Elasticsearch 6.0 (상품 검색 전용 읽기 인덱스, Outbox 기반 동기화)
- 토스페이먼츠 (결제, 테스트모드 연동)
- JUnit5 5.14, Mockito 5.23, Testcontainers 2.0, ArchUnit 1.4
- Docker Compose (로컬 개발)
- springdoc-openapi 2.8 (Swagger/OpenAPI)

## 핵심 유저 플로우

```
장바구니(일부 선택 가능) → 주문서 작성(주소록 선택 또는 신규 입력)
   → 주문 생성 [PENDING] + 재고 "예약" (동시성 제어)
   → 토스페이먼츠 결제창 호출 (제한시간 15분)
   → 결제 승인 콜백/webhook 검증 (멱등성 보장)
   → 성공: 재고 확정 차감, 주문 [PAID]
   → 실패/타임아웃: 재고 예약 해제, 주문 [FAILED]
   → 배송준비 → 배송중 → 배송완료
   → (배송 시작 전까지) 취소 요청 → 환불(부분환불 포함)
```

### 주문 상태 머신

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> PAID : 결제 승인
    PENDING --> FAILED : 결제 실패/타임아웃
    PAID --> CANCEL_REQUESTED : 취소 요청
    CANCEL_REQUESTED --> REFUNDED : 환불 완료
    FAILED --> [*]
    REFUNDED --> [*]
    PAID --> [*]
```

### 결제 상태 머신

```mermaid
stateDiagram-v2
    [*] --> READY
    READY --> PENDING : 결제 시도
    PENDING --> PAID : PG 승인
    PENDING --> FAILED : PG 승인 실패
    PENDING --> EXPIRED : 제한시간(15분) 초과
    PAID --> CANCELED : 취소/환불
    EXPIRED --> CANCELED : 뒤늦은 PG 승인 자동 취소
    FAILED --> [*]
    EXPIRED --> [*]
    CANCELED --> [*]
    PAID --> [*]
```

## 도메인 (Bounded Context)

| Context | 책임 |
|---|---|
| `user` | 회원가입/로그인, JWT+Redis 디바이스별 세션, 배송지 주소록 |
| `catalog` | 상품/카테고리/옵션(SKU)/할인 정책, 검색(Elasticsearch) |
| `inventory` | 재고, 재고 예약(동시성 제어 3전략 비교) |
| `cart` | 장바구니 |
| `order` | 주문, 배송(Shipment) |
| `payment` | 결제(토스페이먼츠), 환불 |

각 컨텍스트의 불변식/정책은 Obsidian `domain/{context}.md`에서 관리한다(이 repo 밖, 개인 vault).

## ERD

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar phone
        varchar status
        timestamp created_at
        timestamp updated_at
    }
    REFRESH_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        timestamp issued_at
        timestamp expires_at
        timestamp revoked_at
        bigint replaced_by_token_id FK
    }
    ADDRESSES {
        bigint id PK
        bigint user_id FK
        varchar recipient_name
        varchar phone
        varchar zip_code
        varchar address_line1
        varchar address_line2
        boolean is_default
    }
    CATEGORIES {
        bigint id PK
        bigint parent_id FK
        varchar name
        int depth
        int display_order
    }
    PRODUCTS {
        bigint id PK
        bigint category_id FK
        varchar name
        text description
        numeric base_price_amount
        varchar base_price_currency
        varchar status
    }
    PRODUCT_OPTIONS {
        bigint id PK
        bigint product_id FK
        varchar sku_code UK
        varchar size
        varchar color
        numeric price_override_amount
        varchar price_override_currency
        boolean is_active
    }
    PRODUCT_DISCOUNTS {
        bigint id PK
        bigint product_id FK
        varchar discount_type
        numeric discount_value
        timestamp starts_at
        timestamp ends_at
        boolean is_active
    }
    STOCKS {
        bigint id PK
        bigint product_option_id UK
        int available_quantity
        int reserved_quantity
        bigint version
    }
    STOCK_RESERVATIONS {
        bigint id PK
        bigint stock_id FK
        bigint order_id
        int quantity
        varchar status
    }
    CARTS {
        bigint id PK
        bigint user_id UK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_option_id
        int quantity
    }
    ORDERS {
        bigint id PK
        bigint user_id
        varchar status
        numeric total_amount
        varchar total_currency
        varchar recipient_name
        varchar address_line1
        varchar zip_code
        varchar phone
        bigint version
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_option_id
        varchar product_name_snapshot
        varchar option_snapshot
        numeric unit_price_amount
        varchar unit_price_currency
        int quantity
    }
    SHIPMENTS {
        bigint id PK
        bigint order_id UK
        varchar status
        varchar carrier
        varchar tracking_number
        timestamp shipped_at
        timestamp delivered_at
    }
    OUTBOX_EVENTS {
        bigint id PK
        varchar aggregate_type
        bigint aggregate_id
        varchar event_type
        jsonb payload
        varchar status
        int retry_count
    }
    PAYMENTS {
        bigint id PK
        bigint order_id UK
        varchar payment_key UK
        varchar method
        numeric amount
        varchar currency
        varchar status
        timestamp expires_at
    }
    PAYMENT_WEBHOOK_EVENTS {
        bigint id PK
        varchar provider_event_id UK
        varchar payment_key
        varchar event_type
        boolean processed
    }
    REFUNDS {
        bigint id PK
        bigint payment_id FK
        bigint order_item_id
        numeric amount
        varchar reason
        varchar status
    }

    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ ADDRESSES : owns
    CATEGORIES ||--o{ CATEGORIES : "parent of"
    CATEGORIES ||--o{ PRODUCTS : classifies
    PRODUCTS ||--o{ PRODUCT_OPTIONS : "SKU"
    PRODUCTS ||--o{ PRODUCT_DISCOUNTS : "할인 정책"
    PRODUCT_OPTIONS ||--o| STOCKS : "재고 추적"
    STOCKS ||--o{ STOCK_RESERVATIONS : reserves
    USERS ||--|| CARTS : owns
    CARTS ||--o{ CART_ITEMS : contains
    PRODUCT_OPTIONS ||--o{ CART_ITEMS : "담김"
    USERS ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEMS : contains
    PRODUCT_OPTIONS ||--o{ ORDER_ITEMS : "주문됨"
    ORDERS ||--|| SHIPMENTS : ships
    ORDERS ||--o{ OUTBOX_EVENTS : emits
    ORDERS ||--o{ STOCK_RESERVATIONS : "재고 예약"
    ORDERS ||--o| PAYMENTS : "결제됨"
    PAYMENTS ||--o{ PAYMENT_WEBHOOK_EVENTS : receives
    PAYMENTS ||--o{ REFUNDS : refunds
    ORDER_ITEMS ||--o{ REFUNDS : "환불 대상"
```

PK/UK/FK가 안 붙은 참조 필드(`orders.user_id`, `stocks.product_option_id` 등)는 스키마 경계를 넘는
논리적 참조다 — 물리적 FK가 없고, mermaid 문법상 시각적으로도 구분되지 않는다. 실제 DDL은
[`db/schema.sql`](./db/schema.sql) 참고.

## 프로젝트 구조

```
.
└── db/schema.sql        PostgreSQL DDL (스키마별)
```
