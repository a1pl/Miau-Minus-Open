package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class KeepRange extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"BackWards", "Stop"});
    public final FloatProperty range = new FloatProperty("range", 3.0F, 0.0F, 6.0F);
    public final BooleanProperty disableNearEdge = new BooleanProperty("disable-near-edge", true);
    public final IntProperty edgeRange = new IntProperty("edge-range", 5, 0, 6, () -> !this.disableNearEdge.getValue());
    public final IntProperty combo = new IntProperty("combo-to-start", 2, 0, 6);
    private boolean edge;
    private int row;

    public KeepRange() {
        super("KeepRange", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.isEnabled()) {
                if (mc.field_71439_g.field_70122_E) {
                    this.edge = false;
                    int range = this.edgeRange.getValue();

                    for (int x = -range; x <= range; x++) {
                        for (int z = -range; z <= range; z++) {
                            for (int y = -5; y <= 0; y++) {
                                Block block = mc.field_71441_e
                                    .func_180495_p(
                                        new BlockPos(
                                            mc.field_71439_g.field_70165_t + x,
                                            mc.field_71439_g.field_70163_u + y,
                                            mc.field_71439_g.field_70161_v + z
                                        )
                                    )
                                    .func_177230_c();
                                boolean air = block instanceof BlockAir;
                                if (!air) {
                                    break;
                                }

                                if (y == 0) {
                                    this.edge = true;
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            EntityLivingBase target = killAura != null ? killAura.getTarget() : null;
            if (target != null && (!this.edge || !this.disableNearEdge.getValue())) {
                if (target.field_70737_aN > 0) {
                    this.row++;
                }

                if (mc.field_71439_g.field_70737_aN > 0) {
                    this.row = 0;
                }

                if (this.row > this.combo.getValue() * 8 || this.combo.getValue() <= 0) {
                    if (PlayerUtil.calculatePerfectRangeToEntity(target) < this.range.getValue().floatValue() - 0.05) {
                        float forward = mc.field_71439_g.field_71158_b.field_78900_b;
                        float strafe = mc.field_71439_g.field_71158_b.field_78902_a;
                        float[] targetRotations = RotationUtil.calculate(target);
                        double angle = MathHelper.func_76138_g(targetRotations[0] - 180.0F);
                        if (forward == 0.0F && strafe == 0.0F) {
                            return;
                        }

                        float closestForward = 0.0F;
                        float closestStrafe = 0.0F;
                        float closestDifference = Float.MAX_VALUE;

                        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
                            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                                if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                                    double predictedAngle = MathHelper.func_76138_g(
                                        Math.toDegrees(
                                            MoveUtil.direction(
                                                mc.field_71439_g.field_70177_z, predictedForward, predictedStrafe
                                            )
                                        )
                                    );
                                    double difference = MoveUtil.wrappedDifference(angle, predictedAngle);
                                    if (difference < closestDifference) {
                                        closestDifference = (float)difference;
                                        closestForward = predictedForward;
                                        closestStrafe = predictedStrafe;
                                    }
                                }
                            }
                        }

                        switch (this.mode.getModeString()) {
                            case "Stop":
                                if (closestForward == forward * -1.0F) {
                                    mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                                }

                                if (closestStrafe == strafe * -1.0F) {
                                    mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
                                }
                                break;
                            case "BackWards":
                                mc.field_71439_g.field_71158_b.field_78900_b = closestForward;
                                mc.field_71439_g.field_71158_b.field_78902_a = closestStrafe;
                        }
                    }
                }
            } else {
                this.row = 0;
            }
        }
    }
}
