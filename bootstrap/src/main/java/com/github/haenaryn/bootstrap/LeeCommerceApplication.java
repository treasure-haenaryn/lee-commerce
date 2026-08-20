package com.github.haenaryn.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableJpaAuditing이 없으면 Entity의 @EntityListeners(AuditingEntityListener.class) +
// @CreatedDate/@LastModifiedDate가 있어도 실제로 값을 채우는 리스너가 등록되지 않는다 —
// created_at/updated_at이 null인 채로 INSERT가 나가 NOT NULL 제약을 위반하게 된다.
@EnableJpaAuditing
// @ConfigurationProperties 애너테이션이 붙은 레코드/클래스는 이걸로 스캔해줘야 Bean으로
// 등록된다 — 그냥 두면 존재하는지도 모르고 무시된다. basePackages를 명시해야 한다 —
// 인자 없이 쓰면 @SpringBootApplication의 scanBasePackages를 물려받지 않고 이
// 애너테이션이 붙은 클래스 자신의 패키지(com.github.haenaryn.bootstrap)에서만
// 스캔한다. 그러면 다른 모듈 패키지(예: user.infrastructure.auth.JwtProperties,
// catalog.interfaces.ApiKeyProperties)의 @ConfigurationProperties는 Bean으로
// 등록되지 않아 그 값을 주입받는 컴포넌트의 기동이 실패한다.
@ConfigurationPropertiesScan(basePackages = "com.github.haenaryn")
// Outbox 폴러(@Scheduled) 같은 주기 작업이 이 애너테이션 없이는 등록만 되고 실행되지 않는다.
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.github.haenaryn")
public class LeeCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeeCommerceApplication.class, args);
    }
}
