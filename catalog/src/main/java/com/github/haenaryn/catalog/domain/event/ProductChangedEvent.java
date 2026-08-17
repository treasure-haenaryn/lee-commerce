package com.github.haenaryn.catalog.domain.event;

// 변경 내용을 담지 않고 "이 상품을 다시 읽어서 색인하라"는 재조회 신호로만 쓴다 —
// 소비 측(폴러)이 항상 최신 상태를 다시 읽어 색인하므로 payload가 stale해질 일이 없다.
public record ProductChangedEvent(Long productId) {
}
