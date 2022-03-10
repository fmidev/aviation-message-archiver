package fi.fmi.avi.archiver.file;

import java.math.BigInteger;

import fi.fmi.avi.archiver.logging.StructuredLoggable;

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
