package com.viaversion.viaversion.protocols.protocol1_9to1_8;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.metadata.MetadataRewriter1_9To1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.packets.EntityPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.packets.InventoryPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.packets.PlayerPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.packets.SpawnPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.packets.WorldPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.BossBarProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CommandBlockProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.EntityIdProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MainHandProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.ClientChunks;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.CommandBlockStorage;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.EntityTracker1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.InventoryTracker;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.MovementTracker;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.SerializerVersion;

public class Protocol1_9To1_8
    extends AbstractProtocol<ClientboundPackets1_8, ClientboundPackets1_9, ServerboundPackets1_8, ServerboundPackets1_9> {
    public static final ValueTransformer<String, JsonElement> STRING_TO_JSON = new ValueTransformer<String, JsonElement>(
        Type.COMPONENT
    ) {
        public JsonElement transform(PacketWrapper wrapper, String line) {
            return ComponentUtil.convertJsonOrEmpty(line, SerializerVersion.V1_8, SerializerVersion.V1_9);
        }
    };
    private final MetadataRewriter1_9To1_8 metadataRewriter = new MetadataRewriter1_9To1_8(this);

    public Protocol1_9To1_8() {
        super(
            ClientboundPackets1_8.class,
            ClientboundPackets1_9.class,
            ServerboundPackets1_8.class,
            ServerboundPackets1_9.class
        );
    }

    public static Item getHandItem(UserConnection info) {
        return Via.getManager().getProviders().get(HandItemProvider.class).getHandItem(info);
    }

    public static boolean isSword(int id) {
        switch (id) {
            case 267:
            case 268:
            case 272:
            case 276:
            case 283:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.registerClientbound(State.LOGIN, 0, 0, wrapper -> {
            if (!wrapper.isReadable(Type.COMPONENT, 0)) {
                STRING_TO_JSON.write(wrapper, wrapper.read(Type.STRING));
            }
        });
        SpawnPackets.register(this);
        InventoryPackets.register(this);
        EntityPackets.register(this);
        PlayerPackets.register(this);
        WorldPackets.register(this);
    }

    @Override
    public void register(ViaProviders providers) {
        providers.register(HandItemProvider.class, new HandItemProvider());
        providers.register(CommandBlockProvider.class, new CommandBlockProvider());
        providers.register(EntityIdProvider.class, new EntityIdProvider());
        providers.register(BossBarProvider.class, new BossBarProvider());
        providers.register(MainHandProvider.class, new MainHandProvider());
        providers.register(CompressionProvider.class, new CompressionProvider());
        providers.register(MovementTransmitterProvider.class, new MovementTransmitterProvider());
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.addEntityTracker(
            (Class<? extends Protocol>)this.getClass(), new EntityTracker1_9(userConnection)
        );
        userConnection.put(new ClientChunks());
        userConnection.put(new MovementTracker());
        userConnection.put(new InventoryTracker());
        userConnection.put(new CommandBlockStorage());
        if (!userConnection.has(ClientWorld.class)) {
            userConnection.put(new ClientWorld());
        }
    }

    public MetadataRewriter1_9To1_8 getEntityRewriter() {
        return this.metadataRewriter;
    }
}
