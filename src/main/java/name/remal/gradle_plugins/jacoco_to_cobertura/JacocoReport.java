package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.DEFAULT_INT;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Value;
import lombok.val;
import org.w3c.dom.Element;

@Value
@SuppressWarnings("java:S3398")
class JacocoReport {

    List<JacocoSession> sessions;
    List<JacocoPackage> packages;
    List<JacocoCounter> counters;

    public JacocoReport(Element element) {
        this.sessions = parseChildren(element, "sessioninfo", JacocoSession::new);
        this.packages = parseChildren(element, "package", JacocoPackage::new);
        this.counters = parseCounters(element);
    }


    @Value
    public static class JacocoSession {

        Instant startedAt;

        public JacocoSession(Element element) {
            this.startedAt = Instant.ofEpochMilli(tryToParseInt(element.getAttribute("start")));
        }

    }


    @Value
    public static class JacocoPackage {

        String name;
        List<JacocoClass> classes;
        List<JacocoSourceFile> sourceFiles;
        List<JacocoCounter> counters;

        public JacocoPackage(Element element) {
            this.name = element.getAttribute("name");
            this.classes = parseChildren(element, "class", JacocoClass::new);
            this.sourceFiles = parseChildren(element, "sourcefile", JacocoSourceFile::new);
            this.counters = parseCounters(element);
        }

    }


    @Value
    public static class JacocoClass {

        String name;
        String sourceFileName;
        List<JacocoMethod> methods;
        List<JacocoCounter> counters;

        public JacocoClass(Element element) {
            this.name = element.getAttribute("name");
            this.sourceFileName = element.getAttribute("sourcefilename");
            this.methods = parseChildren(element, "method", JacocoMethod::new);
            this.counters = parseCounters(element);
        }

    }


    @Value
    public static class JacocoMethod {

        String name;
        String description;
        int line;
        List<JacocoCounter> counters;

        public JacocoMethod(Element element) {
            this.name = element.getAttribute("name");
            this.description = element.getAttribute("desc");
            this.line = tryToParseInt(element.getAttribute("line"));
            this.counters = parseCounters(element);
        }

    }


    @Value
    public static class JacocoSourceFile {

        String name;
        List<JacocoLine> lines;
        List<JacocoCounter> counters;

        public JacocoSourceFile(Element element) {
            this.name = element.getAttribute("name");
            this.lines = parseChildren(element, "line", JacocoLine::new);
            this.counters = parseCounters(element);
        }

    }


    @Value
    public static class JacocoLine {

        int number;
        int instructionsCovered;
        int instructionsMissed;
        int branchesCovered;
        int branchesMissed;

        public JacocoLine(Element element) {
            this.number = tryToParseInt(element.getAttribute("nr"));
            this.instructionsCovered = tryToParseInt(element.getAttribute("ci"));
            this.instructionsMissed = tryToParseInt(element.getAttribute("mi"));
            this.branchesCovered = tryToParseInt(element.getAttribute("cb"));
            this.branchesMissed = tryToParseInt(element.getAttribute("mb"));
        }

    }


    @Value
    public static class JacocoCounter {

        String type;
        int covered;
        int missed;

        public JacocoCounter(Element element) {
            this.type = element.getAttribute("type");
            this.covered = tryToParseInt(element.getAttribute("covered"));
            this.missed = tryToParseInt(element.getAttribute("missed"));
        }

    }


    //#region: Utils

    private static List<JacocoCounter> parseCounters(Element element) {
        return parseChildren(element, "counter", JacocoCounter::new);
    }

    private static <T> List<T> parseChildren(Element element, String childElementName, Function<Element, T> ctor) {
        return streamChildElements(element, childElementName)
            .map(ctor)
            .collect(toList());
    }

    private static Stream<Element> streamChildElements(Element element, String childElementName) {
        val childNodes = element.getChildNodes();
        return IntStream.range(0, childNodes.getLength())
            .mapToObj(childNodes::item)
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .filter(childElement -> childElementName.equals(childElement.getLocalName()));
    }

    private static int tryToParseInt(String string) {
        try {
            return parseInt(string);

        } catch (NumberFormatException ignored) {
            return DEFAULT_INT;
        }
    }

    //#endregion

}
