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

import static org.assertj.core.api.Assertions.assertThat;

public class ProductMessageTypesValidatorTest {

    private static final String TEST_PRODUCT_IDENTIFIER = "test_product";
    private static final String ANOTHER_PRODUCT_IDENTIFIER = "another_product";
    private static final Set<MessageType> TYPE_IDENTIFIERS = ImmutableSet.of(MessagePopulatorTests.TypeId.SPECI.getType(),
            MessagePopulatorTests.TypeId.METAR.getType());
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
    void valid_speci() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.SPECI.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void valid_metar() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.METAR.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void another_product_identifier_with_taf() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.TAF.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(ANOTHER_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void invalid_taf() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.TAF.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    void invalid_type() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(-1);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    public void testNulls() {
        final Class<?> classUnderTest = ProductMessageTypesValidator.class;
        final NullPointerTester tester = new NullPointerTester();
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(String.class, "test");
        tester.setDefault(Map.class, MessagePopulatorTests.TYPE_IDS);
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(productMessageTypesValidator, minimalVisibility);
    }

}
