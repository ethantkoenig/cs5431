package testutils;

import network.ConnectionThread;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.mockito.Mockito;
import server.utils.ConnectionProvider;
import server.utils.RouteWrapper;
import spark.Route;
import spark.TemplateViewRoute;
import utils.Log;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ControllerTest extends RandomizedTest {
    private ConnectionProvider connectionProvider;
    private IDataSet dataSet;

    private static final RouteWrapper wrapper;
    static {
            Log logger = Log.forClass(ControllerTest.class);
            logger.logger().setLevel(Level.OFF);
            wrapper = new RouteWrapper(logger);
    }

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

    protected Route route(RouteWrapper.LoggedRoute loggedRoute) {
        return wrapper.route(loggedRoute);
    }

    protected TemplateViewRoute template(RouteWrapper.LoggedTemplateViewRoute loggedTemplate) {
        return wrapper.template(loggedTemplate);
    }
}
