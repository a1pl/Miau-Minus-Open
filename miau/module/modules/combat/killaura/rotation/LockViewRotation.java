package miau.module.modules.combat.killaura.rotation;

import miau.component.RotationComponent;
import miau.event.impl.UpdateEvent;
import miau.module.modules.combat.KillAura;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;

public class LockViewRotation extends RotationMode {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public LockViewRotation(KillAura killAura) {
        super(killAura, "LOCK_VIEW");
    }

    @Override
    public float[] processRotations(float[] targetRots, float[] lastRots, double rotSpeed, UpdateEvent event) {
        if (rotSpeed != 0.0) {
            RotationComponent.setActive(true, this.killAura.moveFix.getValue());
            float[] result = RotationUtil.smooth(
                lastRots,
                targetRots,
                rotSpeed,
                this.killAura.getTarget(),
                this.killAura.attackRange.getValue().floatValue()
            );
            mc.field_71439_g.field_70177_z = result[0];
            mc.field_71439_g.field_70125_A = result[1];
            mc.field_71439_g.field_70759_as = result[0];
            mc.field_71439_g.field_70761_aq = result[0];
            RotationComponent.markSmoothed(result);
            return result;
        } else {
            return lastRots;
        }
    }
}
