package fi.fmi.avi.archiver.config;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileReadingMessageSource;

public class SourceDirectoryInitializer {
    /**
     * Initializes the source directory listeners
     */

    private final IntegrationFlowContext context;

    private final Set<IntegrationFlowContext.IntegrationFlowRegistration> registerations;

    private final Consumer<SourcePollingChannelAdapterSpec> poller;
    private final Set<String> sourceDirs;
    private final String channel;

    public SourceDirectoryInitializer(final String channel, final IntegrationFlowContext flowContext, final Set<String> sourceDirs,
            final Consumer<SourcePollingChannelAdapterSpec> poller) {
        this.channel = channel;
        this.context = flowContext;
        this.sourceDirs = Collections.unmodifiableSet(sourceDirs);
        this.registerations = new HashSet<>();
        this.poller = poller;
    }

    @PostConstruct
    private void initializeDirectories() {
        sourceDirs.stream()//
                .map(sourceDir -> {
                    final FileReadingMessageSource sourceReader = new FileReadingMessageSource();
                    sourceReader.setDirectory(new File(sourceDir));
                    return sourceReader;
                }).map(source -> context.registration(IntegrationFlows.from(source, poller)//
                        .channel(channel)//
                        .get()//
                ).register()//
        ).forEach(registerations::add);
    }

    public void dispose() {
        registerations.forEach(registeration -> context.remove(registeration.getId()));
    }
}
