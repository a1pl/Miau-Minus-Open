package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.provider;

import com.viaversion.viaversion.api.connection.UserConnection;

final class NoopTransferProvider implements TransferProvider {
    @Override
    public void connectToServer(UserConnection connection, String host, int port) {
    }
}
