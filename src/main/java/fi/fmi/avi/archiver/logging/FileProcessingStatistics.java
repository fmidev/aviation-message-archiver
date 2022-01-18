package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An implementation of this interface records processing results of file, bulletins within file and messages within bulletins, and produces a loggable
 * statistics output of recorded data.
 *
 * <p>
 * When computing statistics, bulletin status is considered as maximum by {@link Status#getComparator()} of all message statuses in bulletin and status
 * explicitly recorded for the bulletin in question. Likewise, file status is considered as maximum of all bulletin and message statuses in file and status
 * explicitly recorded for the file.
 * </p>
 */
public interface FileProcessingStatistics extends AppendingLoggable {
    Status INITIAL_STATUS = Status.NOTHING;

    /**
     * Return a {@code FileProcessingStatistics} that synchronizes each method call backed by provided {@code fileProcessingStatistics} instance.
     *
     * @param fileProcessingStatistics
     *         the file processing statistics instance to be wrapped
     *
     * @return a {@code FileProcessingStatistics} that synchronizes each method call backed by provided {@code fileProcessingStatistics} instance
     */
    @SuppressWarnings("ClassReferencesSubclass")
    static FileProcessingStatistics asSynchronized(final FileProcessingStatistics fileProcessingStatistics) {
        return fileProcessingStatistics instanceof SynchronizedFileProcessingStatistics
                ? fileProcessingStatistics
                : new SynchronizedFileProcessingStatistics(fileProcessingStatistics);
    }

    /**
     * Clear all data recorded.
     * This method returns this object to its initial state.
     */
    void clear();

    /**
     * Initialize this object to report statistics for at least provided {@code amount} of bulletins.
     * Status of each bulletin up to {@code amount} is initialized to {@link #INITIAL_STATUS}, unless a status is already recorded for a bulletin.
     *
     * @param amount
     *         minimum amount of bulletins to report statistics for
     */
    void initBulletins(int amount);

    /**
     * Initialize this object to report statistics for at least provided {@code amount} of messages in bulletin at index {@code bulletinIndex}.
     * Status of each message up to {@code amount} is initialized to {@link #INITIAL_STATUS}, unless a status is already recorded for a message.
     *
     * @param bulletinIndex
     *         index of target bulletin starting from {@code 0}
     * @param amount
     *         minimum amount of messages to report statistics for
     */
    void initMessages(int bulletinIndex, int amount);

    /**
     * Record status of message processing at provided {@code bulletinIndex} and {@code messageIndex}.
     * If an earlier record for the message in question exists, the provided {@code status} is recorded only, if it is considered greater than existing
     * status by the comparator returned from {@link Status#getComparator()}.
     *
     * @param bulletinIndex
     *         index of bulletin within file starting from {@code 0}
     * @param messageIndex
     *         index of message within bulletin starting from {@code 0}
     * @param status
     *         message processing status to record
     */
    void recordMessageStatus(int bulletinIndex, int messageIndex, Status status);

    /**
     * Record overall status of bulletin processing at provided {@code bulletinIndex}.
     * If an earlier record for the bulletin in question exists, the provided {@code status} is recorded only, if it is considered greater than existing
     * status by the comparator returned from {@link Status#getComparator()}.
     *
     * @param bulletinIndex
     *         index of bulletin within file starting from {@code 0}
     * @param status
     *         bulletin processing status to record
     */
    void recordBulletinStatus(int bulletinIndex, Status status);

    /**
     * Record overall status of file processing.
     * If an earlier record for the file exists, the provided {@code status} is recorded only, if it is considered greater than existing status by the
     * comparator returned from {@link Status#getComparator()}.
     *
     * @param status
     *         file processing status to record
     */
    void recordFileStatus(Status status);

    /**
     * Result of processing a file, bulletin or a message.
     */
    enum Status {
        /**
         * Item was not processed, and/or no final status was recorded.
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

        private static final List<Status> VALUES = Collections.unmodifiableList(Arrays.asList(Status.values()));

        private final String abbreviatedName;

        Status(final String abbreviatedName) {
            this.abbreviatedName = requireNonNull(abbreviatedName, "abbreviatedName");
        }

        public static Comparator<Status> getComparator() {
            return Comparator.naturalOrder();
        }

        @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
        public static List<Status> getValues() {
            return VALUES;
        }

        String getAbbreviatedName() {
            return abbreviatedName;
        }
    }
}
