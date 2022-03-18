package fi.fmi.avi.archiver.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.fmi.avi.archiver.logging.LoggableTests;

public class FileReferenceTest {
    private static final String PRODUCT_ID = "test-product-id";
    private static final String FILENAME = "test-filename";

    private FileReference fileReference;

    @BeforeEach
    void setUp() {
        fileReference = FileReference.create(PRODUCT_ID, FILENAME);
    }

    @Test
    void readableCopy_returns_same_instance() {
        assertThat(fileReference.readableCopy()).isSameAs(fileReference);
    }

    @Test
    void getStructureName_returns_expected_name() {
        assertThat(fileReference.getStructureName()).isEqualTo("fileReference");
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate() {
        LoggableTests.assertDecentLengthEstimate(fileReference);
    }

    @Test
    void create_creates_populated_instance() {
        assertSoftly(softly -> {
            softly.assertThat(fileReference.getProductId()).as("getProductId").isEqualTo(PRODUCT_ID);
            softly.assertThat(fileReference.getFilename()).as("getFilename").isEqualTo(FILENAME);
        });
    }

    @Test
    void toString_returns_productId_and_filename_separated_by_slash() {
        assertThat(fileReference.toString()).isEqualTo("test-product-id/test-filename");
    }
}
