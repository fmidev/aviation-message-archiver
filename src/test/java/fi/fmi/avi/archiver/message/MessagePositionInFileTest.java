package fi.fmi.avi.archiver.message;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.testing.EqualsTester;

@SuppressWarnings("UnstableApiUsage")
class MessagePositionInFileTest {
    @Test
    void getInstance_fails_if_bulletinIndex_is_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessagePositionInFile.getInstance(-1, 0));
    }

    @Test
    void getInstance_fails_if_messageIndex_is_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessagePositionInFile.getInstance(0, -1));
    }

    @Test
    void getInstance_fails_if_all_parameters_are_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessagePositionInFile.getInstance(-1, -1));
    }

    @Test
    void testEquals() {
        final MessagePositionInFile instance = MessagePositionInFile.getInstance(0, 1);
        new EqualsTester()//
                .addEqualityGroup(MessagePositionInFile.getInitial(), MessagePositionInFile.getInitial(), MessagePositionInFile.getInstance(0, 0))//
                .addEqualityGroup(instance, instance, MessagePositionInFile.getInstance(0, 1))//
                .addEqualityGroup(MessagePositionInFile.getInstance(1, 0))//
                .addEqualityGroup(MessagePositionInFile.getInstance(1, 1))//
                .testEquals();
    }
}
