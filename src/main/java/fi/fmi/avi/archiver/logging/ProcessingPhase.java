package fi.fmi.avi.archiver.logging;

public enum ProcessingPhase implements StructuredLoggable {
    START, //
    READ, //
    PARSE, //
    POPULATE, //
    STORE, //
    SUCCESS, //
    FAIL, //
    FINISH, //
    ;

    private static final String STRUCTURE_NAME = StructuredLoggables.defaultStructureName(ProcessingPhase.class);

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
        return STRUCTURE_NAME;
    }
}
