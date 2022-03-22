package fi.fmi.avi.archiver.spring.integration.file.filters;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;

/**
 * {@link org.springframework.integration.file.filters.FileListFilter} that stores the file's last modification time and
 * filesize and accepts the file on a subsequent run if the file's last modification time and filesize have not changed.
 */
public class AcceptUnchangedFileListFilter extends AbstractFileListFilter<File> implements ReversibleFileListFilter<File>, ResettableFileListFilter<File> {

    private final Map<File, FileProperties> seenMap = new HashMap<>();
    private final Object monitor = new Object();

    @Override
    public boolean accept(final File file) {
        requireNonNull(file, "file");
        synchronized (monitor) {
            final FileProperties newData = FileProperties.create(file);
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
    @AutoValue
    static abstract class FileProperties {
        FileProperties() {
        }

        static FileProperties create(final File file) {
            requireNonNull(file, "file");
            try {
                final Path path = file.toPath();
                final FileTime lastModified = Files.getLastModifiedTime(path);
                final long size = Files.size(path);
                return new AutoValue_AcceptUnchangedFileListFilter_FileProperties(lastModified, size);
            } catch (final IOException | RuntimeException ignored) {
                return new AutoValue_AcceptUnchangedFileListFilter_FileProperties(FileTime.from(Instant.EPOCH), 0);
            }
        }

        public abstract FileTime getLastModified();

        public abstract long getSize();
    }

}
