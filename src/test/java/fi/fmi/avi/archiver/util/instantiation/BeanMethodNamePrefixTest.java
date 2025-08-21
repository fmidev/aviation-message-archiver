package fi.fmi.avi.archiver.util.instantiation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BeanMethodNamePrefixTest {
    @ParameterizedTest
    @CsvSource(textBlock = """
            "", ""
            a, a
            noPrefix, noPrefix
            is, is
            isA, a
            isnotPrefixed, isnotPrefixed
            isSomeBoolean, someBoolean
            isSOMEBoolean, sOMEBoolean
            isGetSomeBoolean, getSomeBoolean
            ge, ge
            get, get
            getA, a
            getSomeValue, someValue
            getSOMEValue, sOMEValue
            getIsSomeValue, isSomeValue
            se, se
            set, set
            setA, a
            setSomeValue, someValue
            setSOMEValue, sOMEValue
            setGetSomeValue, getSomeValue
            """)
    void stripAny_strips_only_first_prefix_if_any(final String input, final String expected) {
        assertThat(BeanMethodNamePrefix.stripAny(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            IS, "", false
            IS, a, false
            IS, noPrefix, false
            IS, is, false
            IS, isA, true
            IS, isnotPrefixed, false
            IS, isSomething, true
            IS, isSOMEthing, true
            IS, isIsSomething, true
            IS, IsSomething, false
            IS, get, false
            IS, getSomething, false
            GET, "", false
            GET, noPrefix, false
            GET, get, false
            GET, getA, true
            GET, getnotPrefixed, false
            GET, getSomething, true
            GET, getSOMEthing, true
            GET, getGetSomething, true
            GET, GetSomething, false
            GET, is, false
            GET, isSomething, false
            SET, "", false
            SET, noPrefix, false
            SET, set, false
            SET, setA, true
            SET, setnotPrefixed, false
            SET, setSomething, true
            SET, setSOMEthing, true
            SET, setSetSomething, true
            SET, SetSomething, false
            SET, get, false
            SET, getSomething, false
            """)
    void isPrefixed_returns_whether_methodName_is_prefixed(final BeanMethodNamePrefix prefix, final String input, final boolean expected) {
        assertThat(prefix.isPrefixed(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            IS, "", ""
            IS, a, a
            IS, noPrefix, noPrefix
            IS, is, is
            IS, isA, a
            IS, isnotPrefixed, isnotPrefixed
            IS, isSomething, something
            IS, isSOMEthing, sOMEthing
            IS, isIsSomething, isSomething
            IS, IsSomething, IsSomething
            IS, get, get
            IS, getA, getA
            IS, getSomething, getSomething
            GET, "", ""
            GET, noPrefix, noPrefix
            GET, get, get
            GET, getA, a
            GET, getnotPrefixed, getnotPrefixed
            GET, getSomething, something
            GET, getSOMEthing, sOMEthing
            GET, getGetSomething, getSomething
            GET, GetSomething, GetSomething
            GET, is, is
            GET, isA, isA
            GET, isSomething, isSomething
            SET, "", ""
            SET, noPrefix, noPrefix
            SET, set, set
            SET, setA, a
            SET, setnotPrefixed, setnotPrefixed
            SET, setSomething, something
            SET, setSOMEthing, sOMEthing
            SET, setSetSomething, setSomething
            SET, SetSomething, SetSomething
            SET, get, get
            SET, getA, getA
            SET, getSomething, getSomething
            """)
    void strip_strips_only_first_prefix_if_any(final BeanMethodNamePrefix prefix, final String input, final String expected) {
        assertThat(prefix.strip(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', textBlock = """
            IS, "", ""
            IS, a, isA
            IS, is, isIs
            IS, isA, isA
            IS, something, isSomething
            IS, SOMEthing, isSOMEthing
            IS, isSomething, isSomething
            IS, IsSomething, isIsSomething
            IS, issomething, isIssomething
            IS, get, isGet
            IS, getSomething, isGetSomething
            GET, "", ""
            GET, a, getA
            GET, get, getGet
            GET, getA, getA
            GET, something, getSomething
            GET, SOMEthing, getSOMEthing
            GET, getSomething, getSomething
            GET, GetSomething, getGetSomething
            GET, getsomething, getGetsomething
            GET, is, getIs
            GET, isSomething, getIsSomething
            SET, "", ""
            SET, a, setA
            SET, set, setSet
            SET, setA, setA
            SET, something, setSomething
            SET, SOMEthing, setSOMEthing
            SET, setSomething, setSomething
            SET, SetSomething, setSetSomething
            SET, setsomething, setSetsomething
            SET, get, setGet
            SET, getSomething, setGetSomething
            """)
    void prefix_adds_prefix_unless_already_exists(final BeanMethodNamePrefix prefix, final String input, final String expected) {
        assertThat(prefix.prefix(input))
                .isEqualTo(expected);
    }
}
