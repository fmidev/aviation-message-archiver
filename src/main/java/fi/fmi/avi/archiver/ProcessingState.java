package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.file.FileReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class ProcessingState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingState.class);

    private final Clock clock;
    private final ConcurrentMap<FileReference, Status> filesUnderProcessing = new ConcurrentHashMap<>();

    public ProcessingState(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    public void start(final FileReference file) {
        requireNonNull(file, "file");
        final Status newStatus = filesUnderProcessing.compute(file,
                (fileReference, status) -> status == null ? new Status(clock.millis()) : status.increaseProcessingCount());
        if (newStatus.getProcessingCount() > 1) {
            LOGGER.warn("Starting processing of file '{}', now being processed concurrently {} times.", file, newStatus.getProcessingCount());
        }
    }

    public void finish(final FileReference file) {
        requireNonNull(file, "file");
        if (filesUnderProcessing.containsKey(file)) {
            filesUnderProcessing.computeIfPresent(file, (fileReference, status) -> status.decreaseProcessingCount());
        } else {
            LOGGER.warn("Attempted to finish processing of file '{}', but it is not under processing.", file);
        }
    }

    public int getFileCountUnderProcessing() {
        return filesUnderProcessing.size();
    }

    public Duration getRunningFileProcessingMaxElapsed() {
        final long now = clock.millis();
        final long startMin = filesUnderProcessing.values().stream()//
                .mapToLong(Status::getStart)//
                .min()//
                .orElse(now);
        return Duration.ofMillis(now - startMin);
    }

    public boolean isFileUnderProcessing(final FileReference fileReference) {
        requireNonNull(fileReference, "fileReference");
        return filesUnderProcessing.containsKey(fileReference);
    }

    private static final class Status {
        private final long start;
        private final int processingCount;

        Status(final long start) {
            this(start, 1);
        }

        private Status(final long start, final int processingCount) {
            this.start = start;
            this.processingCount = processingCount;
        }

        public long getStart() {
            return start;
        }

        public int getProcessingCount() {
            return processingCount;
        }

        public Status increaseProcessingCount() {
            return new Status(start, processingCount + 1);
        }

        @Nullable
        public Status decreaseProcessingCount() {
            return processingCount <= 1 ? null : new Status(start, processingCount - 1);
        }
    }
}
