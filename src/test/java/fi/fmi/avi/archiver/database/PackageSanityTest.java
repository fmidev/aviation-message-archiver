package fi.fmi.avi.archiver.database;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import java.time.Clock;

import static org.mockito.Mockito.mock;

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
