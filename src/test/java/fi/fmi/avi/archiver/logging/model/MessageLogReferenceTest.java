package fi.fmi.avi.archiver.logging.model;

import static fi.fmi.avi.archiver.logging.model.MessageLogReference.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import fi.fmi.avi.archiver.logging.LoggableTests;

final class MessageLogReferenceTest {
    @Test
    void readableCopy_returns_same_instance() {
        final MessageLogReference messageLogReference = MessageLogReference.builder().buildPartial();
        assertThat(messageLogReference.readableCopy()).isSameAs(messageLogReference);
    }

    @Test
    void getStructureName_returns_expected_name() {
        final MessageLogReference messageLogReference = MessageLogReference.builder().buildPartial();
        assertThat(messageLogReference.getStructureName()).isEqualTo("messageLogReference");
    }

    @Test
    void messageIndex_below_zero_is_forbidden() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().setIndex(-1));
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate() {
        final MessageLogReference messageLogReference = builder()//
                .setIndex(100)//
                .setContent(LoggableTests.createString(100))//
                .build();
        LoggableTests.assertDecentLengthEstimate(messageLogReference);
    }

    @ArgumentsSource(TestValuesProvider.class)
    @ParameterizedTest
    void toString_returns_expected_value(final String expectedStringTemplate, @Nullable final String expectedExcerpt,
            final MessageLogReference messageLogReference) {
        final String expectedString = expectedExcerpt == null ? expectedStringTemplate : expectedStringTemplate.replace("%s", expectedExcerpt);
        assertThat(messageLogReference.toString()).isEqualTo(expectedString);
    }

    @ArgumentsSource(TestValuesProvider.class)
    @ParameterizedTest
    void messageExcerpt_is_expected(final String expectedStringTemplate, @Nullable final String expectedExcerpt,
            final MessageLogReference messageLogReference) {
        assertThat(messageLogReference.getExcerpt().orElse(null)).isEqualTo(expectedExcerpt);
    }

    @Test
    void getMessageExcerpt_returns_empty_when_getMessageContent_returns_empty() {
        final MessageLogReference messageLogReference = builder()//
                .clearContent()//
                .buildPartial();
        assertThat(messageLogReference.getExcerpt()).isEmpty();
    }

    static class TestValuesProvider implements ArgumentsProvider {
        private static Arguments args(final String expectedStringTemplate, @Nullable final String expectedExcerpt,
                final MessageLogReference messageLogReference) {
            return Arguments.of(expectedStringTemplate, expectedExcerpt, messageLogReference);
        }

        @Override
        public Stream<Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of(//
                    args("0", null, MessageLogReference.builder()//
                            .setIndex(0)//
                            .build()), //
                    args("1", null, MessageLogReference.builder()//
                            .setIndex(1)//
                            .clearContent()//
                            .build()), //
                    args("3(%s)", "message content", MessageLogReference.builder()//
                            .setIndex(3)//
                            .setContent("message content")//
                            .build()), //
                    args("5(%s)", "Overlong message content with ?œø? char...", MessageLogReference.builder()//
                            .setIndex(5)//
                            .setContent("Overlong \n message \t\t content     with €œø\u0000\u0888 characters and extra whitespaces")//
                            .build()), //
                    args("6(%s)", "my-test-iwxxm-document-id", MessageLogReference.builder()//
                            .setIndex(6)//
                            .setContent(//
                                    "<iwxxm:iwxxmMessage " //
                                            + "attr1=\"value1\" \n\t" //
                                            + "iwxxm:attr2=\"value2\"   " //
                                            + "ns1:id=\"my-test-iwxxm-document-id\" " //
                                            + "ns2:attr3=\"value3\" >" //
                                            + "\n\t...\n" //
                                            + "</iwxxm:iwxxmMessage>")//
                            .build()), //
                    args("7(%s)", "my-?very??-#!?œø?-overly-long-test-@iwx...", MessageLogReference.builder()//
                            .setIndex(7)//
                            .setContent(//
                                    "<iwxxmMessage id=\"my-(very:)-#!€œø\u0000\u0888-overly-long-test-@iwxxm-document-id-of-plain-crap\">" //
                                            + "\n\t...\n" //
                                            + "</iwxxmMessage>")//
                            .build()), //
                    args("8(%s)", "min-msg", MessageLogReference.builder()//
                            .setIndex(8)//
                            .setContent("<minimalMsg id=\"min-msg\" />")//
                            .build()), //
                    args("9(%s)", "?any uid=\"123\"?...?/any?", MessageLogReference.builder()//
                            .setIndex(9)//
                            .setContent("<any uid=\"123\">...</any>")//
                            .build()));
        }
    }
}
