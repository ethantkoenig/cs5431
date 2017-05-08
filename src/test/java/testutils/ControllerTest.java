package testutils;

import com.google.inject.AbstractModule;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import server.utils.ConnectionProvider;
import server.utils.MailService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;

public abstract class ControllerTest extends RandomizedTest {
    private ConnectionProvider connectionProvider;
    private IDataSet dataSet;

    protected ControllerTest() throws Exception {
        try (InputStream inputStream = new FileInputStream("fixtures.xml")) {
            dataSet = new FlatXmlDataSetBuilder().build(inputStream);
        }
    }

    protected void setConnectionProvider(ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException();
        } else if (this.connectionProvider != null) {
            throw new IllegalStateException();
        }
        this.connectionProvider = connectionProvider;
    }

    @Before
    public final void initTestDatabase() throws Exception {
        if (connectionProvider == null) {
            throw new IllegalStateException();
        }
        Connection connection = connectionProvider.getConnection();
        IDatabaseConnection databaseConnection = new DatabaseConnection(connection);
        DatabaseOperation.CLEAN_INSERT.execute(databaseConnection, dataSet);
    }
}
