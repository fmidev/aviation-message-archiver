package fi.fmi.avi.archiver.logging.model;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.logging.AppendingLoggable;
import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.archiver.logging.model.ImmutableFileProcessingStatistics.ImmutableResultStatistics;

public interface ReadableFileProcessingStatistics extends AppendingLoggable, StructuredLoggable {
    ProcessingResult INITIAL_PROCESSING_RESULT = ProcessingResult.NOTHING;

    @Override
    default ReadableFileProcessingStatistics readableCopy() {
        return ImmutableFileProcessingStatistics.copyOf(this);
    }

    @Override
    default String getStructureName() {
        return FileProcessingStatisticsInternals.getStructureName();
    }

    ProcessingResult getFile();

    ResultStatistics getBulletin();

    default int getBulletinTotal() {
        return getBulletin().getTotal();
    }

    ResultStatistics getMessage();

    default int getMessageTotal() {
        return getMessage().getTotal();
    }

    /**
     * Result of processing a file, bulletin or a message.
     */
    enum ProcessingResult {
        /**
         * Item was not processed, and/or no final processing result was recorded.
         */
        NOTHING("N"),
        /**
         * Item was archived successfully.
         */
        ARCHIVED("A"),
        /**
         * Item was discarded during process.
         */
        DISCARDED("D"),
        /**
         * Item was rejected during process.
         */
        REJECTED("R"),
        /**
         * Processing of item failed.
         */
        FAILED("F");

        private static final List<FileProcessingStatistics.ProcessingResult> VALUES = Collections.unmodifiableList(
                Arrays.asList(ReadableFileProcessingStatistics.ProcessingResult.values()));

        private final String abbreviatedName;

        ProcessingResult(final String abbreviatedName) {
            this.abbreviatedName = requireNonNull(abbreviatedName, "abbreviatedName");
        }

        public static Comparator<FileProcessingStatistics.ProcessingResult> getComparator() {
            return Comparator.naturalOrder();
        }

        @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
        public static List<FileProcessingStatistics.ProcessingResult> getValues() {
            return VALUES;
        }

        String getAbbreviatedName() {
            return abbreviatedName;
        }
    }

    @JsonSerialize(converter = FileProcessingStatisticsInternals.ResultStatisticsToMapConverter.class)
    interface ResultStatistics extends AppendingLoggable, StructuredLoggable {
        int get(ProcessingResult processingResult);

        default int getTotal() {
            int total = 0;
            for (final ProcessingResult processingResult : ProcessingResult.getValues()) {
                total += get(processingResult);
            }
            return total;
        }

        default Map<ProcessingResult, Integer> asMap() {
            final Map<ProcessingResult, Integer> map = new EnumMap<>(ProcessingResult.class);
            for (final ProcessingResult processingResult : ProcessingResult.getValues()) {
                final int count = get(processingResult);
                if (count != 0) {
                    map.put(processingResult, count);
                }
            }
            return map;
        }

        @Override
        default ResultStatistics readableCopy() {
            return ImmutableResultStatistics.copyOf(this);
        }

        @Override
        default String getStructureName() {
            return "processingResultStatistics";
        }
    }
}
