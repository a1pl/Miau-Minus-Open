package miau.module.modules.render;

import miau.module.Module;
import net.minecraft.client.Minecraft;

public class ViewClip extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public ViewClip() {
        super("ViewClip", false);
    }

    @Override
    public void onEnabled() {
        if (mc.field_71441_e != null) {
            mc.field_71438_f.func_72712_a();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.field_71441_e != null) {
            mc.field_71438_f.func_72712_a();
        }
    }
}
