package fi.fmi.avi.archiver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.archiver.initializing.AviationProductsHolder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest({ "auto.startup=false" })
@ContextConfiguration(classes = { AviationMessageArchiver.class },//
        loader = AnnotationConfigContextLoader.class,//
        initializers = { ConfigFileApplicationContextInitializer.class })
public class AviationMessageArchiverTest {

    private static final int WAIT_MILLIS = 100;
    private static final int TIMEOUT_MILLIS = 1000;
    private static final File BASE_DIR = new File(System.getProperty("java.io.tmpdir") + "/.avi-message-archiver");
    private static final File TMP_DIR = new File(BASE_DIR, "temp");
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private AviationProductsHolder aviationProductsHolder;

    @BeforeClass
    public static void startup() {
        final boolean succeeded = TMP_DIR.mkdirs();
        if (!succeeded) {
            throw new IllegalStateException("Cannot write to the temp folder of the system");
        }
    }

    @AfterClass
    public static void cleanup() throws IOException {
        FileUtils.deleteDirectory(BASE_DIR);
    }

    private void writeContentToFile(final String content, final File inputFile) throws IOException {
        final Path tmpFile = File.createTempFile("test", ".txt", TMP_DIR).toPath();
        Files.write(tmpFile, content.getBytes(StandardCharsets.UTF_8));
        Files.move(tmpFile, inputFile.toPath());
    }

    private void assertFilesEquals(final File inputFile, final File expectedOutFile) throws InterruptedException {
        long totalWaitTime = 0;
        while (!expectedOutFile.exists() && totalWaitTime < TIMEOUT_MILLIS) {
            Thread.sleep(WAIT_MILLIS);
            totalWaitTime += WAIT_MILLIS;
        }

        softly.assertThat(expectedOutFile).exists();
        softly.assertThat(expectedOutFile).hasSameContentAs(inputFile);
    }

    private AviationProductsHolder.AviationProduct getTestProduct(final String id) {
        return aviationProductsHolder.getProducts().stream()//
                .filter(aviationProduct -> aviationProduct.getId().equals(id))//
                .findFirst()//
                .orElseThrow(IllegalStateException::new);
    }

    @Test
    public void test_failing_flow() throws IOException, InterruptedException {
        final String content = "Not convertable message";
        final AviationProductsHolder.AviationProduct product = getTestProduct("testProduct");

        final File inputFile = new File(product.getInputDir(), "test_failing_flow.txt");
        writeContentToFile(content, inputFile);

        assertFilesEquals(inputFile, new File(product.getFailedDir(), inputFile.getName()));
    }

    @Test
    public void test_simple_taf() throws IOException, InterruptedException {
        final String content = "FTXX33 XXXX 181500\n" + "TAF XXXX 181500Z 1812/1912 00000KT CAVOK=";
        final AviationProductsHolder.AviationProduct product = getTestProduct("testProduct");

        final File inputFile = new File(product.getInputDir(), "test_simple_taf.txt2");
        writeContentToFile(content, inputFile);

        assertFilesEquals(inputFile, new File(product.getArchivedDir(), inputFile.getName()));
    }

    @Test
    public void test_two_products_with_simple_taf() throws IOException, InterruptedException {
        final String content = "FTXX33 XXXX 181500\n" + "TAF XXXX 181500Z 1812/1912 00000KT CAVOK=";
        final AviationProductsHolder.AviationProduct product = getTestProduct("testProduct");
        final AviationProductsHolder.AviationProduct product2 = getTestProduct("testProduct2");

        final File inputFile = new File(product.getInputDir(), "test_two_products_with_simple_taf.txt");
        final File inputFile2 = new File(product2.getInputDir(), "test_two_products_with_simple_taf.another");

        writeContentToFile(content, inputFile);
        writeContentToFile(content, inputFile2);

        assertFilesEquals(inputFile, new File(product.getArchivedDir(), inputFile.getName()));
        assertFilesEquals(inputFile, new File(product2.getArchivedDir(), inputFile2.getName()));
    }
}
