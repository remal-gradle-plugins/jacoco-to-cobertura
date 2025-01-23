package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.lang.String.join;
import static java.math.RoundingMode.HALF_UP;
import static java.util.stream.Collectors.joining;
import static name.remal.gradle_plugins.toolkit.testkit.TestClasspath.getTestClasspathLibraryFilePaths;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.parseXml;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
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

            build.applyPlugin("java");
            build.applyPlugin("jacoco");
            build.block("tasks.test", test -> {
                test.line("finalizedBy('jacocoTestReport')");
                test.line("useJUnitPlatform()");
            });

            build.block("tasks.jacocoTestReport", taskBlock -> {
                taskBlock.line("dependsOn('test')");
                taskBlock.line("reports.xml.outputLocation = file('build/jacoco.xml')");
            });
            coberturaReportPath = project.resolveRelativePath("build/cobertura-jacoco.xml");

            build.addMavenCentralRepository();
            build.block("dependencies", deps -> {
                deps.line(
                    "testImplementation files(%s)",
                    getTestClasspathLibraryFilePaths("org.junit.jupiter:junit-jupiter-api").stream()
                        .map(Path::toString)
                        .map(path -> "'" + deps.escapeString(path) + "'")
                        .collect(joining(", "))
                );

                deps.line(
                    "testRuntimeOnly files(%s)",
                    getTestClasspathLibraryFilePaths("org.junit.jupiter:junit-jupiter-engine").stream()
                        .map(Path::toString)
                        .map(path -> "'" + deps.escapeString(path) + "'")
                        .collect(joining(", "))
                );
                deps.line(
                    "testRuntimeOnly files(%s)",
                    getTestClasspathLibraryFilePaths("org.junit.platform:junit-platform-launcher").stream()
                        .map(Path::toString)
                        .map(path -> "'" + deps.escapeString(path) + "'")
                        .collect(joining(", "))
                );
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
        project.assertBuildSuccessfully("jacocoTestReportToCobertura");

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

        project.assertBuildSuccessfully("jacocoTestReportToCobertura");

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

        project.assertBuildSuccessfully("jacocoTestReportToCobertura");

        assertThat(coberturaReportPath).exists();
        assertThat(readTotalCoverage()).isEqualTo("0.67");
    }

    private BigDecimal readTotalCoverage() {
        var document = parseXml(coberturaReportPath);
        var coverage = document.getDocumentElement().getAttribute("line-rate");
        return new BigDecimal(coverage).setScale(2, HALF_UP);
    }

}
