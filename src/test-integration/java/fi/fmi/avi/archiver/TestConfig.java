package fi.fmi.avi.archiver;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static java.util.Objects.requireNonNull;

@Configuration
public class TestConfig {
    private static final String TEST_CLASS_NAME_MESSAGE = "Set in your test class: @SpringBootTest(\"testclass.name=test.class.FQN\" })";

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2019-05-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    @Bean
    ApplicationConversionService conversionService() {
        return new ApplicationConversionService();
    }

    @Bean
    TestWorkDirHolder testWorkDirHolder(@Value("${testclass.workdir.path}") final Path workdirPath,
                                        @Value("${testclass.name}") final String testclassName) {
        requireNonNull(workdirPath, "workdirPath");
        requireNonNull(testclassName, "testclassName; " + TEST_CLASS_NAME_MESSAGE);
        Preconditions.checkState(!workdirPath.toString().isEmpty(), "workdirPath must not be empty");
        Preconditions.checkState(!testclassName.isEmpty(), "testclassName must not be empty; " + TEST_CLASS_NAME_MESSAGE);
        return new TestWorkDirHolder(workdirPath);
    }

    static class TestWorkDirHolder implements InitializingBean, DisposableBean {
        private final Path workDir;
        private final Path tmpDir;

        public TestWorkDirHolder(final Path workDir) {
            this.workDir = requireNonNull(workDir, "workDir");
            this.tmpDir = workDir.resolve("temp");
        }

        @Override
        public void afterPropertiesSet() throws IOException {
            Files.createDirectories(tmpDir);
            if (!Files.isWritable(tmpDir)) {
                throw new IllegalStateException("Cannot write to " + tmpDir);
            }
        }

        @Override
        public void destroy() throws IOException {
            FileSystemUtils.deleteRecursively(workDir);
        }
    }
}
