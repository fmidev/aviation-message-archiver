package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.TestMessageProcessorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Pattern;

import static fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper.tryGet;
import static org.assertj.core.api.Assertions.assertThat;

class FileNameDataPopulatorTest {
    private static final InputAviationMessage INPUT_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setFileReference(FileReference.create("testProduct", "null"))//
                    .setFileConfig(FileConfig.builder()//
                            .setPattern(Pattern.compile("msg(-((?<yyyy>\\d{4})(?<MM>\\d{2}))?(?<dd>\\d{2})-(?<hh>\\d{2})(?<mm>\\d{2}))?\\.txt"))//
                            .setNameTimeZone(ZoneOffset.ofHours(10))//
                            .buildPartial())//
                    .setFileModified(Instant.parse("2001-03-05T07:09:11.013Z"))//
                    .buildPartial())//
            .buildPartial();

    @CsvSource({ //
            "msg.txt, ", //
            "msg-05-1608.txt, 2001-03-05T06:08:00Z", //
            "msg-20020227-1608.txt, 2002-02-27T06:08:00Z", //
    })
    @ParameterizedTest
    void populates_messageTime_when_exists(final String fileName, final String expectedMessageTime) {
        final Clock clock = Clock.fixed(Instant.parse("2011-03-05T15:17:19.021Z"), ZoneOffset.UTC);
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(clock);
        final FileNameDataPopulator populator = new FileNameDataPopulator(helper, clock);
        final ArchiveAviationMessage.Builder targetBuilder = ArchiveAviationMessage.builder();
        final InputAviationMessage input = INPUT_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata.mutateFileReference(ref -> ref.setFilename(fileName)))//
                .build();
        final MessageProcessorContext context = TestMessageProcessorContext.create(input);

        populator.populate(context, targetBuilder);

        assertThat(tryGet(targetBuilder, builder -> builder.getMessageTime()))//
                .isEqualTo(Optional.ofNullable(expectedMessageTime).map(Instant::parse));
    }

    @Test
    void populates_messageTime_completing_from_clock_when_fileModified_is_missing() {
        final Clock clock = Clock.fixed(Instant.parse("2011-03-05T15:17:19.021Z"), ZoneOffset.UTC);
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(clock);
        final FileNameDataPopulator populator = new FileNameDataPopulator(helper, clock);
        final InputAviationMessage input = INPUT_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata//
                        .mutateFileReference(ref -> ref.setFilename("msg-05-1608.txt"))//
                        .clearFileModified())//
                .build();
        final MessageProcessorContext context = TestMessageProcessorContext.create(input);
        final ArchiveAviationMessage.Builder targetBuilder = ArchiveAviationMessage.builder();

        populator.populate(context, targetBuilder);

        assertThat(tryGet(targetBuilder, builder -> builder.getMessageTime()))//
                .hasValue(Instant.parse("2011-03-05T06:08:00Z"));
    }
}