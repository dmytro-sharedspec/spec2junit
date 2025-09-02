package dev.spec2test.feature2junit.gherkin;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.spec2test.common.*;
import dev.spec2test.feature2junit.gherkin.utils.JavaDocUtils;
import dev.spec2test.feature2junit.gherkin.utils.LocationUtils;
import dev.spec2test.feature2junit.gherkin.utils.TagUtils;
import io.cucumber.messages.types.Tag;
import io.cucumber.messages.types.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;

@RequiredArgsConstructor
class RuleProcessor implements LoggingSupport, OptionsSupport, BaseTypeSupport {

    @Getter
    private final ProcessingEnvironment processingEnv;

    @Getter
    private final GeneratorOptions options;

    @Getter
    private final TypeElement baseType;

    void processRule(int ruleNumber, Rule rule, TypeSpec.Builder classBuilder) {

        TypeSpec.Builder nestedRuleClassBuilder = TypeSpec
                .classBuilder("Rule_" + ruleNumber)
                .addModifiers(Modifier.PUBLIC);

        String description = rule.getDescription();
        if (StringUtils.isNotBlank(description)) {
            description = JavaDocUtils.trimLeadingAndTrailingWhitespace(description);
            nestedRuleClassBuilder.addJavadoc(description);
        }

        /**
         * add {@link org.junit.jupiter.api.Nested} annotation
         */
        nestedRuleClassBuilder.addAnnotation(
                AnnotationSpec.builder(Nested.class).build()
        );

        /**
         * add {@link Order} annotation
         */
        AnnotationSpec orderAnnotation = AnnotationSpec
                .builder(Order.class)
                .addMember("value", "" + ruleNumber)
                .build();
        nestedRuleClassBuilder.addAnnotation(orderAnnotation);
        /**
         * add {@link TestMethodOrder} annotation
         */
        nestedRuleClassBuilder.addAnnotation(AnnotationSpec
                .builder(TestMethodOrder.class)
                .addMember("value", "$T.class", ClassName.get(MethodOrderer.OrderAnnotation.class))
                .build()
        );

        /**
         * add {@link Tag} annotations
         */
        List<Tag> tags = rule.getTags();
        if (tags != null && !tags.isEmpty()) {
            AnnotationSpec jUnitTagsAnnotation = TagUtils.toJUnitTagsAnnotation(tags);
            nestedRuleClassBuilder.addAnnotation(jUnitTagsAnnotation);
        }

        /**
         * add {@link SourceLine} annotation
         */
        if (options.isAddSourceLineAnnotations()) {
            AnnotationSpec locationAnnotation = LocationUtils.toJUnitTagsAnnotation(rule.getLocation());
            nestedRuleClassBuilder.addAnnotation(locationAnnotation);
        }

        /**
         * add {@link DisplayName} annotation
         */
        String ruleName = rule.getName();
        if (ruleName != null) {
            ruleName = ruleName.replaceAll("\"", "\\\\\"");
        }
        nestedRuleClassBuilder.addAnnotation(
                AnnotationSpec.builder(DisplayName.class)
                        .addMember("value", "\"Rule: " + ruleName + "\"")
                        .build()
        );

        List<RuleChild> children = rule.getChildren();

        int ruleScenarioNumber = 0;

        boolean hasScenarios = false;
        for (RuleChild child : children) {

            if (child.getScenario().isPresent()) {

                Scenario scenario = child.getScenario().get();

                ruleScenarioNumber++;
                ScenarioProcessor scenarioProcessor = new ScenarioProcessor(processingEnv, options, baseType);
                MethodSpec.Builder scenarioMethodBuilder = scenarioProcessor.processScenario(ruleScenarioNumber, scenario, classBuilder);

                MethodSpec scenarioMethod = scenarioMethodBuilder.build();
                nestedRuleClassBuilder.addMethod(scenarioMethod);

                hasScenarios = true;

            } else if (child.getBackground().isPresent()) {

                Background background = child.getBackground().get();

                BackgroundProcessor backgroundProcessor = new BackgroundProcessor(processingEnv, options, baseType);
                MethodSpec.Builder ruleBackgroundMethodBuilder = backgroundProcessor.processRuleBackground(background, classBuilder);

                MethodSpec backgroundMethod = ruleBackgroundMethodBuilder.build();
                nestedRuleClassBuilder.addMethod(backgroundMethod);
            } else {
                throw new ProcessingException("Unsupported rule child type: " + child);
            }
        }

        if (!hasScenarios && options.isFailRulesWithNoScenarios()) {
            /**
             * If there are no scenarios in the rule, we add an empty method that throws exception.
             */
            MethodSpec.Builder noScenariosInRuleMSB = MethodSpec
                    .methodBuilder("noScenariosInRule")
                    .addModifiers(Modifier.PUBLIC);
            noScenariosInRuleMSB.addStatement("$T.fail(\"Rule doesn't have any scenarios\")", Assertions.class);

            AnnotationSpec testAnnotation = AnnotationSpec
                    .builder(Test.class)
                    .build();
            noScenariosInRuleMSB.addAnnotation(testAnnotation);

            String tagForEmptyRules = options.getTagForRulesWithNoScenarios();
            if (StringUtils.isNotBlank(tagForEmptyRules)) {
                /**
                 * add JUnit Tag annotation
                 */
                AnnotationSpec jUnitTagsAnnotation = TagUtils.toJUnitTagsAnnotation(tagForEmptyRules);
                noScenariosInRuleMSB.addAnnotation(jUnitTagsAnnotation);
            }

            MethodSpec noScenariosInRule = noScenariosInRuleMSB.build();
            nestedRuleClassBuilder.addMethod(noScenariosInRule);
        }

        TypeSpec nestedRuleClassSpec = nestedRuleClassBuilder.build();
        classBuilder.addType(nestedRuleClassSpec);
    }

}
