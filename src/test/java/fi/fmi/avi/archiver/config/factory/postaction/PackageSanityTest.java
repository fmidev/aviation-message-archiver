package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQPublisherHealthIndicator;
import fi.fmi.avi.archiver.util.GeneratedClasses;

import java.time.Clock;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        final Clock clock = Clock.systemUTC();
        setDefault(Clock.class, clock);
        setDefault(RabbitMQPublisherHealthIndicator.class, new RabbitMQPublisherHealthIndicator(clock));
    }
}
