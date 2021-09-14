package fi.fmi.avi.archiver.message.populator;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.google.common.testing.NullPointerTester;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

class ReflectionMessagePopulatorFactoryTest {
    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void testNulls() {
        final NullPointerTester tester = new NullPointerTester();
        @SuppressWarnings("rawtypes")
        final Class<ReflectionMessagePopulatorFactory> classUnderTest = ReflectionMessagePopulatorFactory.class;
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PROTECTED;
        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        final ReflectionMessagePopulatorFactory<TestMessagePopulator> instance = new ReflectionMessagePopulatorFactory<>(new TestPropertyConverter(),
                TestMessagePopulator.class);
        tester.testInstanceMethods(instance, minimalVisibility);
    }

    private static class TestPropertyConverter implements AbstractMessagePopulatorFactory.PropertyConverter {
        @Nullable
        @Override
        public Object convert(@Nullable final Object propertyConfigValue, final Method setterMethod) {
            return null;
        }
    }

    public static class TestMessagePopulator implements MessagePopulator {
        @Override
        public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder aviationMessageBuilder) {
        }
    }
}
