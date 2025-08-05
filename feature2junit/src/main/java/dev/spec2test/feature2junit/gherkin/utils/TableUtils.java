package dev.spec2test.feature2junit.gherkin.utils;

import com.squareup.javapoet.MethodSpec;
import io.cucumber.messages.types.DataTable;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;
import io.cucumber.datatable.DataTable.TableConverter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import org.apache.commons.lang3.StringUtils;

public class TableUtils {

    public static List<Integer> workOutMaxColumnLength(DataTable dataTableMsg) {

        List<TableRow> rows = dataTableMsg.getRows();
        List<Integer> maxColumnLength = new ArrayList<>();

        for (TableRow row : rows) {
            List<TableCell> cells = row.getCells();
            for (int i = 0; i < cells.size(); i++) {

                TableCell cellValue = cells.get(i);
                String cellText = cellValue.getValue();

                if (maxColumnLength.size() <= i) {
                    maxColumnLength.add(cellText.length());
                }
                else {
                    int currentMaxLength = maxColumnLength.get(i);
                    if (cellText.length() > currentMaxLength) {
                        maxColumnLength.set(i, cellText.length());
                    }
                }
            }
        }

        return maxColumnLength;
    }

    public static String convertDataTableToString(DataTable dataTableMsg, List<Integer> maxColumnLength) {

        StringBuilder sb = new StringBuilder();

        List<TableRow> rows = dataTableMsg.getRows();

        for (int i = 0; i < rows.size(); i++) {

            TableRow row = rows.get(i);
            List<TableCell> cells = row.getCells();

            sb.append("|");

            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {

                TableCell cellValue = cells.get(columnIndex);
                String value = cellValue.getValue();
                sb.append(value);
                boolean needToPad = columnIndex < maxColumnLength.size()
                        && maxColumnLength.get(columnIndex) > value.length();
                if (needToPad) {
                    int paddingLength = maxColumnLength.get(columnIndex) - value.length();
                    String padding = StringUtils.repeat(" ", paddingLength);
                    sb.append(padding);
                }
                sb.append("|");
            }

            if (i < rows.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static MethodSpec createGetTableConverterMethod(ProcessingEnvironment processingEnv) {

        MethodSpec methodSpec = MethodSpec.methodBuilder("getTableConverter")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TableConverter.class)
                .build();

        return methodSpec;
    }

    public static MethodSpec createDataTableMethod(ProcessingEnvironment processingEnv) {

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("createDataTable")
                //                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(String.class, "tableLines")
                .returns(io.cucumber.datatable.DataTable.class);

        /**
         * Target method body:
         *
         * String[] tableRows = table.split("\n");
         * List<List<String>> rawDataTable = new ArrayList<>(tableRows.length);
         *
         * for (String tableRow : tableRows) {
         *     String trimmedLine = tableRow.trim();
         *     if (!trimmedLine.isEmpty()) {
         *         String[] columns = trimmedLine.split("\\|");
         *         List<String> rowColumns = new ArrayList<>(columns.length);
         *         for (int i = 1; i < columns.length; i++) {
         *             String column = columns[i].trim();
         *             rowColumns.add(column);
         *         }
         *         rawDataTable.add(rowColumns);
         *     }
         * }
         *
         * DataTable dataTable = DataTable.create(rawDataTable, getTableConverter());
         * return dataTable;
         */
        methodSpecBuilder.addCode("\n");
        methodSpecBuilder.addStatement("String[] tableRows = tableLines.split(\"\\\\n\")")
                .addStatement("$T<$T<String>> rawDataTable = new $T<>(tableRows.length)",
                        List.class, List.class, ArrayList.class
                );

        methodSpecBuilder.addCode("\n");
        methodSpecBuilder.beginControlFlow("for (String tableRow : tableRows)");
        methodSpecBuilder.addStatement("String trimmedLine = tableRow.trim()");

        methodSpecBuilder.beginControlFlow("if (!trimmedLine.isEmpty())");
        methodSpecBuilder.addStatement("String[] columns = trimmedLine.split(\"\\\\|\")")
                .addStatement("List<String> rowColumns = new ArrayList<>(columns.length)");

        methodSpecBuilder.beginControlFlow("for (int i = 1; i < columns.length; i++)");
        methodSpecBuilder.addStatement("String column = columns[i].trim()")
                .addStatement("rowColumns.add(column)");
        methodSpecBuilder.endControlFlow(); // end for loop over columns

        methodSpecBuilder.addStatement("rawDataTable.add(rowColumns)");
        methodSpecBuilder.endControlFlow(); // end if trimmedLine is not empty

        methodSpecBuilder.endControlFlow(); // end for loop over tableLines

        methodSpecBuilder.addCode("\n");
        methodSpecBuilder.addStatement("DataTable dataTable = DataTable.create(rawDataTable, getTableConverter())")
                .addStatement("return dataTable");

        MethodSpec methodSpec = methodSpecBuilder.build();
        return methodSpec;
    }

}
