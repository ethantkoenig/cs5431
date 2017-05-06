package server.bodies;


import crypto.ECDSASignature;
import server.utils.RouteUtils;
import utils.ByteUtil;

import java.util.ArrayList;
import java.util.List;

public class SendTransactionBody {
    private String payload;
    private List<SignatureBody> signatures;

    public SendTransactionBody() {
    }

    public SendTransactionBody(String payload, List<SignatureBody> signatures) {
        this.payload = payload;
        this.signatures = signatures;
    }

    public List<ECDSASignature> signatures() throws RouteUtils.InvalidParamException {
        List<ECDSASignature> result = new ArrayList<>();
        for (SignatureBody signature : signatures) {
            result.add(signature.asSignature());
        }
        return result;
    }

    public byte[] payload() throws RouteUtils.InvalidParamException {
        return ByteUtil.hexStringToByteArray(payload).orElseThrow(() ->
                new RouteUtils.InvalidParamException("Invalid transaction payload")
        );
    }
}
