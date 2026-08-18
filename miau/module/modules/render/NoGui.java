package miau.module.modules.render;

import miau.module.Module;
import miau.ui.nogui.NoguiGui;
import net.minecraft.client.Minecraft;

public class NoGui extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public NoGui() {
        super("NoGui", false);
    }

    @Override
    public void onEnabled() {
        this.setEnabled(false);
        mc.func_147108_a(new NoguiGui());
    }
}
