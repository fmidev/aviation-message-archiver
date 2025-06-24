package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.AbstractPostActionFactoryConfig;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.IntegrationFlowConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.util.TestFileUtil;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.processor.postaction.FailingPostActionTest"})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles({"integration-test", "FailingPostActionTest"})
class FailingPostActionTest {

    private static final String FILENAME = "postaction_test_bulletin.txt";
    private static final String PRODUCT = "test_taf";
    private static final String POST_ACTION_ID = "failing";

    @SpyBean(name = "successChannel")
    private MessageChannel successChannel;

    @SpyBean(name = "failChannel")
    private MessageChannel failChannel;

    @Captor
    private ArgumentCaptor<Message<?>> successChannelCaptor;

    @Autowired
    private Map<String, AviationProduct> aviationProducts;

    @Autowired
    private TestPostActionRegistry postActionRegistry;

    @BeforeEach
    void setUp() {
        postActionRegistry.resetAll();
    }

    @Test
    void test_failing_post_action() throws URISyntaxException, IOException, InterruptedException {
        final AviationProduct product = aviationProducts.get(PRODUCT);
        Files.copy(TestFileUtil.getResourcePath(getClass(), FILENAME), product.getInputDir().resolve(FILENAME));
        TestFileUtil.waitUntilFileExists(product.getFailDir().resolve(FILENAME));

        final TestPostAction postAction = postActionRegistry.get(POST_ACTION_ID);
        assertThat(postAction.getInvocations())
                .map(invocation -> invocation.message().getStationIcaoCode())
                .containsExactly("EFYY", "EFXX");

        verify(failChannel, never()).send(any(Message.class));
        verify(successChannel).send(successChannelCaptor.capture());
        final boolean failures = IntegrationFlowConfig.hasProcessingErrors(successChannelCaptor.getValue().getHeaders());
        assertThat(failures).isFalse();
    }

    public static class FailingPostAction extends TestPostAction {
        public FailingPostAction(final TestPostActionRegistry registry, final String id) {
            super(registry, id);
        }

        @Override
        public void doOnRun(final MessageProcessorContext context, final ArchiveAviationMessage message) {
            if (message.getStationIcaoCode().equals("EFYY")) {
                throw new RuntimeException("fail");
            }
        }
    }

    @Configuration
    @Profile({"FailingPostActionTest"})
    static class FailingPostActionConfig extends AbstractPostActionFactoryConfig {
        FailingPostActionConfig(final ConfigValueConverter configValueConverter) {
            super(configValueConverter);
        }

        @Bean
        PostActionFactory<FailingPostAction> failingPostActionFactory(final TestPostActionRegistry testPostActionRegistry) {
            return build(builder(FailingPostAction.class)
                    .addDependencyArg(testPostActionRegistry)
                    .addConfigArg("id", String.class));
        }
    }
}
