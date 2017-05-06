package server.bodies;

import java.util.ArrayList;
import java.util.List;

public class KeysBody {
    public final List<KeyBody> keys;

    public KeysBody() {
        keys = new ArrayList<>();
    }

    public KeysBody(List<KeyBody> keys) {
        this.keys = keys;
    }
}
