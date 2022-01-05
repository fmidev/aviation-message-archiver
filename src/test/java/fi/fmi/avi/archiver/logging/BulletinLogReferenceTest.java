package fi.fmi.avi.archiver.logging;

import static fi.fmi.avi.archiver.logging.BulletinLogReference.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class BulletinLogReferenceTest {
    static Stream<Arguments> logString_is_expected() {
        return Stream.of(//
                Arguments.of("1", BulletinLogReference.builder()//
                        .setBulletinIndex(0)//
                        .build()), //
                Arguments.of("2", BulletinLogReference.builder()//
                        .setBulletinIndex(1)//
                        .clearBulletinHeading()//
                        .setCharIndex(-5)//
                        .build()), //
                Arguments.of("3@426", BulletinLogReference.builder()//
                        .setBulletinIndex(2)//
                        .setCharIndex(425)//
                        .build()), //
                Arguments.of("4(bulletin heading)", BulletinLogReference.builder()//
                        .setBulletinIndex(3)//
                        .setBulletinHeading("bulletin heading")//
                        .build()), //
                Arguments.of("5(BULL HEAD)@7943", BulletinLogReference.builder()//
                        .setBulletinIndex(4)//
                        .setBulletinHeading("BULL HEAD")//
                        .setCharIndex(7942)//
                        .build()), //
                Arguments.of("6(Overlong bulletin heading with ?œø? chara...)@16255", BulletinLogReference.builder()//
                        .setBulletinIndex(5)//
                        .setBulletinHeading("Overlong \n bulletin \t\t heading     with €œø\u0000\u0888 characters and extra whitespaces")//
                        .setCharIndex(16_254)//
                        .build()));
    }

    @Test
    void satisfies_AppendingLoggable_contract() {
        final BulletinLogReference loggable = BulletinLogReference.builder()//
                .setBulletinIndex(47)//
                .setBulletinHeading(LoggableTests.createString(37))//
                .setCharIndex(2022)//
                .build();
        LoggableTests.assertAppendingLoggableContract(loggable);
    }

    @Test
    void bulletinIndex_below_zero_is_forbidden() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().setBulletinIndex(-1));
    }

    @Test
    void negative_charIndex_is_normalized_to_minus_one() {
        final BulletinLogReference bulletinLogReference = builder()//
                .setCharIndex(-2)//
                .buildPartial();
        assertThat(bulletinLogReference.getCharIndex()).isEqualTo(-1);
    }

    @Test
    void estimateLogStringLength_returns_decent_estimate() {
        final BulletinLogReference bulletinLogReference = builder()//
                .setBulletinIndex(100)//
                .setBulletinHeading(LoggableTests.createString(100))//
                .setCharIndex(100_000)//
                .build();
        LoggableTests.assertDecentLengthEstimate(bulletinLogReference);
    }

    @MethodSource
    @ParameterizedTest
    void logString_is_expected(final String expectedString, final BulletinLogReference bulletinLogReference) {
        assertThat(bulletinLogReference.toString()).isEqualTo(expectedString);
    }
}
