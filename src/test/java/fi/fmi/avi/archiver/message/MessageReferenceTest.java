package fi.fmi.avi.archiver.message;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.testing.EqualsTester;

@SuppressWarnings("UnstableApiUsage")
class MessageReferenceTest {
    @Test
    void getInstance_fails_if_bulletinIndex_is_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessageReference.getInstance(-1, 0));
    }

    @Test
    void getInstance_fails_if_messageIndex_is_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessageReference.getInstance(0, -1));
    }

    @Test
    void getInstance_fails_if_all_parameters_are_negative() {
        assertThatIllegalArgumentException().isThrownBy(() -> MessageReference.getInstance(-1, -1));
    }

    @Test
    void testEquals() {
        final MessageReference instance = MessageReference.getInstance(0, 1);
        new EqualsTester()//
                .addEqualityGroup(MessageReference.getInitial(), MessageReference.getInitial(), MessageReference.getInstance(0, 0))//
                .addEqualityGroup(instance, instance, MessageReference.getInstance(0, 1))//
                .addEqualityGroup(MessageReference.getInstance(1, 0))//
                .addEqualityGroup(MessageReference.getInstance(1, 1))//
                .testEquals();
    }
}
