import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    JavaClasses importedClasses = new ClassFileImporter().importPackages("com.sytk..");

    @Test
    void 스캔된_클래스_확인() {
        System.out.println("스캔된 클래스 개수: " + importedClasses.size());
    }

    @Test
    void booking은_독립적이어야_한다() {
        noClasses().that().resideInAPackage("..booking..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..waiting..", "..read..", "..payment..")
                .check(importedClasses);
    }

    @Test
    void waiting은_독립적이어야_한다() {
        noClasses().that().resideInAPackage("..waiting..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..booking..", "..read..", "..payment..")
                .check(importedClasses);
    }

    @Test
    void read와_payment는_booking만_의존한다() {
        classes().that().resideInAnyPackage("..read..", "..payment..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..read..",
                        "..payment..",
                        "..booking..",
                        "java..",
                        "org.springframework.."
                ).check(importedClasses);
    }
}
