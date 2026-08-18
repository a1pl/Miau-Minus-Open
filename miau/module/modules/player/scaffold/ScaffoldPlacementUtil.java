package miau.module.modules.player.scaffold;

import miau.module.modules.player.Scaffold;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public final class ScaffoldPlacementUtil {
    private static final Minecraft MC = Minecraft.func_71410_x();
    private static final double INSET = 0.05;
    private static final double FACE_STEP = 0.2;

    private ScaffoldPlacementUtil() {
    }

    public static double blockReach() {
        return MC.field_71442_b.func_78757_d();
    }

    public static MovingObjectPosition raycastPlacement(float yaw, float pitch) {
        return RotationUtil.rayCastBlock(blockReach(), yaw, pitch);
    }

    public static boolean matchesPlacement(MovingObjectPosition mop, Scaffold.BlockData data) {
        return mop != null
            && mop.field_72313_a == MovingObjectType.BLOCK
            && mop.func_178782_a().equals(data.blockPos)
            && mop.field_178784_b == data.facing;
    }

    public static MovingObjectPosition verifyPlacement(Scaffold.BlockData data, float yaw, float pitch) {
        MovingObjectPosition mop = raycastPlacement(yaw, pitch);
        return matchesPlacement(mop, data) ? mop : null;
    }

    public static ScaffoldPlacementUtil.PlacementAim resolveAim(
        Scaffold.BlockData data, float baseYaw, float basePitch, double[] faceSamples
    ) {
        if (MC.field_71439_g != null && data != null) {
            float bestYaw = Float.NaN;
            float bestPitch = Float.NaN;
            float bestCost = Float.MAX_VALUE;
            Vec3 bestHit = null;
            double[] x = faceSamples;
            double[] y = faceSamples;
            double[] z = faceSamples;
            switch (data.facing) {
                case NORTH:
                    z = new double[]{0.0};
                    break;
                case EAST:
                    x = new double[]{1.0};
                    break;
                case SOUTH:
                    z = new double[]{1.0};
                    break;
                case WEST:
                    x = new double[]{0.0};
                    break;
                case DOWN:
                    y = new double[]{0.0};
                    break;
                case UP:
                    y = new double[]{1.0};
            }

            double reach = blockReach();

            for (double dx : x) {
                for (double dy : y) {
                    for (double dz : z) {
                        double relX = data.blockPos.func_177958_n() + dx - MC.field_71439_g.field_70165_t;
                        double relY = data.blockPos.func_177956_o()
                            + dy
                            - MC.field_71439_g.field_70163_u
                            - MC.field_71439_g.func_70047_e();
                        double relZ = data.blockPos.func_177952_p() + dz - MC.field_71439_g.field_70161_v;
                        float[] rots = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, basePitch);
                        MovingObjectPosition mop = RotationUtil.rayCastBlock(reach, rots[0], rots[1]);
                        if (matchesPlacement(mop, data)) {
                            float cost = Math.abs(MathHelper.func_76142_g(rots[0] - baseYaw))
                                + Math.abs(rots[1] - basePitch);
                            if (cost < bestCost) {
                                bestCost = cost;
                                bestYaw = rots[0];
                                bestPitch = rots[1];
                                bestHit = mop.field_72307_f;
                            }
                        }
                    }
                }
            }

            if (Float.isNaN(bestYaw)) {
                return resolveAimInset(data, baseYaw, basePitch, reach);
            }

            float[] gcd = RotationUtil.flexRotation(bestYaw, bestPitch, baseYaw, basePitch);
            MovingObjectPosition verify = RotationUtil.rayCastBlock(reach, gcd[0], gcd[1]);
            return matchesPlacement(verify, data)
                ? new ScaffoldPlacementUtil.PlacementAim(gcd[0], gcd[1], verify.field_72307_f)
                : new ScaffoldPlacementUtil.PlacementAim(bestYaw, bestPitch, bestHit);
        } else {
            return null;
        }
    }

    private static ScaffoldPlacementUtil.PlacementAim resolveAimInset(
        Scaffold.BlockData data, float baseYaw, float basePitch, double reach
    ) {
        EnumFacing hitFace = data.facing;
        int n = (int)Math.round(5.0);
        float bestYaw = Float.NaN;
        float bestPitch = Float.NaN;
        float bestCost = Float.MAX_VALUE;
        Vec3 bestHit = null;

        for (int r = 0; r <= n; r++) {
            double v = Math.min(1.0, Math.max(0.0, r * 0.2));

            for (int c = 0; c <= n; c++) {
                double u = Math.min(1.0, Math.max(0.0, c * 0.2));
                Vec3 point = facePoint(data.blockPos, hitFace, u, v, 0.05);
                double relX = point.field_72450_a - MC.field_71439_g.field_70165_t;
                double relY = point.field_72448_b - MC.field_71439_g.field_70163_u - MC.field_71439_g.func_70047_e();
                double relZ = point.field_72449_c - MC.field_71439_g.field_70161_v;
                float[] rots = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, basePitch);
                MovingObjectPosition mop = RotationUtil.rayCastBlock(reach, rots[0], rots[1]);
                if (matchesPlacement(mop, data)) {
                    float cost = Math.abs(MathHelper.func_76142_g(rots[0] - baseYaw)) + Math.abs(rots[1] - basePitch);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestYaw = rots[0];
                        bestPitch = rots[1];
                        bestHit = mop.field_72307_f;
                    }
                }
            }
        }

        if (Float.isNaN(bestYaw)) {
            return null;
        }

        float[] gcd = RotationUtil.flexRotation(bestYaw, bestPitch, baseYaw, basePitch);
        MovingObjectPosition verify = RotationUtil.rayCastBlock(reach, gcd[0], gcd[1]);
        return matchesPlacement(verify, data)
            ? new ScaffoldPlacementUtil.PlacementAim(gcd[0], gcd[1], verify.field_72307_f)
            : new ScaffoldPlacementUtil.PlacementAim(bestYaw, bestPitch, bestHit);
    }

    private static Vec3 facePoint(BlockPos pos, EnumFacing face, double u, double v, double inset) {
        switch (face) {
            case NORTH:
                return new Vec3(pos.func_177958_n() + u, pos.func_177956_o() + v, pos.func_177952_p() + inset);
            case EAST:
                return new Vec3(pos.func_177958_n() + 1.0 - inset, pos.func_177956_o() + v, pos.func_177952_p() + u);
            case SOUTH:
                return new Vec3(pos.func_177958_n() + u, pos.func_177956_o() + v, pos.func_177952_p() + 1.0 - inset);
            case WEST:
                return new Vec3(pos.func_177958_n() + inset, pos.func_177956_o() + v, pos.func_177952_p() + u);
            case DOWN:
                return new Vec3(pos.func_177958_n() + u, pos.func_177956_o() + inset, pos.func_177952_p() + v);
            case UP:
                return new Vec3(pos.func_177958_n() + u, pos.func_177956_o() + 1.0 - inset, pos.func_177952_p() + v);
            default:
                return new Vec3(pos.func_177958_n() + 0.5, pos.func_177956_o() + 0.5, pos.func_177952_p() + 0.5);
        }
    }

    public static final class PlacementAim {
        public final float yaw;
        public final float pitch;
        public final Vec3 hitVec;

        public PlacementAim(float yaw, float pitch, Vec3 hitVec) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.hitVec = hitVec;
        }
    }
}
