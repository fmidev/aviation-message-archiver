package fi.fmi.avi.archiver.spring.filter;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link org.springframework.integration.file.filters.FileListFilter} that stores the file's last modification time and
 * filesize and accepts the file once on a subsequent run if the file's last modification time and filesize have not changed.
 */
public class AcceptOnceUnchangedFileListFilter extends AbstractFileListFilter<File> implements ReversibleFileListFilter<File>,
        ResettableFileListFilter<File> {

    @Nullable
    private final Queue<File> seen;
    private final Map<File, FileData> seenMap = new HashMap<>();
    private final Object monitor = new Object();

    /**
     * Creates an AcceptOnceUnchangedFileListFilter that is based on a bounded queue. If the queue overflows,
     * files that fall out will be passed through this filter again if passed to the
     * {@link AbstractFileListFilter#filterFiles(Object[])}
     *
     * @param maxCapacity the maximum number of Files to maintain in the 'seen' queue.
     */
    public AcceptOnceUnchangedFileListFilter(int maxCapacity) {
        this.seen = new LinkedBlockingQueue<>(maxCapacity);
    }

    /**
     * Creates an AcceptOnceUnchangedFileListFilter based on an unbounded queue.
     */
    public AcceptOnceUnchangedFileListFilter() {
        this.seen = null;
    }


    @Override
    public boolean accept(final File file) {
        synchronized (monitor) {
            if (seenMap.containsKey(file)) {
                final FileData existingData = seenMap.get(file);
                if (existingData.isAccepted()) {
                    return false;
                }
                final FileData newData = new FileData(file);
                if (newData.equals(existingData)) {
                    seenMap.put(file, FileData.accept(newData));
                    return true;
                } else {
                    seenMap.put(file, newData);
                    return false;
                }
            }
            if (seen != null && !seen.offer(file)) {
                final File removed = seen.poll();
                seenMap.remove(removed);
                seen.add(file);
            }
            seenMap.put(file, new FileData(file));
            return false;
        }
    }

    @Override
    public void rollback(final File file, List<File> files) {
        synchronized (this.monitor) {
            boolean rollingBack = false;
            for (final File fileToRollback : files) {
                if (fileToRollback.equals(file)) {
                    rollingBack = true;
                }
                if (rollingBack) {
                    remove(fileToRollback);
                }
            }
        }
    }

    @Override
    public boolean remove(final File fileToRemove) {
        @Nullable final FileData fileData = seenMap.remove(fileToRemove);
        if (seen != null) {
            seen.remove(fileToRemove);
        }
        return fileData != null;
    }

    @VisibleForTesting
    static class FileData {
        private FileTime lastModified = FileTime.from(Instant.EPOCH);
        private long size = 0;
        private boolean accepted;

        FileData(File file) {
            try {
                this.lastModified = Files.getLastModifiedTime(file.toPath());
                this.size = Files.size(file.toPath());
            } catch (final Exception ignored) {
                // Use default values
            }
        }

        private FileData(final FileData fileData, final boolean accepted) {
            this.lastModified = fileData.lastModified;
            this.size = fileData.size;
            this.accepted = accepted;
        }

        static FileData accept(final FileData fileData) {
            return new FileData(fileData, true);
        }

        boolean isAccepted() {
            return accepted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final FileData fileData = (FileData) o;
            return size == fileData.size && accepted == fileData.accepted && lastModified.equals(fileData.lastModified);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lastModified, size, accepted);
        }
    }

}
