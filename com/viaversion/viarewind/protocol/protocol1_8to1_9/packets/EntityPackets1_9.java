package com.viaversion.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.CooldownStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.LevitationStorage;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.PlayerPositionTracker;
import com.viaversion.viarewind.utils.math.RelativeMoveUtil;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Vector;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityPackets1_9 {
    public static void register(Protocol1_8To1_9 protocol) {
        protocol.registerClientbound(ClientboundPackets1_9.ENTITY_STATUS, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.handler(wrapper -> {
                    byte status = wrapper.read(Type.BYTE);
                    if (status > 23) {
                        wrapper.cancel();
                    } else {
                        wrapper.write(Type.BYTE, status);
                    }
                });
            }
        });
        protocol.registerClientbound(
            ClientboundPackets1_9.ENTITY_POSITION,
            wrapper -> {
                int entityId = wrapper.passthrough(Type.VAR_INT);
                int deltaX = wrapper.read(Type.SHORT);
                int deltaY = wrapper.read(Type.SHORT);
                int deltaZ = wrapper.read(Type.SHORT);
                Vector[] moves = RelativeMoveUtil.calculateRelativeMoves(
                    wrapper.user(), entityId, deltaX, deltaY, deltaZ
                );
                wrapper.write(Type.BYTE, (byte)moves[0].blockX());
                wrapper.write(Type.BYTE, (byte)moves[0].blockY());
                wrapper.write(Type.BYTE, (byte)moves[0].blockZ());
                boolean onGround = wrapper.passthrough(Type.BOOLEAN);
                if (moves.length > 1) {
                    PacketWrapper secondPacket = PacketWrapper.create(
                        ClientboundPackets1_8.ENTITY_POSITION, wrapper.user()
                    );
                    secondPacket.write(Type.VAR_INT, entityId);
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockX());
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockY());
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockZ());
                    secondPacket.write(Type.BOOLEAN, onGround);
                    secondPacket.scheduleSend(Protocol1_8To1_9.class);
                }
            }
        );
        protocol.registerClientbound(
            ClientboundPackets1_9.ENTITY_POSITION_AND_ROTATION,
            wrapper -> {
                int entityId = wrapper.passthrough(Type.VAR_INT);
                int deltaX = wrapper.read(Type.SHORT);
                int deltaY = wrapper.read(Type.SHORT);
                int deltaZ = wrapper.read(Type.SHORT);
                Vector[] moves = RelativeMoveUtil.calculateRelativeMoves(
                    wrapper.user(), entityId, deltaX, deltaY, deltaZ
                );
                wrapper.write(Type.BYTE, (byte)moves[0].blockX());
                wrapper.write(Type.BYTE, (byte)moves[0].blockY());
                wrapper.write(Type.BYTE, (byte)moves[0].blockZ());
                byte yaw = wrapper.passthrough(Type.BYTE);
                byte pitch = wrapper.passthrough(Type.BYTE);
                boolean onGround = wrapper.passthrough(Type.BOOLEAN);
                EntityType type = wrapper.user()
                    .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                    .entityType(entityId);
                if (type == EntityTypes1_10.EntityType.BOAT) {
                    yaw = (byte)(yaw - 64);
                    wrapper.set(Type.BYTE, 3, yaw);
                }

                if (moves.length > 1) {
                    PacketWrapper secondPacket = PacketWrapper.create(
                        ClientboundPackets1_8.ENTITY_POSITION_AND_ROTATION, wrapper.user()
                    );
                    secondPacket.write(Type.VAR_INT, entityId);
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockX());
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockY());
                    secondPacket.write(Type.BYTE, (byte)moves[1].blockZ());
                    secondPacket.write(Type.BYTE, yaw);
                    secondPacket.write(Type.BYTE, pitch);
                    secondPacket.write(Type.BOOLEAN, onGround);
                    secondPacket.scheduleSend(Protocol1_8To1_9.class);
                }
            }
        );
        protocol.registerClientbound(
            ClientboundPackets1_9.ENTITY_ROTATION,
            wrapper -> {
                int entityId = wrapper.passthrough(Type.VAR_INT);
                EntityType type = wrapper.user()
                    .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                    .entityType(entityId);
                if (type == EntityTypes1_10.EntityType.BOAT) {
                    byte yaw = wrapper.read(Type.BYTE);
                    yaw = (byte)(yaw - 64);
                    wrapper.write(Type.BYTE, yaw);
                }
            }
        );
        protocol.registerClientbound(
            ClientboundPackets1_9.VEHICLE_MOVE,
            ClientboundPackets1_8.ENTITY_TELEPORT,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.handler(wrapper -> {
                        EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
                        int vehicle = tracker.getVehicle(tracker.clientEntityId());
                        if (vehicle == -1) {
                            wrapper.cancel();
                        }

                        wrapper.write(Type.VAR_INT, vehicle);
                    });
                    this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                    this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                    this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                    this.map(Type.FLOAT, Protocol1_8To1_9.DEGREES_TO_ANGLE);
                    this.map(Type.FLOAT, Protocol1_8To1_9.DEGREES_TO_ANGLE);
                    this.handler(wrapper -> {
                        if (!wrapper.isCancelled()) {
                            PlayerPositionTracker position = wrapper.user().get(PlayerPositionTracker.class);
                            double x = wrapper.get(Type.INT, 0).intValue() / 32.0;
                            double y = wrapper.get(Type.INT, 1).intValue() / 32.0;
                            double z = wrapper.get(Type.INT, 2).intValue() / 32.0;
                            position.setPos(x, y, z);
                        }
                    });
                    this.create(Type.BOOLEAN, true);
                    this.handler(
                        wrapper -> {
                            int entityId = wrapper.get(Type.VAR_INT, 0);
                            EntityType type = wrapper.user()
                                .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                                .entityType(entityId);
                            if (type == EntityTypes1_10.EntityType.BOAT) {
                                byte yaw = wrapper.get(Type.BYTE, 1);
                                yaw = (byte)(yaw - 64);
                                wrapper.set(Type.BYTE, 0, yaw);
                                int y = wrapper.get(Type.INT, 1);
                                y += 10;
                                wrapper.set(Type.INT, 1, y);
                            }
                        }
                    );
                }
            }
        );
        protocol.registerClientbound(ClientboundPackets1_9.REMOVE_ENTITY_EFFECT, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);
            int effectId = wrapper.passthrough(Type.BYTE);
            if (effectId > 23) {
                wrapper.cancel();
            }

            EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
            if (effectId == 25 && entityId == tracker.clientEntityId()) {
                wrapper.user().get(LevitationStorage.class).setActive(false);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.ATTACH_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.INT);
                this.create(Type.BOOLEAN, true);
            }
        });
        protocol.registerClientbound(
            ClientboundPackets1_9.ENTITY_EQUIPMENT,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.VAR_INT);
                    this.handler(wrapper -> {
                        int slot = wrapper.read(Type.VAR_INT);
                        if (slot == 1) {
                            wrapper.cancel();
                        } else if (slot > 1) {
                            slot--;
                        }

                        wrapper.write(Type.SHORT, (short)slot);
                    });
                    this.map(Type.ITEM1_8);
                    this.handler(
                        wrapper -> protocol.getItemRewriter()
                            .handleItemToClient(wrapper.user(), wrapper.get(Type.ITEM1_8, 0))
                    );
                }
            }
        );
        protocol.registerClientbound(ClientboundPackets1_9.SET_PASSENGERS, null, wrapper -> {
            wrapper.cancel();
            EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
            int vehicle = wrapper.read(Type.VAR_INT);
            IntList oldPassengers = tracker.getPassengers(vehicle);
            int count = wrapper.read(Type.VAR_INT);
            IntList passengers = new IntArrayList();

            for (int i = 0; i < count; i++) {
                passengers.add(wrapper.read(Type.VAR_INT));
            }

            tracker.setPassengers(vehicle, passengers);
            if (!oldPassengers.isEmpty()) {
                for (Integer passenger : oldPassengers) {
                    PacketWrapper detach = PacketWrapper.create(ClientboundPackets1_8.ATTACH_ENTITY, wrapper.user());
                    detach.write(Type.INT, passenger);
                    detach.write(Type.INT, -1);
                    detach.write(Type.BOOLEAN, false);
                    detach.scheduleSend(Protocol1_8To1_9.class);
                }
            }

            for (int i = 0; i < count; i++) {
                int attachedEntityId = passengers.getInt(i);
                int holdingEntityId = i == 0 ? vehicle : passengers.getInt(i - 1);
                PacketWrapper attach = PacketWrapper.create(ClientboundPackets1_8.ATTACH_ENTITY, wrapper.user());
                attach.write(Type.INT, attachedEntityId);
                attach.write(Type.INT, holdingEntityId);
                attach.write(Type.BOOLEAN, false);
                attach.scheduleSend(Protocol1_8To1_9.class);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.ENTITY_TELEPORT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
                    if (tracker.entityType(entityId) == EntityTypes1_10.EntityType.BOAT) {
                        byte yaw = wrapper.get(Type.BYTE, 1);
                        yaw = (byte)(yaw - 64);
                        wrapper.set(Type.BYTE, 0, yaw);
                        int y = wrapper.get(Type.INT, 1);
                        y += 10;
                        wrapper.set(Type.INT, 1, y);
                    }

                    tracker.resetEntityOffset(entityId);
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_9.ENTITY_PROPERTIES, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);
            EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
            boolean player = entityId == tracker.clientEntityId();
            int removed = 0;
            int size = wrapper.passthrough(Type.INT);

            for (int i = 0; i < size; i++) {
                String key = wrapper.read(Type.STRING);
                double value = wrapper.read(Type.DOUBLE);
                int modifierSize = wrapper.read(Type.VAR_INT);
                boolean valid = protocol.getItemRewriter().VALID_ATTRIBUTES.contains(key);
                if (valid) {
                    wrapper.write(Type.STRING, key);
                    wrapper.write(Type.DOUBLE, value);
                    wrapper.write(Type.VAR_INT, modifierSize);
                }

                List<Pair<Byte, Double>> modifiers = new ArrayList<>();

                for (int j = 0; j < modifierSize; j++) {
                    UUID modifierId = wrapper.read(Type.UUID);
                    double amount = wrapper.read(Type.DOUBLE);
                    byte operation = wrapper.read(Type.BYTE);
                    if (valid) {
                        wrapper.write(Type.UUID, modifierId);
                        wrapper.write(Type.DOUBLE, amount);
                        wrapper.write(Type.BYTE, operation);
                    }

                    modifiers.add(new Pair<>(operation, amount));
                }

                if (!valid) {
                    if (player && key.equals("generic.attackSpeed")) {
                        wrapper.user().get(CooldownStorage.class).setAttackSpeed(value, modifiers);
                    }

                    removed++;
                }
            }

            wrapper.set(Type.INT, 0, size - removed);
        });
        protocol.registerClientbound(ClientboundPackets1_9.ENTITY_EFFECT, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);
            int effectId = wrapper.passthrough(Type.BYTE);
            byte amplifier = wrapper.passthrough(Type.BYTE);
            if (effectId > 23) {
                wrapper.cancel();
            }

            EntityTracker1_9 tracker = wrapper.user().getEntityTracker(Protocol1_8To1_9.class);
            if (effectId == 25 && entityId == tracker.clientEntityId()) {
                LevitationStorage levitation = wrapper.user().get(LevitationStorage.class);
                levitation.setActive(true);
                levitation.setAmplifier(amplifier);
            }
        });
    }
}
