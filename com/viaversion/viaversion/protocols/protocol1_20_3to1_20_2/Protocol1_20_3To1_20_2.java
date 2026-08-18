package com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_3;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.misc.ParticleType;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPacket1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundPacket1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.BlockItemPacketRewriter1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.EntityPacketRewriter1_20_3;
import com.viaversion.viaversion.rewriter.SoundRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.ProtocolUtil;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.UUID;

public final class Protocol1_20_3To1_20_2
    extends AbstractProtocol<ClientboundPacket1_20_2, ClientboundPacket1_20_3, ServerboundPacket1_20_2, ServerboundPacket1_20_3> {
    public static final MappingData MAPPINGS = new MappingDataBase("1.20.2", "1.20.3");
    private final BlockItemPacketRewriter1_20_3 itemRewriter = new BlockItemPacketRewriter1_20_3(this);
    private final EntityPacketRewriter1_20_3 entityRewriter = new EntityPacketRewriter1_20_3(this);
    private final TagRewriter<ClientboundPacket1_20_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_3To1_20_2() {
        super(
            ClientboundPacket1_20_2.class,
            ClientboundPacket1_20_3.class,
            ServerboundPacket1_20_2.class,
            ServerboundPacket1_20_3.class
        );
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.cancelServerbound(ServerboundPackets1_20_3.CONTAINER_SLOT_STATE_CHANGED);
        this.tagRewriter.registerGeneric(ClientboundPackets1_20_2.TAGS);
        SoundRewriter<ClientboundPacket1_20_2> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_2.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_2.ENTITY_SOUND);
        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_2.STATISTICS);
        new CommandRewriter1_19_4<>(this).registerDeclareCommands1_19(ClientboundPackets1_20_2.DECLARE_COMMANDS);
        this.registerClientbound(ClientboundPackets1_20_2.UPDATE_SCORE, wrapper -> {
            wrapper.passthrough(Type.STRING);
            int action = wrapper.read(Type.VAR_INT);
            String objectiveName = wrapper.read(Type.STRING);
            if (action == 1) {
                wrapper.write(Type.OPTIONAL_STRING, objectiveName.isEmpty() ? null : objectiveName);
                wrapper.setPacketType(ClientboundPackets1_20_3.RESET_SCORE);
            } else {
                wrapper.write(Type.STRING, objectiveName);
                wrapper.passthrough(Type.VAR_INT);
                wrapper.write(Type.OPTIONAL_TAG, null);
                wrapper.write(Type.BOOLEAN, false);
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.SCOREBOARD_OBJECTIVE, wrapper -> {
            wrapper.passthrough(Type.STRING);
            byte action = wrapper.passthrough(Type.BYTE);
            if (action == 0 || action == 2) {
                this.convertComponent(wrapper);
                wrapper.passthrough(Type.VAR_INT);
                wrapper.write(Type.BOOLEAN, false);
            }
        });
        this.registerServerbound(ServerboundPackets1_20_3.UPDATE_JIGSAW_BLOCK, wrapper -> {
            wrapper.passthrough(Type.POSITION1_14);
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.STRING);
            wrapper.read(Type.VAR_INT);
            wrapper.read(Type.VAR_INT);
        });
        this.registerClientbound(ClientboundPackets1_20_2.ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    this.convertComponent(wrapper);
                    this.convertComponent(wrapper);
                    this.itemRewriter.handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_20_2));
                    wrapper.passthrough(Type.VAR_INT);
                    int flags = wrapper.passthrough(Type.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING);
                    }

                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                }

                int requirements = wrapper.passthrough(Type.VAR_INT);

                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY);
                }

                wrapper.passthrough(Type.BOOLEAN);
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.TAB_COMPLETE, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.VAR_INT);
            int suggestions = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < suggestions; i++) {
                wrapper.passthrough(Type.STRING);
                this.convertOptionalComponent(wrapper);
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.MAP_DATA, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.BYTE);
            wrapper.passthrough(Type.BOOLEAN);
            if (wrapper.passthrough(Type.BOOLEAN)) {
                int icons = wrapper.passthrough(Type.VAR_INT);

                for (int i = 0; i < icons; i++) {
                    wrapper.passthrough(Type.VAR_INT);
                    wrapper.passthrough(Type.BYTE);
                    wrapper.passthrough(Type.BYTE);
                    wrapper.passthrough(Type.BYTE);
                    this.convertOptionalComponent(wrapper);
                }
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.BOSSBAR, wrapper -> {
            wrapper.passthrough(Type.UUID);
            int action = wrapper.passthrough(Type.VAR_INT);
            if (action == 0 || action == 3) {
                this.convertComponent(wrapper);
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.PLAYER_CHAT, wrapper -> {
            wrapper.passthrough(Type.UUID);
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.OPTIONAL_SIGNATURE_BYTES);
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.LONG);
            wrapper.passthrough(Type.LONG);
            int lastSeen = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < lastSeen; i++) {
                int index = wrapper.passthrough(Type.VAR_INT);
                if (index == 0) {
                    wrapper.passthrough(Type.SIGNATURE_BYTES);
                }
            }

            this.convertOptionalComponent(wrapper);
            int filterMaskType = wrapper.passthrough(Type.VAR_INT);
            if (filterMaskType == 2) {
                wrapper.passthrough(Type.LONG_ARRAY_PRIMITIVE);
            }

            wrapper.passthrough(Type.VAR_INT);
            this.convertComponent(wrapper);
            this.convertOptionalComponent(wrapper);
        });
        this.registerClientbound(ClientboundPackets1_20_2.TEAMS, wrapper -> {
            wrapper.passthrough(Type.STRING);
            byte action = wrapper.passthrough(Type.BYTE);
            if (action == 0 || action == 2) {
                this.convertComponent(wrapper);
                wrapper.passthrough(Type.BYTE);
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.VAR_INT);
                this.convertComponent(wrapper);
                this.convertComponent(wrapper);
            }
        });
        this.registerClientbound(ClientboundConfigurationPackets1_20_2.DISCONNECT, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.DISCONNECT, this::convertComponent);
        this.registerClientbound(
            ClientboundPackets1_20_2.RESOURCE_PACK,
            ClientboundPackets1_20_3.RESOURCE_PACK_PUSH,
            this.resourcePackHandler(ClientboundPackets1_20_3.RESOURCE_PACK_POP)
        );
        this.registerClientbound(ClientboundPackets1_20_2.SERVER_DATA, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.ACTIONBAR, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.TITLE_TEXT, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.TITLE_SUBTITLE, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.DISGUISED_CHAT, wrapper -> {
            this.convertComponent(wrapper);
            wrapper.passthrough(Type.VAR_INT);
            this.convertComponent(wrapper);
            this.convertOptionalComponent(wrapper);
        });
        this.registerClientbound(ClientboundPackets1_20_2.SYSTEM_CHAT, this::convertComponent);
        this.registerClientbound(ClientboundPackets1_20_2.OPEN_WINDOW, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int containerTypeId = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.VAR_INT, MAPPINGS.getMenuMappings().getNewId(containerTypeId));
            this.convertComponent(wrapper);
        });
        this.registerClientbound(ClientboundPackets1_20_2.TAB_LIST, wrapper -> {
            this.convertComponent(wrapper);
            this.convertComponent(wrapper);
        });
        this.registerClientbound(ClientboundPackets1_20_2.COMBAT_KILL, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(wrapper -> Protocol1_20_3To1_20_2.this.convertComponent(wrapper));
            }
        });
        this.registerClientbound(ClientboundPackets1_20_2.PLAYER_INFO_UPDATE, wrapper -> {
            BitSet actions = wrapper.passthrough(Type.PROFILE_ACTIONS_ENUM);
            int entries = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Type.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Type.STRING);
                    int properties = wrapper.passthrough(Type.VAR_INT);

                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Type.STRING);
                        wrapper.passthrough(Type.STRING);
                        wrapper.passthrough(Type.OPTIONAL_STRING);
                    }
                }

                if (actions.get(1) && wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.UUID);
                    wrapper.passthrough(Type.PROFILE_KEY);
                }

                if (actions.get(2)) {
                    wrapper.passthrough(Type.VAR_INT);
                }

                if (actions.get(3)) {
                    wrapper.passthrough(Type.BOOLEAN);
                }

                if (actions.get(4)) {
                    wrapper.passthrough(Type.VAR_INT);
                }

                if (actions.get(5)) {
                    this.convertOptionalComponent(wrapper);
                }
            }
        });
        this.registerServerbound(ServerboundPackets1_20_3.RESOURCE_PACK_STATUS, this.resourcePackStatusHandler());
        this.registerServerbound(ServerboundConfigurationPackets1_20_2.RESOURCE_PACK, this.resourcePackStatusHandler());
        this.registerClientbound(
            ClientboundConfigurationPackets1_20_2.RESOURCE_PACK,
            ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_PUSH,
            this.resourcePackHandler(ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_POP)
        );
        this.registerClientbound(
            ClientboundConfigurationPackets1_20_2.UPDATE_TAGS, this.tagRewriter.getGenericHandler()
        );
    }

    private PacketHandler resourcePackStatusHandler() {
        return wrapper -> {
            wrapper.read(Type.UUID);
            int action = wrapper.read(Type.VAR_INT);
            if (action == 4) {
                wrapper.cancel();
            } else if (action > 4) {
                wrapper.write(Type.VAR_INT, 2);
            } else {
                wrapper.write(Type.VAR_INT, action);
            }
        };
    }

    private PacketHandler resourcePackHandler(ClientboundPacketType popType) {
        return wrapper -> {
            PacketWrapper dropPacksPacket = wrapper.create(popType);
            dropPacksPacket.write(Type.OPTIONAL_UUID, null);
            dropPacksPacket.send(Protocol1_20_3To1_20_2.class);
            String url = wrapper.read(Type.STRING);
            String hash = wrapper.read(Type.STRING);
            wrapper.write(Type.UUID, UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
            wrapper.write(Type.STRING, url);
            wrapper.write(Type.STRING, hash);
            wrapper.passthrough(Type.BOOLEAN);
            this.convertOptionalComponent(wrapper);
        };
    }

    private void convertComponent(PacketWrapper wrapper) throws Exception {
        wrapper.write(Type.TAG, ComponentUtil.jsonToTag(wrapper.read(Type.COMPONENT)));
    }

    private void convertOptionalComponent(PacketWrapper wrapper) throws Exception {
        wrapper.write(Type.OPTIONAL_TAG, ComponentUtil.jsonToTag(wrapper.read(Type.OPTIONAL_COMPONENT)));
    }

    @Override
    protected void onMappingDataLoaded() {
        super.onMappingDataLoaded();
        EntityTypes1_20_3.initialize(this);
        Types1_20_3.PARTICLE
            .filler(this)
            .reader("block", ParticleType.Readers.BLOCK)
            .reader("block_marker", ParticleType.Readers.BLOCK)
            .reader("dust", ParticleType.Readers.DUST)
            .reader("falling_dust", ParticleType.Readers.BLOCK)
            .reader("dust_color_transition", ParticleType.Readers.DUST_TRANSITION)
            .reader("item", ParticleType.Readers.ITEM1_20_2)
            .reader("vibration", ParticleType.Readers.VIBRATION1_20_3)
            .reader("sculk_charge", ParticleType.Readers.SCULK_CHARGE)
            .reader("shriek", ParticleType.Readers.SHRIEK);
    }

    @Override
    public void init(UserConnection connection) {
        this.addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_20_3.PLAYER));
    }

    @Override
    public MappingData getMappingData() {
        return MAPPINGS;
    }

    public BlockItemPacketRewriter1_20_3 getItemRewriter() {
        return this.itemRewriter;
    }

    public EntityPacketRewriter1_20_3 getEntityRewriter() {
        return this.entityRewriter;
    }

    public TagRewriter<ClientboundPacket1_20_2> getTagRewriter() {
        return this.tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_20_2, ClientboundPacket1_20_3, ServerboundPacket1_20_2, ServerboundPacket1_20_3> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            ProtocolUtil.packetTypeMap(
                this.unmappedClientboundPacketType,
                ClientboundPackets1_20_2.class,
                ClientboundConfigurationPackets1_20_2.class
            ),
            ProtocolUtil.packetTypeMap(
                this.mappedClientboundPacketType,
                ClientboundPackets1_20_3.class,
                ClientboundConfigurationPackets1_20_3.class
            ),
            ProtocolUtil.packetTypeMap(
                this.mappedServerboundPacketType,
                ServerboundPackets1_20_2.class,
                ServerboundConfigurationPackets1_20_2.class
            ),
            ProtocolUtil.packetTypeMap(
                this.unmappedServerboundPacketType,
                ServerboundPackets1_20_3.class,
                ServerboundConfigurationPackets1_20_2.class
            )
        );
    }
}
