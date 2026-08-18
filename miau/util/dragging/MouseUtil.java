package miau.util.dragging;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class MouseUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static float[] getMouse() {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * scaledResolution.func_78326_a() / mc.field_71443_c;
        int mouseY = scaledResolution.func_78328_b()
            - Mouse.getY() * scaledResolution.func_78328_b() / mc.field_71440_d
            - 1;
        return new float[]{mouseX, mouseY};
    }
}
