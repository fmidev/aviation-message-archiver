package fi.fmi.avi.archiver.message;

import fi.fmi.avi.archiver.logging.StructuredLoggable;
import fi.fmi.avi.model.bulletin.BulletinHeading;

/**
 * Result code of message processing, indicating successful processing or reason of message rejection.
 */
public enum ProcessingResult implements StructuredLoggable {
    /**
     * Message was processed successfully.
     */
    OK(0),
    /**
     * Message {@link ArchiveAviationMessage#getStationIcaoCode() station ICAO code} is unknown / not in the database.
     */
    UNKNOWN_STATION_ICAO_CODE(1),
    /**
     * Message {@link ArchiveAviationMessage#getMessageTime() time} is further in the future than allowed.
     */
    MESSAGE_TIME_IN_FUTURE(2),
    /**
     * Message {@link ArchiveAviationMessage#getMessageTime() time} is further in the past than allowed.
     */
    MESSAGE_TOO_OLD(3),
    /**
     * Message {@link ArchiveAviationMessage#getType() type} is not allowed in the context of this
     * {@link fi.fmi.avi.archiver.file.InputAviationMessage message}, {@link fi.fmi.avi.archiver.file.InputBulletinHeading heading} or
     * {@link fi.fmi.avi.archiver.config.model.AviationProduct product}.
     */
    FORBIDDEN_MESSAGE_TYPE(4),
    /**
     * Message {@link ArchiveAviationMessage#getStationIcaoCode() station ICAO code} is not allowed in the context of this
     * {@link fi.fmi.avi.archiver.file.InputAviationMessage message}, {@link fi.fmi.avi.archiver.file.InputBulletinHeading heading} or
     * {@link fi.fmi.avi.archiver.config.model.AviationProduct product}.
     */
    FORBIDDEN_MESSAGE_STATION_ICAO_CODE(5),
    /**
     * Message {@link BulletinHeading#getLocationIndicator() bulletin originator} is not allowed in the context of this
     * {@link fi.fmi.avi.archiver.file.InputAviationMessage message}, {@link fi.fmi.avi.archiver.file.InputBulletinHeading heading} or
     * {@link fi.fmi.avi.archiver.config.model.AviationProduct product}.
     */
    FORBIDDEN_BULLETIN_LOCATION_INDICATOR(6);

    private final int code;

    ProcessingResult(final int code) {
        this.code = code;
    }

    /**
     * Return the code representing this processing result / reject reason in the database.
     *
     * @return database code of processing result / reject reason
     */
    public int getCode() {
        return code;
    }

    @Override
    public int estimateLogStringLength() {
        return toString().length();
    }

    @Override
    public StructuredLoggable readableCopy() {
        return this;
    }

    @Override
    public String getStructureName() {
        return "rejectReason";
    }
}
