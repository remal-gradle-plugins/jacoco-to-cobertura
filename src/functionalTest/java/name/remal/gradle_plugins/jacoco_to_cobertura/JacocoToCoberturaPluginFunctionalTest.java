package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.lang.String.join;
import static java.math.RoundingMode.HALF_UP;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.parseXml;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class JacocoToCoberturaPluginFunctionalTest {

    private final GradleProject project;

    Path coberturaReportPath;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.jacoco-to-cobertura");
            build.registerDefaultTask("allJacocoReportToCobertura");

            build.applyPlugin("java");
            build.applyPlugin("jacoco");
            build.appendBlock("tasks.test", test -> {
                test.append("finalizedBy('jacocoTestReport')");
                test.append("useJUnitPlatform()");
            });

            build.append("tasks.jacocoTestReport.dependsOn('test')");
            build.append("tasks.jacocoTestReport.reports.xml.outputLocation = file('build/jacoco.xml')");
            coberturaReportPath = project.resolveRelativePath("build/cobertura-jacoco.xml");

            build.addMavenCentralRepository();
            build.appendBlock("dependencies", deps -> {
                // TODO: replace with dynamic version
                deps.append("testImplementation platform('org.junit:junit-bom:5.9.3')");
                deps.append("testImplementation 'org.junit.jupiter:junit-jupiter-api'");
                deps.append("testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine'");
                deps.append("testRuntimeOnly 'org.junit.platform:junit-platform-launcher'");
            });
        });

        project.writeTextFile("src/main/java/pkg/Clazz.java", join(
            "\n",
            "",
            "package pkg;",
            "",
            "class Clazz {",
            "",
            "    public static void method() {",
            "        EXECUTED = true;",
            "    }",
            "",
            "    private static boolean EXECUTED;",
            "",
            "}",
            ""
        ));
    }


    @Test
    void emptyBuildDoesNotFail() {
        project.assertBuildSuccessfully();

        assertThat(coberturaReportPath).doesNotExist();
    }

    @Test
    void withoutCoverage() {
        project.writeTextFile("src/test/java/pkg/ClazzTest.java", join(
            "\n",
            "",
            "package pkg;",
            "",
            "import org.junit.jupiter.api.Test;",
            "",
            "class ClazzTest {",
            "",
            "    @Test",
            "    void method() {",
            "        // test nothing",
            "    }",
            "",
            "}",
            ""
        ));

        project.assertBuildSuccessfully();

        assertThat(coberturaReportPath).exists();
        assertThat(readTotalCoverage()).isEqualTo("0.00");
    }

    @Test
    void withCoverage() {
        project.writeTextFile("src/test/java/pkg/ClazzTest.java", join(
            "\n",
            "",
            "package pkg;",
            "",
            "import org.junit.jupiter.api.Test;",
            "",
            "class ClazzTest {",
            "",
            "    @Test",
            "    void method() {",
            "        Clazz.method();",
            "    }",
            "",
            "}",
            ""
        ));

        project.assertBuildSuccessfully();

        assertThat(coberturaReportPath).exists();
        assertThat(readTotalCoverage()).isEqualTo("0.67");
    }

    private BigDecimal readTotalCoverage() {
        val document = parseXml(coberturaReportPath);
        val coverage = document.getDocumentElement().getAttribute("line-rate");
        return new BigDecimal(coverage).setScale(2, HALF_UP);
    }

}
