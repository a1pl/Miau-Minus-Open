package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.provider.TransferProvider;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.EntityPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.CookieStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ProtocolUtil;

public final class Protocol1_20_3To1_20_5
    extends BackwardsProtocol<ClientboundPacket1_20_5, ClientboundPacket1_20_3, ServerboundPacket1_20_5, ServerboundPacket1_20_3> {
    public static final BackwardsMappings MAPPINGS = new BackwardsMappings(
        "1.20.5", "1.20.3", Protocol1_20_5To1_20_3.class
    );
    private final EntityPacketRewriter1_20_5 entityRewriter = new EntityPacketRewriter1_20_5(this);
    private final BlockItemPacketRewriter1_20_5 itemRewriter = new BlockItemPacketRewriter1_20_5(this);
    private final TranslatableRewriter<ClientboundPacket1_20_5> translatableRewriter = new TranslatableRewriter<>(
        this, ComponentRewriter.ReadType.NBT
    );
    private final TagRewriter<ClientboundPacket1_20_5> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_3To1_20_5() {
        super(
            ClientboundPacket1_20_5.class,
            ClientboundPacket1_20_3.class,
            ServerboundPacket1_20_5.class,
            ServerboundPacket1_20_3.class
        );
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.tagRewriter.registerGeneric(ClientboundPackets1_20_5.TAGS);
        this.registerClientbound(
            ClientboundConfigurationPackets1_20_5.UPDATE_TAGS,
            wrapper -> {
                PacketWrapper registryDataPacket = wrapper.create(ClientboundConfigurationPackets1_20_3.REGISTRY_DATA);
                registryDataPacket.write(
                    Type.COMPOUND_TAG, wrapper.user().get(RegistryDataStorage.class).registryData().copy()
                );
                registryDataPacket.send(Protocol1_20_3To1_20_5.class);
                this.tagRewriter.getGenericHandler().handle(wrapper);
            }
        );
        this.registerClientbound(
            ClientboundPackets1_20_5.START_CONFIGURATION,
            wrapper -> wrapper.user().get(RegistryDataStorage.class).clear()
        );
        SoundRewriter<ClientboundPacket1_20_5> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_5.STOP_SOUND);
        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_5.STATISTICS);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.ACTIONBAR);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_TEXT);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_SUBTITLE);
        this.translatableRewriter.registerBossBar(ClientboundPackets1_20_5.BOSSBAR);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISCONNECT);
        this.translatableRewriter.registerTabList(ClientboundPackets1_20_5.TAB_LIST);
        this.translatableRewriter.registerCombatKill1_20(ClientboundPackets1_20_5.COMBAT_KILL);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.SYSTEM_CHAT);
        this.translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISGUISED_CHAT);
        this.translatableRewriter.registerPing();
        this.registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO, wrapper -> {
            wrapper.passthrough(Type.STRING);
            wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
            wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
            wrapper.read(Type.BOOLEAN);
        });
        this.registerClientbound(ClientboundPackets1_20_5.SERVER_DATA, wrapper -> {
            wrapper.passthrough(Type.TAG);
            wrapper.passthrough(Type.OPTIONAL_BYTE_ARRAY_PRIMITIVE);
            wrapper.write(Type.BOOLEAN, wrapper.user().get(SecureChatStorage.class).enforcesSecureChat());
        });
        this.registerServerbound(
            ServerboundPackets1_20_3.CHAT_COMMAND, ServerboundPackets1_20_5.CHAT_COMMAND_SIGNED, wrapper -> {
                String command = wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.LONG);
                wrapper.passthrough(Type.LONG);
                int signatures = wrapper.passthrough(Type.VAR_INT);
                if (signatures == 0) {
                    wrapper.cancel();
                    PacketWrapper chatCommand = wrapper.create(ServerboundPackets1_20_5.CHAT_COMMAND);
                    chatCommand.write(Type.STRING, command);
                    chatCommand.sendToServer(Protocol1_20_3To1_20_5.class);
                }
            }
        );
        this.registerClientbound(
            State.LOGIN,
            ClientboundLoginPackets.COOKIE_REQUEST.getId(),
            -1,
            wrapper -> this.handleCookieRequest(wrapper, ServerboundLoginPackets.COOKIE_RESPONSE)
        );
        this.cancelClientbound(ClientboundConfigurationPackets1_20_5.RESET_CHAT);
        this.registerClientbound(
            ClientboundConfigurationPackets1_20_5.COOKIE_REQUEST,
            null,
            wrapper -> this.handleCookieRequest(wrapper, ServerboundConfigurationPackets1_20_5.COOKIE_RESPONSE)
        );
        this.registerClientbound(ClientboundConfigurationPackets1_20_5.STORE_COOKIE, null, this::handleStoreCookie);
        this.registerClientbound(ClientboundConfigurationPackets1_20_5.TRANSFER, null, this::handleTransfer);
        this.registerClientbound(
            ClientboundPackets1_20_5.COOKIE_REQUEST,
            null,
            wrapper -> this.handleCookieRequest(wrapper, ServerboundPackets1_20_5.COOKIE_RESPONSE)
        );
        this.registerClientbound(ClientboundPackets1_20_5.STORE_COOKIE, null, this::handleStoreCookie);
        this.registerClientbound(ClientboundPackets1_20_5.TRANSFER, null, this::handleTransfer);
        this.registerClientbound(ClientboundConfigurationPackets1_20_5.SELECT_KNOWN_PACKS, null, wrapper -> {
            wrapper.cancel();
            PacketWrapper response = wrapper.create(ServerboundConfigurationPackets1_20_5.SELECT_KNOWN_PACKS);
            response.write(Type.VAR_INT, 0);
            response.sendToServer(Protocol1_20_3To1_20_5.class);
        });
        (new CommandRewriter1_19_4<ClientboundPacket1_20_5>(this) {
                @Override
                public void handleArgument(PacketWrapper wrapper, String argumentType) throws Exception {
                    if (!argumentType.equals("minecraft:loot_table")
                        && !argumentType.equals("minecraft:loot_predicate")
                        && !argumentType.equals("minecraft:loot_modifier")) {
                        super.handleArgument(wrapper, argumentType);
                    } else {
                        wrapper.write(Type.VAR_INT, 0);
                    }
                }
            })
            .registerDeclareCommands1_19(ClientboundPackets1_20_5.DECLARE_COMMANDS);
        this.registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE, wrapper -> {
            wrapper.passthrough(Type.UUID);
            wrapper.passthrough(Type.STRING);
            int properties = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < properties; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
            }

            wrapper.read(Type.BOOLEAN);
        });
        this.cancelClientbound(ClientboundPackets1_20_5.PROJECTILE_POWER);
        this.cancelClientbound(ClientboundPackets1_20_5.DEBUG_SAMPLE);
    }

    private void handleStoreCookie(PacketWrapper wrapper) throws Exception {
        wrapper.cancel();
        String resourceLocation = wrapper.read(Type.STRING);
        byte[] data = wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
        if (data.length > 5120) {
            throw new IllegalArgumentException("Cookie data too large");
        }

        wrapper.user().get(CookieStorage.class).cookies().put(resourceLocation, data);
    }

    private void handleCookieRequest(PacketWrapper wrapper, ServerboundPacketType responseType) throws Exception {
        wrapper.cancel();
        String resourceLocation = wrapper.read(Type.STRING);
        byte[] data = wrapper.user().get(CookieStorage.class).cookies().get(resourceLocation);
        PacketWrapper responsePacket = wrapper.create(responseType);
        responsePacket.write(Type.STRING, resourceLocation);
        responsePacket.write(Type.OPTIONAL_BYTE_ARRAY_PRIMITIVE, data);
        responsePacket.sendToServer(Protocol1_20_3To1_20_5.class);
    }

    private void handleTransfer(PacketWrapper wrapper) throws Exception {
        wrapper.cancel();
        String host = wrapper.read(Type.STRING);
        int port = wrapper.read(Type.VAR_INT);
        Via.getManager().getProviders().get(TransferProvider.class).connectToServer(wrapper.user(), host, port);
    }

    @Override
    public void init(UserConnection user) {
        this.addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new SecureChatStorage());
        user.put(new CookieStorage());
        user.put(new RegistryDataStorage());
    }

    @Override
    public void register(ViaProviders providers) {
        providers.register(TransferProvider.class, TransferProvider.NOOP);
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    public EntityPacketRewriter1_20_5 getEntityRewriter() {
        return this.entityRewriter;
    }

    public BlockItemPacketRewriter1_20_5 getItemRewriter() {
        return this.itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPacket1_20_5> getTranslatableRewriter() {
        return this.translatableRewriter;
    }

    public TagRewriter<ClientboundPacket1_20_5> getTagRewriter() {
        return this.tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_20_5, ClientboundPacket1_20_3, ServerboundPacket1_20_5, ServerboundPacket1_20_3> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            ProtocolUtil.packetTypeMap(
                this.unmappedClientboundPacketType,
                ClientboundPackets1_20_5.class,
                ClientboundConfigurationPackets1_20_5.class
            ),
            ProtocolUtil.packetTypeMap(
                this.mappedClientboundPacketType,
                ClientboundPackets1_20_3.class,
                ClientboundConfigurationPackets1_20_3.class
            ),
            ProtocolUtil.packetTypeMap(
                this.mappedServerboundPacketType,
                ServerboundPackets1_20_5.class,
                ServerboundConfigurationPackets1_20_5.class
            ),
            ProtocolUtil.packetTypeMap(
                this.unmappedServerboundPacketType,
                ServerboundPackets1_20_3.class,
                ServerboundConfigurationPackets1_20_2.class
            )
        );
    }
}
