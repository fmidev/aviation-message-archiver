package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FilenamePattern;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@SuppressWarnings("UnnecessaryLocalVariable")
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class FileMetadataPopulatorTest {
    private static final Instant FILE_MODIFIED = Instant.parse("2000-01-02T03:05:34Z");
    private static final InputAviationMessage INPUT_MESSAGE_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setProductIdentifier("testproduct")//
                    .setFileModified(FILE_MODIFIED)//
                    .setFilenamePattern(new FilenamePattern("taf.txt", MessagePopulatorTests.FILE_NAME_PATTERN, ZoneOffset.UTC)))//
            .buildPartial();

    private FileMetadataPopulator populator;

    @BeforeEach
    void setUp() {
        populator = new FileMetadataPopulator();
    }

    // TODO: Depends on #30
    @Test
    void populates_route() {
        fail("TODO");
    }

    // TODO: Depends on #30
    @Test
    void populates_format() {
        fail("TODO");
    }

    @Test
    void populates_fileModified() {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE;
        final Instant expected = FILE_MODIFIED;

        final ArchiveAviationMessage.Builder builder = MessagePopulatorTests.EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getFileModified()).isEqualTo(Optional.of(expected));
    }
}