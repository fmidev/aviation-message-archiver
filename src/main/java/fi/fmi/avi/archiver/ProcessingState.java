package fi.fmi.avi.archiver;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import fi.fmi.avi.archiver.file.FileMetadata;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class ProcessingState {
    private final Clock clock;
    private final Multiset<FileUnderProcessing> filesUnderProcessing = ConcurrentHashMultiset.create();

    public ProcessingState(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    public void start(final FileMetadata file) {
        requireNonNull(file, "file");
        final FileUnderProcessing element = new FileUnderProcessing(file, clock);
        filesUnderProcessing.add(element);
    }

    public void finish(final FileMetadata file) {
        requireNonNull(file, "file");
        final FileUnderProcessing element = new FileUnderProcessing(file, clock);
        filesUnderProcessing.remove(element);
    }

    public int getFileCountUnderProcessing() {
        return filesUnderProcessing.elementSet().size();
    }

    public Duration getRunningFileProcessingMaxElapsed() {
        final long now = clock.millis();
        return Duration.ofMillis(now - filesUnderProcessing.stream()//
                .mapToLong(FileUnderProcessing::getStart)//
                .min()//
                .orElse(now));
    }

    public boolean isFileUnderProcessing(final String productIdentifier, final String filename) {
        requireNonNull(productIdentifier, "productIdentifier");
        requireNonNull(filename, "filename");
        return filesUnderProcessing.stream().anyMatch(underProcessing ->
                underProcessing.getProductIdentifier().equals(productIdentifier) && underProcessing.getFilename().equals(filename));
    }

    private static final class FileUnderProcessing {
        private final FileMetadata fileMetadata;
        private final long start;

        public FileUnderProcessing(final FileMetadata fileMetadata, final Clock clock) {
            this.fileMetadata = requireNonNull(fileMetadata, "fileMetadata");
            this.start = requireNonNull(clock, "clock").millis();
        }

        public String getFilename() {
            return fileMetadata.getFilename();
        }

        public String getProductIdentifier() {
            return fileMetadata.getProductIdentifier();
        }

        public FileMetadata getFileMetadata() {
            return fileMetadata;
        }

        public long getStart() {
            return start;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getProductIdentifier(), getFilename());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof FileUnderProcessing) {
                final FileUnderProcessing other = (FileUnderProcessing) obj;
                return this.getProductIdentifier().equals(other.getProductIdentifier())//
                        && this.getFilename().equals(other.getFilename());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return getProductIdentifier() + ":" + getFilename();
        }
    }
}
