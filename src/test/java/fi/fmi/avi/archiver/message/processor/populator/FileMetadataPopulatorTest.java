package fi.fmi.avi.archiver.message.processor.populator;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
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
            .setFormat(MessageProcessorTestHelper.FormatId.TAC.getFormat())//
            .setFormatId(MessageProcessorTestHelper.FormatId.TAC.getId())//
            .setPattern(MessageProcessorTestHelper.FILE_NAME_PATTERN)//
            .setNameTimeZone(ZoneOffset.UTC)//
            .build();
    private static final FileConfig FILE_CONFIG_2 = FILE_CONFIG_1.toBuilder()//
            .setFormat(MessageProcessorTestHelper.FormatId.IWXXM.getFormat())//
            .setFormatId(MessageProcessorTestHelper.FormatId.IWXXM.getId())//
            .build();
    private static final Map<String, AviationProduct> PRODUCTS = Stream.of(//
                    AviationProduct.builder()//
                            .setId(PRODUCT_ID_1)//
                            .setRoute(MessageProcessorTestHelper.RouteId.TEST.getName())//
                            .setRouteId(MessageProcessorTestHelper.RouteId.TEST.getId())//
                            .addFileConfigs(FILE_CONFIG_1)//
                            .buildPartial(), //
                    AviationProduct.builder()//
                            .setId(PRODUCT_ID_2)//
                            .setRoute(MessageProcessorTestHelper.RouteId.TEST2.getName())//
                            .setRouteId(MessageProcessorTestHelper.RouteId.TEST2.getId())//
                            .addFileConfigs(FILE_CONFIG_2)//
                            .buildPartial())//
            .collect(ImmutableMap.toImmutableMap(AviationProduct::getId, Function.identity()));
    private static final InputAviationMessage INPUT_MESSAGE_TEMPLATE = InputAviationMessage.builder()//
            .setFileMetadata(FileMetadata.builder()//
                    .setFileReference(FileReference.create(PRODUCT_ID_1, "taf.txt"))//
                    .setFileModified(FILE_MODIFIED)//
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
    void populates_route(final String productId, final MessageProcessorTestHelper.RouteId expected) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata.mutateFileReference(ref -> ref.setProductId(productId))//
                        .setFileConfig(PRODUCTS.get(productId).getFileConfigs().getFirst()))//
                .build();
        final TestMessageProcessorContext context = TestMessageProcessorContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = MessageProcessorTestHelper.EMPTY_ARCHIVE_MESSAGE.toBuilder();
        populator.populate(context, builder);
        assertThat(MessageProcessorTestHelper.RouteId.valueOf(builder.getRoute())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ //
            "testproduct1, TAC", //
            "testproduct2, IWXXM", //
    })
    void populates_format(final String productId, final MessageProcessorTestHelper.FormatId expected) {
        final InputAviationMessage inputMessage = INPUT_MESSAGE_TEMPLATE.toBuilder()//
                .mutateFileMetadata(fileMetadata -> fileMetadata.mutateFileReference(ref -> ref.setProductId(productId))//
                        .setFileConfig(PRODUCTS.get(productId).getFileConfigs().getFirst()))//
                .build();
        final TestMessageProcessorContext context = TestMessageProcessorContext.create(inputMessage);

        final ArchiveAviationMessage.Builder builder = MessageProcessorTestHelper.EMPTY_ARCHIVE_MESSAGE.toBuilder();
        populator.populate(context, builder);
        assertThat(MessageProcessorTestHelper.FormatId.valueOf(builder.getFormat())).isEqualTo(expected);
    }

    @Test
    void populates_fileModified() {
        final TestMessageProcessorContext context = TestMessageProcessorContext.create(INPUT_MESSAGE_TEMPLATE);
        final Instant expected = FILE_MODIFIED;

        final ArchiveAviationMessage.Builder builder = MessageProcessorTestHelper.EMPTY_ARCHIVE_MESSAGE.toBuilder();
        populator.populate(context, builder);
        assertThat(builder.getFileModified()).isEqualTo(Optional.of(expected));
    }
}