import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "com.github.haenaryn"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    // 이 모듈은 라이브러리(JAR)로만 쓰인다 — 실행 가능한 fat jar는 bootstrap 모듈만 만든다.
    tasks.named<BootJar>("bootJar") {
        enabled = false
    }
    tasks.named<Jar>("jar") {
        enabled = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito:mockito-junit-jupiter")
    }
}

project(":bootstrap") {
    tasks.named<BootJar>("bootJar") {
        enabled = true
    }
    tasks.named<Jar>("jar") {
        enabled = false
    }
}
