package fi.fmi.avi.archiver.logging.model;

/**
 * An implementation of this interface records the processing results of a file, bulletins within a file and messages within bulletins. It produces a loggable
 * statistics output of the recorded data.
 *
 * <p>
 * When computing statistics, {@link ProcessingResult#getComparator()} considers a bulletin processing result to be the maximum of all the message results in
 * the bulletin and the explicitly recorded processing result for the bulletin itself.
 * Likewise, a file processing result is the maximum of all bulletin and message results of the file and the explicitly recorded file processing result for the file itself.
 * </p>
 */
public interface FileProcessingStatistics extends ReadableFileProcessingStatistics {

    /**
     * Return a {@code FileProcessingStatistics} that synchronizes each method call backed by the provided {@code fileProcessingStatistics} instance.
     *
     * @param fileProcessingStatistics
     *         the file processing statistics instance to be wrapped
     *
     * @return a {@code FileProcessingStatistics} that synchronizes each method call backed by the provided {@code fileProcessingStatistics} instance
     */
    @SuppressWarnings("ClassReferencesSubclass")
    static FileProcessingStatistics asSynchronized(final FileProcessingStatistics fileProcessingStatistics) {
        return fileProcessingStatistics instanceof SynchronizedFileProcessingStatistics
                ? fileProcessingStatistics
                : new SynchronizedFileProcessingStatistics(fileProcessingStatistics);
    }

    /**
     * Clear all recorded data.
     * This method returns this object to its initial state.
     */
    void clear();

    /**
     * Initialize this object to report the statistics for at least the provided {@code amount} of bulletins.
     * The processing result of each bulletin up to {@code amount} is initialized to {@link #INITIAL_PROCESSING_RESULT}, unless a processing result is already
     * recorded for the bulletin.
     *
     * @param amount
     *         minimum amount of bulletins to report statistics for
     */
    void initBulletins(int amount);

    /**
     * Initialize this object to report statistics for at least the provided {@code amount} of messages in bulletin at index {@code bulletinIndex}.
     * The processing result of each message up to {@code amount} is initialized to {@link #INITIAL_PROCESSING_RESULT}, unless a processing result is already
     * recorded for the message.
     *
     * @param bulletinIndex
     *         index of target bulletin starting from {@code 0}
     * @param amount
     *         minimum amount of messages to report statistics for
     */
    void initMessages(int bulletinIndex, int amount);

    /**
     * Record a result of message processing at the provided {@code bulletinIndex} and {@code messageIndex}.
     * If an earlier record for the message exists, the provided {@code processingResult} is recorded only if it is considered greater than
     * an existing processing result by the comparator returned from {@link ProcessingResult#getComparator()}.
     *
     * @param bulletinIndex
     *         index of bulletin within file starting from {@code 0}
     * @param messageIndex
     *         index of message within bulletin starting from {@code 0}
     * @param processingResult
     *         message processing result to record
     */
    void recordMessageResult(int bulletinIndex, int messageIndex, ProcessingResult processingResult);

    /**
     * Record the overall result of bulletin processing at provided {@code bulletinIndex}.
     * If an earlier record for the bulletin in question exists, the provided {@code processingResult} is recorded only if it is considered greater than
     * an existing processing result by the comparator returned from {@link ProcessingResult#getComparator()}.
     *
     * @param bulletinIndex
     *         index of bulletin within file starting from {@code 0}
     * @param processingResult
     *         bulletin processing result to record
     */
    void recordBulletinResult(int bulletinIndex, ProcessingResult processingResult);

    /**
     * Record the overall result of file processing.
     * If an earlier record for the file exists, the provided {@code processingResult} is recorded only if it is considered greater than an existing
     * processing result by the comparator returned from {@link ProcessingResult#getComparator()}.
     *
     * @param processingResult
     *         file processing result to record
     */
    void recordFileResult(ProcessingResult processingResult);

}
