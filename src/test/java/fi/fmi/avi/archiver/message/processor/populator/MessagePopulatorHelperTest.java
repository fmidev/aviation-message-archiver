package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessagePopulatorHelperTest {
    private static final FileMetadata FILE_METADATA_TEMPLATE = FileMetadata.builder()//
            .setFileReference(FileReference.create("testproduct", "null"))//
            .mutateFileConfig(fileConfig -> fileConfig//
                    .setFormat(MessageProcessorTestHelper.FormatId.TAC.getFormat())//
                    .setFormatId(MessageProcessorTestHelper.FormatId.TAC.getId())//
                    .setPattern(MessageProcessorTestHelper.FILE_NAME_PATTERN)//
                    .setNameTimeZone(ZoneOffset.UTC))//
            .buildPartial();

    private static Optional<PartialOrCompleteTimeInstant> partialOrCompleteTimeInstant(
            final @Nullable PartialDateTime partialTime, final @Nullable ZonedDateTime completeTime) {
        if (partialTime == null && completeTime == null) {
            return Optional.empty();
        }
        return Optional.of(PartialOrCompleteTimeInstant.builder()//
                .setNullablePartialTime(partialTime)//
                .setNullableCompleteTime(completeTime)//
                .build());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessagePopulatorHelperTest_testResolveCompleteTime.csv", numLinesToSkip = 1)
    void testResolveCompleteTime(
            final @Nullable PartialDateTime partialTime, final @Nullable ZonedDateTime completeTime, final String filename,
            final @Nullable Instant fileModified, final ZonedDateTime clock, final @Nullable ZonedDateTime expectedTime) {
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(Clock.fixed(clock.toInstant(), clock.getZone()));
        final FileMetadata fileMetadata = FILE_METADATA_TEMPLATE.toBuilder()//
                .mutateFileReference(ref -> ref.setFilename(filename))//
                .setNullableFileModified(fileModified)//
                .build();
        final PartialOrCompleteTimeInstant partialOrCompleteTime = PartialOrCompleteTimeInstant.builder()//
                .setNullablePartialTime(partialTime)//
                .setNullableCompleteTime(completeTime)//
                .build();

        final ZonedDateTime result = helper.resolveCompleteTime(partialOrCompleteTime, fileMetadata).orElse(null);
        assertThat(result).isEqualTo(expectedTime);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessagePopulatorHelperTest_testTryCompletePeriod.csv", numLinesToSkip = 1)
    void testTryCompletePeriod(
            final @Nullable PartialDateTime partialStartTime, final @Nullable ZonedDateTime completeStartTime, //
            final @Nullable PartialDateTime partialEndTime, final @Nullable ZonedDateTime completeEndTime, //
            final @Nullable PartialDateTime partialPrimaryReference, final @Nullable ZonedDateTime completePrimaryReference, //
            final String filename, final @Nullable Instant fileModified, final ZonedDateTime clock, //
            final @Nullable PartialDateTime expectedPartialStartTime, final @Nullable ZonedDateTime expectedCompleteStartTime, //
            final @Nullable PartialDateTime expectedPartialEndTime, final @Nullable ZonedDateTime expectedCompleteEndTime) {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.builder()//
                .setStartTime(partialOrCompleteTimeInstant(partialStartTime, completeStartTime))//
                .setEndTime(partialOrCompleteTimeInstant(partialEndTime, completeEndTime))//
                .build();
        final PartialOrCompleteTimeInstant primaryReference = partialOrCompleteTimeInstant(partialPrimaryReference, completePrimaryReference).orElse(null);
        final PartialOrCompleteTimePeriod expectedPeriod = PartialOrCompleteTimePeriod.builder()//
                .setStartTime(partialOrCompleteTimeInstant(expectedPartialStartTime, expectedCompleteStartTime))//
                .setEndTime(partialOrCompleteTimeInstant(expectedPartialEndTime, expectedCompleteEndTime))//
                .build();
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(Clock.fixed(clock.toInstant(), clock.getZone()));
        final FileMetadata fileMetadata = FILE_METADATA_TEMPLATE.toBuilder()//
                .mutateFileReference(ref -> ref.setFilename(filename))//
                .setNullableFileModified(fileModified)//
                .build();

        final PartialOrCompleteTimePeriod result = helper.tryCompletePeriod(period, primaryReference, fileMetadata);
        assertThat(result).isEqualTo(expectedPeriod);
    }
}
