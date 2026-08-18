package miau.module.modules.ghost.bridgeassist.mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.ghost.BridgeAssist;
import miau.property.Property;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class SilentMode {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BridgeAssist parent;
    private static final EnumFacing[] SIDES = new EnumFacing[]{
        EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST
    };

    public SilentMode(BridgeAssist parent) {
        this.parent = parent;
    }

    public List<Property<?>> getProperties() {
        return Arrays.asList();
    }

    public void onDisabled() {
    }

    public void onMoveInput(MoveInputEvent event) {
    }

    public void onUpdate(UpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (mc.field_71462_r == null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
                    ItemStack held = mc.field_71439_g.func_70694_bm();
                    if (held != null && held.func_77973_b() instanceof ItemBlock) {
                        if (!(mc.field_71439_g.field_70125_A < 70.0F)) {
                            if (!(mc.field_71439_g.field_71158_b.field_78900_b > 0.0F)) {
                                float basePitch = RotationUtil.serverPitch;
                                double reach = mc.field_71442_b.func_78757_d();
                                SilentMode.TargetResult target = this.findTarget(basePitch, reach);
                                if (target != null) {
                                    float baseYaw = RotationUtil.serverYaw;
                                    float[] sm = RotationUtil.smoothRotation(
                                        baseYaw, basePitch, target.yaw, target.pitch, 15, 20.0F
                                    );
                                    e.setRotation(sm[0], sm[1], 2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private SilentMode.TargetResult findTarget(float currentPitch, double reach) {
        float yaw = mc.field_71439_g.field_70177_z;
        AxisAlignedBB bbox = mc.field_71439_g.func_174813_aQ();
        int standY = MathHelper.func_76128_c(bbox.field_72338_b) - 1;
        int minX = MathHelper.func_76128_c(bbox.field_72340_a);
        int maxX = MathHelper.func_76128_c(bbox.field_72336_d);
        int minZ = MathHelper.func_76128_c(bbox.field_72339_c);
        int maxZ = MathHelper.func_76128_c(bbox.field_72334_f);
        ArrayList<SilentMode.FaceTarget> targets = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos standBlock = new BlockPos(x, standY, z);
                if (!this.replaceable(standBlock)) {
                    for (EnumFacing face : SIDES) {
                        BlockPos placed = standBlock.func_177972_a(face);
                        if (this.replaceable(placed)) {
                            targets.add(new SilentMode.FaceTarget(standBlock, face));
                        }
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            return null;
        }

        float bestDelta = Float.MAX_VALUE;
        float bestPitch = Float.NaN;
        BlockPos bestSupport = null;
        EnumFacing bestFace = null;
        float randScale = 0.2F;
        float pitch = 60.0F;

        while (pitch <= 90.0F) {
            float step = 1.0F + (float)(Math.random() * 2.0 - 1.0) * (0.3F + randScale * 0.4F);
            if (step < 0.4F) {
                step = 0.4F;
            }

            if (step > 1.8F) {
                step = 1.8F;
            }

            pitch += step;
            float samplePitch = Math.min(pitch, 90.0F);
            MovingObjectPosition mop = RotationUtil.rayCastBlock(reach, yaw, samplePitch);
            if (mop != null) {
                EnumFacing hitFace = mop.field_178784_b;
                if (hitFace != EnumFacing.UP && hitFace != EnumFacing.DOWN) {
                    BlockPos hitBlock = mop.func_178782_a();
                    Iterator var23 = targets.iterator();

                    while (true) {
                        if (var23.hasNext()) {
                            SilentMode.FaceTarget t = (SilentMode.FaceTarget)var23.next();
                            if (!hitBlock.equals(t.block) || hitFace != t.face) {
                                continue;
                            }

                            float delta = Math.abs(samplePitch - currentPitch);
                            if (delta < bestDelta) {
                                bestDelta = delta;
                                bestPitch = samplePitch;
                                bestSupport = t.block;
                                bestFace = t.face;
                            }
                        }

                        if (pitch >= 90.0F) {
                            return bestSupport != null && bestFace != null && !Float.isNaN(bestPitch)
                                ? new SilentMode.TargetResult(yaw, bestPitch, bestSupport, bestFace)
                                : null;
                        }
                        break;
                    }
                }
            }
        }

        return bestSupport != null && bestFace != null && !Float.isNaN(bestPitch)
            ? new SilentMode.TargetResult(yaw, bestPitch, bestSupport, bestFace)
            : null;
    }

    private boolean replaceable(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos).func_177230_c().func_176200_f(mc.field_71441_e, pos);
    }

    private static class FaceTarget {
        final BlockPos block;
        final EnumFacing face;

        FaceTarget(BlockPos block, EnumFacing face) {
            this.block = block;
            this.face = face;
        }
    }

    private static class TargetResult {
        final float yaw;
        final float pitch;
        final BlockPos support;
        final EnumFacing face;

        TargetResult(float yaw, float pitch, BlockPos support, EnumFacing face) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.support = support;
            this.face = face;
        }
    }
}
