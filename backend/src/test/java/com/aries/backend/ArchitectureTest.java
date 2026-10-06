package com.aries.backend;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import com.aries.backend.shared.interfaces.rest.Result;
import org.springframework.web.bind.annotation.RestController;

/** 把模块边界写成构建时可执行的规则。 */
class ArchitectureTest {
    private final JavaClasses code = new ClassFileImporter().importPackages("com.aries.backend");

    @Test
    void businessModulesDoNotDependOnEachOther() {
        noClasses().that().resideInAPackage("..catalog..")
                .should().dependOnClassesThat().resideInAPackage("..identity..")
                .check(code);
        noClasses().that().resideInAPackage("..identity..")
                .should().dependOnClassesThat().resideInAPackage("..catalog..")
                .check(code);
        noClasses().that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAnyPackage("..catalog..", "..identity..", "..discussion..", "..storage..")
                .check(code);
        noClasses().that().resideInAPackage("..discussion..")
                .should().dependOnClassesThat().resideInAnyPackage("..catalog..", "..identity..")
                .check(code);
        noClasses().that().resideInAnyPackage("..catalog..", "..identity..")
                .should().dependOnClassesThat().resideInAPackage("..discussion..")
                .check(code);
    }

    @Test
    void storageIsIndependentAndSdkStaysInInfrastructure() {
        noClasses().that().haveSimpleName("FileStorageService")
                .should().dependOnClassesThat().haveSimpleName("StorageIdentityProvider")
                .because("the storage tool must not decide the acting user").check(code);
        noClasses().that().resideInAPackage("..storage..")
                .should().dependOnClassesThat().resideInAnyPackage("..catalog..", "..identity..", "..discussion..")
                .check(code);
        noClasses().that().resideInAnyPackage("..identity..", "..catalog..", "..discussion..")
                .should().dependOnClassesThat().resideInAPackage("..storage..")
                .check(code);
        noClasses().that().resideOutsideOfPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("software.amazon.awssdk..")
                .check(code);
    }

    @Test
    void domainHasNoFrameworkOrOuterLayerDependencies() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "com.baomidou..", "org.apache.ibatis..",
                        "jakarta.servlet..", "..application..", "..infrastructure..", "..interfaces..")
                .check(code);
    }

    @Test
    void applicationUsesPortsInsteadOfInfrastructure() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..", "..interfaces..", "com.baomidou..",
                        "org.apache.ibatis..", "cn.dev33.satoken..")
                .check(code);
    }

    @Test
    void persistenceObjectsStayInsideInfrastructure() {
        noClasses().that().resideOutsideOfPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..po..")
                .check(code);
    }

    @Test
    void databaseAccessGoesThroughMybatisPlus() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.jdbc..", "java.sql..")
                .because("database access must go through MyBatis-Plus mappers")
                .check(code);
    }
    @Test
    void jsonControllersReturnResultDirectly() {
        methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .should().haveRawReturnType(Result.class)
                .check(code);
        noClasses().that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.http.ResponseEntity")
                .check(code);
    }

}
