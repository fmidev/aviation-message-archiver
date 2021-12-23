package fi.fmi.avi.archiver.spring.integration.file.filters;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class AcceptUnchangedFileListFilterTest {

    @Test
    void test(@TempDir Path tempDir) {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        final File file = tempFile.toFile();

        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
        // File is removed from seen map
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_content_change(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        final File file = tempFile.toFile();

        assertThat(filter.accept(file)).isFalse();
        Files.write(tempFile, "more content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_multiple_content_changes(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        final File file = tempFile.toFile();

        assertThat(filter.accept(file)).isFalse();
        Files.write(tempFile, "more content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertThat(filter.accept(file)).isFalse();
        Files.write(tempFile, "more content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertThat(filter.accept(file)).isFalse();
        Files.write(tempFile, "more content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_last_modified_change(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:00Z")));
        final File file = tempFile.toFile();

        assertThat(filter.accept(file)).isFalse();
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:01Z")));
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
        assertThat(filter.accept(file)).isFalse();
    }

    @Test
    void test_multiple_last_modified_changes(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:00Z")));
        final File file = tempFile.toFile();

        assertThat(filter.accept(file)).isFalse();
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:01Z")));
        assertThat(filter.accept(file)).isFalse();
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:02Z")));
        assertThat(filter.accept(file)).isFalse();
        Files.setLastModifiedTime(tempFile, FileTime.from(Instant.parse("2020-05-18T12:00:03Z")));
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_filter_files(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        final File file = tempFile.toFile();

        assertThat(filter.filterFiles(new File[]{file})).hasSize(0);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(1);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(0);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(1);
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_filter_files_modification(@TempDir Path tempDir) throws IOException {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final Path tempFile = tempDir.resolve("temp");
        Files.write(tempFile, "temp content".getBytes(StandardCharsets.UTF_8));
        final File file = tempFile.toFile();

        assertThat(filter.filterFiles(new File[]{file})).hasSize(0);
        Files.write(tempFile, "more content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(0);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(1);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(0);
        assertThat(filter.filterFiles(new File[]{file})).hasSize(1);
        assertThat(filter.accept(file)).isFalse();
        assertThat(filter.accept(file)).isTrue();
    }

    @Test
    void test_rollback(@TempDir Path tempDir) {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        testRollback(filter, tempDir);
    }

    @Test
    void test_rollback_composite(@TempDir Path tempDir) {
        final AcceptUnchangedFileListFilter filter = new AcceptUnchangedFileListFilter();
        final CompositeFileListFilter<File> composite = new CompositeFileListFilter<>(Collections.singletonList(filter));
        testRollback(composite, tempDir);
    }

    private void testRollback(final ReversibleFileListFilter<File> filter, final Path tempDir) {
        final File foo = tempDir.resolve("foo").toFile();
        final File bar = tempDir.resolve("bar").toFile();
        final File baz = tempDir.resolve("baz").toFile();
        final File[] files = new File[]{foo, bar, baz};

        List<File> passed = filter.filterFiles(files);
        assertThat(passed).isEmpty();
        passed = filter.filterFiles(files);
        assertThat(passed).contains(files);

        List<File> passedNow = filter.filterFiles(files);
        assertThat(passedNow).hasSize(0);
        filter.rollback(passed.get(1), passed);
        passedNow = filter.filterFiles(files);
        assertThat(passedNow).hasSize(1);
        assertThat(passedNow.get(0)).isEqualTo(foo);
        passedNow = filter.filterFiles(files);
        assertThat(passedNow).hasSize(2);

        assertThat(passedNow.get(0)).isEqualTo(bar);
        assertThat(passedNow.get(1)).isEqualTo(baz);
    }

}
