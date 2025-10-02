package fi.fmi.avi.archiver.config.factory.postaction;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.message.processor.postaction.AbstractRetryingPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import fi.fmi.avi.archiver.util.GeneratedClasses;
import org.springframework.retry.support.RetryTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.mock;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(AbstractRetryingPostAction.RetryParams.class, new AbstractRetryingPostAction.RetryParams(
                mock(ThreadPoolExecutor.class), Duration.ZERO, RetryTemplate.defaultInstance()));
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(SwimRabbitMQPublisher.MessageConfig.class, mock(SwimRabbitMQPublisher.MessageConfig.class));
    }
}
