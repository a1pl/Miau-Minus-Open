package miau.util.misc;

import java.util.UUID;
import java.util.function.Supplier;
import miau.mixin.IAccessorS14PacketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class BackTrackUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static Vec3 interpolatedPosition(Entity entity, float partialTicks) {
        return new Vec3(
            entity.field_70169_q + (entity.field_70165_t - entity.field_70169_q) * partialTicks,
            entity.field_70167_r + (entity.field_70163_u - entity.field_70167_r) * partialTicks,
            entity.field_70166_s + (entity.field_70161_v - entity.field_70166_s) * partialTicks
        );
    }

    public static Vec3 interpolatedPositionFrom(Entity entity, Vec3 start, float partialTicks) {
        return new Vec3(
            start.field_72450_a + (entity.field_70165_t - start.field_72450_a) * partialTicks,
            start.field_72448_b + (entity.field_70163_u - start.field_72448_b) * partialTicks,
            start.field_72449_c + (entity.field_70161_v - start.field_72449_c) * partialTicks
        );
    }

    public static Vec3 getTrueInterpolatedPosition(Entity entity, ITruePosition tp, float partialTicks) {
        return interpolatedPositionFrom(entity, getCurrentTruePosition(tp), partialTicks);
    }

    public static Vec3 getCurrentPosition(Entity entity) {
        return new Vec3(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
    }

    public static Vec3 getPreviousPosition(Entity entity) {
        return new Vec3(entity.field_70169_q, entity.field_70167_r, entity.field_70166_s);
    }

    public static Vec3 getCurrentTruePosition(ITruePosition tp) {
        return tp == null ? new Vec3(0.0, 0.0, 0.0) : new Vec3(tp.getTrueX(), tp.getTrueY(), tp.getTrueZ());
    }

    public static void setPositionAndPrevious(Entity entity, Vec3 current) {
        setPositionAndPrevious(entity, current, current);
    }

    public static void setPositionAndPrevious(Entity entity, Vec3 current, Vec3 previous) {
        double dx = current.field_72450_a - entity.field_70165_t;
        double dy = current.field_72448_b - entity.field_70163_u;
        double dz = current.field_72449_c - entity.field_70161_v;
        entity.func_174826_a(entity.func_174813_aQ().func_72317_d(dx, dy, dz));
        entity.field_70165_t = current.field_72450_a;
        entity.field_70163_u = current.field_72448_b;
        entity.field_70161_v = current.field_72449_c;
        entity.field_70169_q = previous.field_72450_a;
        entity.field_70167_r = previous.field_72448_b;
        entity.field_70166_s = previous.field_72449_c;
    }

    public static <T> T runWithSimulatedPosition(Entity entity, Vec3 position, Supplier<T> action) {
        Vec3 origPos = getCurrentPosition(entity);
        Vec3 origPrevPos = getPreviousPosition(entity);
        AxisAlignedBB origBox = entity.func_174813_aQ();
        double dx = position.field_72450_a - entity.field_70165_t;
        double dy = position.field_72448_b - entity.field_70163_u;
        double dz = position.field_72449_c - entity.field_70161_v;
        entity.func_174826_a(origBox.func_72317_d(dx, dy, dz));
        entity.field_70165_t = position.field_72450_a;
        entity.field_70163_u = position.field_72448_b;
        entity.field_70161_v = position.field_72449_c;
        entity.field_70169_q = position.field_72450_a;
        entity.field_70167_r = position.field_72448_b;
        entity.field_70166_s = position.field_72449_c;
        T result = action.get();
        entity.func_174826_a(origBox);
        entity.field_70165_t = origPos.field_72450_a;
        entity.field_70163_u = origPos.field_72448_b;
        entity.field_70161_v = origPos.field_72449_c;
        entity.field_70169_q = origPrevPos.field_72450_a;
        entity.field_70167_r = origPrevPos.field_72448_b;
        entity.field_70166_s = origPrevPos.field_72449_c;
        return result;
    }

    public static double getDistanceToBox(AxisAlignedBB box) {
        if (mc.field_71439_g != null && box != null) {
            double x = clamp(mc.field_71439_g.field_70165_t, box.field_72340_a, box.field_72336_d);
            double y = clamp(
                mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(), box.field_72338_b, box.field_72337_e
            );
            double z = clamp(mc.field_71439_g.field_70161_v, box.field_72339_c, box.field_72334_f);
            double dx = mc.field_71439_g.field_70165_t - x;
            double dy = mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e() - y;
            double dz = mc.field_71439_g.field_70161_v - z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            return 0.0;
        }
    }

    public static double getDistanceToEntityBox(Entity entity) {
        return entity == null ? 0.0 : getDistanceToBox(entity.func_174813_aQ());
    }

    public static Entity getEntityFromPacket(S14PacketEntity packet) {
        if (mc.field_71441_e == null) {
            return null;
        }

        int entityId = ((IAccessorS14PacketEntity)packet).getEntityId();
        return mc.field_71441_e.func_73045_a(entityId);
    }

    public static Entity getEntityFromTeleport(S18PacketEntityTeleport packet) {
        return mc.field_71441_e == null ? null : mc.field_71441_e.func_73045_a(packet.func_149451_c());
    }

    public static EntityPlayer getPlayerByUUID(UUID uuid) {
        if (mc.field_71441_e != null && uuid != null) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityPlayer && uuid.equals(entity.func_110124_au())) {
                    return (EntityPlayer)entity;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private BackTrackUtil() {
    }
}
