package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult.ARCHIVED;
import static fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult.DISCARDED;
import static fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult.FAILED;
import static fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult.NOTHING;
import static fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.logging.ImmutableFileProcessingStatistics.ImmutableResultStatistics;
import fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult;
import fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ResultStatistics;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC")
class ImmutableFileProcessingStatisticsTest {
    private static final FileProcessingStatisticsSpec SPEC = FileProcessingStatisticsSpec.builder()//
            .setFile(ARCHIVED)//
            .putBulletin(NOTHING, 1)//
            .putBulletin(ARCHIVED, 2)//
            .putBulletin(DISCARDED, 3)//
            .putBulletin(REJECTED, 4)//
            .putBulletin(FAILED, 5)//
            .setBulletinTotal(15)//
            .putMessage(NOTHING, 11)//
            .putMessage(ARCHIVED, 12)//
            .putMessage(DISCARDED, 13)//
            .putMessage(REJECTED, 14)//
            .putMessage(FAILED, 15)//
            .setMessageTotal(65)//
            .setString("M{N:11,A:12,D:13,R:14,F:15,T:65} B{N:1,A:2,D:3,R:4,F:5,T:15} F{A}")//
            .build();

    @Test
    void create_returns_instance() {
        final ImmutableFileProcessingStatistics statistics = ImmutableFileProcessingStatistics.create(//
                SPEC.getFile(), //
                SPEC.createBulletinMock(), //
                SPEC.createMessageMock());

        SPEC.assertEquals(statistics);
    }

    @Test
    void copyOf_given_ReadableFileProcessingStatistics_returns_immutable_copy() {
        final ImmutableFileProcessingStatistics statistics = ImmutableFileProcessingStatistics.copyOf(SPEC.createMock());

        SPEC.assertEquals(statistics);
    }

    @Test
    void copyOf_given_ImmutableFileProcessingStatistics_returns_same_instance() {
        final ImmutableFileProcessingStatistics original = ImmutableFileProcessingStatistics.create(//
                SPEC.getFile(), //
                SPEC.createBulletinMock(), //
                SPEC.createMessageMock());

        final ImmutableFileProcessingStatistics copy = ImmutableFileProcessingStatistics.copyOf(original);

        assertThat(copy).isSameAs(original);
    }

    @Test
    void readableCopy_returns_same_instance() {
        final ImmutableFileProcessingStatistics original = ImmutableFileProcessingStatistics.create(//
                SPEC.getFile(), //
                SPEC.createBulletinMock(), //
                SPEC.createMessageMock());

        final ReadableFileProcessingStatistics copy = original.readableCopy();

        assertThat(copy).isSameAs(original);
    }

    @Nested
    class ImmutableResultStatisticsTest {
        @Test
        void testCompute() {
            final List<ProcessingResult> input = Arrays.asList(//
                    NOTHING, //
                    ARCHIVED, ARCHIVED, //
                    DISCARDED, DISCARDED, DISCARDED, //
                    REJECTED, REJECTED, REJECTED, REJECTED, //
                    FAILED, FAILED, FAILED, FAILED, FAILED);

            final ImmutableResultStatistics result = ImmutableResultStatistics.compute(input.iterator());

            final Map<ProcessingResult, Integer> expected = new EnumMap<>(ProcessingResult.class);
            expected.put(NOTHING, 1);
            expected.put(ARCHIVED, 2);
            expected.put(DISCARDED, 3);
            expected.put(REJECTED, 4);
            expected.put(FAILED, 5);
            FileProcessingStatisticsSpec.assertEquals(expected, result);
        }

        @Test
        void testCopyOf() {
            final Map<ProcessingResult, Integer> expected = SPEC.getMessage();
            final ResultStatistics input = SPEC.createMessageMock();

            final ImmutableResultStatistics result = ImmutableResultStatistics.copyOf(input);

            FileProcessingStatisticsSpec.assertEquals(expected, result);
        }

        @Test
        void testEmpty() {
            FileProcessingStatisticsSpec.assertEquals(Collections.emptyMap(), ImmutableResultStatistics.empty());
        }

        @Test
        void readableCopy_returns_same_instance() {
            final ImmutableResultStatistics original = ImmutableResultStatistics.compute(ProcessingResult.getValues().iterator());

            final ResultStatistics copy = original.readableCopy();

            assertThat(copy).isSameAs(original);
        }
    }
}
