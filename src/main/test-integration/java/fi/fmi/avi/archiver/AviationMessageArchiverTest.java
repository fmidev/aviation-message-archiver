package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.AviationMessageArchiverTest"})
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_message_types_test.sql", "classpath:/h2-data/avidb_message_format_test.sql",
        "classpath:/h2-data/avidb_message_routes_test.sql",
        "classpath:/h2-data/avidb_stations_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class AviationMessageArchiverTest {

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    private static Stream<AviationMessageArchiverTestCase> test_file_flow() {
        return Stream.of(//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAC TAF")//
                        .setProductName("test_taf")//
                        .setInputFileName("simple_taf.txt2")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAC TAF with another product")//
                        .setProductName("test_taf_2")//
                        .setInputFileName("simple_taf.another")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Not convertable message goes to failed dir")//
                        .setProductName("test_taf")//
                        .setInputFileName("inconvertible.txt")//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAC TAF without GTS heading")//
                        .setProductName("test_taf")//
                        .setInputFileName("taf-missing-gts-heading.txt")//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAC TAF GTS Bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin.bul")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Partially valid TAC TAF GTS Bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-partially-valid.bul")//
                        .expectFail()
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF")//
                        .setProductName("test_iwxxm_taf")//
                        .setInputFileName("taf.xml")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin")//
                        .setProductName("test_iwxxm_taf")//
                        .setInputFileName("taf-bulletin.xml")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF with GTS heading")//
                        .setProductName("test_iwxxm_taf")//
                        .setInputFileName("taf-gts-heading.xml")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin with GTS heading")//
                        .setProductName("test_iwxxm_taf")//
                        .setInputFileName("taf-gts-heading-bulletin.xml")//
                        .build()//
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource
    public void test_file_flow(final AviationMessageArchiverTestCase testCase) throws IOException, InterruptedException, URISyntaxException {
        final AviationProductsHolder.AviationProduct product = testCase.getProduct(aviationProductsHolder);
        Files.copy(testCase.getInputFile(), Paths.get(product.getInputDir().getPath() + "/" + testCase.getInputFileName()));
        testCase.assertInputAndOutputFilesEquals(product);
    }

    @FreeBuilder
    static abstract class AviationMessageArchiverTestCase {
        private static final int WAIT_MILLIS = 100;
        private static final int TIMEOUT_MILLIS = 1000;

        AviationMessageArchiverTestCase() {
        }

        public static AviationMessageArchiverTestCase.Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return getName();
        }

        public abstract String getName();

        public abstract String getInputFileName();

        public abstract boolean getExpectFail();

        public Path getInputFile() throws URISyntaxException {
            final URL resource = requireNonNull(AviationMessageArchiverTest.class.getResource(getInputFileName()));
            final Path path = Paths.get(resource.toURI());
            assertThat(path).exists();
            return path;
        }

        public abstract String getProductName();

        public AviationProductsHolder.AviationProduct getProduct(final AviationProductsHolder holder) {
            final String productName = getProductName();
            return requireNonNull(holder.getProducts().get(productName), productName);
        }

        public void assertInputAndOutputFilesEquals(final AviationProductsHolder.AviationProduct product) throws InterruptedException, URISyntaxException {
            final File expectedOutputFile = new File((getExpectFail() ? product.getFailDir() : product.getArchiveDir()) + "/" + getInputFileName());
            waitUntilFileExists(expectedOutputFile);

            assertThat(expectedOutputFile).exists();
            assertThat(expectedOutputFile).hasSameTextualContentAs(getInputFile().toFile());
        }

        private void waitUntilFileExists(final File expectedOutputFile) throws InterruptedException {
            long totalWaitTime = 0;
            while (!expectedOutputFile.exists() && totalWaitTime < TIMEOUT_MILLIS) {
                Thread.sleep(WAIT_MILLIS);
                totalWaitTime += WAIT_MILLIS;
            }
        }

        public static class Builder extends AviationMessageArchiverTest_AviationMessageArchiverTestCase_Builder {

            public Builder() {
                setExpectFail(false);
            }

            public Builder expectFail() {
                return super.setExpectFail(true);
            }

        }
    }

}
