package fi.fmi.avi.archiver.spring.integration.file.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.filters.FileListFilter;

class AnyAcceptFileListFilterTest {

    private File[] inputFilesArray;
    private List<File> inputFilesList;
    private AnyAcceptFileListFilter<File> filter;

    @BeforeEach
    void setUp() {
        inputFilesArray = IntStream.range(0, 9)//
                .mapToObj(i -> new File("file_" + i))//
                .toArray(File[]::new);
        inputFilesList = Arrays.asList(inputFilesArray);
        filter = new AnyAcceptFileListFilter<>();
    }

    @Test
    void accept_without_subfilters_returns_true() {
        assertThat(filter.accept(inputFilesArray[0])).isTrue();
    }

    @Test
    void accpet_given_input_accepted_by_any_subfilter_returns_true() {
        filter.addFilters(//
                new TestFileListFilter(inputFilesList.subList(2, 5)), //
                new TestFileListFilter(inputFilesList.subList(4, 7)));

        for (int i = 2; i < 7; i++) {
            final File file = inputFilesArray[i];
            assertThat(filter.accept(file)).as(file.toString()).isTrue();
        }
    }

    @Test
    void accpet_given_input_not_accepted_by_any_subfilter_returns_false() {
        filter.addFilters(//
                new TestFileListFilter(inputFilesList.subList(2, 5)), //
                new TestFileListFilter(inputFilesList.subList(4, 7)));

        for (int i = 0; i < 2; i++) {
            final File file = inputFilesArray[i];
            assertThat(filter.accept(file)).as(file.toString()).isFalse();
        }
        for (int i = 7; i < 9; i++) {
            final File file = inputFilesArray[i];
            assertThat(filter.accept(file)).as(file.toString()).isFalse();
        }
    }

    @Test
    void accept_given_unaccepted_input_delegates_to_all_subfilters() {
        final TestFileListFilter filter1 = new TestFileListFilter(inputFilesList.subList(2, 5));
        final TestFileListFilter filter2 = new TestFileListFilter(inputFilesList.subList(4, 7));
        filter.addFilters(filter1, filter2);
        final File file = inputFilesArray[7];

        final boolean result = filter.accept(file);

        assertThat(result).isFalse();
        assertThat(filter1.getInvocations()).containsExactly(file);
        assertThat(filter2.getInvocations()).containsExactly(file);
    }

    @Test
    void accept_given_accepted_input_returns_immediately() {
        final TestFileListFilter filter1 = new TestFileListFilter(inputFilesList.subList(2, 5));
        final TestFileListFilter filter2 = new TestFileListFilter(inputFilesList.subList(4, 7));
        filter.addFilters(filter1, filter2);
        final File file = inputFilesArray[4];

        final boolean result = filter.accept(file);

        assertThat(result).isTrue();
        assertThat(filter1.getInvocations()).containsExactly(file);
        assertThat(filter2.getInvocations()).isEmpty();
    }

    @Test
    void filterFiles_given_null_returns_empty_list() {
        assertThat(filter.filterFiles(null)).isEmpty();
    }

    @Test
    void filterFiles_without_subfilters_returns_all_files() {
        assertThat(filter.filterFiles(inputFilesArray)).isEqualTo(inputFilesList);
    }

    @Test
    void filterFiles_returns_list_of_accepted_files_in_original_order_1() {
        filter.addFilters(//
                new TestFileListFilter(inputFilesList.subList(2, 5), true), //
                new TestFileListFilter(inputFilesList.subList(4, 7)));

        final List<File> result = filter.filterFiles(inputFilesArray);

        assertThat(result).containsExactlyElementsOf(inputFilesList.subList(2, 7));
    }

    @Test
    void filterFiles_returns_list_of_accepted_files_in_original_order_2() {
        filter.addFilters(//
                new TestFileListFilter(inputFilesList.subList(6, 9), true), //
                new TestFileListFilter(inputFilesList.subList(0, 3), true), //
                new TestFileListFilter(inputFilesList.subList(2, 5)));

        final List<File> result = filter.filterFiles(inputFilesArray);

        final List<File> expected = new ArrayList<>();
        expected.addAll(inputFilesList.subList(0, 5));
        expected.addAll(inputFilesList.subList(6, 9));
        assertThat(result).containsExactlyElementsOf(expected);
    }

    private static final class TestFileListFilter implements FileListFilter<File> {
        private final List<File> invocations = new ArrayList<>();
        private final Set<File> acceptedFiles;
        private final boolean reverse;

        TestFileListFilter(final Collection<File> acceptedFiles) {
            this(acceptedFiles, false);
        }

        TestFileListFilter(final Collection<File> acceptedFiles, final boolean reverse) {
            this.acceptedFiles = Collections.unmodifiableSet(new HashSet<>(acceptedFiles));
            this.reverse = reverse;
        }

        @Override
        public List<File> filterFiles(final File[] files) {
            final List<File> accepted = Arrays.stream(files)//
                    .filter(this::accept)//
                    .collect(Collectors.toCollection(ArrayList::new));
            if (reverse) {
                Collections.reverse(accepted);
            }
            return accepted;
        }

        @Override
        public boolean accept(final File file) {
            invocations.add(file);
            return acceptedFiles.contains(file);
        }

        @Override
        public boolean supportsSingleFileFiltering() {
            return true;
        }

        public List<File> getInvocations() {
            return Collections.unmodifiableList(invocations);
        }
    }
}
