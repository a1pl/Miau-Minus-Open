package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.module.modules.network.BackTrack;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.SomeUtil;
import miau.util.player.AimAssistRotationUtil;
import miau.util.player.RotationUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class SmoothAimAssist extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final FloatProperty range = new FloatProperty("Range", 4.4F, 1.0F, 8.0F);
    private final BooleanProperty horizontalAim = new BooleanProperty("HorizontalAim", true);
    private final BooleanProperty verticalAim = new BooleanProperty("VerticalAim", true);
    private final IntProperty horizontalSpeed = new IntProperty("HorizontalSpeed", 180, 1, 180);
    private final IntProperty verticalSpeed = new IntProperty("VerticalSpeed", 180, 1, 180);
    private final FloatProperty entropyMax = new FloatProperty("EntropyDisturbMax", 1.0F, 0.0F, 10.0F);
    private final FloatProperty entropyMin = new FloatProperty("EntropyDisturbMin", 0.5F, 0.0F, 10.0F);
    private final FloatProperty entropyFactor = new FloatProperty("EntropyFactor", 0.5F, 0.0F, 10.0F);
    private final FloatProperty randomize = new FloatProperty("Randomize", 0.5F, 0.0F, 5.0F);
    private final BooleanProperty heuristic = new BooleanProperty("Heuristic", true);
    private final FloatProperty fov = new FloatProperty("FOV", 180.0F, 1.0F, 180.0F);
    private final BooleanProperty onClick = new BooleanProperty(
        "OnClick", false, () -> this.horizontalAim.getValue() || this.verticalAim.getValue()
    );
    private final BooleanProperty breakBlocks = new BooleanProperty("BreakBlocks", true);
    private final TimerUtil clickTimer = new TimerUtil();

    public SmoothAimAssist() {
        super("SmoothAimAssist", false);
    }

    @EventTarget
    public void onMotion(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (mc.field_71474_y.field_74312_F.func_151470_d()) {
                    this.clickTimer.reset();
                }

                boolean clicking = mc.field_71474_y.field_74312_F.func_151470_d()
                    || System.currentTimeMillis() - this.clickTimer.getTime() < 150L;
                if (!this.onClick.getValue() || clicking) {
                    Entity nearest = null;
                    double nearestDist = Double.MAX_VALUE;

                    for (Entity entity : mc.field_71441_e.field_72996_f) {
                        Entity candidate = entity;
                        boolean selected = BackTrack.runWithNearestTrackedDistance(
                            candidate,
                            () -> SomeUtil.isSelected(candidate)
                                && mc.field_71439_g.func_70685_l(candidate)
                                && BackTrackUtil.getDistanceToEntityBox(candidate)
                                    <= this.range.getValue().floatValue()
                                && rotationDifference(candidate) <= this.fov.getValue().floatValue()
                        );
                        if (selected) {
                            double dist = BackTrackUtil.getDistanceToEntityBox(candidate);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearest = candidate;
                            }
                        }
                    }

                    if (nearest != null) {
                        if (!((IAccessorPlayerControllerMP)mc.field_71442_b).getIsHittingBlock()
                            || !this.breakBlocks.getValue()) {
                            float[] rotation = AimAssistRotationUtil.face(
                                (EntityLivingBase)nearest,
                                this.horizontalSpeed.getValue().intValue() + (float)Math.random(),
                                this.verticalSpeed.getValue().intValue() + (float)Math.random(),
                                mc.field_71439_g.field_70177_z,
                                mc.field_71439_g.field_70125_A,
                                this.heuristic.getValue(),
                                true,
                                this.entropyMax.getValue(),
                                this.entropyMin.getValue(),
                                this.entropyFactor.getValue(),
                                this.randomize.getValue()
                            );
                            if (rotation != null) {
                                mc.field_71439_g.field_70177_z = rotation[0];
                                mc.field_71439_g.field_70125_A = rotation[1];
                            }
                        }
                    }
                }
            }
        }
    }

    private static double rotationDifference(Entity entity) {
        AxisAlignedBB box = entity.func_174813_aQ();
        Vec3 center = new Vec3(
            box.field_72340_a + (box.field_72336_d - box.field_72340_a) * 0.5,
            box.field_72338_b + (box.field_72337_e - box.field_72338_b) * 0.5,
            box.field_72339_c + (box.field_72334_f - box.field_72339_c) * 0.5
        );
        float[] rotation = RotationUtil.calculate(center);
        double yawDiff = MathHelper.func_76142_g(rotation[0] - mc.field_71439_g.field_70177_z);
        double pitchDiff = rotation[1] - mc.field_71439_g.field_70125_A;
        return Math.hypot(yawDiff, pitchDiff);
    }
}
