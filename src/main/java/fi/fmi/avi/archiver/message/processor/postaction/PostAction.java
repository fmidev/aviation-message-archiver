package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.AbstractTypedConfigObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;

/**
 * {@code PostAction} is a component that runs an action <em>after</em> a message has been archived in the database.
 * It provides an extension point to hook arbitrary actions in the message processing chain.
 *
 * <p>
 * On each archived message the {@link PostActionService} invokes the
 * {@link #run(MessageProcessorContext, ArchiveAviationMessage)} method of all configured post-actions in an unspecified
 * order either sequentially or in parallel. The post-action will not be invoked on messages that have been either
 * <em>discarded</em> or <em>failed</em> in an earlier message processing phase (e.g.
 * {@link MessagePopulator message population phase}). In other words, only messages that have been stored successfully
 * in the database either as <em>archived</em> or <em>rejected</em> will be processed by post-actions. The archival
 * status can be determined from the {@link ArchiveAviationMessage#getArchivalStatus()} property.
 * </p>
 *
 * <p>
 * Some implementation considerations:
 * </p>
 *
 * <ul>
 *     <li>
 *         Post-actions must be thread-safe. Their execution order is not guaranteed and may be sequential or parallel.
 *     </li>
 *     <li>
 *         Design your {@code PostAction} to be configurable. For simple post-actions constructed by
 *         {@link ReflectionObjectFactory}, inject mandatory configuration values as constructor parameters, ordered
 *         after mandatory dependencies of post-action. Inject optional configuration values, that have a decent default
 *         value, through Java bean setter methods. For post actions that require a more complex setup, implement your
 *         factory extending {@link AbstractTypedConfigObjectFactory}, which supports typed configuration interfaces.
 *     </li>
 *     <li>
 *         Execution of post-actions for each message is isolated. If a post-action throws an exception, it will not
 *         affect the processing of any other message or any other post-action.
 *     </li>
 *     <li>
 *         The post-action should typically run unconditionally for reusability. When conditional execution is needed,
 *         it is recommended to compose a conditional instance with {@link ConditionalPostAction}. The application
 *         configuration supports declarative composition.
 *     </li>
 * </ul>
 */
public interface PostAction extends MessageProcessor {

    void run(MessageProcessorContext context, ArchiveAviationMessage message);

}
