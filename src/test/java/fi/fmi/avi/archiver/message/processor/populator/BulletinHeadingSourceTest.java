package fi.fmi.avi.archiver.message.processor.populator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulletinHeadingSourceTest {
    @Test
    void defaultBulletinHeadingSources_is_not_empty() {
        assertThat(BulletinHeadingSource.DEFAULT_SOURCES).isNotEmpty();
    }

    @Test
    void permutations_contains_no_empty_lists() {
        assertThat(BulletinHeadingSource.getPermutations())//
                .allSatisfy(permutation -> assertThat(permutation).isNotEmpty());
    }
}