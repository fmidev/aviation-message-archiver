package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableSet;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageTypeValidatorTest {

    private static final String TEST_PRODUCT_IDENTIFIER = "test_product";
    private static final String ANOTHER_PRODUCT_IDENTIFIER = "another_product";
    private static final Set<Integer> TYPE_IDENTIFIERS = ImmutableSet.of(1, 2);
    private static final FileMetadata TEST_METADATA = FileMetadata.builder()
            .setProductIdentifier(TEST_PRODUCT_IDENTIFIER)
            .buildPartial();
    private static final FileMetadata ANOTHER_METADATA = FileMetadata.builder()
            .setProductIdentifier(ANOTHER_PRODUCT_IDENTIFIER)
            .buildPartial();

    private MessageTypeValidator messageTypeValidator;

    @BeforeEach
    public void setUp() {
        messageTypeValidator = new MessageTypeValidator();
        messageTypeValidator.setProductIdentifier(TEST_PRODUCT_IDENTIFIER);
        messageTypeValidator.setTypeIdentifiers(TYPE_IDENTIFIERS);
    }

    @Test
    public void valid() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(1);
        messageTypeValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_2() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(2);
        messageTypeValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void another_product_identifier() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(3);
        messageTypeValidator.populate(InputAviationMessage.builder().setFileMetadata(ANOTHER_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void invalid() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(3);
        messageTypeValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.INVALID_MESSAGE_TYPE);
    }

    @Test
    public void invalid_2() {
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setType(-1);
        messageTypeValidator.populate(InputAviationMessage.builder().setFileMetadata(TEST_METADATA).buildPartial(), builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.INVALID_MESSAGE_TYPE);
    }

}
