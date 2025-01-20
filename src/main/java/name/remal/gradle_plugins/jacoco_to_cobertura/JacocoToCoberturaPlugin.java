package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.lang.String.format;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtensions;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public class JacocoToCoberturaPlugin implements Plugin<Project> {

    public static final String JACOCO_TO_COBERTURA_TASK_EXTENSION_NAME = doNotInline("jacocoToCoberturaTask");

    @Override
    public void apply(Project project) {
        var tasks = project.getTasks();
        tasks.withType(JacocoReport.class).all(JacocoToCoberturaPlugin::configureJacocoTask);
    }

    private static void configureJacocoTask(JacocoReport jacocoTask) {
        enableJacocoXmlReport(jacocoTask);
        doBeforeTaskExecution(jacocoTask, JacocoToCoberturaPlugin::enableJacocoXmlReport);

        var coberturaTaskProvider = jacocoTask.getProject().getTasks().register(
            jacocoTask.getName() + "ToCobertura",
            JacocoToCobertura.class,
            coberturaTask -> {
                coberturaTask.setDescription(format(
                    "Convert XML report of `%s` task in Cobertura format",
                    jacocoTask.getName()
                ));

                coberturaTask.dependsOn(jacocoTask);

                coberturaTask.getJacocoReport().set(
                    jacocoTask.getReports().getXml().getOutputLocation().getLocationOnly()
                );

                coberturaTask.getSourceDirectories().from(jacocoTask.getSourceDirectories());
                coberturaTask.getSourceDirectories().from(jacocoTask.getAdditionalSourceDirs());
            }
        );

        jacocoTask.finalizedBy(coberturaTaskProvider);

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
