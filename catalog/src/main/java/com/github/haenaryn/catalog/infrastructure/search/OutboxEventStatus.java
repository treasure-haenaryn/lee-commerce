package com.github.haenaryn.catalog.infrastructure.search;

enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
