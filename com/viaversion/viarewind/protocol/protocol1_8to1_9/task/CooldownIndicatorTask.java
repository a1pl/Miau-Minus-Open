package com.viaversion.viarewind.protocol.protocol1_8to1_9.task;

import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.CooldownStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;

public class CooldownIndicatorTask implements Runnable {
    @Override
    public void run() {
        for (UserConnection connection : Via.getManager().getConnectionManager().getConnections()) {
            connection.get(CooldownStorage.class).tick(connection);
        }
    }
}
