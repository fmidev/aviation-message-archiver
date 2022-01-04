package fi.fmi.avi.archiver.file;

import java.math.BigInteger;

public final class FileProcessingIdentifier {
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
    public String toString() {
        return idString;
    }
}
