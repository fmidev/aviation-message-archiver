package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.IntegrationFlowConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
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
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({ "auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.populator.FailingPopulatorTest" })
@Sql(scripts = { "classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = { AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigDataApplicationContextInitializer.class })
@ActiveProfiles({"integration-test", "failingPopulatorTest"})
class FailingPopulatorTest {

    private static final int WAIT_MILLIS = 100;
    private static final int TIMEOUT_MILLIS = 1000;
    private static final String FILENAME = "populator_test_bulletin.txt";
    private static final String PRODUCT = "test_taf";

    @SpyBean(name = "successChannel")
    private MessageChannel successChannel;

    @SpyBean(name = "failChannel")
    private MessageChannel failChannel;

    @SpyBean
    private DatabaseAccess databaseAccess;

    @Captor
    private ArgumentCaptor<Message<?>> failChannelCaptor;

    @Captor
    private ArgumentCaptor<ArchiveAviationMessage> databaseMessageCaptor;

    @Autowired
    private Map<String, AviationProduct> aviationProducts;

    @Test
    void test_failing_populator() throws URISyntaxException, IOException, InterruptedException {
        final AviationProduct product = getProduct(aviationProducts);
        Files.copy(getInputFile(), product.getInputDir().resolve(FILENAME));
        waitUntilFileExists(product.getFailDir().resolve(FILENAME));

        verify(successChannel, times(0)).send(any(Message.class));
        verify(failChannel).send(failChannelCaptor.capture());
        final boolean failures = IntegrationFlowConfig.hasProcessingErrors(failChannelCaptor.getValue().getHeaders());
        assertThat(failures).isTrue();

        verify(databaseAccess).insertAviationMessage(databaseMessageCaptor.capture(), any());
        assertThat(databaseMessageCaptor.getValue().getStationIcaoCode()).isEqualTo("EFXX");
        verify(databaseAccess, times(0)).insertRejectedAviationMessage(any(), any());
    }

    public Path getInputFile() throws URISyntaxException {
        final URL resource = requireNonNull(FailingPopulatorTest.class.getResource(FILENAME));
        final Path path = Paths.get(resource.toURI());
        assertThat(path).exists();
        return path;
    }

    public AviationProduct getProduct(final Map<String, AviationProduct> aviationProducts) {
        return requireNonNull(aviationProducts.get(PRODUCT), PRODUCT);
    }

    private void waitUntilFileExists(final Path expectedOutputFile) throws InterruptedException {
        long totalWaitTime = 0;
        while (!Files.exists(expectedOutputFile) && totalWaitTime < TIMEOUT_MILLIS) {
            //noinspection BusyWait
            Thread.sleep(WAIT_MILLIS);
            totalWaitTime += WAIT_MILLIS;
        }
    }

    public static class FailingPopulator implements MessagePopulator {
        @Override
        public void populate(final MessagePopulatingContext context, final ArchiveAviationMessage.Builder target) {
            final String airportIcaoCode = context.getInputMessage()
                    .getMessage()
                    .getLocationIndicators()
                    .get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
            if (airportIcaoCode.equals("EFYY")) {
                throw new RuntimeException("fail");
            }
        }
    }

    @Configuration
    @Profile("failingPopulatorTest")
    static class FailingPopulatorConfig {
        @Bean
        public MessagePopulatorFactory<FailingPopulator> failingPopulatorFactory(final ConfigValueConverter messagePopulatorConfigValueConverter) {
            return new MessagePopulatorFactory<>(ReflectionObjectFactory.builder(FailingPopulator.class, messagePopulatorConfigValueConverter).build());
        }
    }
}
