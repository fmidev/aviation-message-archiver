package fi.fmi.avi.archiver.message.validator;

import fi.fmi.avi.archiver.message.AviationMessage;

@FunctionalInterface
public interface MessageValidator {

    /**
     * Validates an aviation message. Throws an exception if validation fails.
     *
     * @param message message
     */
    AviationMessage.Builder validate(AviationMessage.Builder message);

}
