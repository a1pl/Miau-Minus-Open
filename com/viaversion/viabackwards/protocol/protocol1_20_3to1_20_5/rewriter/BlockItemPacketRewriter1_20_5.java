package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter.StructuredDataConverter;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20_5
    extends BackwardsStructuredItemRewriter<ClientboundPacket1_20_5, ServerboundPacket1_20_3, Protocol1_20_3To1_20_5> {
    private static final StructuredDataConverter DATA_CONVERTER = new StructuredDataConverter(true);
    private final Protocol1_20_5To1_20_3 vvProtocol = Via.getManager()
        .getProtocolManager()
        .getProtocol(Protocol1_20_5To1_20_3.class);

    public BlockItemPacketRewriter1_20_5(Protocol1_20_3To1_20_5 protocol) {
        super(protocol, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY, Type.ITEM1_20_2, Type.ITEM1_20_2_ARRAY);
        this.enchantmentRewriter.setRewriteIds(false);
    }

    @Override
    public void registerPackets() {
        BlockRewriter<ClientboundPacket1_20_5> blockRewriter = BlockRewriter.for1_20_2(this.protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_20_5.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_20_5.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange1_20(ClientboundPackets1_20_5.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_20_5.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_20_5.CHUNK_DATA, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_20_5.BLOCK_ENTITY_DATA);
        this.registerSetCooldown(ClientboundPackets1_20_5.COOLDOWN);
        this.registerWindowItems1_17_1(ClientboundPackets1_20_5.WINDOW_ITEMS);
        this.registerSetSlot1_17_1(ClientboundPackets1_20_5.SET_SLOT);
        this.registerAdvancements1_20_3(ClientboundPackets1_20_5.ADVANCEMENTS);
        this.registerClickWindow1_17_1(ServerboundPackets1_20_3.CLICK_WINDOW);
        this.registerWindowPropertyEnchantmentHandler(ClientboundPackets1_20_5.WINDOW_PROPERTY);
        this.registerCreativeInvAction(ServerboundPackets1_20_3.CREATIVE_INVENTORY_ACTION);
        this.protocol.registerServerbound(ServerboundPackets1_20_3.CLICK_WINDOW_BUTTON, wrapper -> {
            int containerId = wrapper.read(Type.VAR_INT);
            int buttonId = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.BYTE, (byte)containerId);
            wrapper.write(Type.BYTE, (byte)buttonId);
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_5.SPAWN_PARTICLE,
                wrapper -> {
                    wrapper.write(Type.VAR_INT, 0);
                    wrapper.passthrough(Type.BOOLEAN);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                    float data = wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.INT);
                    Particle particle = wrapper.read(Types1_20_5.PARTICLE);
                    this.rewriteParticle(wrapper.user(), particle);
                    if (particle.id() == this.protocol.getMappingData().getParticleMappings().mappedId("entity_effect")
                        )
                     {
                        int color = particle.<Integer>removeArgument(0).getValue();
                        if (data == 0.0F) {
                            wrapper.set(Type.FLOAT, 3, (float)color);
                        }
                    } else if (particle.id()
                        == this.protocol.getMappingData().getParticleMappings().mappedId("dust_color_transition")) {
                        particle.add(3, Type.FLOAT, particle.<Float>removeArgument(6).getValue());
                    }

                    wrapper.set(Type.VAR_INT, 0, particle.id());

                    for (Particle.ParticleData<?> argument : particle.getArguments()) {
                        argument.write(wrapper);
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_20_5.EXPLOSION, wrapper -> {
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.FLOAT);
            int blocks = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < blocks; i++) {
                wrapper.passthrough(Type.BYTE);
                wrapper.passthrough(Type.BYTE);
                wrapper.passthrough(Type.BYTE);
            }

            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.VAR_INT);
            this.protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_5.PARTICLE, Types1_20_3.PARTICLE);
            this.protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_5.PARTICLE, Types1_20_3.PARTICLE);
            int soundId = wrapper.read(Type.VAR_INT) - 1;
            if (soundId != -1) {
                soundId = this.protocol.getMappingData().getSoundMappings().getNewId(soundId);
                String soundKey = Protocol1_20_5To1_20_3.MAPPINGS.soundName(soundId);
                wrapper.write(Type.STRING, soundKey != null ? soundKey : "minecraft:entity.generic.explode");
                wrapper.write(Type.OPTIONAL_FLOAT, null);
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_5.TRADE_LIST,
                wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);
                    int size = wrapper.passthrough(Type.VAR_INT);

                    for (int i = 0; i < size; i++) {
                        Item input = this.handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM_COST));
                        wrapper.write(Type.ITEM1_20_2, input);
                        Item result = this.handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM));
                        wrapper.write(Type.ITEM1_20_2, result);
                        Item secondInput = this.handleItemToClient(
                            wrapper.user(), wrapper.read(Types1_20_5.OPTIONAL_ITEM_COST)
                        );
                        wrapper.write(Type.ITEM1_20_2, secondInput);
                        wrapper.passthrough(Type.BOOLEAN);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.FLOAT);
                        wrapper.passthrough(Type.INT);
                    }
                }
            );
        RecipeRewriter1_20_3<ClientboundPacket1_20_5> recipeRewriter = new RecipeRewriter1_20_3<>(this.protocol);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_5.DECLARE_RECIPES,
                wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);

                    for (int i = 0; i < size; i++) {
                        String recipeIdentifier = wrapper.read(Type.STRING);
                        int serializerTypeId = wrapper.read(Type.VAR_INT);
                        String serializerType = this.protocol
                            .getMappingData()
                            .getRecipeSerializerMappings()
                            .mappedIdentifier(serializerTypeId);
                        wrapper.write(Type.STRING, serializerType);
                        wrapper.write(Type.STRING, recipeIdentifier);
                        recipeRewriter.handleRecipeType(wrapper, Key.stripMinecraftNamespace(serializerType));
                    }
                }
            );
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToClient(connection, item);
        return this.vvProtocol.getItemRewriter().toOldItem(item, DATA_CONVERTER);
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        Item structuredItem = this.vvProtocol.getItemRewriter().toStructuredItem(connection, item);
        return super.handleItemToServer(connection, structuredItem);
    }
}
