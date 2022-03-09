package fi.fmi.avi.archiver.logging.model;

import static fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult.NOTHING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.inferred.freebuilder.FreeBuilder;
import org.mockito.Mockito;

import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult;
import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ResultStatistics;

@FreeBuilder
public abstract class FileProcessingStatisticsSpec {
    private static final FileProcessingStatisticsSpec EMPTY = builder()//
            .setString("M{T:0} B{T:0} F{N}")//
            .build();

    public static Builder builder() {
        return new Builder();
    }

    public static FileProcessingStatisticsSpec from(final ReadableFileProcessingStatistics statistics) {
        return builder()//
                .copyFrom(statistics)//
                .build();
    }

    public static void assertEmpty(final ReadableFileProcessingStatistics statistics) {
        EMPTY.assertEquals(statistics);
    }

    public static void assertEquals(final Map<ProcessingResult, Integer> expected, final ResultStatistics actual) {
        assertThat(expected).isNotNull();
        assertThat(actual).isNotNull();
        assertSoftly(softly -> {
            ProcessingResult.getValues().forEach(processingResult -> //
                    softly.assertThat(actual.get(processingResult))//
                            .as(processingResult.toString())//
                            .isEqualTo(expected.getOrDefault(processingResult, 0)));
            softly.assertThat(actual.asMap()).as("map").isEqualTo(expected);
            softly.assertThat(actual.getTotal()).as("total").isEqualTo(expected.values().stream().mapToInt(i -> i).sum());
        });
    }

    public ReadableFileProcessingStatistics createMock() {
        final ReadableFileProcessingStatistics mock = Mockito.mock(ReadableFileProcessingStatistics.class);
        // create beforehand - Mockito cannot create/stub bulletinMock inside thenReturn() while stubbing mock.
        final ResultStatistics bulletinMock = createBulletinMock();
        final ResultStatistics messageMock = createMessageMock();

        when(mock.getFile()).thenReturn(getFile());
        when(mock.getBulletin()).thenReturn(bulletinMock);
        when(mock.getBulletinTotal()).thenReturn(getBulletinTotal());
        when(mock.getMessage()).thenReturn(messageMock);
        when(mock.getMessageTotal()).thenReturn(getMessageTotal());
        when(mock.getStructureName()).thenCallRealMethod();
        when(mock.readableCopy()).thenAnswer(invocation -> createMock());

        return mock;
    }

    public abstract ProcessingResult getFile();

    public abstract Map<ProcessingResult, Integer> getBulletin();

    public abstract int getBulletinTotal();

    public ResultStatistics createBulletinMock() {
        final ResultStatistics mock = Mockito.mock(ResultStatistics.class);
        getBulletin().forEach((processingResult, count) -> when(mock.get(processingResult)).thenReturn(count));
        when(mock.getTotal()).thenReturn(getBulletinTotal());
        when(mock.asMap()).thenReturn(getBulletin());
        when(mock.readableCopy()).thenAnswer(invocation -> createBulletinMock());
        when(mock.getStructureName()).thenCallRealMethod();
        return mock;
    }

    public abstract Map<ProcessingResult, Integer> getMessage();

    public ResultStatistics createMessageMock() {
        final ResultStatistics mock = Mockito.mock(ResultStatistics.class);
        getMessage().forEach((processingResult, count) -> when(mock.get(processingResult)).thenReturn(count));
        when(mock.getTotal()).thenReturn(getMessageTotal());
        when(mock.asMap()).thenReturn(getMessage());
        when(mock.readableCopy()).thenAnswer(invocation -> createMessageMock());
        when(mock.getStructureName()).thenCallRealMethod();
        return mock;
    }

    public abstract int getMessageTotal();

    public abstract String getString();

    public void assertEquals(final ReadableFileProcessingStatistics statistics) {
        assertThat(statistics).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(statistics.getFile()).as("file").isEqualTo(getFile());
            softly.assertThat(statistics.getBulletin()).as("bulletin").satisfies(stats -> assertEquals(getBulletin(), stats));
            softly.assertThat(statistics.getBulletin().getTotal()).as("bulletin.total").isEqualTo(getBulletinTotal());
            softly.assertThat(statistics.getBulletinTotal()).as("bulletinTotal").isEqualTo(getBulletinTotal());
            softly.assertThat(statistics.getMessage()).as("message").satisfies(stats -> assertEquals(getMessage(), stats));
            softly.assertThat(statistics.getMessage().getTotal()).as("message.total").isEqualTo(getMessageTotal());
            softly.assertThat(statistics.getMessageTotal()).as("messageTotal").isEqualTo(getMessageTotal());
            softly.assertThat(statistics.toString()).as("string").isEqualTo(getString());
        });
    }

    static class Builder extends FileProcessingStatisticsSpec_Builder {
        Builder() {
            setFile(NOTHING);
            setBulletinTotal(0);
            setMessageTotal(0);
        }

        public Builder copyFrom(final ReadableFileProcessingStatistics statistics) {
            setFile(statistics.getFile());
            putAllBulletin(statistics.getBulletin().asMap());
            setBulletinTotal(statistics.getBulletinTotal());
            putAllMessage(statistics.getMessage().asMap());
            setMessageTotal(statistics.getMessageTotal());
            setString(statistics.toString());
            return this;
        }

        public void assertEquals(final ReadableFileProcessingStatistics statistics) {
            build().assertEquals(statistics);
        }
    }
}
