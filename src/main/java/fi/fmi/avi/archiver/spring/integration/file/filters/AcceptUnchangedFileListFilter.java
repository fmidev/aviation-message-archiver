package fi.fmi.avi.archiver.spring.integration.file.filters;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * {@link org.springframework.integration.file.filters.FileListFilter} that stores the file's last modification time and
 * filesize and accepts the file on a subsequent run if the file's last modification time and filesize have not changed.
 */
public class AcceptUnchangedFileListFilter extends AbstractFileListFilter<File> implements ReversibleFileListFilter<File>,
        ResettableFileListFilter<File> {

    private final Map<File, FileData> seenMap = new HashMap<>();
    private final Object monitor = new Object();

    @Override
    public boolean accept(final File file) {
        requireNonNull(file, "file");
        synchronized (monitor) {
            final FileData newData = new FileData(file);
            if (seenMap.containsKey(file)) {
                if (newData.equals(seenMap.get(file))) {
                    seenMap.remove(file);
                    return true;
                }
            }
            seenMap.put(file, newData);
            return false;
        }
    }

    @Override
    public void rollback(final File file, final List<File> files) {
        requireNonNull(file, "file");
        requireNonNull(files, "files");
        synchronized (monitor) {
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
        requireNonNull(fileToRemove, "fileToRemove");
        synchronized (monitor) {
            return seenMap.remove(fileToRemove) != null;
        }
    }

    @VisibleForTesting
    static class FileData {
        private FileTime lastModified = FileTime.from(Instant.EPOCH);
        private long size = 0;

        FileData(final File file) {
            try {
                this.lastModified = Files.getLastModifiedTime(file.toPath());
                this.size = Files.size(file.toPath());
            } catch (final Exception ignored) {
                // Use default values
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileData fileData = (FileData) o;
            return size == fileData.size && lastModified.equals(fileData.lastModified);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lastModified, size);
        }
    }

}
