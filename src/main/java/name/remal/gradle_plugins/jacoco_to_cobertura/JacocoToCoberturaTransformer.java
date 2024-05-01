package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newOutputStream;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.ResourceUtils.getResourceUrl;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrow;
import static name.remal.gradle_plugins.toolkit.UrlUtils.openInputStreamForUrl;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.compactXmlString;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.parseXml;
import static name.remal.gradle_plugins.toolkit.xml.XmlUtils.prettyXmlString;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.w3c.dom.Document;

@NoArgsConstructor(access = PRIVATE)
abstract class JacocoToCoberturaTransformer {

    @SneakyThrows
    public static void transformJacocoToCobertura(
        Path jacocoReportPath,
        Path coberturaReportPath,
        Collection<String> sources
    ) {
        val jacocoReportDocument = parseXml(jacocoReportPath);
        val transformSource = new DOMSource(jacocoReportDocument, jacocoReportPath.toString());
        val transformResult = new DOMResult();
        TEMPLATES.newTransformer().transform(transformSource, transformResult);


        val document = (Document) transformResult.getNode();
        val documentElement = document.getDocumentElement();

        /*
         * XSLT 1.0 doesn't support sequences as parameters.
         * That's why we need to transform to DOM document and insert sources manually.
         */
        val sourcesNode = document.createElement("sources");
        documentElement.insertBefore(sourcesNode, documentElement.getFirstChild());
        sources.stream()
            .filter(Objects::nonNull)
            .distinct()
            .forEach(source -> {
                val sourceNode = document.createElement("source");
                sourceNode.setTextContent(source);
                sourcesNode.appendChild(sourceNode);
            });

        try (val outputStream = newOutputStream(createParentDirectories(coberturaReportPath))) {
            val documentString = isInTest()
                ? prettyXmlString(document)
                : compactXmlString(document);
            outputStream.write(documentString.getBytes(UTF_8));
        }
    }


    private static final Templates TEMPLATES;

    private static final boolean WITH_CUSTOM_ERROR_LISTENER = true;

    static {
        val transformerFactory = TransformerFactory.newInstance();

        if (WITH_CUSTOM_ERROR_LISTENER) {
            transformerFactory.setErrorListener(new ErrorListener() {
                @Override
                public void fatalError(TransformerException exception) throws TransformerException {
                    throw exception;
                }

                @Override
                public void error(TransformerException exception) throws TransformerException {
                    fatalError(exception);
                }

                @Override
                public void warning(TransformerException exception) throws TransformerException {
                    error(exception);
                }
            });
        }

        transformerFactory.setURIResolver((href, base) -> {
            throw new UnsupportedOperationException();
        });

        tryToSetAttribute(transformerFactory, ACCESS_EXTERNAL_DTD, "");
        tryToSetAttribute(transformerFactory, ACCESS_EXTERNAL_STYLESHEET, "");
        tryToSetAttribute(transformerFactory, FEATURE_SECURE_PROCESSING, true);
        tryToSetAttribute(transformerFactory, "package-name", packageNameOf(JacocoToCoberturaTransformer.class));


        val xsltFileUrl = getResourceUrl("jacoco-to-cobertura.xsl", JacocoToCoberturaTransformer.class);
        try (val inputStream = openInputStreamForUrl(xsltFileUrl)) {
            val source = new StreamSource(inputStream, xsltFileUrl.toString());
            TEMPLATES = transformerFactory.newTemplates(source);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    @SuppressWarnings("java:S2583")
    private static void tryToSetAttribute(TransformerFactory factory, String name, Object value) {
        try {
            factory.setAttribute(name, value);
        } catch (IllegalArgumentException ignored) {
            // do nothing
        }

        if (value instanceof Boolean) {
            try {
                factory.setFeature(name, (Boolean) value);
            } catch (TransformerConfigurationException ignored) {
                // do nothing
            }
        }
    }

}
