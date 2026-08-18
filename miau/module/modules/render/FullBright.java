package miau.module.modules.render;

import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class FullBright extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private float prevGamma = Float.NaN;
    private boolean appliedNightVision = false;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"GAMMA", "EFFECT"});

    public FullBright() {
        super("Fullbright", true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            switch (this.mode.getValue()) {
                case 0:
                    mc.field_71474_y.field_74333_Y = 1000.0F;
                    break;
                case 1:
                    mc.field_71439_g.func_70690_d(new PotionEffect(Potion.field_76439_r.field_76415_H, 25940, 0));
            }
        }
    }

    @Override
    public void onEnabled() {
        switch (this.mode.getValue()) {
            case 0:
                this.prevGamma = mc.field_71474_y.field_74333_Y;
                break;
            case 1:
                this.appliedNightVision = true;
        }
    }

    @Override
    public void onDisabled() {
        if (!Float.isNaN(this.prevGamma)) {
            mc.field_71474_y.field_74333_Y = this.prevGamma;
            this.prevGamma = Float.NaN;
        }

        if (this.appliedNightVision) {
            if (mc.field_71439_g != null) {
                mc.field_71439_g.func_70618_n(Potion.field_76439_r.field_76415_H);
            }

            this.appliedNightVision = false;
        }
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
            this.onEnabled();
        }
    }
}
