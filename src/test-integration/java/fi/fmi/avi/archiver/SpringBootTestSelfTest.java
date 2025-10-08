package fi.fmi.avi.archiver;

import com.google.common.reflect.ClassPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class SpringBootTestSelfTest {
    private static final String TEST_CLASS_PROPERTY_NAME = "testclass.name";
    private static final String TEST_CLASS_PROPERTY_PREFIX = TEST_CLASS_PROPERTY_NAME + "=";
    private static final String INTEGRATION_TEST_PROFILE_NAME = "integration-test";

    private static Stream<? extends Class<?>> getTestClasses() throws IOException {
        final Class<SpringBootTestSelfTest> thisClass = SpringBootTestSelfTest.class;
        final String rootPackage = thisClass.getPackageName();
        return ClassPath.from(thisClass.getClassLoader())
                .getAllClasses()
                .stream()
                .filter(classpathElement ->
                        classpathElement.getPackageName().startsWith(rootPackage)
                                && classpathElement.getSimpleName().endsWith("Test"))
                .map(ClassPath.ClassInfo::load);
    }

    static Stream<? extends Class<?>> springBootTestClassesProvider() throws IOException {
        return getTestClasses()
                .filter(cls -> cls.getAnnotation(SpringBootTest.class) != null);
    }

    @ParameterizedTest
    @MethodSource("springBootTestClassesProvider")
    void test_class_has_valid_testclass_name(final Class<?> testClass) {
        final SpringBootTest springBootTest = testClass.getAnnotation(SpringBootTest.class);
        final List<String> foundTestClassNameValues = Stream.concat(
                        Arrays.stream(springBootTest.value()),
                        Arrays.stream(springBootTest.properties()))
                .filter(property -> property.startsWith(TEST_CLASS_PROPERTY_PREFIX))
                .map(property -> property.substring(TEST_CLASS_PROPERTY_PREFIX.length()))
                .toList();
        assertThat(foundTestClassNameValues)
                .as(TEST_CLASS_PROPERTY_NAME + " property")
                .containsExactly(testClass.getCanonicalName());
    }

    @ParameterizedTest
    @MethodSource("springBootTestClassesProvider")
    void test_class_activates_integration_test_profile(final Class<?> testClass) {
        assumeThat(testClass.getCanonicalName())
                .as("test classes intentionally not activating the integration-test profile")
                .isNotIn(/* List canonical names of ignored classes here */);

        final ActiveProfiles activeProfiles = testClass.getAnnotation(ActiveProfiles.class);
        final List<String> foundActiveProfiles = Stream.concat(
                        Arrays.stream(activeProfiles.value()),
                        Arrays.stream(activeProfiles.profiles()))
                .toList();
        assertThat(foundActiveProfiles)
                .as(INTEGRATION_TEST_PROFILE_NAME + " profile")
                .containsOnlyOnce(INTEGRATION_TEST_PROFILE_NAME);
    }
}
