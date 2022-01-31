package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductMessageTypesValidatorTest {

    private static final String METAR_PRODUCT = "metar_product";
    private static final String TAF_PRODUCT = "taf_product";
    private static final String OTHER_PRODUCT = "other_product";
    private static final Map<String, AviationProduct> PRODUCTS = Stream.of(//
                    AviationProduct.builder()//
                            .setId(METAR_PRODUCT)//
                            .buildPartial(), //
                    AviationProduct.builder()//
                            .setId(TAF_PRODUCT)//
                            .buildPartial())//
            .collect(ImmutableMap.toImmutableMap(AviationProduct::getId, Function.identity()));
    private static final Set<MessageType> METAR_TYPES = ImmutableSet.of(MessagePopulatorTests.TypeId.SPECI.getType(),
            MessagePopulatorTests.TypeId.METAR.getType());
    private static final Set<MessageType> TAF_TYPE = Collections.singleton(MessageType.TAF);
    private static final Map<String, Set<MessageType>> PRODUCT_MESSAGE_TYPES = ImmutableMap.of(METAR_PRODUCT, METAR_TYPES, TAF_PRODUCT, TAF_TYPE);
    private static final FileMetadata METAR_METADATA = FileMetadata.builder()//
            .setFileReference(FileReference.create(METAR_PRODUCT, "anyfile"))//
            .buildPartial();
    private static final FileMetadata TAF_METADATA = FileMetadata.builder()//
            .setFileReference(FileReference.create(TAF_PRODUCT, "anyfile"))//
            .buildPartial();
    private static final FileMetadata OTHER_METADATA = FileMetadata.builder()//
            .setFileReference(FileReference.create(OTHER_PRODUCT, "anyfile"))//
            .buildPartial();

    private ProductMessageTypesValidator productMessageTypesValidator;

    @BeforeEach
    public void setUp() {
        productMessageTypesValidator = new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, PRODUCTS, PRODUCT_MESSAGE_TYPES);
    }

    @Test
    void valid_speci() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.SPECI.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(METAR_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void valid_metar() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.METAR.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(METAR_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void another_product_identifier_with_taf() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.TAF.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(OTHER_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void invalid_taf() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.TAF.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(METAR_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    void valid_taf() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(MessagePopulatorTests.TypeId.TAF.getId());
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(TAF_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void invalid_type() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(-1);
        productMessageTypesValidator.populate(InputAviationMessage.builder().setFileMetadata(METAR_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_TYPE);
    }

    @Test
    void misconfigured_message_type() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, PRODUCTS, ImmutableMap.of(TAF_PRODUCT, Collections.singleton(new MessageType("test")))));

    }

    @Test
    void misconfigured_product() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, PRODUCTS, ImmutableMap.of(OTHER_PRODUCT, Collections.singleton(MessageType.METAR))));
    }

    // Test nulls apart from constructor
    @Test
    public void testNulls() {
        final Class<?> classUnderTest = ProductMessageTypesValidator.class;
        final NullPointerTester tester = new NullPointerTester();
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(productMessageTypesValidator, minimalVisibility);
    }

    // Test constructor nulls
    @Test
    void testConstructorNulls() {
        assertThrows(NullPointerException.class, () -> new ProductMessageTypesValidator(null, PRODUCTS, PRODUCT_MESSAGE_TYPES));
        assertThrows(NullPointerException.class, () -> new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, null, PRODUCT_MESSAGE_TYPES));
        assertThrows(NullPointerException.class, () -> new ProductMessageTypesValidator(MessagePopulatorTests.TYPE_IDS, PRODUCTS, null));
    }

}
