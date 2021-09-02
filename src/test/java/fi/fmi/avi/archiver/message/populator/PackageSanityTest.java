package fi.fmi.avi.archiver.message.populator;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.springframework.messaging.MessageHeaders;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
        setDefault(MessageHeaders.class, new MessageHeaders(Collections.emptyMap()));
        setDefault(DatabaseAccess.class, mock(DatabaseAccess.class));
        setDistinctValues(Instant.class, Instant.now(), Instant.now().plusSeconds(1));
    }

}
