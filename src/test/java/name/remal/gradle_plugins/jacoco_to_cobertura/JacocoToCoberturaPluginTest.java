package name.remal.gradle_plugins.jacoco_to_cobertura;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class JacocoToCoberturaPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(JacocoToCoberturaPlugin.class);
    }

    @Test
    void test() {
        assertTrue(project.getPlugins().hasPlugin(JacocoToCoberturaPlugin.class));
    }

}
