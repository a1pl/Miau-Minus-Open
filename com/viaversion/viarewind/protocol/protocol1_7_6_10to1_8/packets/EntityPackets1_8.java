package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.packets;

import com.viaversion.viarewind.api.type.Types1_7_6_10;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10To1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.data.VirtualHologramEntity;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.EntityTracker1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.GameProfileStorage;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.PlayerSessionStorage;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import java.util.UUID;

public class EntityPackets1_8 {
    public static void register(Protocol1_7_6_10To1_8 protocol) {
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_EQUIPMENT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.SHORT);
                this.map(Type.ITEM1_8, Types1_7_6_10.COMPRESSED_NBT_ITEM);
                this.handler(wrapper -> {
                    Item item = wrapper.get(Types1_7_6_10.COMPRESSED_NBT_ITEM, 0);
                    protocol.getItemRewriter().handleItemToClient(wrapper.user(), item);
                    wrapper.set(Types1_7_6_10.COMPRESSED_NBT_ITEM, 0, item);
                });
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    int id = wrapper.get(Type.INT, 0);
                    int limit = tracker.clientEntityId() == id ? 3 : 4;
                    if (wrapper.get(Type.SHORT, 0) > limit) {
                        wrapper.cancel();
                    }
                });
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    short slot = wrapper.get(Type.SHORT, 0);
                    UUID uuid = tracker.getPlayerUUID(wrapper.get(Type.INT, 0));
                    if (uuid != null) {
                        Item item = wrapper.get(Types1_7_6_10.COMPRESSED_NBT_ITEM, 0);
                        wrapper.user().get(PlayerSessionStorage.class).setPlayerEquipment(uuid, item, slot);
                        GameProfileStorage storage = wrapper.user().get(GameProfileStorage.class);
                        GameProfileStorage.GameProfile profile = storage.get(uuid);
                        if (profile != null && profile.gamemode == 3) {
                            wrapper.cancel();
                        }
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.USE_BED, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.POSITION1_8, Types1_7_6_10.U_BYTE_POSITION);
            }
        });
        protocol.registerClientbound(
            ClientboundPackets1_8.COLLECT_ITEM,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.VAR_INT, Type.INT);
                    this.map(Type.VAR_INT, Type.INT);
                    this.handler(
                        wrapper -> wrapper.user()
                            .<EntityTracker>getEntityTracker(Protocol1_7_6_10To1_8.class)
                            .removeEntity(wrapper.get(Type.INT, 0))
                    );
                }
            }
        );
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_VELOCITY, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_MOVEMENT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.read(Type.BOOLEAN);
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    VirtualHologramEntity hologram = tracker.getHolograms().get(wrapper.get(Type.INT, 0));
                    if (hologram != null) {
                        wrapper.cancel();
                        int x = wrapper.get(Type.BYTE, 0);
                        int y = wrapper.get(Type.BYTE, 1);
                        int z = wrapper.get(Type.BYTE, 2);
                        hologram.setRelativePosition(x / 32.0, y / 32.0, z / 32.0);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_ROTATION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.read(Type.BOOLEAN);
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    VirtualHologramEntity hologram = tracker.getHolograms().get(wrapper.get(Type.INT, 0));
                    if (hologram != null) {
                        wrapper.cancel();
                        int yaw = wrapper.get(Type.BYTE, 0);
                        int pitch = wrapper.get(Type.BYTE, 1);
                        hologram.setRotation(yaw * 360.0F / 256.0F, pitch * 360.0F / 256.0F);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_POSITION_AND_ROTATION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.read(Type.BOOLEAN);
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    VirtualHologramEntity hologram = tracker.getHolograms().get(wrapper.get(Type.INT, 0));
                    if (hologram != null) {
                        wrapper.cancel();
                        int x = wrapper.get(Type.BYTE, 0);
                        int y = wrapper.get(Type.BYTE, 1);
                        int z = wrapper.get(Type.BYTE, 2);
                        int yaw = wrapper.get(Type.BYTE, 3);
                        int pitch = wrapper.get(Type.BYTE, 4);
                        hologram.setRelativePosition(x / 32.0, y / 32.0, z / 32.0);
                        hologram.setRotation(yaw * 360.0F / 256.0F, pitch * 360.0F / 256.0F);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_TELEPORT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.INT);
                this.map(Type.INT);
                this.map(Type.INT);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.read(Type.BOOLEAN);
                this.handler(wrapper -> {
                    int entityId = wrapper.get(Type.INT, 0);
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    if (tracker.entityType(entityId) == EntityTypes1_10.EntityType.MINECART_ABSTRACT) {
                        int y = wrapper.get(Type.INT, 2);
                        y += 12;
                        wrapper.set(Type.INT, 2, y);
                    }

                    VirtualHologramEntity hologram = tracker.getHolograms().get(entityId);
                    if (hologram != null) {
                        wrapper.cancel();
                        int x = wrapper.get(Type.INT, 1);
                        int y = wrapper.get(Type.INT, 2);
                        int z = wrapper.get(Type.INT, 3);
                        int yaw = wrapper.get(Type.BYTE, 0);
                        int pitch = wrapper.get(Type.BYTE, 1);
                        hologram.setPosition(x / 32.0, y / 32.0, z / 32.0);
                        hologram.setRotation(yaw * 360.0F / 256.0F, pitch * 360.0F / 256.0F);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_HEAD_LOOK, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    VirtualHologramEntity hologram = tracker.getHolograms().get(wrapper.get(Type.INT, 0));
                    if (hologram != null) {
                        wrapper.cancel();
                        int yaw = wrapper.get(Type.BYTE, 0);
                        hologram.setHeadYaw(yaw * 360.0F / 256.0F);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ATTACH_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    boolean leash = wrapper.get(Type.BOOLEAN, 0);
                    if (!leash) {
                        EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                        int passenger = wrapper.get(Type.INT, 0);
                        int vehicle = wrapper.get(Type.INT, 1);
                        tracker.setPassenger(vehicle, passenger);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.VAR_INT, Type.SHORT);
                this.read(Type.BYTE);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.REMOVE_ENTITY_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Type.BYTE);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.ENTITY_PROPERTIES, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.handler(wrapper -> {
                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    if (tracker.getHolograms().containsKey(wrapper.get(Type.INT, 0))) {
                        wrapper.cancel();
                    } else {
                        int amount = wrapper.passthrough(Type.INT);

                        for (int i = 0; i < amount; i++) {
                            wrapper.passthrough(Type.STRING);
                            wrapper.passthrough(Type.DOUBLE);
                            int modifierLength = wrapper.read(Type.VAR_INT);
                            wrapper.write(Type.SHORT, (short)modifierLength);

                            for (int j = 0; j < modifierLength; j++) {
                                wrapper.passthrough(Type.UUID);
                                wrapper.passthrough(Type.DOUBLE);
                                wrapper.passthrough(Type.BYTE);
                            }
                        }
                    }
                });
            }
        });
        protocol.cancelClientbound(ClientboundPackets1_8.UPDATE_ENTITY_NBT);
    }
}
