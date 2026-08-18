package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data.BlockEntityIds;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPackets1_18
    extends BackwardsItemRewriter<ClientboundPackets1_18, ServerboundPackets1_17, Protocol1_17_1To1_18> {
    public BlockItemPackets1_18(Protocol1_17_1To1_18 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        new RecipeRewriter<>(this.protocol).register(ClientboundPackets1_18.DECLARE_RECIPES);
        this.registerSetCooldown(ClientboundPackets1_18.COOLDOWN);
        this.registerWindowItems1_17_1(ClientboundPackets1_18.WINDOW_ITEMS);
        this.registerSetSlot1_17_1(ClientboundPackets1_18.SET_SLOT);
        this.registerEntityEquipmentArray(ClientboundPackets1_18.ENTITY_EQUIPMENT);
        this.registerTradeList(ClientboundPackets1_18.TRADE_LIST);
        this.registerAdvancements(ClientboundPackets1_18.ADVANCEMENTS);
        this.registerClickWindow1_17_1(ServerboundPackets1_17.CLICK_WINDOW);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_18.EFFECT,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.INT);
                        this.map(Type.POSITION1_14);
                        this.map(Type.INT);
                        this.handler(
                            wrapper -> {
                                int id = wrapper.get(Type.INT, 0);
                                int data = wrapper.get(Type.INT, 1);
                                if (id == 1010) {
                                    wrapper.set(
                                        Type.INT,
                                        1,
                                        BlockItemPackets1_18.this.protocol.getMappingData().getNewItemId(data)
                                    );
                                }
                            }
                        );
                    }
                }
            );
        this.registerCreativeInvAction(ServerboundPackets1_17.CREATIVE_INVENTORY_ACTION);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_18.SPAWN_PARTICLE,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.INT);
                        this.map(Type.BOOLEAN);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.FLOAT);
                        this.map(Type.FLOAT);
                        this.map(Type.FLOAT);
                        this.map(Type.FLOAT);
                        this.map(Type.INT);
                        this.handler(
                            wrapper -> {
                                int id = wrapper.get(Type.INT, 0);
                                if (id == 3) {
                                    int blockState = wrapper.read(Type.VAR_INT);
                                    if (blockState == 7786) {
                                        wrapper.set(Type.INT, 0, 3);
                                    } else {
                                        wrapper.set(Type.INT, 0, 2);
                                    }
                                } else {
                                    ParticleMappings mappings = BlockItemPackets1_18.this.protocol
                                        .getMappingData()
                                        .getParticleMappings();
                                    if (mappings.isBlockParticle(id)) {
                                        int data = wrapper.passthrough(Type.VAR_INT);
                                        wrapper.set(
                                            Type.VAR_INT,
                                            0,
                                            BlockItemPackets1_18.this.protocol
                                                .getMappingData()
                                                .getNewBlockStateId(data)
                                        );
                                    } else if (mappings.isItemParticle(id)) {
                                        BlockItemPackets1_18.this.handleItemToClient(
                                            wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)
                                        );
                                    }

                                    int newId = BlockItemPackets1_18.this.protocol
                                        .getMappingData()
                                        .getNewParticleId(id);
                                    if (newId != id) {
                                        wrapper.set(Type.INT, 0, newId);
                                    }
                                }
                            }
                        );
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_18.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.POSITION1_14);
                this.handler(wrapper -> {
                    int id = wrapper.read(Type.VAR_INT);
                    CompoundTag tag = wrapper.read(Type.NAMED_COMPOUND_TAG);
                    int mappedId = BlockEntityIds.mappedId(id);
                    if (mappedId == -1) {
                        wrapper.cancel();
                    } else {
                        String identifier = BlockItemPackets1_18.this.protocol.getMappingData().blockEntities().get(id);
                        if (identifier == null) {
                            wrapper.cancel();
                        } else {
                            CompoundTag newTag = tag == null ? new CompoundTag() : tag;
                            Position pos = wrapper.get(Type.POSITION1_14, 0);
                            newTag.putString("id", Key.namespaced(identifier));
                            newTag.putInt("x", pos.x());
                            newTag.putInt("y", pos.y());
                            newTag.putInt("z", pos.z());
                            BlockItemPackets1_18.this.handleSpawner(id, newTag);
                            wrapper.write(Type.UNSIGNED_BYTE, (short)mappedId);
                            wrapper.write(Type.NAMED_COMPOUND_TAG, newTag);
                        }
                    }
                });
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_18.CHUNK_DATA,
                wrapper -> {
                    EntityTracker tracker = this.protocol.getEntityRewriter().tracker(wrapper.user());
                    ChunkType1_18 chunkType = new ChunkType1_18(
                        tracker.currentWorldSectionHeight(),
                        MathUtil.ceilLog2(this.protocol.getMappingData().getBlockStateMappings().mappedSize()),
                        MathUtil.ceilLog2(tracker.biomesSent())
                    );
                    Chunk oldChunk = wrapper.read(chunkType);
                    ChunkSection[] sections = oldChunk.getSections();
                    BitSet mask = new BitSet(oldChunk.getSections().length);
                    int[] biomeData = new int[sections.length * 64];
                    int biomeIndex = 0;

                    for (int j = 0; j < sections.length; j++) {
                        ChunkSection section = sections[j];
                        DataPalette biomePalette = section.palette(PaletteType.BIOMES);

                        for (int i = 0; i < 64; i++) {
                            biomeData[biomeIndex++] = biomePalette.idAt(i);
                        }

                        if (section.getNonAirBlocksCount() == 0) {
                            sections[j] = null;
                        } else {
                            mask.set(j);
                        }
                    }

                    List<CompoundTag> blockEntityTags = new ArrayList<>(oldChunk.blockEntities().size());

                    for (BlockEntity blockEntity : oldChunk.blockEntities()) {
                        String id = this.protocol.getMappingData().blockEntities().get(blockEntity.typeId());
                        if (id != null) {
                            CompoundTag tag;
                            if (blockEntity.tag() != null) {
                                tag = blockEntity.tag();
                                this.handleSpawner(blockEntity.typeId(), tag);
                            } else {
                                tag = new CompoundTag();
                            }

                            blockEntityTags.add(tag);
                            tag.putInt("x", (oldChunk.getX() << 4) + blockEntity.sectionX());
                            tag.putInt("y", blockEntity.y());
                            tag.putInt("z", (oldChunk.getZ() << 4) + blockEntity.sectionZ());
                            tag.putString("id", Key.namespaced(id));
                        }
                    }

                    Chunk chunk = new BaseChunk(
                        oldChunk.getX(),
                        oldChunk.getZ(),
                        true,
                        false,
                        mask,
                        oldChunk.getSections(),
                        biomeData,
                        oldChunk.getHeightMap(),
                        blockEntityTags
                    );
                    wrapper.write(new ChunkType1_17(tracker.currentWorldSectionHeight()), chunk);
                    PacketWrapper lightPacket = wrapper.create(ClientboundPackets1_17_1.UPDATE_LIGHT);
                    lightPacket.write(Type.VAR_INT, chunk.getX());
                    lightPacket.write(Type.VAR_INT, chunk.getZ());
                    lightPacket.write(Type.BOOLEAN, wrapper.read(Type.BOOLEAN));
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    int skyLightLength = wrapper.read(Type.VAR_INT);
                    lightPacket.write(Type.VAR_INT, skyLightLength);

                    for (int i = 0; i < skyLightLength; i++) {
                        lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }

                    int blockLightLength = wrapper.read(Type.VAR_INT);
                    lightPacket.write(Type.VAR_INT, blockLightLength);

                    for (int i = 0; i < blockLightLength; i++) {
                        lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }

                    lightPacket.send(Protocol1_17_1To1_18.class);
                }
            );
        this.protocol.cancelClientbound(ClientboundPackets1_18.SET_SIMULATION_DISTANCE);
    }

    private void handleSpawner(int typeId, CompoundTag tag) {
        if (typeId == 8) {
            CompoundTag spawnData = tag.getCompoundTag("SpawnData");
            CompoundTag entity;
            if (spawnData != null && (entity = spawnData.getCompoundTag("entity")) != null) {
                tag.put("SpawnData", entity);
            }
        }
    }
}
