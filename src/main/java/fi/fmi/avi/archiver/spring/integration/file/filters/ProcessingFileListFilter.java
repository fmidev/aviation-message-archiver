package fi.fmi.avi.archiver.spring.integration.file.filters;

import static java.util.Objects.requireNonNull;

import java.io.File;

import org.springframework.integration.file.filters.AbstractFileListFilter;

import fi.fmi.avi.archiver.ProcessingState;
import fi.fmi.avi.archiver.file.FileReference;

public class ProcessingFileListFilter extends AbstractFileListFilter<File> {

    private final ProcessingState processingState;
    private final String productIdentifier;

    public ProcessingFileListFilter(final ProcessingState processingState, final String productIdentifier) {
        this.processingState = requireNonNull(processingState, "processingState");
        this.productIdentifier = requireNonNull(productIdentifier, "productIdentifier");
    }

    @Override
    public boolean accept(final File file) {
        return !processingState.isFileUnderProcessing(FileReference.create(productIdentifier, file.getName()));
    }

}
