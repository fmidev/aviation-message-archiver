package fi.fmi.avi.archiver.message.populator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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