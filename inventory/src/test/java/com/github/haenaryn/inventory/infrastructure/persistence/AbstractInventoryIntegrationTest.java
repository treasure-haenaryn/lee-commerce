package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.InventoryTestApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(classes = InventoryTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class AbstractInventoryIntegrationTest {

    // Singleton Container 패턴 — 여러 통합 테스트 클래스가 이 컨테이너를 공유한다.
    // @Testcontainers/@Container(클래스별 생명주기 관리)를 쓰면 첫 번째 테스트 클래스가
    // 끝날 때 공유 static 필드인 이 컨테이너까지 함께 stop되어 이후 클래스들이 죽은
    // 컨테이너에 연결을 시도하게 된다. 그래서 여기서는 직접 start()만 하고 stop()은
    // 호출하지 않는다 — JVM 종료 시 Testcontainers의 Ryuk reaper가 정리한다.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18"))
        .withCopyFileToContainer(
            MountableFile.forHostPath("../db/schema.sql"), "/docker-entrypoint-initdb.d/01-schema.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
