package fi.fmi.avi.archiver.initializing;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.messaging.MessageChannel;

import fi.fmi.avi.archiver.config.FileTypeHolder;

public class FileInspectionInitializer {

    /**
     * Initializes filtering of the files based on the file names and regular expression patterns
     */

    private final IntegrationFlowContext context;

    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registerations;

    private final FileTypeHolder fileTypeHolder;
    private final MessageChannel inputChannel;
    private final MessageChannel outputChannel;

    public FileInspectionInitializer(final IntegrationFlowContext context, final FileTypeHolder fileTypeHolder, final MessageChannel inputChannel,
            final MessageChannel outputChannel) {
        this.context = context;
        this.registerations = new HashSet<>();
        this.fileTypeHolder = fileTypeHolder;
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
    }

    @PostConstruct
    private void initializeFilePatternFlows() {
        fileTypeHolder.getTypes().forEach(fileType -> {
            registerations.add(context.registration(IntegrationFlows.from(inputChannel)//
                    .bridge()//
                    .filter(new RegexPatternFileListFilter(fileType.getPattern()))//
                    .channel(outputChannel)//
                    .get()//
            ).register());
        });
    }

    public void dispose() {
        registerations.forEach(registeration -> context.remove(registeration.getId()));
    }

}
