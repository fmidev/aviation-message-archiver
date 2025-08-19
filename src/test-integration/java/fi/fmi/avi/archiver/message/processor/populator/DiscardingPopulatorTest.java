package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.AbstractMessagePopulatorFactoryConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.IntegrationFlowConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.util.TestFileUtil;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
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
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.processor.populator.DiscardingPopulatorTest"})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class,
        DiscardingPopulatorTest.DiscardingPopulatorConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles({"integration-test", "DiscardingPopulatorTest"})
public class DiscardingPopulatorTest {

    private static final String FILENAME = "populator_test_bulletin.txt";
    private static final String PRODUCT = "test_taf";

    @SpyBean(name = "successChannel")
    private MessageChannel successChannel;

    @SpyBean(name = "failChannel")
    private MessageChannel failChannel;

    @SpyBean
    private DatabaseAccess databaseAccess;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    @Captor
    private ArgumentCaptor<ArchiveAviationMessage> databaseMessageCaptor;

    @Autowired
    private Map<String, AviationProduct> aviationProducts;

    @Test
    void test_discarding_populator() throws URISyntaxException, IOException, InterruptedException {
        final AviationProduct product = aviationProducts.get(PRODUCT);
        Files.copy(TestFileUtil.getResourcePath(getClass(), FILENAME), product.getInputDir().resolve(FILENAME));
        TestFileUtil.waitUntilFileExists(product.getFailDir().resolve(FILENAME));

        verify(successChannel).send(messageCaptor.capture());
        @SuppressWarnings("unchecked") final List<InputAndArchiveAviationMessage> successes = (List<InputAndArchiveAviationMessage>) messageCaptor.getValue().getPayload();
        assertThat(successes).hasSize(1);
        assertThat(successes.getFirst().archiveMessage().getStationIcaoCode()).isEqualTo("EFXX");

        verify(failChannel, times(0)).send(any(Message.class));
        final boolean failures = IntegrationFlowConfig.hasProcessingErrors(messageCaptor.getValue().getHeaders());
        assertThat(failures).isFalse();

        verify(databaseAccess).insertAviationMessage(databaseMessageCaptor.capture(), any());
        assertThat(databaseMessageCaptor.getValue().getStationIcaoCode()).isEqualTo("EFXX");
        verify(databaseAccess, times(0)).insertRejectedAviationMessage(any(), any());
    }

    public static class DiscardingPopulator implements MessagePopulator {
        @Override
        public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) throws MessageDiscardedException {
            final String airportIcaoCode = context.getInputMessage()
                    .getMessage()
                    .getLocationIndicators()
                    .get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME);
            if (airportIcaoCode.equals("EFYY")) {
                throw new MessageDiscardedException("Discarded a message with location indicator: " + airportIcaoCode);
            }
        }
    }

    @Configuration
    @Profile({"DiscardingPopulatorTest"})
    static class DiscardingPopulatorConfig extends AbstractMessagePopulatorFactoryConfig {
        DiscardingPopulatorConfig(final ConfigValueConverter configValueConverter) {
            super(configValueConverter);
        }

        @Bean
        public MessagePopulatorFactory<DiscardingPopulator> discardingPopulatorFactory() {
            return build(builder(DiscardingPopulator.class));
        }
    }
}
