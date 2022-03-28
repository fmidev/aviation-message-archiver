package fi.fmi.avi.archiver.logging.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessagePositionInFile;

/**
 * Loggable context information on processing a file of aviation messages with methods to update current state and statistics.
 * It maintains information on file, bulletin within the file and message within the bulletin being under processing, and produces loggable output of this
 * information. It also holds a reference to file processing statistics.
 *
 * <p>
 * All bulletin and message indices start from {@code 0}.
 * </p>
 */
public interface LoggingContext extends ReadableLoggingContext {
    /**
     * Return a {@code LoggingContext} that synchronizes each method call backed by the provided {@code loggingContext} instance.
     * This makes the returned object thread-safe in terms of <em>serial</em> invocations from different threads and logging output. But it does
     * <strong>not</strong> guarantee safe <em>parallel</em> processing of different messages within a file.
     *
     * @param loggingContext
     *         the logging context to be wrapped
     *
     * @return a {@code LoggingContext} that synchronizes each method call backed by provided {@code loggingContext} instance
     */
    @SuppressWarnings("ClassReferencesSubclass")
    static LoggingContext asSynchronized(final LoggingContext loggingContext) {
        return loggingContext instanceof SynchronizedLoggingContext ? loggingContext : new SynchronizedLoggingContext(loggingContext);
    }

    /**
     * Register a file referred by the provided {@code file} reference as being processed.
     * Resets current {@link #leaveBulletin() bulletin} and {@link #leaveMessage() message} state.
     *
     * <p>
     * If the provided {@code file} is {@code null}, behavior is equivalent to {@link #leaveFile()}, latter being the preferred method for better readability.
     * See method description for details.
     * </p>
     *
     * @param file
     *         reference to the file currently under processing, or {@code null} to indicate end of processing
     */
    void enterFile(@Nullable FileReference file);

    /**
     * Register the processing of the currently registered file finished.
     * This completely clears the current state, resetting current file, {@link #leaveBulletin() bulletin} and {@link #leaveMessage() message}, forgets all
     * registered {@link #getAllBulletins() BulletinLogReference}s and {@link #getBulletinMessages() MessageLogReference}s, and
     * {@link FileProcessingStatistics#clear() clears statistics}.
     *
     * <p>
     * Equivalent to, and preferred over {@link #enterFile(FileReference) enterFile(null)} for better readability.
     * </p>
     *
     * <p>
     * The default implementation invokes {@code enterFile(null)}.
     * </p>
     */
    default void leaveFile() {
        enterFile(null);
    }

    /**
     * Register a bulletin referred by the provided {@code bulletin} reference as under processing.
     * A previously registered {@code BulletinLogReference} with the same {@link BulletinLogReference#getIndex() bulletin index} is replaced with the provided
     * {@code BulletinLogReference}.
     * Resets current {@link #leaveMessage() message} state.
     *
     * <p>
     * If the provided {@code bulletin} is {@code null}, behavior is equivalent to {@link #leaveBulletin()}, latter being the preferred method for better
     * readability. See method description for details.
     * </p>
     *
     * @param bulletin
     *         reference to the bulletin currently under processing, or {@code null} to indicate end of processing
     */
    void enterBulletin(@Nullable BulletinLogReference bulletin);

    /**
     * Register a bulletin at the provided {@code index} within the currently processed file,  starting from {@code 0}.
     * Resets the current {@link #leaveMessage() message} state.
     * When a {@link BulletinLogReference} is previously registered at the provided {@code index}, it is restored into the current state.
     * If no {@code BulletinLogReference} is previously registered at the provided {@code index} or at any of the preceding indices, a new instance containing only
     * the bulletin index will be registered at each previously unregistered index up to the provided {@code index}.
     *
     * <p>
     * If the provided {@code index} is {@code -1}, behavior is equivalent to {@link #leaveBulletin()}, latter being the preferred method for better
     * readability. See method description for details.
     * </p>
     *
     * @param index
     *         index of a bulletin within a file starting from {@code 0} currently under processing, or {@code -1} to indicate end of processing
     */
    void enterBulletin(int index);

    /**
     * Register the processing of the currently registered bulletin finished.
     * Resets current bulletin and {@link #leaveMessage() message} state.
     *
     * <p>
     * Equivalent to, and preferred over {@link #enterBulletin(BulletinLogReference) enterBulletin(null)} for better readability.
     * </p>
     *
     * <p>
     * The default implementation invokes {@code enterBulletin(null)}.
     * </p>
     */
    default void leaveBulletin() {
        enterBulletin(null);
    }

    /**
     * Return all registered {@code BulletinLogReference}s in order of appearance within a file.
     * The element index within the returned list matches the bulletin index within the file, and the list contains no {@code null} elements.
     *
     * <p>
     * The returned list should be unmodifiable, and may be an immutable copy or a live view of current state.
     * </p>
     *
     * @return all registered {@code BulletinLogReference}s
     */
    @JsonIgnore
    List<BulletinLogReference> getAllBulletins();

    /**
     * Modify the {@code BulletinLogReference} referring to the bulletin currently under processing, applying the provided {@code operator} on it.
     * If no bulletin is currently under processing, this method does nothing. Otherwise it registers the modified bulletin state and resets current
     * {@link #leaveMessage() message} state.
     * The provided {@code operator} should not modify the bulletin index.
     *
     * <p>
     * This is equivalent to
     * </p>
     * <pre><code>
     * getBulletin()
     *         .map(operator)
     *         .ifPresent(this::enterBulletin);
     * </code></pre>
     *
     * <p>
     * It is also the default implementation.
     * </p>
     *
     * @param operator
     *         operator to apply on the {@code BulletinLogReference} referring to the bulletin currently under processing
     *
     * @throws NullPointerException
     *         if {@code operator} is {@code null}
     */
    default void modifyBulletin(final UnaryOperator<BulletinLogReference> operator) {
        getBulletin()//
                .map(operator)//
                .ifPresent(this::enterBulletin);
    }

    /**
     * Register a message referred by the provided {@code message} reference as currently being processed within the current bulletin.
     * A previously registered {@code MessageLogReference} with same {@link MessageLogReference#getIndex() message index} is replaced with provided
     * {@code MessageLogReference}.
     * If no bulletin is registered to be under processing, this method will implicitly register a {@code BulletinLogReference} at index {@code 0} as
     * under processing.
     *
     * <p>
     * If the provided {@code message} is {@code null}, behavior is equivalent to {@link #leaveMessage()}, latter being the preferred method for better
     * readability. See method description for details.
     * </p>
     *
     * @param message
     *         reference to message being currently under processing, or {@code null} to indicate end of processing
     */
    void enterMessage(@Nullable MessageLogReference message);

    /**
     * Register a message as currently being processed at the provided {@code index} within the current bulletin starting from {@code 0}.
     * When a {@link MessageLogReference} is previously registered at the provided {@code index}, it is restored into the current state.
     * If no {@code MessageLogReference} is previously registered at the provided {@code index} or at any of preceding indices, a new instance containing only
     * the message index will be registered at each previously unregistered index up to the provided {@code index}.
     * If no bulletin is registered as under processing, this method will implicitly register a {@code BulletinLogReference} at index {@code 0} as
     * under processing.
     *
     * <p>
     * If the provided {@code index} is {@code -1}, behavior is equivalent to {@link #leaveMessage()}, latter being the preferred method for better readability.
     * See method description for details.
     * </p>
     *
     * @param index
     *         index of message within bulletin starting from {@code 0} being currently under processing, or {@code -1} to indicate end of processing
     */
    void enterMessage(int index);

    /**
     * Register a bulletin and a message referred by provided {@code MessagePositionInFile} as currently being processed.
     *
     * <p>
     * Note, that this method does not allow {@code null} value as a parameter, as it would be ambiguous whether it would indicate {@link #leaveMessage()} or
     * {@link #leaveBulletin()}.
     * </p>
     *
     * <p>
     * The default implementation invokes {@link #enterBulletin(int)} and {@link #enterMessage(int)} with indices in provided {@code messagePositionInFile}.
     * </p>
     *
     * @param messagePositionInFile
     *         message position in file being currently under processing
     *
     * @throws NullPointerException
     *         if {@code messagePositionInFile} is {@code null}
     */
    default void enterBulletinMessage(final MessagePositionInFile messagePositionInFile) {
        requireNonNull(messagePositionInFile, "messagePositionInFile");
        enterBulletin(messagePositionInFile.getBulletinIndex());
        enterMessage(messagePositionInFile.getMessageIndex());
    }

    /**
     * Register the processing of the currently registered message as finished.
     *
     * <p>
     * Equivalent to, and preferred over {@link #enterMessage(MessageLogReference) enterMessage(null)} for better readability.
     * </p>
     *
     * <p>
     * The default implementation invokes {@code enterMessage(null)}.
     * </p>
     */
    default void leaveMessage() {
        enterMessage(null);
    }

    /**
     * Return all registered {@code MessageLogReference}s of the bulletin being currently processed in order of appearance within the bulletin.
     * The element index within the returned list matches the message index within the bulletin, and the list contains no {@code null} elements.
     * If no bulletin is registered as under processing, an empty list is returned.
     *
     * <p>
     * The returned list should be unmodifiable, and may be an immutable copy or a live view of current state.
     * </p>
     *
     * @return all {@code MessageLogReference}s registered for bulletin being currently under processing, or an empty list
     */
    @JsonIgnore
    List<MessageLogReference> getBulletinMessages();

    /**
     * Modify the {@code MessageLogReference} referring to the message being currently processed, applying the provided {@code operator} on it.
     * If no message is currently under processing, this method does nothing. Otherwise it registers the modified message state.
     * The provided {@code operator} should not modify the message index.
     *
     * <p>
     * This is equivalent to
     * </p>
     * <pre><code>
     * getMessage()
     *         .map(operator)
     *         .ifPresent(this::enterMessage);
     * </code></pre>
     *
     * <p>
     * It is also the default implementation.
     * </p>
     *
     * @param operator
     *         operator to apply on the {@code MessageLogReference} referring to message being currently under processing
     *
     * @throws NullPointerException
     *         if {@code operator} is {@code null}
     */
    default void modifyMessage(final UnaryOperator<MessageLogReference> operator) {
        getMessage()//
                .map(operator)//
                .ifPresent(this::enterMessage);
    }

    /**
     * Return the instance of file processing statistics this context holds.
     *
     * @return file processing statistics of this context
     */
    @JsonIgnore
    FileProcessingStatistics getStatistics();

    /**
     * Initialize statistics for all registered bulletins and messages.
     * In general this method invokes {@link FileProcessingStatistics#initBulletins(int)} for all registered bulletins and
     * {@link FileProcessingStatistics#initMessages(int, int)} for all registered messages within each bulletin.
     */
    void initStatistics();

    /**
     * Record the processing result of the current state in {@link #getStatistics() file processing statistics}.
     * When both bulletin and message are registered in the current state, the provided {@code processingResult} is recorded for the
     * {@link FileProcessingStatistics#recordMessageResult(int, int, FileProcessingStatistics.ProcessingResult) message}. If only a bulletin is registered
     * in the current state, the provided {@code processingResult} is recorded for the
     * {@link FileProcessingStatistics#recordBulletinResult(int, FileProcessingStatistics.ProcessingResult) bulletin}. Otherwise the provided
     * {@code processingResult} is recorded for the {@link FileProcessingStatistics#recordFileResult(FileProcessingStatistics.ProcessingResult) file}.
     *
     * <p>
     * The default implementation tests the current state of a bulletin and a message invoking {@link #getBulletinIndex()} and {@link #getMessageIndex()}.
     * </p>
     *
     * @param processingResult
     *         processing result to record on current state
     */
    default void recordProcessingResult(final FileProcessingStatistics.ProcessingResult processingResult) {
        requireNonNull(processingResult, "processingResult");
        final int bulletinIndex = getBulletinIndex();
        if (bulletinIndex < 0) {
            getStatistics().recordFileResult(processingResult);
        } else {
            final int messageIndex = getMessageIndex();
            if (messageIndex < 0) {
                getStatistics().recordBulletinResult(bulletinIndex, processingResult);
                leaveBulletin();
            } else {
                getStatistics().recordMessageResult(bulletinIndex, messageIndex, processingResult);
                leaveMessage();
            }
        }
    }
}
