package fi.fmi.avi.archiver.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

public class FileReferenceTest {

    private static final String PRODUCT_ID = "test-product-id";
    private static final String FILENAME = "test-filename";

    @Test
    void create_creates_populated_instance() {
        final FileReference fileReference = FileReference.create(PRODUCT_ID, FILENAME);
        assertSoftly(softly -> {
            softly.assertThat(fileReference.getProductIdentifier()).as("getProductIdentifier").isEqualTo(PRODUCT_ID);
            softly.assertThat(fileReference.getFilename()).as("getFilename").isEqualTo(FILENAME);
        });
    }

    @Test
    void toString_returns_productId_and_filename_separated_by_colon() {
        final FileReference fileReference = FileReference.create(PRODUCT_ID, FILENAME);
        assertThat(fileReference.toString()).isEqualTo("test-product-id:test-filename");
    }
}
