package fi.fmi.avi.archiver.message.populator;

import com.google.common.testing.NullPointerTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.TestMessageProcessorContext;
import fi.fmi.avi.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class FixedTypePopulatorTest {
    private static final MessagePopulatorTests.TypeId TYPE_ID = MessagePopulatorTests.TypeId.METAR;
    private FixedTypePopulator messagePopulator;

    @BeforeEach
    void setUp() {
        messagePopulator = new FixedTypePopulator(MessagePopulatorTests.TYPE_IDS, TYPE_ID.getType());
    }

    @Test
    void populate_sets_fixed_type_id() {
        final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();

        messagePopulator.populate(context, target);

        assertThat(target.getType()).isEqualTo(TYPE_ID.getId());
    }

    @Test
    void populator_with_illegal_type() {
        final MessageType illegalType = new MessageType("ILLEGAL");
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> new FixedTypePopulator(MessagePopulatorTests.TYPE_IDS, illegalType))//
                .withMessageContaining(illegalType.toString());
    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void testNulls() {
        final Class<?> classUnderTest = FixedTypePopulator.class;
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(messagePopulator, minimalVisibility);
    }
}
