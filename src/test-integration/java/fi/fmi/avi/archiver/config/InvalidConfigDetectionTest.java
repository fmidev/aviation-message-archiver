package fi.fmi.avi.archiver.config;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.DateTimeException;
import java.util.regex.PatternSyntaxException;

import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;

public class InvalidConfigDetectionTest {

    private ConfigurableApplicationContext applicationContext;

    private static SpringApplication createContextBuilder(final String testProfile) {
        return new SpringApplicationBuilder()//
                .bannerMode(Banner.Mode.OFF)//
                .sources(AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class)//
                .properties(//
                        "testclass.name=fi.fmi.avi.archiver.config.InvalidConfigDetectionTest", //
                        "spring.config.location=classpath:application.yml,classpath:" + InvalidConfigDetectionTest.class.getName().replace('.', '/') + ".yml" //
                )//
                .profiles("local", "h2", testProfile)//
                .build();
    }

    private static String containsWord(final String word) {
        return "^.*\\b(?:" + word + ")\\b.*$";
    }

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    private void assertThatNoExceptionIsThrownByProfile(final String profile) {
        final SpringApplication application = createContextBuilder(profile);
        assertThatNoException()//
                .isThrownBy(() -> applicationContext = application.run());
    }

    private ThrowableAssertAlternative<?> assertThatExceptionIsThrownByProfile(final String profile) {
        final SpringApplication application = createContextBuilder(profile);
        return assertThatExceptionOfType(RuntimeException.class)//
                .isThrownBy(() -> {
                    try {
                        applicationContext = application.run();
                    } catch (final Exception leafException) {
                        Throwable exception = leafException;
                        while (exception.getClass().getName().startsWith("org.springframework.") && exception.getCause() != null) {
                            exception = exception.getCause();
                        }
                        throw exception;
                    }
                });
    }

    @Test
    void testProductOk() {
        assertThatNoExceptionIsThrownByProfile("testProductOk");
    }

    @Test
    void testMissingProductId() {
        assertThatExceptionIsThrownByProfile("testMissingProductId")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("id"))//
        ;
    }

    @Test
    void testMissingProductRoute() {
        assertThatExceptionIsThrownByProfile("testMissingProductRoute")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("route"))//
        ;
    }

    @Test
    void testMissingProductInputDir() {
        assertThatExceptionIsThrownByProfile("testMissingProductInputDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("inputDir"))//
        ;
    }

    @Test
    void testMissingProductArchiveDir() {
        assertThatExceptionIsThrownByProfile("testMissingProductArchiveDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("archiveDir"))//
        ;
    }

    @Test
    void testMissingProductFailDir() {
        assertThatExceptionIsThrownByProfile("testMissingProductFailDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("failDir"))//
        ;
    }

    @Test
    void testMissingProductFiles() {
        assertThatExceptionIsThrownByProfile("testMissingProductFiles")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("fileConfigs|files"))//
        ;
    }

    @Test
    void testMissingProductFilePattern() {
        assertThatExceptionIsThrownByProfile("testMissingProductFilePattern")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("pattern"))//
        ;
    }

    @Test
    void testMissingProductFileNameTimeZone() {
        assertThatExceptionIsThrownByProfile("testMissingProductFileNameTimeZone")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("nameTimeZone"))//
        ;
    }

    @Test
    void testMissingProductFileFormat() {
        assertThatExceptionIsThrownByProfile("testMissingProductFileFormat")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("format"))//
        ;
    }

    @Test
    void testEmptyProducts() {
        assertThatExceptionIsThrownByProfile("testEmptyProducts")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("products"))//
        ;
    }

    @Test
    void testEmptyProductId() {
        assertThatExceptionIsThrownByProfile("testEmptyProductId")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("id"))//
        ;
    }

    @Test
    void testEmptyProductRoute() {
        assertThatExceptionIsThrownByProfile("testEmptyProductRoute")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageContaining("Unknown route")//
        ;
    }

    @Test
    void testEmptyProductInputDir() {
        assertThatExceptionIsThrownByProfile("testEmptyProductInputDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("inputDir"))//
        ;
    }

    @Test
    void testEmptyProductArchiveDir() {
        assertThatExceptionIsThrownByProfile("testEmptyProductArchiveDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("archiveDir"))//
        ;
    }

    @Test
    void testEmptyProductFailDir() {
        assertThatExceptionIsThrownByProfile("testEmptyProductFailDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("failDir"))//
        ;
    }

    @Test
    void testEmptyProductFiles() {
        assertThatExceptionIsThrownByProfile("testEmptyProductFiles")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("fileConfigs|files"))//
        ;
    }

    @Test
    void testEmptyProductFilePattern() {
        assertThatExceptionIsThrownByProfile("testEmptyProductFilePattern")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("pattern"))//
        ;
    }

    @Test
    void testEmptyProductFileNameTimeZone() {
        assertThatExceptionIsThrownByProfile("testEmptyProductFileNameTimeZone")//
                .isInstanceOf(DateTimeException.class)//
                .withMessageContaining("Zone")//
                .withMessageContaining("Invalid ID")//
        ;
    }

    @Test
    void testEmptyProductFileFormat() {
        assertThatExceptionIsThrownByProfile("testEmptyProductFileFormat")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageMatching(containsWord("[Nn]ot set"))//
                .withMessageMatching(containsWord("format"))//
        ;
    }

    @Test
    void testInvalidProductRoute() {
        assertThatExceptionIsThrownByProfile("testInvalidProductRoute")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("INVALID_ROUTE");
    }

    @Test
    void testInvalidProductFilePattern() {
        assertThatExceptionIsThrownByProfile("testInvalidProductFilePattern")//
                .isInstanceOf(PatternSyntaxException.class)//
                .withMessageContaining("Unclosed group")//
        ;
    }

    @Test
    void testInvalidProductFileNameTimeZone() {
        assertThatExceptionIsThrownByProfile("testInvalidProductFileNameTimeZone")//
                .isInstanceOf(DateTimeException.class)//
                .withMessageContaining("INVALID_ZONE")//
        ;
    }

    @Test
    void testInvalidProductFileFormat() {
        assertThatExceptionIsThrownByProfile("testInvalidProductFileFormat")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageContaining("INVALID_FORMAT")//
        ;
    }

    @Test
    void testDuplicateProductId() {
        assertThatExceptionIsThrownByProfile("testDuplicateProductId")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageContaining("product1")//
        ;
    }

    @Test
    void testDuplicateProductFilePattern() {
        assertThatExceptionIsThrownByProfile("testDuplicateProductFilePattern")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <0>:")//
                .withMessageContaining("Duplicate pattern")//
        ;
    }

    @Test
    void testDuplicateProductFilePatternInDifferentProduct() {
        assertThatExceptionIsThrownByProfile("testDuplicateProductFilePatternInDifferentProduct")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid product configuration at index <1>:")//
                .withMessageContaining("Duplicate pattern")//
        ;
    }

    @Test
    void testInputDirEqualToArchiveDir() {
        assertThatExceptionIsThrownByProfile("testInputDirEqualToArchiveDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("input directory")//
                .withMessageContaining("archive directory")//
        ;
    }

    @Test
    void testInputDirEqualToArchiveDirInOtherProduct() {
        assertThatExceptionIsThrownByProfile("testInputDirEqualToArchiveDirInOtherProduct")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("product2")//
                .withMessageContaining("input directory")//
                .withMessageContaining("archive directory")//
        ;
    }

    @Test
    void testInputDirEqualToFailDir() {
        assertThatExceptionIsThrownByProfile("testInputDirEqualToFailDir")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("input directory")//
                .withMessageContaining("fail directory")//
        ;
    }

    @Test
    void testInputDirEqualToFailDirInOtherProduct() {
        assertThatExceptionIsThrownByProfile("testInputDirEqualToFailDirInOtherProduct")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("product2")//
                .withMessageContaining("input directory")//
                .withMessageContaining("fail directory")//
        ;
    }

    @Test
    void testArchiveDirEqualToFailDirInOtherProduct() {
        assertThatExceptionIsThrownByProfile("testArchiveDirEqualToFailDirInOtherProduct")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("product2")//
                .withMessageContaining("archive directory")//
                .withMessageContaining("fail directory")//
        ;
    }

    @Test
    void testFailDirEqualToArchiveDirInOtherProduct() {
        assertThatExceptionIsThrownByProfile("testFailDirEqualToArchiveDirInOtherProduct")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageContaining("product1")//
                .withMessageContaining("product2")//
                .withMessageContaining("archive directory")//
                .withMessageContaining("fail directory")//
        ;
    }

    @Test
    void testEqualArchiveAndFailDirsInMultipleProductsOk() {
        assertThatNoExceptionIsThrownByProfile("testEqualArchiveAndFailDirsInMultipleProductsOk");
    }

    @Test
    void testMissingMessagePopulatorExecutionChain() {
        assertThatExceptionIsThrownByProfile("testMissingMessagePopulatorExecutionChain")//
                .isInstanceOf(NullPointerException.class)//
                .withMessageMatching(containsWord("executionChain"))//
        ;
    }

    @Test
    void testEmptyMessagePopulatorExecutionChain() {
        assertThatExceptionIsThrownByProfile("testEmptyMessagePopulatorExecutionChain")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid message populators configuration: executionChain is empty")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("executionChain"))//
        ;
    }

    @Test
    void testMissingStaticRouteIds() {
        assertThatExceptionIsThrownByProfile("testMissingStaticRouteIds")//
                .isInstanceOf(NullPointerException.class)//
                .withMessageMatching(containsWord("routeIds"))//
        ;
    }

    @Test
    void testMissingStaticFormatIds() {
        assertThatExceptionIsThrownByProfile("testMissingStaticFormatIds")//
                .isInstanceOf(NullPointerException.class)//
                .withMessageMatching(containsWord("formatIds"))//
        ;
    }

    @Test
    void testMissingStaticTypeIds() {
        assertThatExceptionIsThrownByProfile("testMissingStaticTypeIds")//
                .isInstanceOf(NullPointerException.class)//
                .withMessageMatching(containsWord("typeIds"))//
        ;
    }

    @Test
    void testEmptyStaticRouteIds() {
        assertThatExceptionIsThrownByProfile("testEmptyStaticRouteIds")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("routeIds"))//
        ;
    }

    @Test
    void testEmptyStaticFormatIds() {
        assertThatExceptionIsThrownByProfile("testEmptyStaticFormatIds")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("formatIds"))//
        ;
    }

    @Test
    void testEmptyStaticTypeIds() {
        assertThatExceptionIsThrownByProfile("testEmptyStaticTypeIds")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("typeIds"))//
        ;
    }
}
