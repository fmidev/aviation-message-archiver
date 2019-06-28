package fi.fmi.avi.archiver;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;

@SpringBootTest({ "auto.startup=false" })
@ContextConfiguration(classes = { AviationMessageArchiver.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigFileApplicationContextInitializer.class })
public class AviationMessageArchiverTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();
    private static final File BASE_DIR = new File(System.getProperty("java.io.tmpdir") + "/.avi-message-archiver");
    private static final File TMP_DIR = new File(BASE_DIR, "temp");
    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @BeforeAll
    public static void startup() throws IOException {
        FileUtils.deleteDirectory(BASE_DIR);
        final boolean succeeded = TMP_DIR.mkdirs();
        if (!succeeded) {
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
                        .setExpectedOutputPath("dst/simple_taf.txt2")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAF with another product")//
                        .setProductName("testProduct2")//
                        .setInputFileName("simple_taf.another")//
                        .setExpectedOutputPath("dst2/simple_taf.another")//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Not convertable message goes to failed dir")//
                        .setProductName("testProduct")//
                        .setInputFileName("not_convertable.txt")//
                        .setExpectedOutputPath("failed/not_convertable.txt")//
                        .build()//
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource
    public void test_file_flow(final AviationMessageArchiverTestCase testCase) throws IOException, InterruptedException, URISyntaxException {
        final AviationProductsHolder.AviationProduct product = testCase.getProduct(aviationProductsHolder);
        Files.copy(testCase.getInputFile(), Paths.get(product.getInputDir().getPath() + "/" + testCase.getInputFileName()));
        testCase.assertInputAndOutputFilesEquals();
    }

    @FreeBuilder
    static abstract class AviationMessageArchiverTestCase {
        private static final int WAIT_MILLIS = 100;
        private static final int TIMEOUT_MILLIS = 1000;
        private static final String TEST_DATA_ROOT = "fi/fmi/avi/archiver/";

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

        public Path getInputFile() throws URISyntaxException {
            final URL resource = requireNonNull(AviationMessageArchiverTest.class.getClassLoader().getResource(TEST_DATA_ROOT + getInputFileName()));
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

        public void assertInputAndOutputFilesEquals() throws InterruptedException, URISyntaxException {
            final File expectedOutputFile = new File(BASE_DIR + "/" + getExpectedOutputPath());
            long totalWaitTime = 0;
            while (!expectedOutputFile.exists() && totalWaitTime < TIMEOUT_MILLIS) {
                Thread.sleep(WAIT_MILLIS);
                totalWaitTime += WAIT_MILLIS;
            }

            assertThat(expectedOutputFile).exists();
            assertThat(expectedOutputFile).hasSameContentAs(getInputFile().toFile());
        }

        public abstract String getExpectedOutputPath();

        public static class Builder extends AviationMessageArchiverTest_AviationMessageArchiverTestCase_Builder {

            public Builder() {

            }

        }

    }

}
