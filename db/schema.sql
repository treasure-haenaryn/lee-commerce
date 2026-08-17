-- Bounded Context별로 PostgreSQL 스키마를 분리한다: user, catalog, inventory, cart, order, payment.
-- 스키마를 넘는 참조는 물리적 FK를 걸지 않고 ID만 저장한다 — 컨텍스트 경계를 DB 레벨에서도
-- 강제해 실수로 다른 컨텍스트 테이블과 JOIN하는 것을 막기 위함이며, 정합성은 Facade 호출
-- 시점에 애플리케이션이 검증한다. 이런 컬럼은 COMMENT ON COLUMN으로 논리적 참조 대상을 명시했다.
--
-- "user", "order"는 PostgreSQL 예약어이므로 스키마명을 큰따옴표로 감싸야 한다.
-- 애플리케이션/JPA 설정(예: Hibernate default_schema)에서도 동일하게 다뤄야 한다.
--
-- 재고/장바구니/주문은 상품(product)이 아니라 상품 옵션(product_option, SKU) 단위로 다룬다.
-- 옵션이 없는 단순 상품도 옵션 1개(size/color 모두 NULL)로 등록해서 재고·주문 흐름을 통일한다.
--
-- users.status, refunds.status는 값 집합이 아직 확정되지 않아 CHECK 제약 없이 varchar로만 둔다.
-- products.status, payments.status, stock_reservations.status, shipments.status, orders.status는
-- 값 집합이 이미 확정되어 있어 CHECK 제약으로 반영했다.

-- =====================================================================
-- 0. 스키마
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS "user";
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS cart;
CREATE SCHEMA IF NOT EXISTS "order";
CREATE SCHEMA IF NOT EXISTS payment;

-- =====================================================================
-- 1. user 스키마
-- =====================================================================

CREATE TABLE "user".users (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           varchar(255)  NOT NULL,
    password_hash   varchar(255)  NOT NULL,
    name            varchar(100)  NOT NULL,
    phone           varchar(20),
    status          varchar(20)   NOT NULL,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE "user".refresh_tokens (
    id                      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 bigint       NOT NULL REFERENCES "user".users (id),
    token_hash              varchar(255) NOT NULL,
    issued_at               timestamptz  NOT NULL DEFAULT now(),
    expires_at              timestamptz  NOT NULL,
    revoked_at              timestamptz,
    replaced_by_token_id    bigint REFERENCES "user".refresh_tokens (id),
    version                 bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user_id ON "user".refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires_at ON "user".refresh_tokens (expires_at);

COMMENT ON TABLE "user".refresh_tokens IS
    'DB는 회전(rotation) 이력의 감사 추적용이다. "현재 유효한 세션"의 실시간 조회/폐기는 '
    'Redis(user_id:device_id 키)가 맡는다 — 두 저장소가 이력/현재 상태로 역할이 나뉜다.';
COMMENT ON COLUMN "user".refresh_tokens.version IS
    '같은 토큰으로 동시에 재발급 요청이 들어와도 하나만 성공하도록 낙관적 락으로 막는다 — '
    '없으면 두 요청이 동시에 미폐기 상태를 읽어 각각 새 토큰을 발급해버릴 수 있다.';

CREATE TABLE "user".addresses (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         bigint       NOT NULL REFERENCES "user".users (id),
    recipient_name  varchar(100) NOT NULL,
    phone           varchar(20)  NOT NULL,
    zip_code        varchar(20)  NOT NULL,
    address_line1   varchar(255) NOT NULL,
    address_line2   varchar(255),
    is_default      boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX ix_addresses_user_id ON "user".addresses (user_id);
-- 사용자당 기본 배송지는 최대 1개만 존재할 수 있다.
CREATE UNIQUE INDEX uk_addresses_user_default ON "user".addresses (user_id) WHERE is_default;

-- =====================================================================
-- 2. catalog 스키마
-- =====================================================================

CREATE TABLE catalog.categories (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id       bigint REFERENCES catalog.categories (id),
    name            varchar(100) NOT NULL,
    depth           int          NOT NULL DEFAULT 0,
    display_order   int          NOT NULL DEFAULT 0
);

CREATE INDEX ix_categories_parent_id ON catalog.categories (parent_id);

CREATE TABLE catalog.products (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id           bigint        NOT NULL REFERENCES catalog.categories (id),
    name                  varchar(255)  NOT NULL,
    description           text,
    base_price_amount     numeric(14,2) NOT NULL,
    base_price_currency   varchar(3)    NOT NULL,
    status                varchar(20)   NOT NULL,
    created_at            timestamptz   NOT NULL DEFAULT now(),
    updated_at            timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_products_base_price_nonneg CHECK (base_price_amount >= 0),
    CONSTRAINT ck_products_status CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'HIDDEN', 'DISCONTINUED'))
);

CREATE INDEX ix_products_category_id ON catalog.products (category_id);
CREATE INDEX ix_products_status ON catalog.products (status);

COMMENT ON CONSTRAINT ck_products_status ON catalog.products IS
    'SOLD_OUT은 재입고 가능성이 있는 일시 품절, DISCONTINUED는 재입고 없이 판매를 끝낸 단종 — '
    '재고가 0이 됐다고 자동으로 DISCONTINUED가 되지 않는다.';

CREATE TABLE catalog.product_options (
    id                          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id                  bigint       NOT NULL REFERENCES catalog.products (id),
    sku_code                    varchar(50)  NOT NULL,
    size                        varchar(20),
    color                       varchar(30),
    price_override_amount       numeric(14,2),
    price_override_currency     varchar(3),
    is_active                   boolean      NOT NULL DEFAULT true,
    created_at                  timestamptz  NOT NULL DEFAULT now(),
    updated_at                  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_options_sku_code UNIQUE (sku_code),
    CONSTRAINT ck_product_options_price_override_nonneg
        CHECK (price_override_amount IS NULL OR price_override_amount >= 0),
    CONSTRAINT ck_product_options_price_override_pair
        CHECK ((price_override_amount IS NULL) = (price_override_currency IS NULL))
);

CREATE INDEX ix_product_options_product_id ON catalog.product_options (product_id);

COMMENT ON TABLE catalog.product_options IS
    '재고/가격/주문은 항상 이 테이블(SKU) 단위로만 다룬다. 옵션이 없는 단순 상품도 '
    'size/color가 모두 NULL인 옵션 1개로 등록해서 흐름을 통일한다.';
COMMENT ON COLUMN catalog.product_options.price_override_amount IS
    'NULL이면 상품의 base_price를 그대로 쓴다. 옵션별로 가격이 달라질 때만 값을 채운다.';

CREATE TABLE catalog.product_discounts (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      bigint        NOT NULL REFERENCES catalog.products (id),
    discount_type   varchar(30)   NOT NULL,
    discount_value  numeric(14,2) NOT NULL,
    starts_at       timestamptz,
    ends_at         timestamptz,
    is_active       boolean       NOT NULL DEFAULT true,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_product_discounts_value_pos CHECK (discount_value > 0)
);

CREATE INDEX ix_product_discounts_product_id ON catalog.product_discounts (product_id);

COMMENT ON COLUMN catalog.product_discounts.discount_type IS
    '할인 계산 전략(정률/정액 등)을 고르는 dispatch 키. 전략 패턴으로 구현해 새 방식을 '
    '추가할 때 마이그레이션이 필요 없도록 일부러 CHECK 제약을 두지 않는다.';

-- aggregate_id는 이벤트를 발행하는 애그리거트(현재는 products)를 가리키는 범용 컬럼이라
-- 특정 테이블에 대한 FK를 걸지 않는다.
CREATE TABLE catalog.outbox_events (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_type  varchar(50) NOT NULL,
    aggregate_id    bigint      NOT NULL,
    event_type      varchar(100) NOT NULL,
    payload         jsonb       NOT NULL,
    status          varchar(20) NOT NULL,
    retry_count     int         NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL DEFAULT now(),
    published_at    timestamptz,
    -- 폴러가 이 행을 집어간(PROCESSING으로 바꾼) 시각. 리스가 만료된 PROCESSING 행을
    -- 재선점하는 데 쓴다 — 폴러가 죽어도 이 행이 영원히 멈춰 있지 않도록 하기 위함.
    processing_at   timestamptz,
    -- "user".refresh_tokens.version과 같은 목적의 낙관적 락이다 — 리스가 만료돼 다른
    -- 폴러가 이 행을 재선점한 뒤에, 원래 작업자가 뒤늦게 처리 결과를 반영하려다 그
    -- 재선점 작업자의 갱신을 덮어쓰는 것을 막는다.
    version         bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_catalog_outbox_events_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_catalog_outbox_events_retry_count_nonneg CHECK (retry_count >= 0)
);

-- 폴러(@Scheduled)가 SELECT ... FOR UPDATE SKIP LOCKED로 PENDING 이벤트를 긁어가는 쿼리를 지원.
CREATE INDEX ix_catalog_outbox_events_status_created_at ON catalog.outbox_events (status, created_at);
CREATE INDEX ix_catalog_outbox_events_aggregate ON catalog.outbox_events (aggregate_type, aggregate_id);
-- 같은 쿼리가 리스 만료된 PROCESSING 행을 재선점하는 부분을 지원. PROCESSING 행만
-- 대상이라 부분 인덱스로 좁힌다.
CREATE INDEX ix_catalog_outbox_events_processing_at ON catalog.outbox_events (processing_at)
    WHERE status = 'PROCESSING';
-- 보존 기간이 지난 PUBLISHED 행을 정리하는 배치 삭제 쿼리를 지원.
CREATE INDEX ix_catalog_outbox_events_status_published_at ON catalog.outbox_events (status, published_at);

-- =====================================================================
-- 3. inventory 스키마
-- =====================================================================

CREATE TABLE inventory.stocks (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_option_id   bigint  NOT NULL,
    available_quantity  int     NOT NULL DEFAULT 0,
    reserved_quantity   int     NOT NULL DEFAULT 0,
    version             bigint  NOT NULL DEFAULT 0,
    CONSTRAINT uk_stocks_product_option_id UNIQUE (product_option_id),
    CONSTRAINT ck_stocks_available_nonneg CHECK (available_quantity >= 0),
    CONSTRAINT ck_stocks_reserved_nonneg CHECK (reserved_quantity >= 0)
);

COMMENT ON COLUMN inventory.stocks.product_option_id IS
    '논리적 참조: catalog.product_options.id (스키마 간 물리적 FK 없음)';

CREATE TABLE inventory.stock_reservations (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stock_id    bigint      NOT NULL REFERENCES inventory.stocks (id),
    order_id    bigint      NOT NULL,
    quantity    int         NOT NULL,
    status      varchar(20) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_reservations_order_stock UNIQUE (order_id, stock_id),
    CONSTRAINT ck_stock_reservations_quantity_pos CHECK (quantity > 0),
    CONSTRAINT ck_stock_reservations_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED'))
);

COMMENT ON COLUMN inventory.stock_reservations.order_id IS
    '논리적 참조: "order".orders.id (스키마 간 물리적 FK 없음). '
    'UNIQUE(order_id, stock_id)는 재고 확정/해제를 재시도해도 같은 주문이 같은 재고를 '
    '중복 차감하지 않도록 막는 멱등성 근거다.';

CREATE INDEX ix_stock_reservations_order_id ON inventory.stock_reservations (order_id);
CREATE INDEX ix_stock_reservations_status ON inventory.stock_reservations (status);

-- =====================================================================
-- 4. cart 스키마
-- =====================================================================

CREATE TABLE cart.carts (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_carts_user_id UNIQUE (user_id)
);

COMMENT ON COLUMN cart.carts.user_id IS
    '논리적 참조: "user".users.id (스키마 간 물리적 FK 없음)';

CREATE TABLE cart.cart_items (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id             bigint  NOT NULL REFERENCES cart.carts (id),
    product_option_id   bigint  NOT NULL,
    quantity            int     NOT NULL,
    CONSTRAINT ck_cart_items_quantity_pos CHECK (quantity > 0)
);

COMMENT ON COLUMN cart.cart_items.product_option_id IS
    '논리적 참조: catalog.product_options.id (스키마 간 물리적 FK 없음). 옵션이 품절/판매중지/삭제돼도 '
    '이 로우는 지우지 않는다 — 구매 가능 여부 표시는 조회 시점에 판단한다.';

CREATE INDEX ix_cart_items_cart_id ON cart.cart_items (cart_id);
CREATE INDEX ix_cart_items_product_option_id ON cart.cart_items (product_option_id);

-- =====================================================================
-- 5. order 스키마
-- =====================================================================

CREATE TABLE "order".orders (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         bigint        NOT NULL,
    status          varchar(20)   NOT NULL,
    total_amount    numeric(14,2) NOT NULL,
    total_currency  varchar(3)    NOT NULL,
    recipient_name  varchar(100)  NOT NULL,
    address_line1   varchar(255)  NOT NULL,
    address_line2   varchar(255),
    zip_code        varchar(20)   NOT NULL,
    phone           varchar(20)   NOT NULL,
    version         bigint        NOT NULL DEFAULT 0,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_orders_total_amount_nonneg CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_status CHECK (
        status IN ('PENDING', 'PAID', 'FAILED', 'CANCEL_REQUESTED', 'REFUNDED')
    )
);

COMMENT ON COLUMN "order".orders.user_id IS
    '논리적 참조: "user".users.id (스키마 간 물리적 FK 없음)';
COMMENT ON COLUMN "order".orders.recipient_name IS
    '주문 시점 배송지 스냅샷이다 — "user".addresses에서 골랐든 새로 입력했든, 이후 주소록이 '
    '바뀌거나 삭제돼도 과거 주문의 배송지는 불변이어야 하므로 참조가 아니라 값을 복사해 저장한다.';
COMMENT ON CONSTRAINT ck_orders_status ON "order".orders IS
    '주문 상태 머신: PENDING -> PAID/FAILED, PAID -> CANCEL_REQUESTED -> REFUNDED';

CREATE INDEX ix_orders_user_id ON "order".orders (user_id);
CREATE INDEX ix_orders_status ON "order".orders (status);

CREATE TABLE "order".order_items (
    id                      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                bigint        NOT NULL REFERENCES "order".orders (id),
    product_option_id       bigint        NOT NULL,
    product_name_snapshot   varchar(255)  NOT NULL,
    option_snapshot         varchar(255),
    unit_price_amount       numeric(14,2) NOT NULL,
    unit_price_currency     varchar(3)    NOT NULL,
    quantity                int           NOT NULL,
    CONSTRAINT ck_order_items_quantity_pos CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_nonneg CHECK (unit_price_amount >= 0)
);

COMMENT ON COLUMN "order".order_items.product_option_id IS
    '논리적 참조: catalog.product_options.id (스키마 간 물리적 FK 없음)';
COMMENT ON COLUMN "order".order_items.option_snapshot IS
    '주문 시점 옵션 표시값(예: "사이즈 L / 색상 Red") 스냅샷. product_name_snapshot과 같은 이유로, '
    '옵션이 이후 바뀌거나 삭제돼도 과거 주문 표시는 불변이어야 한다.';

CREATE INDEX ix_order_items_order_id ON "order".order_items (order_id);

CREATE TABLE "order".shipments (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        bigint      NOT NULL REFERENCES "order".orders (id),
    status          varchar(20) NOT NULL DEFAULT 'PREPARING',
    carrier         varchar(50),
    tracking_number varchar(100),
    shipped_at      timestamptz,
    delivered_at    timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_shipments_order_id UNIQUE (order_id),
    CONSTRAINT ck_shipments_status CHECK (status IN ('PREPARING', 'SHIPPING', 'DELIVERED'))
);

COMMENT ON TABLE "order".shipments IS
    '배송은 주문 상태와 별도로 전이된다 — 결제/취소 흐름(orders.status)과 배송 흐름의 관심사를 '
    '분리했다. 다만 주문 취소는 이 테이블의 status가 PREPARING일 때만 허용한다(애플리케이션 규칙).';
COMMENT ON CONSTRAINT uk_shipments_order_id ON "order".shipments IS
    '주문당 배송은 하나만 있다고 가정한다(분할배송 미지원) — 분할배송이 필요해지면 이 제약부터 풀어야 한다.';

CREATE INDEX ix_shipments_status ON "order".shipments (status);

-- aggregate_id는 이벤트를 발행하는 애그리거트(현재는 orders)를 가리키는 범용 컬럼이라
-- 특정 테이블에 대한 FK를 걸지 않는다.
CREATE TABLE "order".outbox_events (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_type  varchar(50) NOT NULL,
    aggregate_id    bigint      NOT NULL,
    event_type      varchar(100) NOT NULL,
    payload         jsonb       NOT NULL,
    status          varchar(20) NOT NULL,
    retry_count     int         NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL DEFAULT now(),
    published_at    timestamptz
);

-- 폴러(@Scheduled)가 SELECT ... FOR UPDATE SKIP LOCKED로 PENDING 이벤트를 긁어가는 쿼리를 지원.
CREATE INDEX ix_outbox_events_status_created_at ON "order".outbox_events (status, created_at);
CREATE INDEX ix_outbox_events_aggregate ON "order".outbox_events (aggregate_type, aggregate_id);

-- =====================================================================
-- 6. payment 스키마
-- =====================================================================

CREATE TABLE payment.payments (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        bigint        NOT NULL,
    payment_key     varchar(255)  NOT NULL,
    method          varchar(30)   NOT NULL,
    amount          numeric(14,2) NOT NULL,
    currency        varchar(3)    NOT NULL,
    status          varchar(20)   NOT NULL,
    expires_at      timestamptz,
    approved_at     timestamptz,
    canceled_at     timestamptz,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT uk_payments_payment_key UNIQUE (payment_key),
    CONSTRAINT ck_payments_amount_nonneg CHECK (amount >= 0),
    CONSTRAINT ck_payments_status CHECK (
        status IN ('READY', 'PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELED')
    )
);

COMMENT ON COLUMN payment.payments.order_id IS
    '논리적 참조: "order".orders.id (스키마 간 물리적 FK 없음)';
COMMENT ON COLUMN payment.payments.payment_key IS
    '토스페이먼츠가 발급한 키. 승인 API를 재시도해도 중복 승인되지 않도록 멱등키로 재사용한다.';
COMMENT ON COLUMN payment.payments.expires_at IS
    'PENDING 진입 시점 + 15분. 폴러가 이 시각이 지난 PENDING 건을 조건부 UPDATE(WHERE status = '
    '''PENDING'')로 원자적으로 EXPIRED 처리하는 기준 — PAID 전이와 동시에 들어와도 둘 중 하나만 성공한다.';

CREATE INDEX ix_payments_status_expires_at ON payment.payments (status, expires_at);

CREATE TABLE payment.payment_webhook_events (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_event_id   varchar(255) NOT NULL,
    payment_key         varchar(255) NOT NULL,
    event_type          varchar(100) NOT NULL,
    payload             jsonb        NOT NULL,
    processed           boolean      NOT NULL DEFAULT false,
    received_at         timestamptz  NOT NULL DEFAULT now(),
    processed_at        timestamptz,
    CONSTRAINT uk_payment_webhook_events_provider_event_id UNIQUE (provider_event_id)
);

COMMENT ON COLUMN payment.payment_webhook_events.provider_event_id IS
    'PG(토스페이먼츠)의 webhook 중복 발송을 감지하는 1차 멱등 방어선';
COMMENT ON COLUMN payment.payment_webhook_events.payment_key IS
    '논리적 참조: payment.payments.payment_key (같은 스키마이지만 UK 컬럼 참조라 물리적 FK를 걸지 않음)';

CREATE INDEX ix_payment_webhook_events_payment_key ON payment.payment_webhook_events (payment_key);

CREATE TABLE payment.refunds (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id      bigint        NOT NULL REFERENCES payment.payments (id),
    order_item_id   bigint,
    amount          numeric(14,2) NOT NULL,
    reason          varchar(255),
    status          varchar(20)   NOT NULL,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    completed_at    timestamptz,
    CONSTRAINT ck_refunds_amount_pos CHECK (amount > 0)
);

COMMENT ON COLUMN payment.refunds.order_item_id IS
    '논리적 참조: "order".order_items.id (스키마 간 물리적 FK 없음). NULL이면 특정 상품이 아니라 '
    '주문 전체 단위 환불.';

CREATE INDEX ix_refunds_payment_id ON payment.refunds (payment_id);

-- 환불 합계가 결제 금액을 넘으면 안 된다는 건 돈 문제라 애플리케이션 검증만 믿지 않고 DB에서도
-- 막는다. BEFORE 트리거로 이 로우가 refunds에 실제로 들어가기 전에 payments를 먼저 잠근다 —
-- 그래야 동시에 들어온 다른 환불 삽입은 이 잠금이 풀릴 때까지 대기하고, 잠금을 얻은 뒤에는
-- 새 스냅샷으로 SUM을 다시 읽어 그사이 커밋된 환불까지 정확히 반영한다. 자기 자신은 아직 삽입
-- 전이라 SUM에 안 잡히므로 NEW.amount를 직접 더해서 비교한다.
CREATE FUNCTION payment.enforce_refund_total() RETURNS trigger AS $$
DECLARE
    payment_amount  numeric(14,2);
    other_total     numeric(14,2);
BEGIN
    SELECT amount INTO payment_amount
        FROM payment.payments WHERE id = NEW.payment_id FOR UPDATE;
    SELECT COALESCE(SUM(amount), 0) INTO other_total
        FROM payment.refunds WHERE payment_id = NEW.payment_id AND id <> NEW.id;

    IF other_total + NEW.amount > payment_amount THEN
        RAISE EXCEPTION '환불 합계(%)가 결제 금액(%)을 초과합니다', other_total + NEW.amount, payment_amount;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_refunds_total_check
    BEFORE INSERT OR UPDATE ON payment.refunds
    FOR EACH ROW EXECUTE FUNCTION payment.enforce_refund_total();
