package fi.fmi.avi.archiver;

import com.google.common.reflect.ClassPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootTestSelfTest {
    private static final String TEST_CLASS_PROPERTY_NAME = "testclass.name";
    private final String TEST_CLASS_PROPERTY_PREFIX = TEST_CLASS_PROPERTY_NAME + "=";

    static Stream<? extends Class<?>> springBootTestClassesProvider() throws IOException {
        final Class<SpringBootTestSelfTest> thisClass = SpringBootTestSelfTest.class;
        final String rootPackage = thisClass.getPackageName();
        return ClassPath.from(thisClass.getClassLoader())
                .getAllClasses()
                .stream()
                .filter(classpathElement ->
                        classpathElement.getPackageName().startsWith(rootPackage)
                                && classpathElement.getSimpleName().endsWith("Test"))
                .map(ClassPath.ClassInfo::load)
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
}
