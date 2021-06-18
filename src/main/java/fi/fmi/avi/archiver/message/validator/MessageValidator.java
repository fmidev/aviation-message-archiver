package fi.fmi.avi.archiver.message.validator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface MessageValidator {

    /**
     * Validates an aviation message. Throws an exception if validation fails.
     *
     * @param message message
     */
    ArchiveAviationMessage.Builder validate(ArchiveAviationMessage.Builder message);

}
