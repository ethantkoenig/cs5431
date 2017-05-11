package server.access;

import utils.Log;
import utils.ShaTwoFiftySix;

import java.nio.charset.StandardCharsets;

public abstract class AbstractAccess {
    private static final Log LOGGER = Log.forClass(AbstractAccess.class);

    final String hashOfGuid(String guid) {
        return ShaTwoFiftySix.hashOf(guid.getBytes(StandardCharsets.UTF_8)).toString();
    }

    final void checkRowCount(int actualRowCount, int expectedRowCount) {
        if (actualRowCount != expectedRowCount) {
            LOGGER.severe("Insert affected %d rows, expected %d",
                    actualRowCount, expectedRowCount);
        }
    }
}
