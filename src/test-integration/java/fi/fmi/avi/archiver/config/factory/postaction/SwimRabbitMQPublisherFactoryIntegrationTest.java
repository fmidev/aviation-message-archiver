package fi.fmi.avi.archiver.config.factory.postaction;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.config.factory.postaction.SwimRabbitMQPublisherFactoryIntegrationTest"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},
        loader = AnnotationConfigContextLoader.class,
        initializers = {ConfigDataApplicationContextInitializer.class})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ActiveProfiles({"integration-test", "SwimRabbitMQPublisherFactoryIntegrationTest"})
class SwimRabbitMQPublisherFactoryIntegrationTest {
    @Test
    void test() {
        // instantiation succeeds if no exceptions
        // TODO: proper tests as part of SDM-2639
    }
}
