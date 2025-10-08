package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

/**
 * An {@code ObjectFactory} with restricted type parameter to ease configuration by type.
 *
 * @param <T> type of post-action
 */
public interface PostActionFactory<T extends PostAction> extends ObjectFactory<T> {
}
