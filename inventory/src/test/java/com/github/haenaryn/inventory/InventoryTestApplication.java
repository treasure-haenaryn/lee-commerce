package com.github.haenaryn.inventory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @EnableJpaAuditing이 없으면 StockReservation의 @CreatedDate/@LastModifiedDate가
// 채워지지 않아 NOT NULL 컬럼 제약 위반으로 저장이 실패한다(bootstrap의 선례와 동일한 이유).
@SpringBootApplication
@EnableJpaAuditing
public class InventoryTestApplication {
}
