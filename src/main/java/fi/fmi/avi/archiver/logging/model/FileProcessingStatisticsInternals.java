package fi.fmi.avi.archiver.logging.model;

import java.util.Map;

import com.fasterxml.jackson.databind.util.StdConverter;

import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ProcessingResult;
import fi.fmi.avi.archiver.logging.model.ReadableFileProcessingStatistics.ResultStatistics;

final class FileProcessingStatisticsInternals {
    private FileProcessingStatisticsInternals() {
        throw new AssertionError();
    }

    static String getStructureName() {
        return StructuredLoggable.defaultStructureName(FileProcessingStatistics.class);
    }

    static class ResultStatisticsToMapConverter extends StdConverter<ResultStatistics, Map<ProcessingResult, Integer>> {
        @Override
        public Map<ProcessingResult, Integer> convert(final ResultStatistics value) {
            return value.asMap();
        }
    }
}
