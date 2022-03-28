package fi.fmi.avi.archiver.file;

import java.math.BigInteger;

import fi.fmi.avi.archiver.logging.StructuredLoggable;

/**
 * A unique identifier of a single file processing.
 *
 * <p><strong>Specification:</strong></p>
 *
 * <ul>
 *     <li>
 *         It is a sequence of digits (0-9) and letters (a-z).
 *     </li>
 *     <li>
 *         This identifier is guaranteed to be unique within a single Java virtual machine, and between different Java virtual machines that are not running
 *         in parallel at the same time.
 *     </li>
 * </ul>
 *
 * <p><strong>Implementation characteristics, though not guaranteed features:</strong></p>
 *
 * <ul>
 *     <li>
 *         Typical length of string representation is 15 characters.
 *     </li>
 *     <li>
 *         String representations of identifiers are naturally ascending based on their creation time.
 *     </li>
 * </ul>
 */
public final class FileProcessingIdentifier implements StructuredLoggable {
    private static final String STRUCTURE_NAME = "processingId";
    private final String idString = newIdString();

    private FileProcessingIdentifier() {
    }

    public static FileProcessingIdentifier newInstance() {
        return new FileProcessingIdentifier();
    }

    private String newIdString() {
        return BigInteger.valueOf(System.currentTimeMillis())//
                .shiftLeft(Integer.SIZE)//
                .or(BigInteger.valueOf(System.identityHashCode(this)))//
                .toString(Character.MAX_RADIX);
    }

    @Override
    public int estimateLogStringLength() {
        return idString.length();
    }

    @Override
    public String toString() {
        return idString;
    }

    @Override
    public StructuredLoggable readableCopy() {
        return this;
    }

    @Override
    public String getStructureName() {
        return STRUCTURE_NAME;
    }
}
