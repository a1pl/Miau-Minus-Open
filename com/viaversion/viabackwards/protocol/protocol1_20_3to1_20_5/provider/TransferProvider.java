package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.providers.Provider;

@FunctionalInterface
public interface TransferProvider extends Provider {
    TransferProvider NOOP = new NoopTransferProvider();

    void connectToServer(UserConnection var1, String var2, int var3);
}
