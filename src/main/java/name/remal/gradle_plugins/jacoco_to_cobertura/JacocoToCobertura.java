package name.remal.gradle_plugins.jacoco_to_cobertura;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.jacoco_to_cobertura.JacocoToCoberturaTransformer.transformJacocoToCobertura;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

public abstract class JacocoToCobertura extends DefaultTask {

    @SkipWhenEmpty
    @org.gradle.api.tasks.Optional
    @InputFile
    public abstract RegularFileProperty getJacocoReport();

    {
        onlyIf(__ -> {
            val jacocoReportFile = getJacocoReport()
                .getLocationOnly()
                .map(RegularFile::getAsFile)
                .getOrNull();
            return jacocoReportFile != null && jacocoReportFile.exists();
        });
    }

    @org.gradle.api.tasks.Optional
    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirectories();

    @OutputFile
    public abstract RegularFileProperty getCoberturaReport();

    {
        getCoberturaReport().convention(getProviders().provider(() -> {
            val jacocoReportFile = getJacocoReport()
                .getLocationOnly()
                .map(RegularFile::getAsFile)
                .getOrNull();
            if (jacocoReportFile == null) {
                return null;
            }

            final File resultFile;
            val parentFile = jacocoReportFile.getParentFile();
            val fileName = jacocoReportFile.getName();
            if (parentFile != null) {
                resultFile = new File(parentFile, "cobertura-" + fileName);
            } else {
                resultFile = new File("cobertura-" + fileName);
            }
            return getLayout().getBuildDirectory().get().file(resultFile.getAbsolutePath());
        }));
    }


    @TaskAction
    @SneakyThrows
    public void execute() {
        val coberturaReportPath = getCoberturaReport()
            .getLocationOnly()
            .map(RegularFile::getAsFile)
            .map(File::toPath)
            .map(PathUtils::normalizePath)
            .get();
        deleteIfExists(coberturaReportPath);

        val jacocoReportPath = getJacocoReport()
            .getLocationOnly()
            .map(RegularFile::getAsFile)
            .map(File::toPath)
            .map(PathUtils::normalizePath)
            .get();
        if (!exists(jacocoReportPath)) {
            return;
        }

        transformJacocoToCobertura(
            jacocoReportPath,
            coberturaReportPath,
            getSourceDirectories().getFiles().stream()
                .map(File::getAbsolutePath)
                .collect(toList())
        );
    }


    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ProjectLayout getLayout();

}
