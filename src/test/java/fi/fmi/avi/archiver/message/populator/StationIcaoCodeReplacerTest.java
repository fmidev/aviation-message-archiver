package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class StationIcaoCodeReplacerTest {

    private StationIcaoCodeReplacer stationIcaoCodeReplacer;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        stationIcaoCodeReplacer = new StationIcaoCodeReplacer(Pattern.compile("^YU..$"), "XXXX");
    }

    @Test
    public void replace() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setStationIcaoCode("YUDO");
        stationIcaoCodeReplacer.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("XXXX");
    }

    @Test
    public void no_match() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setStationIcaoCode("YADO");
        stationIcaoCodeReplacer.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("YADO");
    }

    @Test
    public void backreference_capture_group() {
        stationIcaoCodeReplacer = new StationIcaoCodeReplacer(Pattern.compile("^YU(..)$"), "XX$1");
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setStationIcaoCode("XXDO");
        stationIcaoCodeReplacer.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("XXDO");
    }

}
