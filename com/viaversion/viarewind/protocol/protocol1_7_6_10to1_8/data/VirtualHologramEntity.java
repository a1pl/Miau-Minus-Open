package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.data;

import com.viaversion.viarewind.api.type.Types1_7_6_10;
import com.viaversion.viarewind.api.type.metadata.MetaType1_7_6_10;
import com.viaversion.viarewind.protocol.protocol1_7_2_5to1_7_6_10.ClientboundPackets1_7_2_5;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10To1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.metadata.MetaIndex;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.metadata.MetadataRewriter1_7_6_10To1_8;
import com.viaversion.viarewind.utils.math.AABB;
import com.viaversion.viarewind.utils.math.Vector3d;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.TrackedEntityImpl;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEvent;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEventImpl;
import java.util.ArrayList;
import java.util.List;

public class VirtualHologramEntity {
    private final List<Metadata> metadataTracker = new ArrayList<>();
    private double locX;
    private double locY;
    private double locZ;
    private final UserConnection user;
    private final int entityId;
    private int[] entityIds = null;
    private VirtualHologramEntity.State currentState = null;
    private String name = null;
    private float yaw;
    private float pitch;
    private float headYaw;
    private boolean small = false;
    private boolean marker = false;

    public VirtualHologramEntity(UserConnection user, int entityId) {
        this.user = user;
        this.entityId = entityId;
    }

    public void setPosition(double x, double y, double z) throws Exception {
        if (x != this.locX || y != this.locY || z != this.locZ) {
            this.locX = x;
            this.locY = y;
            this.locZ = z;
            this.updateLocation(false);
        }
    }

    public void setRelativePosition(double x, double y, double z) throws Exception {
        if (x != 0.0 || y != 0.0 || z != 0.0) {
            this.locX += x;
            this.locY += y;
            this.locZ += z;
            this.updateLocation(false);
        }
    }

    public void setRotation(float yaw, float pitch) throws Exception {
        if (this.yaw != yaw && this.headYaw != yaw && this.pitch != pitch) {
            this.yaw = yaw;
            this.headYaw = yaw;
            this.pitch = pitch;
            this.updateLocation(false);
        }
    }

    public void setHeadYaw(float yaw) throws Exception {
        if (this.headYaw != yaw) {
            this.headYaw = yaw;
            this.updateLocation(false);
        }
    }

    public void syncState(MetadataRewriter1_7_6_10To1_8 entityRewriter, List<Metadata> metadataList) throws Exception {
        for (Metadata metadata : metadataList) {
            this.metadataTracker.removeIf(m -> m.id() == metadata.id());
            this.metadataTracker.add(metadata);
        }

        byte flags = 0;
        byte armorStandFlags = 0;

        for (Metadata metadata : this.metadataTracker) {
            if (metadata.id() == 0 && metadata.metaType() == MetaType1_8.Byte) {
                flags = ((Number)metadata.getValue()).byteValue();
            } else if (metadata.id() == 2 && metadata.metaType() == MetaType1_8.String) {
                this.name = metadata.getValue().toString();
                if (this.name != null && this.name.isEmpty()) {
                    this.name = null;
                }
            } else if (metadata.id() == 10 && metadata.metaType() == MetaType1_8.Byte) {
                armorStandFlags = ((Number)metadata.getValue()).byteValue();
            }
        }

        boolean invisible = (flags & 32) != 0;
        this.small = (armorStandFlags & 1) != 0;
        this.marker = (armorStandFlags & 16) != 0;
        VirtualHologramEntity.State prevState = this.currentState;
        if (invisible && this.name != null) {
            this.currentState = VirtualHologramEntity.State.HOLOGRAM;
        } else {
            this.currentState = VirtualHologramEntity.State.ZOMBIE;
        }

        if (this.currentState != prevState) {
            this.deleteEntity();
            this.sendSpawnPacket(entityRewriter);
        } else {
            this.sendMetadataUpdate(entityRewriter);
            this.updateLocation(false);
        }
    }

    private void updateLocation(boolean remount) throws Exception {
        if (this.entityIds != null) {
            if (this.currentState == VirtualHologramEntity.State.ZOMBIE) {
                this.teleportEntity(this.entityId, this.locX, this.locY, this.locZ, this.yaw, this.pitch);
                PacketWrapper entityHeadLook = PacketWrapper.create(
                    ClientboundPackets1_7_2_5.ENTITY_HEAD_LOOK, this.user
                );
                entityHeadLook.write(Type.INT, this.entityId);
                entityHeadLook.write(Type.BYTE, (byte)(this.headYaw / 360.0F * 256.0F));
                entityHeadLook.send(Protocol1_7_6_10To1_8.class);
            } else if (this.currentState == VirtualHologramEntity.State.HOLOGRAM) {
                if (remount) {
                    PacketWrapper detach = PacketWrapper.create(ClientboundPackets1_7_2_5.ATTACH_ENTITY, this.user);
                    detach.write(Type.INT, this.entityIds[1]);
                    detach.write(Type.INT, -1);
                    detach.write(Type.BOOLEAN, false);
                    detach.send(Protocol1_7_6_10To1_8.class);
                }

                this.teleportEntity(
                    this.entityIds[0],
                    this.locX,
                    this.locY + (this.marker ? 54.85 : (this.small ? 56.0 : 57.0)) - 0.16,
                    this.locZ,
                    0.0F,
                    0.0F
                );
                if (remount) {
                    this.teleportEntity(this.entityIds[1], this.locX, this.locY + 56.75, this.locZ, 0.0F, 0.0F);
                    PacketWrapper attach = PacketWrapper.create(
                        ClientboundPackets1_7_2_5.ATTACH_ENTITY, null, this.user
                    );
                    attach.write(Type.INT, this.entityIds[1]);
                    attach.write(Type.INT, this.entityIds[0]);
                    attach.write(Type.BOOLEAN, false);
                    attach.send(Protocol1_7_6_10To1_8.class);
                }
            }
        }
    }

    protected void teleportEntity(int entityId, double x, double y, double z, float yaw, float pitch) throws Exception {
        PacketWrapper entityTeleport = PacketWrapper.create(ClientboundPackets1_7_2_5.ENTITY_TELEPORT, this.user);
        entityTeleport.write(Type.INT, entityId);
        entityTeleport.write(Type.INT, (int)(x * 32.0));
        entityTeleport.write(Type.INT, (int)(y * 32.0));
        entityTeleport.write(Type.INT, (int)(z * 32.0));
        entityTeleport.write(Type.BYTE, (byte)(yaw / 360.0F * 256.0F));
        entityTeleport.write(Type.BYTE, (byte)(pitch / 360.0F * 256.0F));
        entityTeleport.send(Protocol1_7_6_10To1_8.class);
    }

    protected void spawnEntity(int entityId, int type, double locX, double locY, double locZ) throws Exception {
        PacketWrapper spawnMob = PacketWrapper.create(ClientboundPackets1_7_2_5.SPAWN_MOB, null, this.user);
        spawnMob.write(Type.VAR_INT, entityId);
        spawnMob.write(Type.UNSIGNED_BYTE, (short)type);
        spawnMob.write(Type.INT, (int)(locX * 32.0));
        spawnMob.write(Type.INT, (int)(locY * 32.0));
        spawnMob.write(Type.INT, (int)(locZ * 32.0));
        spawnMob.write(Type.BYTE, (byte)0);
        spawnMob.write(Type.BYTE, (byte)0);
        spawnMob.write(Type.BYTE, (byte)0);
        spawnMob.write(Type.SHORT, (short)0);
        spawnMob.write(Type.SHORT, (short)0);
        spawnMob.write(Type.SHORT, (short)0);
        spawnMob.write(Types1_7_6_10.METADATA_LIST, new ArrayList<>());
        spawnMob.send(Protocol1_7_6_10To1_8.class);
    }

    public void sendMetadataUpdate(MetadataRewriter1_7_6_10To1_8 entityRewriter) throws Exception {
        if (this.entityIds != null) {
            PacketWrapper metadataPacket = PacketWrapper.create(ClientboundPackets1_7_2_5.ENTITY_METADATA, this.user);
            if (this.currentState == VirtualHologramEntity.State.ZOMBIE) {
                this.writeZombieMeta(entityRewriter, metadataPacket);
            } else {
                if (this.currentState != VirtualHologramEntity.State.HOLOGRAM) {
                    return;
                }

                this.writeHologramMeta(metadataPacket);
            }

            metadataPacket.send(Protocol1_7_6_10To1_8.class);
        }
    }

    private void writeZombieMeta(MetadataRewriter1_7_6_10To1_8 entityRewriter, PacketWrapper wrapper) {
        wrapper.write(Type.INT, this.entityIds[0]);
        List<Metadata> metadataList = new ArrayList<>();

        for (Metadata metadata : this.metadataTracker) {
            if (metadata.id() >= 0 && metadata.id() <= 9) {
                metadataList.add(new Metadata(metadata.id(), metadata.metaType(), metadata.getValue()));
            }
        }

        if (this.small) {
            metadataList.add(new Metadata(12, MetaType1_8.Byte, (byte)1));
        }

        for (Metadata metadata : metadataList.toArray(new Metadata[0])) {
            MetaHandlerEvent event = new MetaHandlerEventImpl(
                wrapper.user(), new TrackedEntityImpl(EntityTypes1_10.EntityType.ZOMBIE), -1, metadata, metadataList
            );

            try {
                entityRewriter.handleMetadata(event, metadata);
            } catch (Exception e) {
                metadataList.remove(metadata);
                break;
            }

            if (event.cancelled()) {
                metadataList.remove(metadata);
                break;
            }
        }

        wrapper.write(Types1_7_6_10.METADATA_LIST, metadataList);
    }

    private void writeHologramMeta(PacketWrapper wrapper) {
        wrapper.write(Type.INT, this.entityIds[1]);
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata(MetaIndex.ENTITY_AGEABLE_AGE.getIndex(), MetaType1_7_6_10.Int, -1700000));
        metadataList.add(new Metadata(MetaIndex.ENTITY_LIVING_NAME_TAG.getIndex(), MetaType1_7_6_10.String, this.name));
        metadataList.add(
            new Metadata(MetaIndex.ENTITY_LIVING_NAME_TAG_VISIBILITY.getIndex(), MetaType1_7_6_10.Byte, (byte)1)
        );
        wrapper.write(Types1_7_6_10.METADATA_LIST, metadataList);
    }

    public void sendSpawnPacket(MetadataRewriter1_7_6_10To1_8 entityRewriter) throws Exception {
        if (this.entityIds != null) {
            this.deleteEntity();
        }

        if (this.currentState == VirtualHologramEntity.State.ZOMBIE) {
            this.spawnEntity(this.entityId, 54, this.locX, this.locY, this.locZ);
            this.entityIds = new int[]{this.entityId};
        } else if (this.currentState == VirtualHologramEntity.State.HOLOGRAM) {
            int[] entityIds = new int[]{this.entityId, this.additionalEntityId()};
            PacketWrapper spawnSkull = PacketWrapper.create(ClientboundPackets1_7_2_5.SPAWN_ENTITY, this.user);
            spawnSkull.write(Type.VAR_INT, entityIds[0]);
            spawnSkull.write(Type.BYTE, (byte)66);
            spawnSkull.write(Type.INT, (int)(this.locX * 32.0));
            spawnSkull.write(Type.INT, (int)(this.locY * 32.0));
            spawnSkull.write(Type.INT, (int)(this.locZ * 32.0));
            spawnSkull.write(Type.BYTE, (byte)0);
            spawnSkull.write(Type.BYTE, (byte)0);
            spawnSkull.write(Type.INT, 0);
            spawnSkull.send(Protocol1_7_6_10To1_8.class);
            this.spawnEntity(entityIds[1], 100, this.locX, this.locY, this.locZ);
            this.entityIds = entityIds;
        }

        this.sendMetadataUpdate(entityRewriter);
        this.updateLocation(true);
    }

    public AABB getBoundingBox() {
        double width = this.small ? 0.25 : 0.5;
        double height = this.small ? 0.9875 : 1.975;
        Vector3d min = new Vector3d(this.locX - width / 2.0, this.locY, this.locZ - width / 2.0);
        Vector3d max = new Vector3d(this.locX + width / 2.0, this.locY + height, this.locZ + width / 2.0);
        return new AABB(min, max);
    }

    private int additionalEntityId() {
        return 2147467647 - this.entityId;
    }

    public void deleteEntity() throws Exception {
        if (this.entityIds != null) {
            PacketWrapper despawn = PacketWrapper.create(ClientboundPackets1_7_2_5.DESTROY_ENTITIES, this.user);
            despawn.write(Type.BYTE, (byte)this.entityIds.length);

            for (int id : this.entityIds) {
                despawn.write(Type.INT, id);
            }

            this.entityIds = null;
            despawn.send(Protocol1_7_6_10To1_8.class);
        }
    }

    public int getEntityId() {
        return this.entityId;
    }

    private enum State {
        HOLOGRAM,
        ZOMBIE;
    }
}
