package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.FormatId;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductIdPropertyReaderTest {
    private static final Map<FormatId, String> TEST_PRODUCT_FILENAMES = Maps.immutableEnumMap(ImmutableMap.of(//
            FormatId.TAC, "message.txt", //
            FormatId.IWXXM, "message.xml"));

    @ParameterizedTest
    @EnumSource(TestProduct.class)
    void readValue_given_target_with_productIdentifier_returns_productIdentifier(final TestProduct testProduct) {
        final InputAviationMessage input = testProduct.setProductId(InputAviationMessage.builder()//
                        .setMessage(GenericAviationWeatherMessageImpl.builder().buildPartial()))//
                .buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        final ProductIdPropertyReader propertyReader = new ProductIdPropertyReader(TestProduct.PRODUCTS);

        final String result = propertyReader.readValue(input, target);

        assertThat(result).isEqualTo(testProduct.getId());
    }

    @Test
    void validate_given_known_productIdentifier_returns_true() {
        final ProductIdPropertyReader propertyReader = new ProductIdPropertyReader(
                ImmutableBiMap.of(TestProduct.PRODUCT2.getId(), TestProduct.PRODUCT2.getProduct()));

        final boolean result = propertyReader.validate(TestProduct.PRODUCT2.getId());

        assertThat(result).isTrue();
    }

    @Test
    void validate_given_unknown_productIdentifier_returns_true() {
        final ProductIdPropertyReader propertyReader = new ProductIdPropertyReader(
                ImmutableBiMap.of(TestProduct.PRODUCT2.getId(), TestProduct.PRODUCT2.getProduct()));

        final boolean result = propertyReader.validate(TestProduct.PRODUCT1.getId());

        assertThat(result).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<String> {
            @Nullable
            @Override
            public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
                return null;
            }
        }
        final ProductIdPropertyReader reader = new ProductIdPropertyReader(ImmutableBiMap.of());
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class ProductIdPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public ProductIdPropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.processor.conditional.ProductIdPropertyReader reader //
                = new fi.fmi.avi.archiver.message.processor.conditional.ProductIdPropertyReader(ImmutableBiMap.of());
        final ProductIdPropertyReader controlReader = new ProductIdPropertyReader();
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final ProductIdPropertyReader propertyReader = new ProductIdPropertyReader(ImmutableBiMap.of());
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }

    private enum TestProduct {
        PRODUCT1, PRODUCT2;

        private static final Map<String, AviationProduct> PRODUCTS = Arrays.stream(values())//
                .map(TestProduct::getProduct)//
                .collect(ImmutableMap.toImmutableMap(AviationProduct::getId, Function.identity()));

        private final AviationProduct product;

        TestProduct() {
            this.product = AviationProduct.builder()//
                    .setId(name())//
                    .addAllFileConfigs(Stream.of(FormatId.values())//
                            .map(formatId -> FileConfig.builder()//
                                    .setFormat(formatId.getFormat())//
                                    .setFormatId(formatId.getId())//
                                    .setNameTimeZone(ZoneOffset.UTC)//
                                    .setPattern(Pattern.compile("^" + Pattern.quote(TEST_PRODUCT_FILENAMES.get(formatId)) + "$"))//
                                    .build()))//
                    .buildPartial();
        }

        String getId() {
            return product.getId();
        }

        AviationProduct getProduct() {
            return product;
        }

        InputAviationMessage.Builder setProductId(final InputAviationMessage.Builder builder) {
            final FileConfig fileConfig = getProduct().getFileConfigs().getFirst();
            final FormatId formatId = FormatId.valueOf(fileConfig.getFormat());
            return builder//
                    .mutateFileMetadata(metadataBuilder -> metadataBuilder//
                            .setFileConfig(fileConfig)//
                            .setFileReference(FileReference.builder()//
                                    .setProductId(getId())//
                                    .setFilename(TEST_PRODUCT_FILENAMES.get(formatId))//
                                    .build()))//
                    .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)//
                            .setMessageFormat(formatId.getFormat())//
                            .build());
        }
    }
}
