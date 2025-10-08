package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;

/**
 * An {@code ObjectFactory} with restricted type parameter to ease configuration by type.
 *
 * @param <T> type of message populator
 */
public interface MessagePopulatorFactory<T extends MessagePopulator> extends ObjectFactory<T> {
}
