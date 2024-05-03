package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.lang.String.format;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtensions;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public class JacocoToCoberturaPlugin implements Plugin<Project> {

    public static final String JACOCO_TO_COBERTURA_TASK_EXTENSION_NAME = doNotInline("jacocoToCoberturaTask");

    @Override
    public void apply(Project project) {
        val tasks = project.getTasks();
        tasks.withType(JacocoReport.class).all(JacocoToCoberturaPlugin::configureJacocoTask);
    }

    private static void configureJacocoTask(JacocoReport jacocoTask) {
        enableJacocoXmlReport(jacocoTask);
        doBeforeTaskExecution(jacocoTask, JacocoToCoberturaPlugin::enableJacocoXmlReport);

        val coberturaTaskProvider = jacocoTask.getProject().getTasks().register(
            jacocoTask.getName() + "ToCobertura",
            JacocoToCobertura.class,
            coberturaTask -> {
                coberturaTask.setDescription(format(
                    "Convert XML report of `%s` task in Cobertura format",
                    jacocoTask.getName()
                ));

                coberturaTask.dependsOn(jacocoTask);

                // avoid unnecessary dependency to XML report location:
                coberturaTask.getJacocoReport().set(coberturaTask.getProject().provider(() ->
                    jacocoTask.getReports().getXml().getOutputLocation().getOrNull()
                ));

                coberturaTask.getSourceDirectories().from(jacocoTask.getSourceDirectories());
                coberturaTask.getSourceDirectories().from(jacocoTask.getAdditionalSourceDirs());
            }
        );

        getExtensions(jacocoTask).add(
            new TypeOf<TaskProvider<JacocoToCobertura>>() { },
            JACOCO_TO_COBERTURA_TASK_EXTENSION_NAME,
            coberturaTaskProvider
        );
    }

    private static void enableJacocoXmlReport(JacocoReport jacocoTask) {
        jacocoTask.getReports().getXml().getRequired().set(true);
    }

}
