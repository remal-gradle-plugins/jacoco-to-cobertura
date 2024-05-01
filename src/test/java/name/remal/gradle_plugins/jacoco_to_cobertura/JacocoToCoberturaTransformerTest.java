package name.remal.gradle_plugins.jacoco_to_cobertura;

import static com.google.common.jimfs.Configuration.unix;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.util.stream.Collectors.joining;
import static name.remal.gradle_plugins.jacoco_to_cobertura.JacocoToCoberturaTransformer.transformJacocoToCobertura;
import static name.remal.gradle_plugins.toolkit.ResourceUtils.readResource;
import static name.remal.gradle_plugins.toolkit.xml.DomUtils.traverseNodeDescendants;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.parseXml;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.prettyXmlString;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import io.github.classgraph.ClassGraph;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedGradleVersion;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedJavaVersion;
import net.razvan.Cobertura;
import net.razvan.Jacoco;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlunit.builder.DiffBuilder;

@MinSupportedJavaVersion(11)
@MinSupportedGradleVersion("6.8")
class JacocoToCoberturaTransformerTest {

    private static final List<String> sources = ImmutableList.of("/src/main/java");

    final FileSystem fs = Jimfs.newFileSystem(unix());

    @AfterEach
    void afterEach() throws Throwable {
        fs.close();
    }


    @ParameterizedTest
    @MethodSource("scenarioJacocoReportResources")
    @SuppressWarnings("LanguageMismatch")
    void scenario(String resourceName) throws Throwable {
        val jacocoReportBytes = readResource(resourceName, JacocoToCoberturaTransformerTest.class);
        val jacocoReportPath = fs.getPath("/jacoco");
        write(jacocoReportPath, jacocoReportBytes);

        val coberturaReportPath = fs.getPath("/cobertura");
        transformJacocoToCobertura(
            jacocoReportPath,
            coberturaReportPath,
            sources
        );

        val coberturaReportPathByPlugin = transformWithJacocoToCoberturaPlugin(jacocoReportPath, sources);

        val coberturaReportByPlugin = new String(readAllBytes(coberturaReportPathByPlugin), UTF_8);
        val coberturaReport = new String(readAllBytes(coberturaReportPath), UTF_8);
        assertXmlSimilar(coberturaReportByPlugin, coberturaReport);
    }

    static List<String> scenarioJacocoReportResources() {
        val classGraph = new ClassGraph()
            .overrideClassLoaders(JacocoToCoberturaTransformerTest.class.getClassLoader())
            .ignoreParentClassLoaders()
            .acceptPathsNonRecursive("");
        try (val scanResult = classGraph.scan()) {
            return scanResult.getResourcesMatchingWildcard("scenario-jacoco-*.xml").getPaths();
        }
    }


    private static void assertXmlSimilar(Object expected, Object actual) {
        val diff = DiffBuilder.compare(expected).withTest(actual)
            .ignoreComments()
            .ignoreWhitespace()
            .checkForSimilar()
            .build();
        if (diff.hasDifferences()) {
            fail(format(
                "Expected XML:\n%s\n\nActual XML:\n%s\n\nDifferences:\n%s",
                expected,
                actual,
                StreamSupport.stream(diff.getDifferences().spliterator(), false)
                    .map(Object::toString)
                    .collect(joining("\n  ", "  ", ""))
            ));
        }
    }

    @SneakyThrows
    private Path transformWithJacocoToCoberturaPlugin(
        Path jacocoReportPath,
        Collection<String> sources
    ) {
        final Jacoco.Report jacocoReport;
        try (val inputStream = newInputStream(jacocoReportPath)) {
            jacocoReport = new Persister().read(Jacoco.Report.class, inputStream);
        }

        val coberturaReport = new Cobertura.Coverage(jacocoReport, sources);

        final Document document;
        try (val writer = new StringWriter()) {
            val format = new Format("<?xml version=\"1.0\" encoding= \"UTF-8\"?>");
            new Persister(format).write(coberturaReport, writer);
            document = parseXml(writer.toString());
        }

        traverseNodeDescendants(document, Element.class, element -> {
            val attrs = element.getAttributes();
            for (int attrIndex = 0; attrIndex < attrs.getLength(); ++attrIndex) {
                val attr = attrs.item(attrIndex);
                try {
                    BigDecimal number = new BigDecimal(attr.getNodeValue());
                    number = number.stripTrailingZeros();
                    attr.setNodeValue(number.toPlainString());
                } catch (NumberFormatException ignored) {
                    // do nothing
                }
            }
        });

        val resultPath = fs.getPath("/cobertura-by-plugin");
        write(resultPath, prettyXmlString(document).getBytes(UTF_8));

        return resultPath;
    }

}
