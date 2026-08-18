package com.viaversion.viarewind.api.rewriter;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;

public abstract class VREntityRewriter<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>>
    extends LegacyEntityRewriter<C, T> {
    public VREntityRewriter(T protocol) {
        super(protocol, MetaType1_8.String, MetaType1_8.Byte);
    }

    public VREntityRewriter(T protocol, MetaType displayType, MetaType displayVisibilityType) {
        super(protocol, displayType, displayVisibilityType);
    }

    protected void registerJoinGame1_8(C packetType) {
        this.protocol
            .registerClientbound(
                packetType,
                new PacketHandlers() {
                    @Override
                    protected void register() {
                        this.map(Type.INT);
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.BYTE);
                        this.handler(VREntityRewriter.this.playerTrackerHandler());
                        this.handler(
                            wrapper -> wrapper.user().get(ClientWorld.class).setEnvironment(wrapper.get(Type.BYTE, 0))
                        );
                    }
                }
            );
    }

    protected void untrackEntities(UserConnection connection, int[] entities) {
        EntityTrackerBase tracker = this.tracker(connection);

        for (int entityId : entities) {
            tracker.removeEntity(entityId);
        }
    }

    @Override
    protected Object getDisplayVisibilityMetaValue() {
        return (byte)1;
    }

    @Override
    protected boolean alwaysShowOriginalMobName() {
        return ViaRewind.getConfig().alwaysShowOriginalMobName();
    }
}
