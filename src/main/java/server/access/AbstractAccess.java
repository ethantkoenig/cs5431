package server.access;

import utils.Config;
import utils.ShaTwoFiftySix;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public abstract class AbstractAccess {
    private static final Logger LOGGER =
        Logger.getLogger(Config.getLogParent() + "." + AbstractAccess.class.getName());

    final String hashOfGuid(String guid) {
        return ShaTwoFiftySix.hashOf(guid.getBytes(StandardCharsets.UTF_8)).toString();
    }

    final void checkRowCount(int actualRowCount, int expectedRowCount) {
        if (actualRowCount != expectedRowCount) {
            String msg = String.format("Insert affected %d rows, expected %d",
                    actualRowCount, expectedRowCount);
            LOGGER.severe(msg);
        }
    }
}
