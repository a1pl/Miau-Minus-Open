package com.viaversion.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.data.EffectMappings;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Environment;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionImpl;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_8;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import java.util.ArrayList;

public class WorldPackets1_9 {
    public static void register(Protocol1_8To1_9 protocol) {
        protocol.registerClientbound(ClientboundPackets1_9.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.POSITION1_8);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.NAMED_COMPOUND_TAG);
                this.handler(wrapper -> {
                    CompoundTag tag = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                    CompoundTag spawnData = tag.remove("SpawnData");
                    if (spawnData != null) {
                        StringTag id = spawnData.remove("id");
                        if (id != null) {
                            tag.put("EntityId", id);
                        }
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.BLOCK_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.POSITION1_8);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int block = wrapper.get(Type.VAR_INT, 0);
                    if (block >= 219 && block <= 234) {
                        wrapper.set(Type.VAR_INT, 0, 130);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.NAMED_SOUND, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.STRING);
                this.handler(wrapper -> {
                    String name = wrapper.get(Type.STRING, 0);
                    name = protocol.getMappingData().getMappedNamedSound(name);
                    if (name == null) {
                        wrapper.cancel();
                    } else {
                        wrapper.set(Type.STRING, 0, name);
                    }
                });
                this.read(Type.VAR_INT);
                this.map(Type.INT);
                this.map(Type.INT);
                this.map(Type.INT);
                this.map(Type.FLOAT);
                this.map(Type.UNSIGNED_BYTE);
            }
        });
        protocol.registerClientbound(
            ClientboundPackets1_9.UNLOAD_CHUNK,
            ClientboundPackets1_8.CHUNK_DATA,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.handler(
                        wrapper -> {
                            Environment environment = wrapper.user().get(ClientWorld.class).getEnvironment();
                            int chunkX = wrapper.read(Type.INT);
                            int chunkZ = wrapper.read(Type.INT);
                            wrapper.write(
                                ChunkType1_8.forEnvironment(environment),
                                new BaseChunk(
                                    chunkX, chunkZ, true, false, 0, new ChunkSection[16], null, new ArrayList<>()
                                )
                            );
                        }
                    );
                }
            }
        );
        protocol.registerClientbound(
            ClientboundPackets1_9.CHUNK_DATA,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.handler(
                        wrapper -> {
                            Environment environment = wrapper.user().get(ClientWorld.class).getEnvironment();
                            Chunk chunk = wrapper.read(ChunkType1_9_1.forEnvironment(environment));

                            for (ChunkSection section : chunk.getSections()) {
                                if (section != null) {
                                    DataPalette palette = section.palette(PaletteType.BLOCKS);

                                    for (int i = 0; i < palette.size(); i++) {
                                        int block = palette.idByIndex(i);
                                        int replacedBlock = protocol.getItemRewriter().handleBlockId(block);
                                        palette.setIdByIndex(i, replacedBlock);
                                    }
                                }
                            }

                            if (chunk.isFullChunk() && chunk.getBitmask() == 0) {
                                boolean skylight = environment == Environment.NORMAL;
                                ChunkSection[] sections = new ChunkSection[16];
                                ChunkSection section = new ChunkSectionImpl(true);
                                sections[0] = section;
                                section.palette(PaletteType.BLOCKS).addId(0);
                                if (skylight) {
                                    section.getLight().setSkyLight(new byte[2048]);
                                }

                                chunk = new BaseChunk(
                                    chunk.getX(),
                                    chunk.getZ(),
                                    true,
                                    false,
                                    1,
                                    sections,
                                    chunk.getBiomeData(),
                                    chunk.getBlockEntities()
                                );
                            }

                            wrapper.write(ChunkType1_8.forEnvironment(environment), chunk);
                            UserConnection user = wrapper.user();
                            chunk.getBlockEntities()
                                .forEach(
                                    nbt -> {
                                        if (nbt.contains("x")
                                            && nbt.contains("y")
                                            && nbt.contains("z")
                                            && nbt.contains("id")) {
                                            Position position = new Position(
                                                (Integer)nbt.<Tag>get("x").getValue(),
                                                (Integer)nbt.<Tag>get("y").getValue(),
                                                (Integer)nbt.<Tag>get("z").getValue()
                                            );
                                            String id = (String)nbt.<Tag>get("id").getValue();
                                            short action;
                                            switch (id) {
                                                case "minecraft:mob_spawner":
                                                    action = 1;
                                                    break;
                                                case "minecraft:command_block":
                                                    action = 2;
                                                    break;
                                                case "minecraft:beacon":
                                                    action = 3;
                                                    break;
                                                case "minecraft:skull":
                                                    action = 4;
                                                    break;
                                                case "minecraft:flower_pot":
                                                    action = 5;
                                                    break;
                                                case "minecraft:banner":
                                                    action = 6;
                                                    break;
                                                default:
                                                    return;
                                            }

                                            PacketWrapper updateTileEntity = PacketWrapper.create(9, null, user);
                                            updateTileEntity.write(Type.POSITION1_8, position);
                                            updateTileEntity.write(Type.UNSIGNED_BYTE, action);
                                            updateTileEntity.write(Type.NBT, nbt);

                                            try {
                                                updateTileEntity.scheduleSend(Protocol1_8To1_9.class, false);
                                            } catch (Exception e) {
                                                ViaRewind.getPlatform()
                                                    .getLogger()
                                                    .warning(
                                                        "Error sending tile entity update packet: " + e.getMessage()
                                                    );
                                            }
                                        }
                                    }
                                );
                        }
                    );
                }
            }
        );
        protocol.registerClientbound(ClientboundPackets1_9.EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.POSITION1_8);
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    id = EffectMappings.getOldId(id);
                    if (id == -1) {
                        wrapper.cancel();
                    } else {
                        wrapper.set(Type.INT, 0, id);
                        if (id == 2001) {
                            int replacedBlock = protocol.getItemRewriter().handleBlockId(wrapper.get(Type.INT, 1));
                            wrapper.set(Type.INT, 1, replacedBlock);
                        }
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.handler(wrapper -> {
                    int type = wrapper.get(Type.INT, 0);
                    if (type > 41 && !ViaRewind.getConfig().isReplaceParticles()) {
                        wrapper.cancel();
                    } else {
                        if (type == 42) {
                            wrapper.set(Type.INT, 0, 24);
                        } else if (type == 43) {
                            wrapper.set(Type.INT, 0, 3);
                        } else if (type == 44) {
                            wrapper.set(Type.INT, 0, 34);
                        } else if (type == 45) {
                            wrapper.set(Type.INT, 0, 1);
                        }
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.MAP_DATA, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.BYTE);
                this.read(Type.BOOLEAN);
            }
        });
        protocol.registerClientbound(
            ClientboundPackets1_9.SOUND, ClientboundPackets1_8.NAMED_SOUND, new PacketHandlers() {
                @Override
                public void register() {
                    this.handler(wrapper -> {
                        int soundId = wrapper.read(Type.VAR_INT);
                        String soundName = protocol.getMappingData().soundName(soundId);
                        if (soundName == null) {
                            wrapper.cancel();
                        } else {
                            wrapper.write(Type.STRING, protocol.getMappingData().getMappedNamedSound(soundName));
                        }
                    });
                    this.read(Type.VAR_INT);
                    this.map(Type.INT);
                    this.map(Type.INT);
                    this.map(Type.INT);
                    this.map(Type.FLOAT);
                    this.map(Type.UNSIGNED_BYTE);
                }
            }
        );
    }
}
