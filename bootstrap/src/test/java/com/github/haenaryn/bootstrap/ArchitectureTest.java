package com.github.haenaryn.bootstrap;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// 레이어/모듈 경계를 코드로 강제한다. 아직 각 모듈에 실제 클래스가 없어 규칙의 that() 조건에
// 걸리는 클래스가 0개인데, ArchUnit은 기본적으로 그런 "아무것도 검사 못 한" 규칙을 실패로
// 처리한다(오타로 조건이 잘못돼 있어도 항상 통과해버리는 걸 막기 위한 안전장치). 지금 시점엔
// 그게 오타가 아니라 코드가 아직 없어서인 게 맞으므로 allowEmptyShould(true)로 명시한다 —
// 코드가 들어오면 그때부터 실제로 검사가 시작된다.
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.github.haenaryn";

    private static final List<String> MODULES = List.of(
        "user", "catalog", "inventory", "cart", "order", "payment"
    );

    private final JavaClasses importedClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE_PACKAGE);

    @Test
    void domain_레이어는_infrastructure_레이어에_의존하지_않는다() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void domain_레이어는_interfaces_레이어에_의존하지_않는다() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..interfaces..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void application_레이어는_interfaces_레이어에_의존하지_않는다() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..interfaces..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void domain_레이어는_spring_프레임워크에_의존하지_않는다() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void 다른_모듈에는_facade를_통해서만_접근한다() {
        for (String module : MODULES) {
            String modulePackage = BASE_PACKAGE + "." + module + "..";

            ArchRule rule = classes()
                .that().resideInAPackage(modulePackage)
                .and().haveSimpleNameNotEndingWith("Facade")
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(modulePackage)
                .allowEmptyShould(true);

            rule.check(importedClasses);
        }
    }
}
