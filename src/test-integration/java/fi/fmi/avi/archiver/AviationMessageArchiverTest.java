package fi.fmi.avi.archiver;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import org.apache.commons.io.FileUtils;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
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

@SpringBootTest({"auto.startup=false"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, AviationMessageArchiverTest.TestConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
public class AviationMessageArchiverTest {

    private static final File BASE_DIR = new File(System.getProperty("java.io.tmpdir") + "/.avi-message-archiver");
    private static final File TMP_DIR = new File(BASE_DIR, "temp");

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @BeforeAll
    public static void startup() throws IOException {
        FileUtils.deleteDirectory(BASE_DIR);
        if (!TMP_DIR.mkdirs()) {
            throw new IllegalStateException("Cannot write to the temp folder of the system");
        }
    }

    @AfterAll
    public static void done() throws IOException {
        FileUtils.deleteDirectory(BASE_DIR);
    }

    private static Stream<AviationMessageArchiverTestCase> test_file_flow() {
        return Stream.of(//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAF")//
                        .setProductName("testProduct")//
                        .setInputFileName("simple_taf.txt2")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAF with another product")//
                        .setProductName("testProduct2")//
                        .setInputFileName("simple_taf.another")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Not convertable message goes to failed dir")//
                        .setProductName("testProduct")//
                        .setInputFileName("not_convertable.txt")//
                        .expectFail()//
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
            return holder.getProducts().stream()//
                    .filter(aviationProduct -> aviationProduct.getId().equals(getProductName()))//
                    .findFirst()//
                    .orElseThrow(IllegalStateException::new);
        }

        public void assertInputAndOutputFilesEquals(final AviationProductsHolder.AviationProduct product) throws InterruptedException, URISyntaxException {
            final File expectedOutputFile = new File((getExpectFail() ? product.getFailedDir() : product.getArchivedDir()) + "/" + getInputFileName());
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

    @Configuration
    static class TestConfig {
        @Bean
        public ApplicationConversionService conversionService() {
            return new ApplicationConversionService();
        }
    }
}
