package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class IcaoAirportCodeReplacerTest {

    private IcaoAirportCodeReplacer icaoAirportCodeReplacer;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        icaoAirportCodeReplacer = new IcaoAirportCodeReplacer();
        icaoAirportCodeReplacer.setPattern(Pattern.compile("^YU..$"));
        icaoAirportCodeReplacer.setReplacement("XXXX");
    }

    @Test
    public void replace() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setIcaoAirportCode("YUDO");
        icaoAirportCodeReplacer.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getIcaoAirportCode()).isEqualTo("XXXX");
    }

    @Test
    public void no_match() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setIcaoAirportCode("YADO");
        icaoAirportCodeReplacer.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getIcaoAirportCode()).isEqualTo("YADO");
    }

}
