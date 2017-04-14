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
        byte[] senderKeyBytes = mockUserAccess.fixtures.key.getPublicKey();
        ECDSAPublicKey recipientKey = Crypto.signatureKeyPair().publicKey;
        ShaTwoFiftySix inputHash = randomShaTwoFiftySix();

        Request request = new MockRequest()
                .addQueryParam("index", "0")
                .addQueryParam("amount", "100")
                .addQueryParamHex("senderpublickey", senderKeyBytes)
                .addQueryParamHex("recipientpublickey", ByteUtil.asByteArray(recipientKey::serialize))
                .addQueryParamHex("transaction", inputHash.copyOfHash())
                .addSessionAttribute("username", mockUserAccess.fixtures.user.getUsername())
                .get();

        Response response = Mockito.mock(Response.class);
        controller.transact(request, response); // TODO actually check response
    }

    @Test
    public void testSendTransaction() throws Exception {
        // TODO actually test
    }
}
