package fi.fmi.avi.archiver.message;

/**
 * Status of message archival to database.
 */
public enum ArchivalStatus {
    /**
     * Archival has not been attempted yet.
     */
    PENDING,

    /**
     * Message was successfully archived to the main aviation messages table.
     */
    ARCHIVED,

    /**
     * Message was rejected and stored in the rejected messages table.
     */
    REJECTED,

    /**
     * Database insertion failed due to an error.
     */
    FAILED

}