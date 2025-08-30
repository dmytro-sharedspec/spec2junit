package dev.spec2test.feature2junit.gherkin.utils;

import com.squareup.javapoet.MethodSpec;
import dev.spec2test.common.ProcessingException;

import java.util.List;

/**
 * Utility class for generating method names from Gherkin step definitions.
 */
public class MethodNamingUtils {

    /**
     * Generates a method name from the first line of a Gherkin step definition.
     *
     * @param stepFirstLine            the first line of the Gherkin step definition
     * @param scenarioStepsMethodSpecs a list of MethodSpec objects representing the scenario steps
     * @return a sanitized method name suitable for use in Java code
     */
    public static String getStepMethodName(String stepFirstLine, List<MethodSpec> scenarioStepsMethodSpecs) {

        StringBuilder methodNameBuilder = new StringBuilder();

        String[] words = stepFirstLine.split("\\s+");
        for (int i = 0; i < words.length; i++) {

            String word = words[i];

            if (i == 0 && word.equalsIgnoreCase("and") || word.equalsIgnoreCase("but")) {
                word = getPreviousGWTStepWord(stepFirstLine, scenarioStepsMethodSpecs);
            }

            StringBuilder sanitizedWordBuilder = new StringBuilder();

            boolean isFirstWord = methodNameBuilder.isEmpty();

            if (isFirstWord) {
                // first word in method name - should not be capitalized and should start with
                // a character suitable for a method name
                for (char c : word.toCharArray()) {
                    if (sanitizedWordBuilder.length() == 0) {
                        // nothing added yet - so check if char is suitable as a starting char
                        if (Character.isJavaIdentifierStart(c)) {
                            char wordFirstChar = Character.toLowerCase(c);
                            sanitizedWordBuilder.append(wordFirstChar);
                        } else {
                            // skip characters that are not allowed for method name start
                        }
                    } else {
                        if (Character.isJavaIdentifierPart(c)) {
                            // always convert to lower case
                            char charInLowerCase = Character.toLowerCase(c);
                            sanitizedWordBuilder.append(charInLowerCase);
                        } else {
                            // skip
                        }
                    }
                }

            } else {
                /**
                 * all other words should be capitalized
                 */
                for (char c : word.toCharArray()) {

                    if (Character.isJavaIdentifierPart(c)) {
                        // always convert to lower case
                        char charToAppend;
                        if (sanitizedWordBuilder.isEmpty()) {
                            charToAppend = Character.toUpperCase(c);
                        } else {
                            charToAppend = Character.toLowerCase(c);
                        }
                        sanitizedWordBuilder.append(charToAppend);
                    } else {
                        // skip
                    }
                }
            }

            String sanitizedWord = sanitizedWordBuilder.toString();
            methodNameBuilder.append(sanitizedWord);

        }

        String sanitizedMethodName = methodNameBuilder.toString();
        return sanitizedMethodName;
    }

    private static String getPreviousGWTStepWord(String stepFirstLine, List<MethodSpec> scenarioStepsMethodSpecs) {

        /**
         * need to replace the 'And' or 'But' keywords with one from GWT as those are just aliases
         */
        if (scenarioStepsMethodSpecs.isEmpty()) {
            throw new ProcessingException(
                    "Step's first line - '" + stepFirstLine
                            + "' starts with 'And', but there are no previous scenario steps defined");
        }

        MethodSpec lastScenarioMethodSpec = scenarioStepsMethodSpecs.get(scenarioStepsMethodSpecs.size() - 1);
        String lastMethodName = lastScenarioMethodSpec.name;
        if (lastMethodName.startsWith("given")) {
            return "given";
        } else if (lastMethodName.startsWith("when")) {
            return "when";
        } else if (lastMethodName.startsWith("then")) {
            return "then";
        } else {
            throw new ProcessingException(
                    "Step's first line - '" + stepFirstLine
                            + "' starts with 'And', but the previous step is not a GWT step");
        }
    }
}
