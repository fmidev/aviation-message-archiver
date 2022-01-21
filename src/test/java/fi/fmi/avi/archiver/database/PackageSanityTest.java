package fi.fmi.avi.archiver.database;

import static org.mockito.Mockito.mock;

import java.time.Clock;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(DatabaseAccess.class, mock(DatabaseAccess.class));
        setDefault(NamedParameterJdbcTemplate.class, new NamedParameterJdbcTemplate(new DelegatingDataSource()));
    }

}
