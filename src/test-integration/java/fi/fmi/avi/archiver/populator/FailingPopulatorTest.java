package fi.fmi.avi.archiver.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.populator.FailingPopulatorTest"})
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, FailingPopulatorTest.FailingPopulatorConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class FailingPopulatorTest {

    private static final int WAIT_MILLIS = 100;
    private static final int TIMEOUT_MILLIS = 1000;
    private static final String FILENAME = "populator_fail_bulletin.txt";
    private static final String PRODUCT = "test_taf";

    @SpyBean(name = "successChannel")
    private MessageChannel successChannel;

    @SpyBean(name = "failChannel")
    private MessageChannel failChannel;

    @SpyBean
    private DatabaseAccess databaseAccess;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Captor
    private ArgumentCaptor<Message<?>> failChannelCaptor;

    @Captor
    private ArgumentCaptor<ArchiveAviationMessage> databaseMessageCaptor;

    @Test
    public void test_failing_populator() throws URISyntaxException, IOException, InterruptedException {
        final AviationProductsHolder.AviationProduct product = getProduct(aviationProductsHolder);
        Files.copy(getInputFile(), Paths.get(product.getInputDir().getPath() + "/" + FILENAME));
        waitUntilFileExists(new File(product.getFailDir().getPath() + "/" + FILENAME));

        verify(successChannel, times(0)).send(any(Message.class));
        verify(failChannel).send(failChannelCaptor.capture());
        @SuppressWarnings("unchecked")
        final List<InputAviationMessage> failures = (List<InputAviationMessage>) failChannelCaptor.getValue()
                .getHeaders()
                .get(MessageFileMonitorInitializer.FAILED_MESSAGES);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getMessage().getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME)).isEqualTo("EFYY");

        verify(databaseAccess).insertAviationMessage(databaseMessageCaptor.capture());
        assertThat(databaseMessageCaptor.getValue().getIcaoAirportCode()).isEqualTo("EFXX");
        verify(databaseAccess, times(0)).insertRejectedAviationMessage(any());
    }

    public Path getInputFile() throws URISyntaxException {
        final URL resource = requireNonNull(FailingPopulatorTest.class.getResource(FILENAME));
        final Path path = Paths.get(resource.toURI());
        assertThat(path).exists();
        return path;
    }

    public AviationProductsHolder.AviationProduct getProduct(final AviationProductsHolder holder) {
        return requireNonNull(holder.getProducts().get(PRODUCT), PRODUCT);
    }

    private void waitUntilFileExists(final File expectedOutputFile) throws InterruptedException {
        long totalWaitTime = 0;
        while (!expectedOutputFile.exists() && totalWaitTime < TIMEOUT_MILLIS) {
            Thread.sleep(WAIT_MILLIS);
            totalWaitTime += WAIT_MILLIS;
        }
    }

    @Configuration
    static class FailingPopulatorConfig {
        @Bean
        public MessagePopulator failingPopulator() {
            return (inputAviationMessage, aviationMessageBuilder) -> {
                final String airportIcaoCode = inputAviationMessage.getMessage()
                        .getLocationIndicators()
                        .get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
                if (airportIcaoCode.equals("EFYY")) {
                    throw new RuntimeException("fail");
                }
            };
        }
    }
}
