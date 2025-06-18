package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.TestMessageProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class StationIcaoCodeReplacerTest {
    private final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());

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
