package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;

public class BlockAttack extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"RealAttack", "OnlySwing"});
    private final KeyBinding keyBindUseItem = mc.field_71474_y.field_74313_G;

    public BlockAttack() {
        super("BlockAttack", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (mc.field_71439_g.func_70632_aY() && mc.field_71474_y.field_74312_F.func_151470_d()) {
                KeyBinding.func_74510_a(this.keyBindUseItem.func_151463_i(), true);
                mc.field_71439_g.func_71038_i();
                if (this.mode.getModeString().equals("OnlySwing")) {
                    mc.field_71442_b.func_78764_a(mc.field_71439_g, null);
                } else {
                    if (mc.field_71476_x != null && mc.field_71476_x.field_72308_g instanceof EntityLivingBase) {
                        EntityLivingBase target = (EntityLivingBase)mc.field_71476_x.field_72308_g;
                        if (!target.field_70128_L) {
                            mc.field_71442_b.func_78764_a(mc.field_71439_g, target);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        KeyBinding.func_74510_a(this.keyBindUseItem.func_151463_i(), false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
