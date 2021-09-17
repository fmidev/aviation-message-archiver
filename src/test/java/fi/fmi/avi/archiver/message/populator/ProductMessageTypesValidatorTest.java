package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static fi.fmi.avi.model.MessageType.METAR;
import static fi.fmi.avi.model.MessageType.SPECI;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductMessageTypesValidatorTest {

    private static final String TEST_PRODUCT_IDENTIFIER = "test_product";
    private static final String ANOTHER_PRODUCT_IDENTIFIER = "another_product";
    private static final Set<MessageType> TYPE_IDENTIFIERS = ImmutableSet.of(SPECI, METAR);
    private static final FileMetadata TEST_METADATA = FileMetadata.builder()
            .setProductIdentifier(TEST_PRODUCT_IDENTIFIER)
            .buildPartial();
    private static final FileMetadata ANOTHER_METADATA = FileMetadata.builder()
            .setProductIdentifier(ANOTHER_PRODUCT_IDENTIFIER)
            .buildPartial();

    private ProductMessageTypesValidator productMessageTypesValidator;

    @BeforeEach
    public void setUp() {
        productMessageTypesValidator = new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, TEST_PRODUCT_IDENTIFIER, TYPE_IDENTIFIERS);
    }

    @Test
    public void valid() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(1);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_2() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(2);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void another_product_identifier() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(3);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(ANOTHER_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void invalid() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(3);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    public void invalid_2() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(-1);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    public void testNulls() {
        final Class<?> classUnderTest = ProductMessageTypesValidator.class;
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(String.class, "test");
        tester.setDefault(Map.class, MessagePopulatorTests.TYPE_IDS);
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testAllPublicStaticMethods(classUnderTest);
        tester.testAllPublicConstructors(classUnderTest);
        tester.testAllPublicInstanceMethods(productMessageTypesValidator);
    }

}
