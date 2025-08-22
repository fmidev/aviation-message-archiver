package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.ProcessingServiceContext;
import fi.fmi.avi.archiver.config.AbstractMessagePopulatorFactoryConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.config.util.SpringProcessingServiceContextHelper;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.processor.populator.FailingPopulatorTest"})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles({"integration-test", "FailingPopulatorTest"})
class FailingPopulatorTest {

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
        final AviationProduct product = aviationProducts.get(PRODUCT);
        Files.copy(TestFileUtil.getResourcePath(getClass(), FILENAME), product.getInputDir().resolve(FILENAME));
        TestFileUtil.waitUntilFileExists(product.getFailDir().resolve(FILENAME));

        verify(successChannel, times(0)).send(any(Message.class));
        verify(failChannel).send(failChannelCaptor.capture());
        final ProcessingServiceContext processingServiceContext = SpringProcessingServiceContextHelper.getProcessingServiceContext(failChannelCaptor.getValue().getHeaders());
        assertThat(processingServiceContext.isProcessingErrors()).isTrue();

        verify(databaseAccess).insertAviationMessage(databaseMessageCaptor.capture(), any());
        assertThat(databaseMessageCaptor.getValue().getStationIcaoCode()).isEqualTo("EFXX");
        verify(databaseAccess, times(0)).insertRejectedAviationMessage(any(), any());
    }

    public static class FailingPopulator implements MessagePopulator {
        @Override
        public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
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
    @Profile({"FailingPopulatorTest"})
    static class FailingPopulatorConfig extends AbstractMessagePopulatorFactoryConfig {
        FailingPopulatorConfig(final ConfigValueConverter configValueConverter) {
            super(configValueConverter);
        }

        @Bean
        public MessagePopulatorFactory<FailingPopulator> failingPopulatorFactory() {
            return build(builder(FailingPopulator.class));
        }
    }
}
