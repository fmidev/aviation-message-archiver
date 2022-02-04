package fi.fmi.avi.archiver.message.populator;

import static fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

class MessagePopulatorHelperTest {
    private static final FileMetadata FILE_METADATA_TEMPLATE = FileMetadata.builder()//
            .setFileReference(FileReference.create("testproduct", "null"))//
            .mutateFileConfig(fileConfig -> fileConfig//
                    .setFormat(MessagePopulatorTests.FormatId.TAC.getFormat())//
                    .setFormatId(MessagePopulatorTests.FormatId.TAC.getId())//
                    .setPattern(MessagePopulatorTests.FILE_NAME_PATTERN)//
                    .setNameTimeZone(ZoneOffset.UTC))//
            .buildPartial();

    private static Optional<PartialOrCompleteTimeInstant> partialOrCompleteTimeInstant(@Nullable final PartialDateTime partialTime,
            @Nullable final ZonedDateTime completeTime) {
        if (partialTime == null && completeTime == null) {
            return Optional.empty();
        }
        return Optional.of(PartialOrCompleteTimeInstant.builder()//
                .setNullablePartialTime(partialTime)//
                .setNullableCompleteTime(completeTime)//
                .build());
    }

    @Test
    void defaultBulletinHeadingSources_is_not_empty() {
        assertThat(DEFAULT_BULLETIN_HEADING_SOURCES).isNotEmpty();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessagePopulatorHelperTest_testResolveCompleteTime.csv", numLinesToSkip = 1)
    void testResolveCompleteTime(@Nullable final PartialDateTime partialTime, @Nullable final ZonedDateTime completeTime, final String filename,
            @Nullable final Instant fileModified, final ZonedDateTime clock, @Nullable final ZonedDateTime expectedTime) {
        final MessagePopulatorHelper helper = new MessagePopulatorHelper(Clock.fixed(clock.toInstant(), clock.getZone()));
        final FileMetadata fileMetadata = FILE_METADATA_TEMPLATE.toBuilder()//
                .mutateFileReference(ref -> ref.setFilename(filename))//
                .setNullableFileModified(fileModified)//
                .build();
        final PartialOrCompleteTimeInstant partialOrCompleteTime = PartialOrCompleteTimeInstant.builder()//
                .setNullablePartialTime(partialTime)//
                .setNullableCompleteTime(completeTime)//
                .build();

        @Nullable
        final ZonedDateTime result = helper.resolveCompleteTime(partialOrCompleteTime, fileMetadata).orElse(null);
        assertThat(result).isEqualTo(expectedTime);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "MessagePopulatorHelperTest_testTryCompletePeriod.csv", numLinesToSkip = 1)
    void testTryCompletePeriod(//
            @Nullable final PartialDateTime partialStartTime, @Nullable final ZonedDateTime completeStartTime, //
            @Nullable final PartialDateTime partialEndTime, @Nullable final ZonedDateTime completeEndTime, //
            @Nullable final PartialDateTime partialPrimaryReference, @Nullable final ZonedDateTime completePrimaryReference, //
            final String filename, @Nullable final Instant fileModified, final ZonedDateTime clock, //
            @Nullable final PartialDateTime expectedPartialStartTime, @Nullable final ZonedDateTime expectedCompleteStartTime, //
            @Nullable final PartialDateTime expectedPartialEndTime, @Nullable final ZonedDateTime expectedCompleteEndTime) {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.builder()//
                .setStartTime(partialOrCompleteTimeInstant(partialStartTime, completeStartTime))//
                .setEndTime(partialOrCompleteTimeInstant(partialEndTime, completeEndTime))//
                .build();
        @Nullable
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

        @Nullable
        final PartialOrCompleteTimePeriod result = helper.tryCompletePeriod(period, primaryReference, fileMetadata);
        assertThat(result).isEqualTo(expectedPeriod);
    }
}
