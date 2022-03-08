package fi.fmi.avi.archiver.logging;

import java.util.Map;

import com.fasterxml.jackson.databind.util.StdConverter;

import fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ProcessingResult;
import fi.fmi.avi.archiver.logging.ReadableFileProcessingStatistics.ResultStatistics;

final class FileProcessingStatisticsInternals {
    private FileProcessingStatisticsInternals() {
        throw new AssertionError();
    }

    static String getStructureName() {
        return StructuredLoggables.defaultStructureName(FileProcessingStatistics.class);
    }

    static class ResultStatisticsToMapConverter extends StdConverter<ResultStatistics, Map<ProcessingResult, Integer>> {
        @Override
        public Map<ProcessingResult, Integer> convert(final ResultStatistics value) {
            return value.asMap();
        }
    }
}
