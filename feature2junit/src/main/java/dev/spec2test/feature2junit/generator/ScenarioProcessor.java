package dev.spec2test.feature2junit.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.spec2test.feature2junit.generator.naming.ParameterNamingUtils;
import dev.spec2test.feature2junit.generator.tables.TableUtils;
import io.cucumber.messages.types.DataTable;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.lang.model.element.Modifier;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ScenarioProcessor {

    static MethodSpec.Builder processScenario(int scenarioNumber, Scenario scenario, TypeSpec.Builder classBuilder) {

        List<MethodSpec> allMethodSpecs = classBuilder.methodSpecs;

        List<Step> scenarioSteps = scenario.getSteps();
        List<MethodSpec> scenarioStepsMethodSpecs = new ArrayList<>(scenarioSteps.size());

        String scenarioMethodName = "scenario_" + scenarioNumber;
        MethodSpec.Builder scenarioMethodBuilder = MethodSpec
                .methodBuilder(scenarioMethodName)
//                .addParameter(TestInfo.class, "testInfo")
                .addModifiers(Modifier.PUBLIC);

        AnnotationSpec orderAnnotation = AnnotationSpec
                .builder(Order.class)
                .addMember("value", "" + scenarioNumber)
                .build();
        scenarioMethodBuilder.addAnnotation(orderAnnotation);

        List<Examples> examples = scenario.getExamples();
        List<String> scenarioParameterNames;
        List<String> testMethodParameterNames;

        if (examples != null && !examples.isEmpty()) {

            addDisplayNameAnnotation(scenarioMethodBuilder, scenario);

            scenarioMethodBuilder.addComment("This scenario has examples: " + examples);
            scenarioParameterNames = addJUnitAnnotationsForParameterizedTest(scenarioMethodBuilder, scenario);
            testMethodParameterNames = new ArrayList<>(scenarioParameterNames.size());

            for (String scenarioParameterName : scenarioParameterNames) {
                String methodParameterName = ParameterNamingUtils.toMethodParameterName(scenarioParameterName);
                testMethodParameterNames.add(methodParameterName);
                scenarioMethodBuilder.addParameter(String.class, methodParameterName);
            }
        }
        else {
            addJUnitAnnotationsForSingleTest(scenarioMethodBuilder, scenario);
            scenarioParameterNames = null;
            testMethodParameterNames = null;

            addDisplayNameAnnotation(scenarioMethodBuilder, scenario);
        }

        for (Step scenarioStep : scenarioSteps) {

            List<MethodSpec> methodSpecs = classBuilder.methodSpecs;
            String javaDoc = methodSpecs.isEmpty() ? "First method javadoc comment" : null;

            MethodSpec stepMethodSpec = StepProcessor.processStep(
                    scenarioStep, scenarioMethodBuilder, scenarioStepsMethodSpecs,
                    scenarioParameterNames, testMethodParameterNames, javaDoc
            );
            scenarioStepsMethodSpecs.add(stepMethodSpec);

            String stepMethodName = stepMethodSpec.name;
            MethodSpec existingMethodSpec =
                    allMethodSpecs.stream().filter(methodSpec -> methodSpec.name.equals(stepMethodName))
                            .findFirst()
                            .orElse(null);

            if (existingMethodSpec == null) {
                // If the method already exists, we can skip creating it again
                classBuilder.addMethod(stepMethodSpec);
            }
        }

        return scenarioMethodBuilder;
    }

    private static void addDisplayNameAnnotation(MethodSpec.Builder scenarioMethodBuilder, Scenario scenario) {

        AnnotationSpec displayNameAnnotation = AnnotationSpec
                .builder(DisplayName.class)
                .addMember("value", "\"Scenario: " + scenario.getName() + "\"")
                .build();
        scenarioMethodBuilder.addAnnotation(displayNameAnnotation);
    }

    private static void addOrderAnnotation(MethodSpec.Builder scenarioMethodBuilder, Scenario scenario) {

        AnnotationSpec displayNameAnnotation = AnnotationSpec
                .builder(DisplayName.class)
                .addMember("value", "\"Scenario: " + scenario.getName() + "\"")
                .build();
        scenarioMethodBuilder.addAnnotation(displayNameAnnotation);
    }

    private static void addJUnitAnnotationsForSingleTest(MethodSpec.Builder scenarioMethodBuilder, Scenario scenario) {

        AnnotationSpec testAnnotation = AnnotationSpec
                .builder(Test.class)
                .build();
        scenarioMethodBuilder.addAnnotation(testAnnotation);
    }

    private static List<String> addJUnitAnnotationsForParameterizedTest(
            MethodSpec.Builder scenarioMethodBuilder,
            Scenario scenario) {

        List<Examples> examples = scenario.getExamples();
        if (examples.size() > 1) {
            throw new IllegalArgumentException(
                    "Having more than 1 Examples section for a Scenario Outline is not currently supported, total examples "
                            + examples.size());
        }

        Examples examplesTable = examples.get(0);

        /**
         * convert Examples into data table so that we can format it easily with pipe characters
         */
        TableRow tableHeader = examplesTable.getTableHeader().get();
        List<TableRow> tableBody = examplesTable.getTableBody();
        List<TableRow> allRows = new ArrayList<>(tableBody.size() + 1);
        allRows.add(tableHeader);
        allRows.addAll(tableBody);

        Location examplesTableLocation = examplesTable.getLocation();
        DataTable examplesDataTable = new DataTable(examplesTableLocation, allRows);

        List<Integer> maxColumnLengths = TableUtils.workOutMaxColumnLength(examplesDataTable);

        StringBuilder textBlockSB = new StringBuilder();
        textBlockSB.append("\"\"\"\n");

//        String dataTableAsString = TableUtils.convertDataTableToString(examplesDataTable, maxColumnLengths);
//        textBlockSB.append(dataTableAsString);

        for (TableRow row : allRows) {

            List<TableCell> rowCells = row.getCells();
            List<String> paddedCellValues = new ArrayList<>(rowCells.size());

            for (int i = 0; i < rowCells.size(); i++) {
                TableCell cell = rowCells.get(i);
                String value = cell.getValue();
                int maxColumnLength = maxColumnLengths.get(i);
                String paddedValue = StringUtils.rightPad(value, maxColumnLength);
                paddedCellValues.add(paddedValue);
            }

            String rowLine = String.join(" | ", paddedCellValues);
            textBlockSB.append(rowLine + "\n");
        }

        textBlockSB.append("\"\"\"");

        String textBlock = textBlockSB.toString();

        AnnotationSpec parameterizedTestAnnotation = AnnotationSpec
                .builder(ParameterizedTest.class)
//                .addMember("name", "\"Scenario: " + scenario.getName() + " [{arguments}]\"")
                .addMember("name", "\"Example: [{arguments}]\"")
                .build();
        scenarioMethodBuilder
                .addAnnotation(parameterizedTestAnnotation);

        AnnotationSpec csvSourceAnnotation = AnnotationSpec
                .builder(CsvSource.class)
                .addMember("useHeadersInDisplayName", "true")
//                .addMember("delimiterString", "\"|\"")
//                delimiter = '|',
                .addMember("delimiter", "'|'")
                .addMember("textBlock", textBlock)
                .build();
        scenarioMethodBuilder
                .addAnnotation(csvSourceAnnotation);

        List<String> headerCells = examplesTable.getTableHeader().get().getCells().stream().map(TableCell::getValue).toList();
        return headerCells;
    }

}
