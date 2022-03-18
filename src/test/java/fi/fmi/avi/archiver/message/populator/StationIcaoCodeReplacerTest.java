package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class StationIcaoCodeReplacerTest {
    private final MessagePopulatingContext context = TestMessagePopulatingContext.create(InputAviationMessage.builder().buildPartial());

    private StationIcaoCodeReplacer stationIcaoCodeReplacer;

    @BeforeEach
    public void setUp() {
        stationIcaoCodeReplacer = new StationIcaoCodeReplacer(Pattern.compile("^YU..$"), "XXXX");
    }

    @Test
    void replace() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder().setStationIcaoCode("YUDO");
        stationIcaoCodeReplacer.populate(context, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("XXXX");
    }

    @Test
    void no_match() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder().setStationIcaoCode("YADO");
        stationIcaoCodeReplacer.populate(context, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("YADO");
    }

    @Test
    void backreference_capture_group() {
        stationIcaoCodeReplacer = new StationIcaoCodeReplacer(Pattern.compile("^YU(..)$"), "XX$1");
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder().setStationIcaoCode("XXDO");
        stationIcaoCodeReplacer.populate(context, aviationMessage);
        assertThat(aviationMessage.getStationIcaoCode()).isEqualTo("XXDO");
    }

}
