package testutils;

import com.google.inject.AbstractModule;
import crypto.Crypto;
import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import server.access.DatabaseUserAccess;
import server.access.UserAccess;
import server.utils.ConnectionProvider;
import server.utils.MailService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;

public abstract class ControllerTest extends DBTestCase {

    public ControllerTest() throws SQLException {
        super();
        Crypto.init();

        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS,
                TestConnectionProvider.DRIVER_CLASS_NAME);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL,
                TestConnectionProvider.CONNECTION_URL);
    }

    @Override
    protected IDataSet getDataSet() throws Exception {
        try (InputStream inputStream = new FileInputStream("fixtures.xml")) {
            return new FlatXmlDataSetBuilder().build(inputStream);
        }
    }

    public static final class Model extends AbstractModule {
        @Override
        protected void configure() {
            bind(ConnectionProvider.class).to(TestConnectionProvider.class);
            bind(UserAccess.class).to(DatabaseUserAccess.class);
            bind(MailService.class).to(MockMailService.class);
        }
    }
}
