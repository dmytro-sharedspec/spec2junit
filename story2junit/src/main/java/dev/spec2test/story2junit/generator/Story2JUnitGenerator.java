package dev.spec2test.story2junit.generator;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import dev.spec2test.story2junit.Story2JUnit;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * APT processor that generates JUnit test subclasses based on the Story2JUnit annotation.
 */
@SupportedAnnotationTypes("dev.spec2test.story2junit.Story2JUnit")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class Story2JUnitGenerator extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        message("Running " + this.getClass().getSimpleName() + " processor");

        for (TypeElement annotation : annotations) {

            String annotationName = annotation.getQualifiedName().toString();
            if (!annotationName.equals(Story2JUnit.class.getName())) {
                /**
                 * not our target annotation
                 */
                continue;
            }

            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element annotatedElement : annotatedElements) {

                message("annotatedElement.simpleName = " + annotatedElement.getSimpleName());

                Story2JUnit targetAnnotation = annotatedElement.getAnnotation(Story2JUnit.class);
                if (targetAnnotation == null) {
                    continue; // shouldn't really happen
                }

                TypeElement annotatedClass = (TypeElement) annotatedElement;

                TestSubclassGenerator subclassGenerator = new TestSubclassGenerator(processingEnv, processingEnv);

//                Set<? extends Element> rootElements = roundEnv.getRootElements();
//                Map<String, String> options = processingEnv.getOptions();

                JavaFile javaFile;
                try {
                    javaFile = subclassGenerator.createTestSubclass(annotatedElement, targetAnnotation);
                } catch (IOException e) {
                    throw new RuntimeException("An error occurred while processing annotated element - " + annotatedClass.getSimpleName(), e);
                }

                final String suffixForGeneratedClass = "Scenarios";
                Filer filer = processingEnv.getFiler();
                String subclassFullyQualifiedName = annotatedClass.getQualifiedName() + suffixForGeneratedClass;

                JavaFileObject subclassFile = null;
                try {
                    subclassFile = filer.createSourceFile(subclassFullyQualifiedName);
                } catch (IOException e) {
                    throw new RuntimeException("An error occurred while attempting to create subclass file with a name - '" + subclassFullyQualifiedName + "'", e);
                }

                try (PrintWriter out = new PrintWriter(subclassFile.openWriter())) {
                    javaFile.writeTo(out);
                } catch (IOException e) {
                    throw new RuntimeException("An error occurred while attempting to write Java file named - '" + subclassFile.getName() + "'", e);
                }

            }
        }

        return true;
    }

    private void message(String message) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, message);
    }

    private void messageError(String message) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }
}