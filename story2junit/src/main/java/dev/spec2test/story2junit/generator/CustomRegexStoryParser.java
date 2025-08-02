package dev.spec2test.story2junit.generator;

import dev.spec2test.common.MessageSupport;
import dev.spec2test.common.fileutils.AptFileUtils;
import javax.annotation.processing.ProcessingEnvironment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jbehave.core.model.Story;
import org.jbehave.core.parsers.RegexStoryParser;

@RequiredArgsConstructor
class CustomRegexStoryParser extends RegexStoryParser implements MessageSupport {

    @Getter
    private final ProcessingEnvironment processingEnv;

    public CustomRegexStoryParser(ProcessingEnvironment processingEnv, ProcessingEnvironment env) {

        this.processingEnv = processingEnv;
    }

    public Story parseUsingStoryPath(String storyPath) {

        String fileContent = AptFileUtils.loadFileContent(storyPath, processingEnv);

        Story story = parseStory(fileContent);
        return story;
    }

    @Override
    public Story parseStory(String storyAsText) {

        try {

            // Assuming the parseStory method is defined in CustomRegexStoryParser
            Story story = super.parseStory(storyAsText);
            // Do something with the parsed story
            return story;
        } catch (Throwable e) {
            logError("Error parsing story:\n" + e.getMessage(), processingEnv);
            throw e;
        }
    }

}