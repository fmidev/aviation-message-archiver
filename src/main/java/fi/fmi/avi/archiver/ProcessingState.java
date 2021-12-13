package fi.fmi.avi.archiver;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;

public class ProcessingState {
    private final Clock clock;
    private final ConcurrentMap<FileReference, Status> filesUnderProcessing = new ConcurrentHashMap<>();

    public ProcessingState(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    public void start(final FileMetadata file) {
        requireNonNull(file, "file");
        filesUnderProcessing.compute(file.getFileReference(),
                (fileReference, status) -> status == null ? new Status(clock.millis()) : status.increaseProcessingCount());
    }

    public void finish(final FileMetadata file) {
        requireNonNull(file, "file");
        filesUnderProcessing.computeIfPresent(file.getFileReference(), (fileReference, status) -> status.decreaseProcessingCount());
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
