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
    static Stream<Arguments> toString_returns_expected_value() {
        return Stream.of(//
                Arguments.of("0", BulletinLogReference.builder()//
                        .setIndex(0)//
                        .build()), //
                Arguments.of("1", BulletinLogReference.builder()//
                        .setIndex(1)//
                        .clearHeading()//
                        .setCharIndex(-5)//
                        .build()), //
                Arguments.of("2@425", BulletinLogReference.builder()//
                        .setIndex(2)//
                        .setCharIndex(425)//
                        .build()), //
                Arguments.of("3(bulletin heading)", BulletinLogReference.builder()//
                        .setIndex(3)//
                        .setHeading("bulletin heading")//
                        .build()), //
                Arguments.of("4(BULL HEAD)@7942", BulletinLogReference.builder()//
                        .setIndex(4)//
                        .setHeading("BULL HEAD")//
                        .setCharIndex(7942)//
                        .build()), //
                Arguments.of("5(Overlong bulletin heading with ?œø? chara...)@16254", BulletinLogReference.builder()//
                        .setIndex(5)//
                        .setHeading("Overlong \n bulletin \t\t heading     with €œø\u0000\u0888 characters and extra whitespaces")//
                        .setCharIndex(16_254)//
                        .build()));
    }

    @Test
    void readableCopy_returns_same_instance() {
        final BulletinLogReference bulletinLogReference = builder().buildPartial();
        assertThat(bulletinLogReference.readableCopy()).isSameAs(bulletinLogReference);
    }

    @Test
    void getStructureName_returns_expected_name() {
        final BulletinLogReference bulletinLogReference = builder().buildPartial();
        assertThat(bulletinLogReference.getStructureName()).isEqualTo("bulletinLogReference");
    }

    @Test
    void bulletinIndex_below_zero_is_forbidden() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().setIndex(-1));
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
                .setIndex(100)//
                .setHeading(LoggableTests.createString(100))//
                .setCharIndex(100_000)//
                .build();
        LoggableTests.assertDecentLengthEstimate(bulletinLogReference);
    }

    @MethodSource
    @ParameterizedTest
    void toString_returns_expected_value(final String expectedString, final BulletinLogReference bulletinLogReference) {
        assertThat(bulletinLogReference.toString()).isEqualTo(expectedString);
    }
}
