package fi.fmi.avi.archiver.config;

import com.google.common.base.Preconditions;
import fi.fmi.avi.archiver.AviationMessageArchiver;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

public abstract class AbstractConfigValidityTest {

    private ConfigurableApplicationContext applicationContext;

    protected AbstractConfigValidityTest() {
    }

    protected static String containsWord(final String word) {
        requireNonNull(word, "word");
        return "^.*\\b(?:" + word + ")\\b.*$";
    }

    @AfterEach
    void closeApplicationContext() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    private SpringApplication createContextBuilder(final String testProfile) {
        final String testClassName = getClass().getCanonicalName();
        return new SpringApplicationBuilder()//
                .bannerMode(Banner.Mode.OFF)//
                .sources(AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class)//
                .properties(//
                        "testclass.name=" + testClassName, //
                        "spring.config.additional-location=classpath:" + testClassName.replace('.', '/') + ".yml"
                )//
                .profiles("integration-test", "local", "h2", testProfile)//
                .build();
    }

    protected void assertThatNoExceptionIsThrownByProfile(final String profile) {
        requireNonNull(profile, "profile");
        Preconditions.checkArgument(!profile.isEmpty(), "profile is empty");
        final SpringApplication application = createContextBuilder(profile);
        assertThatNoException()//
                .isThrownBy(() -> applicationContext = application.run());
    }

    protected ThrowableAssertAlternative<?> assertThatExceptionIsThrownByProfile(final String profile) {
        requireNonNull(profile, "profile");
        Preconditions.checkArgument(!profile.isEmpty(), "profile is empty");
        final SpringApplication application = createContextBuilder(profile);
        return assertThatExceptionOfType(RuntimeException.class)//
                .isThrownBy(() -> {
                    try {
                        applicationContext = application.run();
                    } catch (final Exception leafException) {
                        Throwable exception = leafException;
                        while (exception.getClass().getName().startsWith("org.springframework.") && exception.getCause() != null) {
                            exception = exception.getCause();
                        }
                        throw exception;
                    }
                });
    }

    protected <T> T bean(final String name, final Class<T> beanType) {
        requireNonNull(name, "name");
        requireNonNull(beanType, "beanType");
        return applicationContext.getBean(name, beanType);
    }
}
