package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;

import java.util.function.Function;

/**
 * {@code MessagePopulator} is a component responsible for populating one or more properties of target {@code ArchiveAviationMessage} builder.
 * Each message populator focuses on a single responsibility, and together all configured populators construct the complete message entity to be archived.
 * {@code MessagePopulator} is the main extension point for customizing the message properties and content to be archived.
 *
 * <p>
 * On each parsed message the {@link MessagePopulationService} invokes the {@link #populate(MessageProcessorContext, ArchiveAviationMessage.Builder)} method
 * of all configured message populator instances in the configured order. A populator invoked later in the execution chain may then override values set by
 * previously invoked populators. All populators are always invoked, except when a populator throws an exception. In that case remaining populators in the
 * execution chain are skipped.
 * </p>
 *
 * <p>
 * Message populator is in the key position when deciding how the message will be handled. Message populator can signal message to be:
 * </p>
 *
 * <ul>
 *     <li>
 *         <strong>eligible for archival</strong> by setting {@link ArchiveAviationMessage.Builder#setProcessingResult(ProcessingResult) processing result}
 *         to {@link ProcessingResult#OK OK}. If a later populator does not change the processing result, the message will be stored in the messages database table.
 *     </li>
 *     <li>
 *         <strong>rejected</strong> by setting {@link ArchiveAviationMessage.Builder#setProcessingResult(ProcessingResult) processing result} to a
 *         non-{@code OK} value. If a later populator does not change the processing result, the message will be stored in the rejected messages database table.
 *     </li>
 *     <li>
 *         <strong>discarded</strong> by throwing {@link MessageDiscardedException}. No further populators will be invoked on the message. The discard will be
 *         logged at <em>info</em> level, and the message will <em>not</em> be stored in the database.
 *     </li>
 *     <li>
 *         <strong>failed</strong> by throwing any other {@link RuntimeException} than specified above. No further populators are invoked. The failure will
 *         be logged at <em>error</em> level, and the message will <em>not</em> be stored in the database.
 *     </li>
 * </ul>
 *
 * <p>
 * Therefore, instead of populating and/or modifying properties of the target builder object, it may alternatively act as a validator for it.
 * </p>
 *
 * <p>
 * Some implementation considerations:
 * </p>
 *
 * <ul>
 *     <li>
 *         Design your {@code MessagePopulator} to be responsible for a single aspect of message populating. Separate validation from populating functionality
 *         into a {@code MessagePopulator} implementation of its own, unless validation is tightly coupled with populating in your case.
 *     </li>
 *     <li>
 *         Design your {@code MessagePopulator} to be configurable. Inject mandatory configuration values as constructor parameters, ordered after mandatory
 *         dependencies of populator. Inject optional configuration values, that have a decent default value, through Java bean setter methods.
 *     </li>
 *     <li>
 *         Message populator may choose to use {@link MessageProcessorContext} as its input, or the properties already populated in the target builder,
 *         depending on the nature of the populator.
 *     </li>
 *     <li>
 *         Beware of unintentional exceptions. Any exception thrown by a populator is immediately considered as a failure of processing the message in question.
 *         <ul>
 *             <li>
 *                 If recovery from a possible error is safe, surround any unsafe operations with a try-catch block and handle exceptions properly.
 *             </li>
 *             <li>
 *                 Avoid invoking {@link ArchiveAviationMessage.Builder#build()} on the target builder. It may be in an unbuildable state. If a build is
 *                 unavoidable, always surround it within a try-catch block and handle exceptions properly.
 *             </li>
 *             <li>
 *                 Whenever accessing the target {@link ArchiveAviationMessage.Builder}, be always prepared for value being unset. It is recommended to use
 *                 {@link MessageProcessorHelper#tryGet(Object, Function)} and its variants.
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         Take advantage of utility methods in {@link MessagePopulatorHelper}.
 *     </li>
 *     <li>
 *         The populator should typically execute unconditionally for reusability. When conditional execution is needed, it is recommended to compose a
 *         conditional instance with {@link ConditionalMessagePopulator}. The application configuration supports declarative composition.
 *     </li>
 * </ul>
 */
public interface MessagePopulator extends MessageProcessor {
    /**
     * Populate selected properties of {@code target}.
     * See {@link MessagePopulator class description} for details.
     *
     * @param context context object providing input
     * @param target  target builder to populate
     * @throws MessageDiscardedException if the message is to be discarded
     * @throws NullPointerException      if any of provided parameters is {@code null}
     * @throws RuntimeException          in case of an error that prevents reliable recovery of message processing
     */
    void populate(final MessageProcessorContext context, ArchiveAviationMessage.Builder target) throws MessageDiscardedException;
}
