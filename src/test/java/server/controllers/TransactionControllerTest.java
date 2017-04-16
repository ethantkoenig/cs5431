package server.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crypto.Crypto;
import crypto.ECDSAPublicKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;
import spark.Response;
import spark.utils.Assert;
import testutils.MockRequest;
import testutils.MockUserAccess;
import testutils.RandomizedTest;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

public class TransactionControllerTest extends RandomizedTest {
    private MockUserAccess mockUserAccess = null;
    private TransactionController controller = null;

    @Before
    public void initController() throws Exception {
        MockUserAccess.reset();
        mockUserAccess = MockUserAccess.get();
        Injector injector = Guice.createInjector(new MockUserAccess.Model());
        controller = injector.getInstance(TransactionController.class);
        controller.init();
    }

    @Test
    public void testTransact() throws Exception {
        // TODO figure out how to test (since this talks with the crypto-currency node)
        Assert.notNull(mockUserAccess);
        Assert.notNull(controller);
    }

    @Test
    public void testSendTransaction() throws Exception {
        // TODO figure out how to test (since this talks with the crypto-currency node)
    }
}
