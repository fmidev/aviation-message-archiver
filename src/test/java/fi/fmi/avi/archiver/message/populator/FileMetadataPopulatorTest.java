package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("UnnecessaryLocalVariable")
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class FileMetadataPopulatorTest {
    private static final Instant FILE_MODIFIED = Instant.parse("2000-01-02T03:05:34Z");
    private static final String PRODUCT_ID_1 = "testproduct1";
    private static final String PRODUCT_ID_2 = "testproduct2";
    private static final FileConfig FILE_CONFIG_1 = FileConfig.builder()//
            .setFormat(MessagePopulatorTests.FormatId.TAC.getFormat())//
            .setFormatId(MessagePopulatorTests.FormatId.TAC.getId())//
            .setPattern(MessagePopulatorTests.FILE_NAME_PATTERN)//
            .setNameTimeZone(ZoneOffset.UTC)//
            .build();
    private static final FileConfig FILE_CONFIG_2 = FILE_CONFIG_1.toBuilder()//
            .setFormat(MessagePopulatorTests.FormatId.IWXXM.getFormat())//
            .setFormatId(MessagePopulatorTests.FormatId.IWXXM.getId())//
            .build();
    private static final Map<String, AviationProduct> PRODUCTS = Stream.of(//
                    AviationProduct.builder()//
                            .setId(PRODUCT_ID_1)//
                            .setRoute(MessagePopulatorTests.RouteId.TEST.getName())//
                            .setRouteId(MessagePopulatorTests.RouteId.TEST.getId())//
                            .addFileConfigs(FILE_CONFIG_1)//
                            .buildPartial(), //
                    AviationProduct.builder()//
                            .setId(PRODUCT_ID_2)//
                            .setRoute(MessagePopulatorTests.RouteId.TEST2.getName())//
                            .setRouteId(MessagePopulatorTests.RouteId.TEST2.getId())//
                            .addFileConfigs(FILE_CONFIG_2)//
                            .buildPartial())//
            .collect(ImmutableMap.toImmutableMap(AviationProduct::getId, Function.identity()));
    private static final InputAviationMessage INPUT_MESSAGE_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setProductIdentifier(PRODUCT_ID_1)//
                    .setFileModified(FILE_MODIFIED)//
                    .setFilename("taf.txt")//
                    .setFileConfig(FILE_CONFIG_1))//
            .buildPartial();
    private FileMetadataPopulator populator;

    @BeforeEach
    void setUp() {
        populator = new FileMetadataPopulator(PRODUCTS);
    }

    @ParameterizedTest
    @CsvSource({ //
            "testproduct1, TEST", //
            "testproduct2, TEST2", //
    })
    void populates_route(final String productId, final MessagePopulatorTests.RouteId expected) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata//
                        .setProductIdentifier(productId)//
                        .setFileConfig(PRODUCTS.get(productId).getFileConfigs().get(0)))//
                .build();

        final ArchiveAviationMessage.Builder builder = MessagePopulatorTests.EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(MessagePopulatorTests.RouteId.valudOf(builder.getRoute())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ //
            "testproduct1, TAC", //
            "testproduct2, IWXXM", //
    })
    void populates_format(final String productId, final MessagePopulatorTests.FormatId expected) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata//
                        .setProductIdentifier(productId)//
                        .setFileConfig(PRODUCTS.get(productId).getFileConfigs().get(0)))//
                .build();

        final ArchiveAviationMessage.Builder builder = MessagePopulatorTests.EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(MessagePopulatorTests.FormatId.valueOf(builder.getFormat())).isEqualTo(expected);
    }

    @Test
    void populates_fileModified() {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE;
        final Instant expected = FILE_MODIFIED;

        final ArchiveAviationMessage.Builder builder = MessagePopulatorTests.EMPTY_RESULT.toBuilder();
        populator.populate(inputMessage, builder);
        assertThat(builder.getFileModified()).isEqualTo(Optional.of(expected));
    }
}