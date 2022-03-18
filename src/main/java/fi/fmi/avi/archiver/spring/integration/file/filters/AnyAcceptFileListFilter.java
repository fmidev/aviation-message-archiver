package fi.fmi.avi.archiver.spring.integration.file.filters;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * A composite file list filter that accepts files that <strong>any</strong> of the configured {@code FileListFilter}s accept.
 *
 * @param <F>
 *         the type that will be filtered
 */
public class AnyAcceptFileListFilter<F> extends CompositeFileListFilter<F> {
    public AnyAcceptFileListFilter() {
    }

    public AnyAcceptFileListFilter(final Collection<? extends FileListFilter<F>> fileFilters) {
        super(fileFilters);
    }

    @Override
    public List<F> filterFiles(@Nullable final F[] files) {
        if (files == null) {
            return new ArrayList<>(0);
        }
        if (this.fileFilters.isEmpty()) {
            return new ArrayList<>(Arrays.asList(files));
        }
        final Set<F> accepted = new HashSet<>();
        for (final FileListFilter<F> filter : this.fileFilters) {
            accepted.addAll(filter.filterFiles(files));
        }
        // Retain original order
        return Arrays.stream(files)//
                .filter(accepted::contains)//
                .collect(Collectors.toList());
    }

    @Override
    public boolean accept(final F file) {
        requireNonNull(file, "file");
        for (final FileListFilter<F> filter : this.fileFilters) {
            if (filter.accept(file)) {
                return true;
            }
        }
        return this.fileFilters.isEmpty();
    }
}
