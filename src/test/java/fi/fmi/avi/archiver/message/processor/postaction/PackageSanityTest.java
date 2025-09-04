package fi.fmi.avi.archiver.message.processor.postaction;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.util.GeneratedClasses;

import java.time.Clock;

import static org.mockito.Mockito.mock;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(SwimRabbitMQPublisher.MessageConfig.class, mock(SwimRabbitMQPublisher.MessageConfig.class));
    }

}
