package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class ConditionPropertyReaderRegistryTest {
    private static final String READER_NAME = "readerName";

    @Mock
    private ConditionPropertyReader<?> reader;
    private AutoCloseable mocks;

    private ConditionPropertyReaderRegistry registry;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        registry = new ConditionPropertyReaderRegistry();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void getInstance_given_unknown_property_name_throws_exception() {
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> registry.getInstance(READER_NAME))//
                .withMessageContaining(READER_NAME);
    }

    @Test
    void getInstance_returns_registered_ConditionPropertyReader() {
        registry.register(READER_NAME, reader);
        assertThat(registry.getInstance(READER_NAME)).isSameAs(reader);
    }

    @Test
    void register_given_reader_registers_reader_for_name_provided_by_reader() {
        when(reader.getPropertyName()).thenReturn(READER_NAME);

        registry.register(reader);

        assertThat(registry.getInstance(READER_NAME)).isSameAs(reader);
    }

    @Test
    void unregister_removes_reader_from_registry() {
        registry.register(READER_NAME, reader);
        assertThat(registry.getInstance(READER_NAME)).isSameAs(reader);

        registry.unregister(READER_NAME);

        assertThatIllegalArgumentException().isThrownBy(() -> registry.getInstance(READER_NAME));
    }
}
