package com.viaversion.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.BlockPlaceDestroyTracker;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.BossBarStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.CooldownStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.PlayerPositionTracker;
import com.viaversion.viarewind.utils.ChatUtil;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_8;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;
import java.util.ArrayList;
import java.util.UUID;

public class PlayerPackets1_9 {
    public static void register(Protocol1_8To1_9 protocol) {
        protocol.registerClientbound(ClientboundPackets1_9.BOSSBAR, null, wrapper -> {
            wrapper.cancel();
            BossBarStorage bossbar = wrapper.user().get(BossBarStorage.class);
            UUID uuid = wrapper.read(Type.UUID);
            int action = wrapper.read(Type.VAR_INT);
            if (action == 0) {
                JsonElement title = wrapper.read(Type.COMPONENT);
                float health = wrapper.read(Type.FLOAT);
                wrapper.read(Type.VAR_INT);
                wrapper.read(Type.VAR_INT);
                wrapper.read(Type.UNSIGNED_BYTE);
                bossbar.add(uuid, ChatUtil.jsonToLegacy(wrapper.user(), title), health);
            } else if (action == 1) {
                bossbar.remove(uuid);
            } else if (action == 2) {
                float health = wrapper.read(Type.FLOAT);
                bossbar.updateHealth(uuid, health);
            } else if (action == 3) {
                JsonElement title = wrapper.read(Type.COMPONENT);
                bossbar.updateTitle(uuid, ChatUtil.jsonToLegacy(wrapper.user(), title));
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.TEAMS, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.STRING);
                this.map(Type.BYTE);
                this.handler(wrapper -> {
                    byte mode = wrapper.get(Type.BYTE, 0);
                    if (mode == 0 || mode == 2) {
                        wrapper.passthrough(Type.STRING);
                        wrapper.passthrough(Type.STRING);
                        wrapper.passthrough(Type.STRING);
                        wrapper.passthrough(Type.BYTE);
                        wrapper.passthrough(Type.STRING);
                        wrapper.read(Type.STRING);
                    }
                });
            }
        });
        protocol.cancelClientbound(ClientboundPackets1_9.COOLDOWN);
        protocol.registerClientbound(
            ClientboundPackets1_9.PLUGIN_MESSAGE,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.STRING);
                    this.handlerSoftFail(
                        wrapper -> {
                            String channel = wrapper.get(Type.STRING, 0);
                            if (channel.equals("MC|TrList")) {
                                wrapper.passthrough(Type.INT);
                                int size;
                                if (wrapper.isReadable(Type.BYTE, 0)) {
                                    size = wrapper.passthrough(Type.BYTE);
                                } else {
                                    size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                                }

                                ItemRewriter<?> itemRewriter = protocol.getItemRewriter();

                                for (int i = 0; i < size; i++) {
                                    wrapper.write(
                                        Type.ITEM1_8,
                                        itemRewriter.handleItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_8))
                                    );
                                    wrapper.write(
                                        Type.ITEM1_8,
                                        itemRewriter.handleItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_8))
                                    );
                                    boolean has3Items = wrapper.passthrough(Type.BOOLEAN);
                                    if (has3Items) {
                                        wrapper.write(
                                            Type.ITEM1_8,
                                            itemRewriter.handleItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_8))
                                        );
                                    }

                                    wrapper.passthrough(Type.BOOLEAN);
                                    wrapper.passthrough(Type.INT);
                                    wrapper.passthrough(Type.INT);
                                }
                            } else if (channel.equals("MC|BOpen")) {
                                wrapper.read(Type.VAR_INT);
                            }
                        }
                    );
                }
            }
        );
        protocol.registerClientbound(ClientboundPackets1_9.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.BYTE);
                this.handler(wrapper -> {
                    PlayerPositionTracker pos = wrapper.user().get(PlayerPositionTracker.class);
                    pos.setConfirmId(wrapper.read(Type.VAR_INT));
                    byte flags = wrapper.get(Type.BYTE, 0);
                    double x = wrapper.get(Type.DOUBLE, 0);
                    double y = wrapper.get(Type.DOUBLE, 1);
                    double z = wrapper.get(Type.DOUBLE, 2);
                    float yaw = wrapper.get(Type.FLOAT, 0);
                    float pitch = wrapper.get(Type.FLOAT, 1);
                    wrapper.set(Type.BYTE, 0, (byte)0);
                    if (flags != 0) {
                        if ((flags & 1) != 0) {
                            x += pos.getPosX();
                            wrapper.set(Type.DOUBLE, 0, x);
                        }

                        if ((flags & 2) != 0) {
                            y += pos.getPosY();
                            wrapper.set(Type.DOUBLE, 1, y);
                        }

                        if ((flags & 4) != 0) {
                            z += pos.getPosZ();
                            wrapper.set(Type.DOUBLE, 2, z);
                        }

                        if ((flags & 8) != 0) {
                            yaw += pos.getYaw();
                            wrapper.set(Type.FLOAT, 0, yaw);
                        }

                        if ((flags & 16) != 0) {
                            pitch += pos.getPitch();
                            wrapper.set(Type.FLOAT, 1, pitch);
                        }
                    }

                    pos.setPos(x, y, z);
                    pos.setYaw(yaw);
                    pos.setPitch(pitch);
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.handler(wrapper -> wrapper.user().get(BossBarStorage.class).reset());
                this.handler(wrapper -> {
                    ClientWorld world = wrapper.user().get(ClientWorld.class);
                    world.setEnvironment(wrapper.get(Type.INT, 0));
                });
            }
        });
        protocol.registerServerbound(
            ServerboundPackets1_8.CHAT_MESSAGE,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.STRING);
                    this.handler(
                        wrapper -> {
                            if (ViaRewind.getConfig().isEnableOffhand()) {
                                String msg = wrapper.get(Type.STRING, 0);
                                if (msg.toLowerCase().trim().startsWith(ViaRewind.getConfig().getOffhandCommand())) {
                                    wrapper.cancel();
                                    PacketWrapper swapItems = PacketWrapper.create(
                                        ServerboundPackets1_9.PLAYER_DIGGING, wrapper.user()
                                    );
                                    swapItems.write(Type.VAR_INT, 6);
                                    swapItems.write(Type.POSITION1_8, new Position(0, 0, 0));
                                    swapItems.write(Type.BYTE, (byte)-1);
                                    swapItems.sendToServer(Protocol1_8To1_9.class);
                                }
                            }
                        }
                    );
                }
            }
        );
        protocol.registerServerbound(ServerboundPackets1_8.INTERACT_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int type = wrapper.get(Type.VAR_INT, 1);
                    if (type == 2) {
                        wrapper.passthrough(Type.FLOAT);
                        wrapper.passthrough(Type.FLOAT);
                        wrapper.passthrough(Type.FLOAT);
                    }

                    if (type == 2 || type == 0) {
                        wrapper.write(Type.VAR_INT, 0);
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.PLAYER_MOVEMENT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    wrapper.user().get(PlayerPositionTracker.class).sendAnimations();
                    EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
                    if (tracker.isInsideVehicle(tracker.clientEntityId())) {
                        wrapper.cancel();
                    }
                });
            }
        });
        protocol.registerServerbound(
            ServerboundPackets1_8.PLAYER_POSITION,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.DOUBLE);
                    this.map(Type.DOUBLE);
                    this.map(Type.DOUBLE);
                    this.map(Type.BOOLEAN);
                    this.handler(
                        wrapper -> {
                            wrapper.user().get(PlayerPositionTracker.class).sendAnimations();
                            PlayerPositionTracker pos = wrapper.user().get(PlayerPositionTracker.class);
                            if (pos.getConfirmId() == -1) {
                                pos.setPos(
                                    wrapper.get(Type.DOUBLE, 0),
                                    wrapper.get(Type.DOUBLE, 1),
                                    wrapper.get(Type.DOUBLE, 2)
                                );
                                pos.setOnGround(wrapper.get(Type.BOOLEAN, 0));
                            }
                        }
                    );
                    this.handler(wrapper -> wrapper.user().get(BossBarStorage.class).updateLocation());
                }
            }
        );
        protocol.registerServerbound(ServerboundPackets1_8.PLAYER_ROTATION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    wrapper.user().get(PlayerPositionTracker.class).sendAnimations();
                    PlayerPositionTracker pos = wrapper.user().get(PlayerPositionTracker.class);
                    if (pos.getConfirmId() == -1) {
                        pos.setYaw(wrapper.get(Type.FLOAT, 0));
                        pos.setPitch(wrapper.get(Type.FLOAT, 1));
                        pos.setOnGround(wrapper.get(Type.BOOLEAN, 0));
                    }
                });
                this.handler(wrapper -> wrapper.user().get(BossBarStorage.class).updateLocation());
            }
        });
        protocol.registerServerbound(
            ServerboundPackets1_8.PLAYER_POSITION_AND_ROTATION,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.DOUBLE);
                    this.map(Type.DOUBLE);
                    this.map(Type.DOUBLE);
                    this.map(Type.FLOAT);
                    this.map(Type.FLOAT);
                    this.map(Type.BOOLEAN);
                    this.handler(
                        wrapper -> {
                            wrapper.user().get(PlayerPositionTracker.class).sendAnimations();
                            double x = wrapper.get(Type.DOUBLE, 0);
                            double y = wrapper.get(Type.DOUBLE, 1);
                            double z = wrapper.get(Type.DOUBLE, 2);
                            float yaw = wrapper.get(Type.FLOAT, 0);
                            float pitch = wrapper.get(Type.FLOAT, 1);
                            boolean onGround = wrapper.get(Type.BOOLEAN, 0);
                            PlayerPositionTracker pos = wrapper.user().get(PlayerPositionTracker.class);
                            if (pos.getConfirmId() != -1) {
                                if (pos.getPosX() == x
                                    && pos.getPosY() == y
                                    && pos.getPosZ() == z
                                    && pos.getYaw() == yaw
                                    && pos.getPitch() == pitch) {
                                    PacketWrapper confirmTeleport = wrapper.create(0);
                                    confirmTeleport.write(Type.VAR_INT, pos.getConfirmId());
                                    confirmTeleport.sendToServer(Protocol1_8To1_9.class);
                                    pos.setConfirmId(-1);
                                }
                            } else {
                                pos.setPos(x, y, z);
                                pos.setYaw(yaw);
                                pos.setPitch(pitch);
                                pos.setOnGround(onGround);
                                wrapper.user().get(BossBarStorage.class).updateLocation();
                            }
                        }
                    );
                }
            }
        );
        protocol.registerServerbound(ServerboundPackets1_8.PLAYER_DIGGING, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.POSITION1_8);
                this.handler(wrapper -> {
                    int state = wrapper.get(Type.VAR_INT, 0);
                    if (state == 0) {
                        wrapper.user().get(BlockPlaceDestroyTracker.class).setMining();
                    } else if (state == 2) {
                        BlockPlaceDestroyTracker tracker = wrapper.user().get(BlockPlaceDestroyTracker.class);
                        tracker.setMining();
                        tracker.setLastMining(System.currentTimeMillis() + 100L);
                        wrapper.user().get(CooldownStorage.class).setLastHit(0L);
                    } else if (state == 1) {
                        BlockPlaceDestroyTracker tracker = wrapper.user().get(BlockPlaceDestroyTracker.class);
                        tracker.setMining();
                        tracker.setLastMining(0L);
                        wrapper.user().get(CooldownStorage.class).hit();
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.PLAYER_BLOCK_PLACEMENT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.POSITION1_8);
                this.map(Type.BYTE, Type.VAR_INT);
                this.read(Type.ITEM1_8);
                this.create(Type.VAR_INT, 0);
                this.map(Type.BYTE, Type.UNSIGNED_BYTE);
                this.map(Type.BYTE, Type.UNSIGNED_BYTE);
                this.map(Type.BYTE, Type.UNSIGNED_BYTE);
                this.handler(wrapper -> {
                    if (wrapper.get(Type.VAR_INT, 0) == -1) {
                        wrapper.cancel();
                        PacketWrapper useItem = PacketWrapper.create(29, null, wrapper.user());
                        useItem.write(Type.VAR_INT, 0);
                        useItem.sendToServer(Protocol1_8To1_9.class);
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.HELD_ITEM_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                this.handler(wrapper -> wrapper.user().get(CooldownStorage.class).hit());
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.ANIMATION, new PacketHandlers() {
            @Override
            public void register() {
                this.handler(wrapper -> {
                    wrapper.cancel();
                    wrapper.cancel();
                    PacketWrapper delayedPacket = PacketWrapper.create(26, null, wrapper.user());
                    delayedPacket.write(Type.VAR_INT, 0);
                    wrapper.user().get(PlayerPositionTracker.class).queueAnimation(delayedPacket);
                });
                this.handler(wrapper -> {
                    wrapper.user().get(BlockPlaceDestroyTracker.class).updateMining();
                    wrapper.user().get(CooldownStorage.class).hit();
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.ENTITY_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 1);
                    if (action == 6) {
                        wrapper.set(Type.VAR_INT, 1, 7);
                    } else if (action == 0) {
                        PlayerPositionTracker pos = wrapper.user().get(PlayerPositionTracker.class);
                        if (!pos.isOnGround()) {
                            PacketWrapper elytra = PacketWrapper.create(20, null, wrapper.user());
                            elytra.write(Type.VAR_INT, wrapper.get(Type.VAR_INT, 0));
                            elytra.write(Type.VAR_INT, 8);
                            elytra.write(Type.VAR_INT, 0);
                            elytra.scheduleSendToServer(Protocol1_8To1_9.class);
                        }
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.STEER_VEHICLE, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.UNSIGNED_BYTE);
                this.handler(wrapper -> {
                    EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
                    int vehicle = tracker.getVehicle(tracker.clientEntityId());
                    if (vehicle != -1 && tracker.entityType(vehicle) == EntityTypes1_10.EntityType.BOAT) {
                        PacketWrapper steerBoat = PacketWrapper.create(17, null, wrapper.user());
                        float left = wrapper.get(Type.FLOAT, 0);
                        float forward = wrapper.get(Type.FLOAT, 1);
                        steerBoat.write(Type.BOOLEAN, forward != 0.0F || left < 0.0F);
                        steerBoat.write(Type.BOOLEAN, forward != 0.0F || left > 0.0F);
                        steerBoat.scheduleSendToServer(Protocol1_8To1_9.class);
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.UPDATE_SIGN, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.POSITION1_8);
                this.handler(wrapper -> {
                    for (int i = 0; i < 4; i++) {
                        wrapper.write(Type.STRING, ChatUtil.jsonToLegacy(wrapper.user(), wrapper.read(Type.COMPONENT)));
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.TAB_COMPLETE, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.STRING);
                this.create(Type.BOOLEAN, false);
                this.map(Type.OPTIONAL_POSITION1_8);
            }
        });
        protocol.registerServerbound(
            ServerboundPackets1_8.CLIENT_SETTINGS,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.STRING);
                    this.map(Type.BYTE);
                    this.map(Type.BYTE, Type.VAR_INT);
                    this.map(Type.BOOLEAN);
                    this.map(Type.UNSIGNED_BYTE);
                    this.create(Type.VAR_INT, 1);
                    this.handler(
                        wrapper -> {
                            short flags = wrapper.get(Type.UNSIGNED_BYTE, 0);
                            PacketWrapper updateSkin = PacketWrapper.create(28, null, wrapper.user());
                            updateSkin.write(
                                Type.VAR_INT,
                                wrapper.user().<EntityTracker>getEntityTracker(Protocol1_8To1_9.class).clientEntityId()
                            );
                            ArrayList<Metadata> metadata = new ArrayList<>();
                            metadata.add(new Metadata(10, MetaType1_8.Byte, (byte)flags));
                            updateSkin.write(Types1_8.METADATA_LIST, metadata);
                            updateSkin.scheduleSend(Protocol1_8To1_9.class);
                        }
                    );
                }
            }
        );
        protocol.registerServerbound(ServerboundPackets1_8.PLUGIN_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.STRING);
                this.handlerSoftFail(wrapper -> {
                    String channel = wrapper.get(Type.STRING, 0);
                    if (channel.equals("MC|BEdit") || channel.equals("MC|BSign")) {
                        Item book = wrapper.passthrough(Type.ITEM);
                        book.setIdentifier(386);
                        CompoundTag tag = book.tag();
                        if (tag.contains("pages")) {
                            ListTag<StringTag> pages = tag.getListTag("pages", StringTag.class);
                            if (pages.size() > ViaRewind.getConfig().getMaxBookPages()) {
                                wrapper.user().disconnect("Too many book pages");
                                return;
                            }

                            for (int i = 0; i < pages.size(); i++) {
                                StringTag page = pages.get(i);
                                String value = page.getValue();
                                if (value.length() > ViaRewind.getConfig().getMaxBookPageSize()) {
                                    wrapper.user().disconnect("Book page too large");
                                    return;
                                }

                                value = ChatUtil.jsonToLegacy(wrapper.user(), value);
                                page.setValue(value);
                            }
                        }
                    } else if (channel.equals("MC|AdvCdm")) {
                        wrapper.set(Type.STRING, 0, "MC|AdvCmd");
                    }
                });
            }
        });
    }
}
