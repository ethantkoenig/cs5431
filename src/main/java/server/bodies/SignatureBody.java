package server.bodies;

import crypto.ECDSASignature;
import server.utils.RouteUtils;

import java.math.BigInteger;

public class SignatureBody {
    private String r;
    private String s;

    public SignatureBody() {
    }

    public SignatureBody(String r, String s) {
        this.r = r;
        this.s = s;
    }

    public ECDSASignature asSignature() throws RouteUtils.InvalidParamException {
        try {
            return new ECDSASignature(new BigInteger(r, 16), new BigInteger(s, 16));
        } catch (NumberFormatException e) {
            throw new RouteUtils.InvalidParamException("Invalid signature");
        }
    }
}
