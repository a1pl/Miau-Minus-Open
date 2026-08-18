package de.florianmichael.vialoadingbase.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.base.BaseVersionProvider;
import de.florianmichael.vialoadingbase.ViaLoadingBase;

public class VLBBaseVersionProvider extends BaseVersionProvider {
    @Override
    public ProtocolVersion getClosestServerProtocol(UserConnection connection) throws Exception {
        return connection.isClientSide()
            ? ViaLoadingBase.getInstance().getTargetVersion()
            : super.getClosestServerProtocol(connection);
    }
}
