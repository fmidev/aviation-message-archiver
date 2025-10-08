package fi.fmi.avi.archiver.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.util.regex.PatternSyntaxException;

public class InvalidConfigDetectionTest extends AbstractConfigValidityTest {

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
                .withMessageMatching(containsWord("is empty"))//
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
    void testMissingMessagePopulatorExecutionChain() {
        assertThatExceptionIsThrownByProfile("testMissingMessagePopulatorExecutionChain")//
                .isInstanceOf(NullPointerException.class)//
                .withMessageMatching(containsWord("messagePopulators"))//
        ;
    }

    @Test
    void testEmptyMessagePopulatorExecutionChain() {
        assertThatExceptionIsThrownByProfile("testEmptyMessagePopulatorExecutionChain")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid message populators configuration: messagePopulators is empty")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("messagePopulators"))//
        ;
    }

    @Test
    void testNonExistentMessagePopulator() {
        assertThatExceptionIsThrownByProfile("testNonExistentMessagePopulator")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageMatching(containsWord("Unknown"))//
                .withMessageMatching(containsWord("message populator"))//
                .withMessageMatching(containsWord("NonExistentMessagePopulator"))
        ;
    }

    @Test
    void testMessagePopulatorWithMissingMandatoryConfig() {
        assertThatExceptionIsThrownByProfile("testMessagePopulatorWithMissingMandatoryConfig")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Missing required config option")//
                .withMessageMatching(containsWord("FixedDurationValidityPeriodPopulator"))//
                .withMessageMatching(containsWord("validityEndOffset"))//
        ;
    }

    @Test
    void testMessagePopulatorWithNonExistentConfig() {
        assertThatExceptionIsThrownByProfile("testMessagePopulatorWithNonExistentConfig")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Unknown config option")//
                .withMessageMatching(containsWord("NoOp"))//
                .withMessageMatching(containsWord("nonExistentConfig"))//
        ;
    }

    @Test
    void testMessagePopulatorWithInvalidConfigValue() {
        assertThatExceptionIsThrownByProfile("testMessagePopulatorWithInvalidConfigValue")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Unable to convert")//
                .withMessageMatching(containsWord("NoOp"))//
                .withMessageMatching(containsWord("dummyInt"))//
        ;
    }

    /**
     * Test MessagePopulator with non-existent property.
     *
     * <p>
     * This test is disabled, because decent solution to implement the behavior is not obvious.
     * Setting {@code @ConfigurationProperties(ignoreUnknownFields = false)} in {@link ProductionLineConfig}
     * breaks things elsewhere.
     * </p>
     */
    @Disabled("Unable to implement the behavior")
    @Test
    void testMessagePopulatorWithNonExistentProperty() {
        assertThatExceptionIsThrownByProfile("testMessagePopulatorWithNonExistentProperty")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageMatching(containsWord("FileMetadataPopulator"))//
                .withMessageMatching(containsWord("nonExistentProperty"))//
        ;
    }

    @Test
    void testNonExistentPostAction() {
        assertThatExceptionIsThrownByProfile("testNonExistentPostAction")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageMatching(containsWord("Unknown"))//
                .withMessageMatching(containsWord("post-action"))//
                .withMessageMatching(containsWord("NonExistentPostAction"))
        ;
    }

    @Test
    void testPostActionWithMissingMandatoryConfig() {
        assertThatExceptionIsThrownByProfile("testPostActionWithMissingMandatoryConfig")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Missing required config option")//
                .withMessageMatching(containsWord("TestPostAction"))//
                .withMessageMatching(containsWord("id"))//
        ;
    }

    @Test
    void testPostActionWithNonExistentConfig() {
        assertThatExceptionIsThrownByProfile("testPostActionWithNonExistentConfig")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Unknown config option")//
                .withMessageMatching(containsWord("NoOp"))//
                .withMessageMatching(containsWord("nonExistentConfig"))//
        ;
    }

    /**
     * Test PostAction with non-existent property.
     *
     * <p>
     * This test is disabled, because decent solution to implement the behavior is not obvious.
     * Setting {@code @ConfigurationProperties(ignoreUnknownFields = false)} in {@link ProductionLineConfig}
     * breaks things elsewhere.
     * </p>
     */
    @Disabled("Unable to implement the behavior")
    @Test
    void testPostActionWithNonExistentProperty() {
        assertThatExceptionIsThrownByProfile("testPostActionWithNonExistentProperty")//
                .isInstanceOf(IllegalArgumentException.class)//
                .withMessageMatching(containsWord("FileMetadataPopulator"))//
                .withMessageMatching(containsWord("nonExistentProperty"))//
        ;
    }

    @Test
    void testPostActionWithInvalidConfigValue() {
        assertThatExceptionIsThrownByProfile("testPostActionWithInvalidConfigValue")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Unable to convert")//
                .withMessageMatching(containsWord("NoOp"))//
                .withMessageMatching(containsWord("dummyInt"))//
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
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("routeIds"))//
        ;
    }

    @Test
    void testEmptyStaticFormatIds() {
        assertThatExceptionIsThrownByProfile("testEmptyStaticFormatIds")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("formatIds"))//
        ;
    }

    @Test
    void testEmptyStaticTypeIds() {
        assertThatExceptionIsThrownByProfile("testEmptyStaticTypeIds")//
                .isInstanceOf(IllegalStateException.class)//
                .withMessageContaining("Invalid configuration:")//
                .withMessageMatching(containsWord("is empty"))//
                .withMessageMatching(containsWord("typeIds"))//
        ;
    }
}
