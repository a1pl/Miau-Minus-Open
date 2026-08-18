package miau.module.modules.render;

import java.awt.Color;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;

public class Utility extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty hurtIndicator = new BooleanProperty("Hurt Indicator", false);
    public final BooleanProperty fpsIndicator = new BooleanProperty("FPS Indicator", false);
    public final BooleanProperty targetHud = new BooleanProperty("TargetHud", false);

    public Utility() {
        super("Utility", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            float cx = sr.func_78326_a() / 2.0F;
            float cy = sr.func_78328_b() / 2.0F + 20.0F;
            if (this.hurtIndicator.getValue() && mc.field_71439_g.field_70737_aN != 0) {
                int color = new Color(255 / mc.field_71439_g.field_70737_aN, 17, 49).getRGB();
                String text = "Hurted: " + mc.field_71439_g.field_70737_aN;
                mc.field_71466_p.func_175065_a(text, cx - 24.0F, cy, color, true);
            }

            if (this.fpsIndicator.getValue()) {
                int fps = Minecraft.func_175610_ah();
                if (fps != 0) {
                    mc.field_71466_p.func_175065_a("FPS: " + fps, 2.0F, 2.0F, -1, true);
                }
            }

            if (this.targetHud.getValue()) {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                EntityLivingBase target = killAura != null ? killAura.getTarget() : null;
                if (target != null) {
                    float health = target.func_110143_aJ();
                    String displayName = mc.field_71439_g.func_145748_c_() != null
                        ? mc.field_71439_g.func_145748_c_().func_150254_d()
                        : "Player";
                    mc.field_71466_p.func_175065_a("Name: " + displayName + ".", cx - 24.0F, cy - 24.0F, 1, true);
                    mc.field_71466_p
                        .func_175065_a(
                            "Distance: " + String.format("%.1f", mc.field_71439_g.func_70032_d(target)) + " Blocks.",
                            cx - 24.0F,
                            cy - 30.0F,
                            1,
                            true
                        );
                }
            }
        }
    }
}
