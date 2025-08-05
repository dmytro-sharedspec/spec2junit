package dev.spec2test.feature2junit.gherkin.utils;

import com.squareup.javapoet.AnnotationSpec;
import dev.spec2test.common.SourceLine;
import io.cucumber.messages.types.Location;
import lombok.experimental.UtilityClass;

/**
 * Utility class for converting Cucumber Location objects to JUnit annotations.
 */
@UtilityClass
public class LocationUtils {

    /**
     * Converts a Cucumber Location object to a JUnit SourceLine annotation.
     * @param location the Cucumber Location object to convert
     * @return a {@link SourceLine} annotation spec representing the line number of the location
     */
    public static AnnotationSpec toJUnitTagsAnnotation(Location location) {

        AnnotationSpec.Builder annotationSpecBuilder;

        annotationSpecBuilder = AnnotationSpec.builder(SourceLine.class)
                .addMember("value", "$L", location.getLine());

        AnnotationSpec annotationSpec = annotationSpecBuilder.build();
        return annotationSpec;
    }

}
