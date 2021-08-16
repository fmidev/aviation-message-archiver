package fi.fmi.avi.archiver;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Preconditions;

@Configuration
public class TestConfig {
    private static final String TEST_CLASS_NAME_MESSAGE = "Set in your test class: @SpringBootTest(\"testclass.name=test.class.FQN\" })";

    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse("2019-05-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    @Bean
    public ApplicationConversionService conversionService() {
        return new ApplicationConversionService();
    }

    @Bean
    public TestWorkDirHolder testWorkDirHolder(@Value("${testclass.workdir.path}") final File workdirPath,
            @Value("${testclass.name}") final String testclassName) {
        requireNonNull(workdirPath, "workdirPath");
        requireNonNull(testclassName, "testclassName; " + TEST_CLASS_NAME_MESSAGE);
        Preconditions.checkState(!workdirPath.toString().isEmpty(), "workdirPath must not be empty");
        Preconditions.checkState(!testclassName.isEmpty(), "testclassName must not be empty; " + TEST_CLASS_NAME_MESSAGE);
        return new TestWorkDirHolder(workdirPath);
    }

    public static class TestWorkDirHolder implements InitializingBean, DisposableBean {
        private final File workDir;
        private final File tmpDir;

        public TestWorkDirHolder(final File workDir) {
            this.workDir = requireNonNull(workDir, "workDir");
            this.tmpDir = new File(workDir, "temp");
        }

        public File getWorkDir() {
            return workDir;
        }

        public File getTmpDir() {
            return tmpDir;
        }

        @Override
        public void afterPropertiesSet() {
            if (!tmpDir.mkdirs() && !tmpDir.canWrite()) {
                throw new IllegalStateException("Cannot write to " + tmpDir);
            }
        }

        @Override
        public void destroy() throws IOException {
            FileUtils.deleteDirectory(workDir);
        }
    }
}
