package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;

public abstract class BulletinHeadingSourceSensitiveTester<T> {
    private static final InputBulletinHeading EMPTY_HEADING = InputBulletinHeading.builder().build();

    protected abstract InputAviationMessage getInput();

    protected abstract Map<BulletinHeadingSource, T> getExpectedResults();

    @Nullable
    protected abstract T invoke(InputAviationMessage input, List<BulletinHeadingSource> bulletinHeadingSources);

    @ParameterizedTest
    @ArgumentsSource(ActualIndexAndBulletinHeadingSourcesProvider.class)
    final void readValue_returns_first_found_value_in_order_of_bulletinHeadingSources(final int actualIndex,
            final List<BulletinHeadingSource> bulletinHeadingSources) {
        final InputAviationMessage.Builder inputBuilder = getInput().toBuilder();
        bulletinHeadingSources.subList(0, actualIndex)//
                .forEach(bulletinHeadingSource -> bulletinHeadingSource.set(inputBuilder, EMPTY_HEADING));

        final T result = invoke(inputBuilder.build(), bulletinHeadingSources);

        final T expected = getExpectedResults().get(bulletinHeadingSources.get(actualIndex));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    final void readValue_given_empty_input_returns_null() {
        final InputAviationMessage.Builder inputBuilder = getInput().toBuilder();
        final List<BulletinHeadingSource> bulletinHeadingSources = Arrays.asList(BulletinHeadingSource.values());
        bulletinHeadingSources//
                .forEach(bulletinHeadingSource -> bulletinHeadingSource.set(inputBuilder, EMPTY_HEADING));

        final T result = invoke(inputBuilder.build(), bulletinHeadingSources);

        assertThat(result).isNull();
    }

    static final class ActualIndexAndBulletinHeadingSourcesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            final int permutationMaxSize = BulletinHeadingSource.values().length;
            return BulletinHeadingSource.getPermutations().stream()//
                    .flatMap(permutation -> IntStream.range(0, Math.min(permutation.size(), permutationMaxSize))//
                            .mapToObj(actualIndex -> Arguments.of(actualIndex, permutation)));
        }
    }
}
