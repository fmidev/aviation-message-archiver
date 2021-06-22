package fi.fmi.avi.archiver.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({"auto.startup=false"})
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_message_types_test.sql",
        "classpath:/h2-data/avidb_message_format_test.sql", "classpath:/h2-data/avidb_message_routes_test.sql",
        "classpath:/h2-data/avidb_stations_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class,
        FailingPopulatorTest.FailingPopulatorConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class FailingPopulatorTest {

    private static final int WAIT_MILLIS = 100;
    private static final int TIMEOUT_MILLIS = 1000;
    private static final File BASE_DIR = new File(System.getProperty("java.io.tmpdir") + "/.avi-message-archiver");
    private static final File TMP_DIR = new File(BASE_DIR, "temp");
    private static final String FILENAME = "populator_fail_bulletin.txt";
    private static final String PRODUCT = "testProduct";

    @SpyBean(name = "successChannel")
    private MessageChannel successChannel;

    @SpyBean(name = "failChannel")
    private MessageChannel failChannel;

    @SpyBean
    private DatabaseAccess databaseAccess;

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @Captor
    private ArgumentCaptor<ArchiveAviationMessage> messageCaptor;

    @BeforeAll
    public static void startup() throws IOException {
        FileUtils.deleteDirectory(BASE_DIR);
        if (!TMP_DIR.mkdirs()) {
            throw new IllegalStateException("Cannot write to the temp folder of the system");
        }
    }

    @Test
    public void test_failing_populator() throws URISyntaxException, IOException, InterruptedException {
        AviationProductsHolder.AviationProduct product = getProduct(aviationProductsHolder);
        Files.copy(getInputFile(), Paths.get(product.getInputDir().getPath() + "/" + FILENAME));
        waitUntilFileExists(new File(product.getFailDir().getPath() + "/" + FILENAME));

        verify(successChannel, times(0)).send(any(Message.class));
        verify(failChannel).send(any(Message.class));
        verify(databaseAccess).insertAviationMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getIcaoAirportCode()).isEqualTo("EFXX");
        verify(databaseAccess, times(0)).insertRejectedAviationMessage(any());
    }

    public Path getInputFile() throws URISyntaxException {
        final URL resource = requireNonNull(FailingPopulatorTest.class.getResource(FILENAME));
        final Path path = Paths.get(resource.toURI());
        assertThat(path).exists();
        return path;
    }

    public AviationProductsHolder.AviationProduct getProduct(final AviationProductsHolder holder) {
        return holder.getProducts().stream()//
                .filter(aviationProduct -> aviationProduct.getId().equals(PRODUCT))//
                .findFirst()//
                .orElseThrow(IllegalStateException::new);
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
                final String airportIcaoCode = inputAviationMessage.getMessage().getLocationIndicators()
                        .get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
                if (airportIcaoCode.equals("EFYY")) {
                    throw new RuntimeException("fail");
                }
            };
        }
    }
}
