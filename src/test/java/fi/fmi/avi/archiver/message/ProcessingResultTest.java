package fi.fmi.avi.archiver.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import fi.fmi.avi.archiver.logging.LoggableTests;

class ProcessingResultTest {
    @EnumSource(ProcessingResult.class)
    @ParameterizedTest
    void readableCopy_returns_same_instance(final ProcessingResult processingResult) {
        assertThat(processingResult.readableCopy()).isSameAs(processingResult);
    }

    @EnumSource(ProcessingResult.class)
    @ParameterizedTest
    void getStructureName_returns_expected_name(final ProcessingResult processingResult) {
        assertThat(processingResult.getStructureName()).isEqualTo("rejectReason");
    }

    @EnumSource(ProcessingResult.class)
    @ParameterizedTest
    void estimateLogStringLength_returns_decent_estimate(final ProcessingResult processingResult) {
        LoggableTests.assertDecentLengthEstimate(processingResult);
    }

    @Test
    void getCode_returns_unique_value() {
        final int[] codes = Arrays.stream(ProcessingResult.values())//
                .mapToInt(ProcessingResult::getCode)//
                .toArray();

        assertThat(codes).doesNotHaveDuplicates();
    }
}
