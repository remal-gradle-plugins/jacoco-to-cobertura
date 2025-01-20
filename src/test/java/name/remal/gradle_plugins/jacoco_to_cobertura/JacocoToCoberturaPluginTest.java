package name.remal.gradle_plugins.jacoco_to_cobertura;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
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
    void pluginTasksDoNotHavePropertyProblems() {
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("jacoco");

        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(JacocoToCoberturaPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                var taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}
