package com.github.haenaryn.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @EnableJpaAuditing이 없으면 Entity의 @EntityListeners(AuditingEntityListener.class) +
// @CreatedDate/@LastModifiedDate가 있어도 실제로 값을 채우는 리스너가 등록되지 않는다 —
// created_at/updated_at이 null인 채로 INSERT가 나가 NOT NULL 제약을 위반하게 된다.
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.github.haenaryn")
public class LeeCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeeCommerceApplication.class, args);
    }
}
