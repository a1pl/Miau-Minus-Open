package miau.module.modules.player.scaffold;

import miau.util.math.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;

public class ScaffoldUtils {
    public static EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    public static double distanceToEdge(Minecraft mc, EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return mc.field_71439_g.field_70161_v - Math.floor(mc.field_71439_g.field_70161_v);
            case EAST:
                return Math.ceil(mc.field_71439_g.field_70165_t) - mc.field_71439_g.field_70165_t;
            case SOUTH:
                return Math.ceil(mc.field_71439_g.field_70161_v) - mc.field_71439_g.field_70161_v;
            case WEST:
            default:
                return mc.field_71439_g.field_70165_t - Math.floor(mc.field_71439_g.field_70165_t);
        }
    }

    public static double getRandomOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    public static boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }
}
