package name.remal.gradle_plugins.jacoco_to_cobertura;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class JacocoToCoberturaPluginAppliedViaSettingsFunctionalTest {

    private final GradleProject project;

    @Test
    void appliedViaSettingsIsAppliedToProject() {
        project.forSettingsFile(settings -> settings.applyPlugin("name.remal.jacoco-to-cobertura"));

        // The plugin must NOT be applied via the project's build file: it should reach the project
        // solely through the Settings-level application propagating via GradleLifecycle.beforeProject.
        //
        // The hasPlugin() check is evaluated at configuration time and captured into a local
        // variable, because accessing `project` from inside `doLast` (i.e. at execution time)
        // is unsupported with the configuration cache / Isolated Projects.
        project.getBuildFile().line(
            "def isPluginApplied = project.pluginManager.hasPlugin('name.remal.jacoco-to-cobertura')"
        );
        project.getBuildFile().line(
            "tasks.register('assertPluginApplied') { doLast { assert isPluginApplied } }"
        );

        project.assertBuildSuccessfully("assertPluginApplied");
    }

}
