package fi.fmi.avi.archiver.logging.model;

import fi.fmi.avi.archiver.logging.AbstractAppendingLoggable;

public abstract class AbstractFileProcessingStatistics extends AbstractAppendingLoggable implements ReadableFileProcessingStatistics {
    private static final int FILE_STATISTICS_LENGTH_ESTIMATE = 4;
    private static final int STATISTICS_CATEGORIES_COUNT = 2; // message, bulletin
    private static final int STRING_LENGTH_ESTIMATE =
            (AbstractResultStatistics.STRING_LENGTH_ESTIMATE + 4) * STATISTICS_CATEGORIES_COUNT + FILE_STATISTICS_LENGTH_ESTIMATE;

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append("M{");
        getMessage().appendTo(builder);
        builder.append("} B{");
        getBulletin().appendTo(builder);
        builder.append("} F{")//
                .append(getFile().getAbbreviatedName())//
                .append("}");
    }

    @Override
    public int estimateLogStringLength() {
        return STRING_LENGTH_ESTIMATE;
    }

    public abstract static class AbstractResultStatistics extends AbstractAppendingLoggable implements ResultStatistics {
        private static final int STRING_LENGTH_PER_PROCESSING_RESULT_ESTIMATE = 3 /* control chars */ + 4 /* number */;
        static final int STRING_LENGTH_ESTIMATE = (ProcessingResult.getValues().size() + 1) * STRING_LENGTH_PER_PROCESSING_RESULT_ESTIMATE;

        @Override
        public void appendTo(final StringBuilder builder) {
            for (final ProcessingResult processingResult : ProcessingResult.getValues()) {
                final int amount = get(processingResult);
                if (amount > 0) {
                    builder.append(processingResult.getAbbreviatedName())//
                            .append(':')//
                            .append(amount)//
                            .append(',');
                }
            }
            builder//
                    .append("T:")//
                    .append(getTotal());
        }

        @Override
        public int estimateLogStringLength() {
            return STRING_LENGTH_ESTIMATE;
        }
    }
}
