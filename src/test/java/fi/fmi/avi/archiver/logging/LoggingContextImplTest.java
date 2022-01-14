package fi.fmi.avi.archiver.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.logging.FileProcessingStatistics.Status;
import fi.fmi.avi.archiver.message.MessageReference;

class LoggingContextImplTest {
    private static final FileReference FILE_REFERENCE = FileReference.create("productId", "test_file.txt");
    private static final String FILE_REFERENCE_STRING = FILE_REFERENCE.getProductIdentifier() + "/" + FILE_REFERENCE.getFilename();

    private FileProcessingIdentifier id;
    private LoggingContextImpl loggingContext;

    private AutoCloseable mocksCloseable;
    @Mock
    private FileProcessingStatistics statistics;

    private static BulletinLogReference bulletinLogReference(final int bulletinIndex) {
        return BulletinLogReference.builder()//
                .setBulletinIndex(bulletinIndex)//
                .build();
    }

    private static MessageLogReference messageLogReference(final int messageIndex) {
        return MessageLogReference.builder()//
                .setMessageIndex(messageIndex)//
                .build();
    }

    @BeforeEach
    void setUp() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        id = FileProcessingIdentifier.newInstance();
        loggingContext = new LoggingContextImpl(id, statistics);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    private void assertEmptyState() {
        assertState(null, null, null);
    }

    private void assertState(@Nullable final FileReference expectedFileReference, final @Nullable BulletinLogReference expectedBulletinLogReference,
            final @Nullable MessageLogReference expectedMessageLogReference) {
        final int expectedBulletinIndex = expectedBulletinLogReference == null ? -1 : expectedBulletinLogReference.getBulletinIndex();
        final int expectedMessageIndex = expectedMessageLogReference == null ? -1 : expectedMessageLogReference.getMessageIndex();
        assertSoftly(softly -> {
            softly.assertThat(loggingContext.getFileReference().orElse(null))//
                    .as("fileReference")//
                    .isEqualTo(expectedFileReference);
            softly.assertThat(loggingContext.getBulletinLogReference().orElse(null))//
                    .as("bulletinLogReference")//
                    .isEqualTo(expectedBulletinLogReference);
            softly.assertThat(loggingContext.getBulletinIndex())//
                    .as("bulletinIndex")//
                    .isEqualTo(expectedBulletinIndex);
            softly.assertThat(loggingContext.getMessageLogReference().orElse(null))//
                    .as("messageLogReference")//
                    .isEqualTo(expectedMessageLogReference);
            softly.assertThat(loggingContext.getMessageIndex())//
                    .as("messageIndex")//
                    .isEqualTo(expectedMessageIndex);
        });
    }

    @Test
    void has_initially_empty_state() {
        assertEmptyState();
    }

    @Test
    void enterFile_sets_fileReference() {
        loggingContext.enterFile(FILE_REFERENCE);

        assertState(FILE_REFERENCE, null, null);
    }

    @Test
    void enterFile_equal_FileReference_resets_bulletin_and_message() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder().build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);

        assertState(fileReference2, null, null);
    }

    @Test
    void enterFile_different_FileReference_resets_bulletin_and_message() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder()//
                .setFilename("file2.txt")//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);

        assertState(fileReference2, null, null);
    }

    @Test
    void enterFile_equal_FileReference_retains_known_BulletinLogReferences() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder().build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);

        assertThat(loggingContext.getAllBulletinLogReferences()).containsExactly(bulletinLogReference(0), bulletinLogReference(1), bulletinLogReference(2));
    }

    @Test
    void enterFile_equal_FileReference_retains_known_BulletinMessageLogReferences() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder().build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);

        assertSoftly(softly -> {
            loggingContext.enterBulletin(0);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 0").isEmpty();
            loggingContext.enterBulletin(1);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 1").isEmpty();
            loggingContext.enterBulletin(2);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences())
                    .as("bulletin 2")
                    .containsExactly(messageLogReference(0), messageLogReference(1), messageLogReference(2), messageLogReference(3));
        });
    }

    @Test
    void enterFile_equal_FileReference_retains_statistics() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder().build();
        loggingContext.enterFile(FILE_REFERENCE);
        clearInvocations(statistics);

        loggingContext.enterFile(fileReference2);

        verifyNoInteractions(statistics);
    }

    @Test
    void enterFile_different_FileReference_clears_known_BulletinLogReferences() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder()//
                .setFilename("file2.txt")//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);

        assertThat(loggingContext.getAllBulletinLogReferences()).isEmpty();
    }

    @Test
    void enterFile_different_FileReference_clears_known_BulletinMessageLogReferences() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder()//
                .setFilename("file2.txt")//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(fileReference2);
        loggingContext.enterBulletin(2);

        assertSoftly(softly -> {
            loggingContext.enterBulletin(0);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 0").isEmpty();
            loggingContext.enterBulletin(1);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 1").isEmpty();
            loggingContext.enterBulletin(2);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 2").isEmpty();
        });
    }

    @Test
    void enterFile_different_FileReference_clears_statistics() {
        final FileReference fileReference2 = FILE_REFERENCE.toBuilder()//
                .setFilename("file2.txt")//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        clearInvocations(statistics);

        loggingContext.enterFile(fileReference2);

        verify(statistics).clear();
    }

    @Test
    void enterFile_null_resets_file_and_bulletin_and_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterFile(null);

        assertEmptyState();
    }

    @Test
    void leaveFile_resets_file_and_bulletin_and_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.leaveFile();

        assertEmptyState();
    }

    @Test
    void leaveFile_clears_known_BulletinLogReferences() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.leaveFile();

        assertThat(loggingContext.getAllBulletinLogReferences()).isEmpty();
    }

    @Test
    void leaveFile_clears_known_BulletinMessageLogReferences() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.leaveFile();

        assertSoftly(softly -> {
            loggingContext.enterBulletin(0);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 0").isEmpty();
            loggingContext.enterBulletin(1);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 1").isEmpty();
            loggingContext.enterBulletin(2);
            softly.assertThat(loggingContext.getBulletinMessageLogReferences()).as("bulletin 2").isEmpty();
        });
    }

    @Test
    void leaveFile_clears_statistics() {
        loggingContext.enterFile(FILE_REFERENCE);
        clearInvocations(statistics);

        loggingContext.leaveFile();

        verify(statistics).clear();
    }

    @Test
    void enterBulletin_BulletinLogReference_sets_bulletinLogReference() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();

        loggingContext.enterBulletin(bulletinLogReference);

        assertState(null, bulletinLogReference, null);
    }

    @Test
    void enterBulletin_BulletinLogReference_sets_bulletinLogReference_on_subsequent_invocations() {
        final BulletinLogReference bulletinLogReference1a = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();
        final BulletinLogReference bulletinLogReference1b = bulletinLogReference1a.toBuilder()//
                .setBulletinHeading("MODIFIED HEADING")//
                .build();

        loggingContext.enterBulletin(bulletinLogReference1a);
        loggingContext.enterBulletin(bulletinLogReference1b);

        assertState(null, bulletinLogReference1b, null);
    }

    @Test
    void enterBulletin_index_creates_new_bulletinLogReference() {
        loggingContext.enterBulletin(3);

        assertState(null, bulletinLogReference(3), null);
    }

    @Test
    void enterBulletin_BulletinLogReference_and_index_restores_previously_stored_bulletinLogReference() {
        final BulletinLogReference bulletinLogReference1 = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();
        final BulletinLogReference bulletinLogReference2 = bulletinLogReference(2);
        loggingContext.enterFile(FILE_REFERENCE);

        loggingContext.enterBulletin(bulletinLogReference1);
        loggingContext.enterBulletin(2);
        assertState(FILE_REFERENCE, bulletinLogReference2, null);
        loggingContext.enterBulletin(1);
        assertState(FILE_REFERENCE, bulletinLogReference1, null);
    }

    @Test
    void enterBulletin_null_resets_bulletin_and_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterBulletin(null);

        assertState(FILE_REFERENCE, null, null);
    }

    @Test
    void leaveBulletin_resets_bulletin_and_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.leaveBulletin();

        assertState(FILE_REFERENCE, null, null);
    }

    @Test
    void getAllBulletinLogReferences_returns_initially_empty() {
        assertThat(loggingContext.getAllBulletinLogReferences()).isEmpty();
    }

    @Test
    void getAllBulletinLogReferences_returns_all_recorded_and_implicitly_created_bulletinLogReferences() {
        final BulletinLogReference bulletinLogReference0 = bulletinLogReference(0);
        final BulletinLogReference bulletinLogReference1 = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();
        final BulletinLogReference bulletinLogReference2 = bulletinLogReference(2);
        // loggingContext.enterBulletin(0); is implicit
        loggingContext.enterBulletin(2);
        // enter order does not matter
        loggingContext.enterBulletin(bulletinLogReference1);

        final List<BulletinLogReference> result = loggingContext.getAllBulletinLogReferences();

        assertThat(result).containsExactly(bulletinLogReference0, bulletinLogReference1, bulletinLogReference2);
    }

    @Test
    void modifyBulletinReference_does_nothing_when_missing() {
        loggingContext.modifyBulletinLogReference(ref -> ref.toBuilder().setBulletinHeading("MODIFIED HEADING").build());

        assertEmptyState();
    }

    @Test
    void modifyBulletinReference_modifies_current_BulletinLogReference_when_exists() {
        final BulletinLogReference modifiedReference = bulletinLogReference(1).toBuilder().setBulletinHeading("MODIFIED HEADING").build();
        loggingContext.enterBulletin(1);

        loggingContext.modifyBulletinLogReference(ref -> modifiedReference);

        assertState(null, modifiedReference, null);
    }

    @Test
    void modifyBulletinReference_handles_change_of_bulletinIndex() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();
        final BulletinLogReference modifiedReference = bulletinLogReference.toBuilder()//
                .setBulletinIndex(2)//
                .build();
        loggingContext.enterBulletin(bulletinLogReference);

        loggingContext.modifyBulletinLogReference(ref -> modifiedReference);

        assertState(null, modifiedReference, null);
        loggingContext.enterBulletin(bulletinLogReference.getBulletinIndex());
        assertState(null, bulletinLogReference, null);
    }

    @Test
    void enterMessage_MessageLogReference_sets_messageLogReference() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(123)//
                .build();
        final MessageLogReference messageLogReference = MessageLogReference.builder()//
                .setMessageIndex(2)//
                .setMessageContent("MESSAGE CONTENT")//
                .build();
        loggingContext.enterBulletin(bulletinLogReference);

        loggingContext.enterMessage(messageLogReference);

        assertState(null, bulletinLogReference, messageLogReference);
    }

    @Test
    void enterMessage_MessageLogReference_sets_initial_bulletinLogReference_if_absent() {
        final MessageLogReference messageLogReference = MessageLogReference.builder()//
                .setMessageIndex(2)//
                .setMessageContent("MESSAGE CONTENT")//
                .build();

        loggingContext.enterMessage(messageLogReference);

        assertState(null, bulletinLogReference(0), messageLogReference);
    }

    @Test
    void enterMessage_MessageLogReference_sets_messageLogReference_on_subsequent_invocations() {
        final MessageLogReference messageLogReference1a = MessageLogReference.builder()//
                .setMessageIndex(1)//
                .setMessageContent("TEST MESSAGE")//
                .build();
        final MessageLogReference messageLogReference1b = messageLogReference1a.toBuilder()//
                .setMessageContent("MODIFIED MESSAGE")//
                .build();

        loggingContext.enterMessage(messageLogReference1a);
        loggingContext.enterMessage(messageLogReference1b);

        assertState(null, bulletinLogReference(0), messageLogReference1b);
    }

    @Test
    void enterMessage_index_creates_new_messageLogReference() {
        loggingContext.enterMessage(3);

        assertState(null, bulletinLogReference(0), messageLogReference(3));
    }

    @Test
    void enterMessage_MessageLogReference_and_index_restores_previously_stored_messageLogReference() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(321)//
                .build();
        final MessageLogReference messageLogReference2 = MessageLogReference.builder()//
                .setMessageIndex(2)//
                .setMessageContent("TEST MESSAGE")//
                .build();
        final MessageLogReference messageLogReference3 = messageLogReference(3);
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(bulletinLogReference);

        loggingContext.enterMessage(messageLogReference2);
        loggingContext.enterMessage(3);
        assertState(FILE_REFERENCE, bulletinLogReference, messageLogReference3);
        loggingContext.enterMessage(2);
        assertState(FILE_REFERENCE, bulletinLogReference, messageLogReference2);
    }

    @Test
    void enterMessage_MessageReference_enters_bulletin_and_message() {
        loggingContext.enterMessage(MessageReference.getInstance(2, 4));

        assertState(null, bulletinLogReference(2), messageLogReference(4));
    }

    @Test
    void enterMessage_null_resets_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.enterMessage((MessageLogReference) null);

        assertState(FILE_REFERENCE, bulletinLogReference(2), null);
    }

    @Test
    void leaveMessage_resets_message() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(3);

        loggingContext.leaveMessage();

        assertState(FILE_REFERENCE, bulletinLogReference(2), null);
    }

    @Test
    void getBulletinMessageLogReferences_returns_initially_empty() {
        assertThat(loggingContext.getBulletinMessageLogReferences()).isEmpty();
    }

    @Test
    void getBulletinMessageLogReferences_returns_all_recorded_and_implicitly_created_bulletinLogReferences() {
        final MessageLogReference messageLogReference0 = messageLogReference(0);
        final MessageLogReference messageLogReference1 = MessageLogReference.builder()//
                .setMessageIndex(1)//
                .setMessageContent("TEST HEADING")//
                .build();
        final MessageLogReference messageLogReference2 = messageLogReference(2);
        // loggingContext.enterMessage(0); is implicit
        loggingContext.enterMessage(2);
        // enter order does not matter
        loggingContext.enterMessage(messageLogReference1);

        final List<MessageLogReference> result = loggingContext.getBulletinMessageLogReferences();

        assertThat(result).containsExactly(messageLogReference0, messageLogReference1, messageLogReference2);
    }

    @Test
    void modifyMessageLogReference_does_nothing_when_missing() {
        loggingContext.modifyMessageLogReference(ref -> ref.toBuilder().setMessageContent("MODIFIED CONTENT").build());

        assertEmptyState();
    }

    @Test
    void modifyMessageLogReference_modifies_current_MessageLogReference_when_exists() {
        final MessageLogReference modifiedReference = messageLogReference(1).toBuilder().setMessageContent("MODIFIED CONTENT").build();
        loggingContext.enterMessage(1);

        loggingContext.modifyMessageLogReference(ref -> modifiedReference);

        assertState(null, bulletinLogReference(0), modifiedReference);
    }

    @Test
    void modifyMessageLogReference_handles_change_of_messageIndex() {
        final BulletinLogReference bulletinLogReference = bulletinLogReference(0);
        final MessageLogReference messageLogReference = MessageLogReference.builder()//
                .setMessageIndex(1)//
                .setMessageContent("TEST CONTENT")//
                .build();
        final MessageLogReference modifiedReference = messageLogReference.toBuilder()//
                .setMessageIndex(2)//
                .build();
        loggingContext.enterMessage(messageLogReference);

        loggingContext.modifyMessageLogReference(ref -> modifiedReference);

        assertState(null, bulletinLogReference, modifiedReference);
        loggingContext.enterMessage(messageLogReference.getMessageIndex());
        assertState(null, bulletinLogReference, messageLogReference);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_minimal_content() {
        LoggableTests.assertDecentLengthEstimate(loggingContext);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate_on_long_content() {
        loggingContext.enterFile(FileReference.builder()//
                .setProductIdentifier("a_product_identifier_for_a_test")//
                .setFilename("A long file name just for testing purposes that is assumable going to be truncated at some point but to achieve this goal "
                        + "the file name must be over 128 characters long.txt")//
                .build());
        loggingContext.enterBulletin(BulletinLogReference.builder()//
                .setBulletinIndex(100)//
                .setBulletinHeading("TEST HEADING THAT IS QUITE LONG FOR A HEADING STILL BEING JUST A DUMMY STRING")//
                .setCharIndex(123456)//
                .build());
        loggingContext.enterMessage(MessageLogReference.builder()//
                .setMessageIndex(1000)//
                .setMessageContent("TEST CONTENT THAT IS VERY LONG CONTAINING NO INFORMATION OF WHATEVER SUBJECT AND GOES JUST BLA BLA BLA...")//
                .build());

        LoggableTests.assertDecentLengthEstimate(loggingContext);
    }

    @Test
    void toString_returns_initially_only_fileProcessingIdentifier() {
        assertThat(loggingContext.toString()).isEqualTo(id + "");
    }

    @Test
    void toString_file() {
        loggingContext.enterFile(FILE_REFERENCE);

        assertThat(loggingContext.toString()).isEqualTo(id + ":" + FILE_REFERENCE_STRING);
    }

    @Test
    void toString_file_bulletin() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(321)//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(bulletinLogReference);

        assertThat(loggingContext.toString()).isEqualTo(id + ":" + FILE_REFERENCE_STRING + ":" + bulletinLogReference);
    }

    @Test
    void toString_file_bulletin_message() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(321)//
                .build();
        final MessageLogReference messageLogReference = MessageLogReference.builder()//
                .setMessageIndex(2)//
                .setMessageContent("MESSAGE CONTENT")//
                .build();
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(bulletinLogReference);
        loggingContext.enterMessage(messageLogReference);

        assertThat(loggingContext.toString()).isEqualTo(id + ":" + FILE_REFERENCE_STRING + ":" + bulletinLogReference + ":" + messageLogReference);
    }

    @Test
    void toString_bulletin_message() {
        final BulletinLogReference bulletinLogReference = BulletinLogReference.builder()//
                .setBulletinIndex(1)//
                .setBulletinHeading("TEST HEADING")//
                .setCharIndex(321)//
                .build();
        final MessageLogReference messageLogReference = MessageLogReference.builder()//
                .setMessageIndex(2)//
                .setMessageContent("MESSAGE CONTENT")//
                .build();
        loggingContext.enterBulletin(bulletinLogReference);
        loggingContext.enterMessage(messageLogReference);

        assertThat(loggingContext.toString()).isEqualTo(id + "::" + bulletinLogReference + ":" + messageLogReference);
    }

    @Test
    void getStatistics_returns_provided_statistics() {
        assertThat(loggingContext.getStatistics()).isSameAs(statistics);
    }

    @Test
    void initStatistics_does_nothing_in_initial_state() {
        loggingContext.initStatistics();

        verify(statistics).initBulletins(0);
        verify(statistics, never()).initMessages(anyInt(), anyInt());
    }

    @Test
    void initStatistics_initializes_known_bulletins_and_messages() {
        loggingContext.enterBulletin(2);
        loggingContext.enterMessage(4);

        loggingContext.initStatistics();

        verify(statistics).initBulletins(3);
        verify(statistics, atMostOnce()).initMessages(0, 0);
        verify(statistics, atMostOnce()).initMessages(1, 0);
        verify(statistics).initMessages(2, 5);
        verify(statistics, never()).initMessages(eq(3), anyInt());
    }

    @Test
    void recordStatus_initially_records_file_status() {
        loggingContext.recordStatus(Status.ARCHIVED);

        verify(statistics).recordFileStatus(Status.ARCHIVED);
    }

    @Test
    void recordStatus_on_file_records_file_status() {
        loggingContext.enterFile(FILE_REFERENCE);

        loggingContext.recordStatus(Status.ARCHIVED);

        verify(statistics).recordFileStatus(Status.ARCHIVED);
    }

    @Test
    void recordStatus_on_bulletin_records_bulletin_status() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(1);

        loggingContext.recordStatus(Status.ARCHIVED);

        verify(statistics).recordBulletinStatus(1, Status.ARCHIVED);
    }

    @Test
    void recordStatus_on_message_records_message_status() {
        loggingContext.enterFile(FILE_REFERENCE);
        loggingContext.enterBulletin(1);
        loggingContext.enterMessage(2);

        loggingContext.recordStatus(Status.ARCHIVED);

        verify(statistics).recordMessageStatus(1, 2, Status.ARCHIVED);
    }
}
