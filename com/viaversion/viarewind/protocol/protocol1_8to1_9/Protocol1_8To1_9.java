package com.viaversion.viarewind.protocol.protocol1_8to1_9;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.data.RewindMappings;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.metadata.MetadataRewriter1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.packets.BlockItemPackets1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.packets.EntityPackets1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.packets.PlayerPackets1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.packets.WorldPackets1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.BlockPlaceDestroyTracker;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.BossBarStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.CooldownStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.LevitationStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.PlayerPositionTracker;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.WindowTracker;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.task.CooldownIndicatorTask;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.task.LevitationUpdateTask;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;
import java.util.concurrent.TimeUnit;

public class Protocol1_8To1_9
    extends BackwardsProtocol<ClientboundPackets1_9, ClientboundPackets1_8, ServerboundPackets1_9, ServerboundPackets1_8> {
    public static final ValueTransformer<Double, Integer> DOUBLE_TO_INT_TIMES_32 = new ValueTransformer<Double, Integer>(
        Type.INT
    ) {
        public Integer transform(PacketWrapper wrapper, Double inputValue) {
            return (int)(inputValue * 32.0);
        }
    };
    public static final ValueTransformer<Float, Byte> DEGREES_TO_ANGLE = new ValueTransformer<Float, Byte>(Type.BYTE) {
        public Byte transform(PacketWrapper packetWrapper, Float degrees) {
            return (byte)(degrees / 360.0F * 256.0F);
        }
    };
    public static final RewindMappings MAPPINGS = new RewindMappings();
    private final BlockItemPackets1_9 itemRewriter = new BlockItemPackets1_9(this);
    private final MetadataRewriter1_8To1_9 metadataRewriter = new MetadataRewriter1_8To1_9(this);

    public Protocol1_8To1_9() {
        super(
            ClientboundPackets1_9.class,
            ClientboundPackets1_8.class,
            ServerboundPackets1_9.class,
            ServerboundPackets1_8.class
        );
    }

    @Override
    protected void registerPackets() {
        this.metadataRewriter.register();
        this.itemRewriter.register();
        EntityPackets1_9.register(this);
        PlayerPackets1_9.register(this);
        WorldPackets1_9.register(this);
    }

    @Override
    public void init(UserConnection connection) {
        connection.addEntityTracker((Class<? extends Protocol>)this.getClass(), new EntityTracker1_9(connection));
        connection.put(new WindowTracker(connection));
        connection.put(new LevitationStorage());
        connection.put(new PlayerPositionTracker());
        connection.put(new CooldownStorage());
        connection.put(new BlockPlaceDestroyTracker());
        connection.put(new BossBarStorage(connection));
        if (!connection.has(ClientWorld.class)) {
            connection.put(new ClientWorld());
        }
    }

    @Override
    public void register(ViaProviders providers) {
        Via.getManager().getScheduler().scheduleRepeating(new LevitationUpdateTask(), 0L, 50L, TimeUnit.MILLISECONDS);
        Via.getManager().getScheduler().scheduleRepeating(new CooldownIndicatorTask(), 0L, 50L, TimeUnit.MILLISECONDS);
    }

    public RewindMappings getMappingData() {
        return MAPPINGS;
    }

    public BlockItemPackets1_9 getItemRewriter() {
        return this.itemRewriter;
    }

    public MetadataRewriter1_8To1_9 getEntityRewriter() {
        return this.metadataRewriter;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
