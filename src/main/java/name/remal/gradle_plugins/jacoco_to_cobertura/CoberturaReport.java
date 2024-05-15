package name.remal.gradle_plugins.jacoco_to_cobertura;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
class CoberturaReport {

    @NonNull
    Instant timestamp;

    @Singular
    List<String> sources;

    @Singular
    List<CoberturaPackage> packages;

    double lineRate;
    double branchRate;
    double complexity;


    @Value
    @Builder
    public static class CoberturaPackage {

        @NonNull
        String name;

        double lineRate;
        double branchRate;
        double complexity;

    }


    @Value
    @Builder
    public static class CoberturaClass {

        @NonNull
        String name;

        @NonNull
        String fileName;

        double lineRate;
        double branchRate;
        double complexity;

    }

}
