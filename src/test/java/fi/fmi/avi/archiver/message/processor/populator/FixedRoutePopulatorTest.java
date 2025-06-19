package fi.fmi.avi.archiver.message.processor.populator;

import com.google.common.testing.NullPointerTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class FixedRoutePopulatorTest {
    private static final MessagePopulatorTests.RouteId ROUTE_ID = MessagePopulatorTests.RouteId.TEST2;
    private FixedRoutePopulator messagePopulator;

    @BeforeEach
    void setUp() {
        messagePopulator = new FixedRoutePopulator(MessagePopulatorTests.ROUTE_IDS, ROUTE_ID.getName());
    }

    @Test
    void populate_sets_fixed_route_id() {
        final MessageProcessorContext context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();

        messagePopulator.populate(context, target);

        assertThat(target.getRoute()).isEqualTo(ROUTE_ID.getId());
    }

    @Test
    void populator_with_empty_route() {
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> new FixedRoutePopulator(Collections.emptyMap(), ""));
    }

    @Test
    void populator_with_illegal_route() {
        final String routeName = "test";
        assertThatIllegalArgumentException()//
                .isThrownBy(() -> new FixedRoutePopulator(MessagePopulatorTests.ROUTE_IDS, routeName))//
                .withMessageContaining(routeName);
    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void testNulls() {
        final Class<?> classUnderTest = FixedRoutePopulator.class;
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(messagePopulator, minimalVisibility);
    }
}
