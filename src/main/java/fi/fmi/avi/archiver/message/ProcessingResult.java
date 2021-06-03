package fi.fmi.avi.archiver.message;

public enum ProcessingResult {

    OK(0), UNKNOWN_ICAO_CODE(1), MESSAGE_TIME_IN_FUTURE(2), MESSAGE_TOO_OLD(3);

    private final int code;

    ProcessingResult(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
