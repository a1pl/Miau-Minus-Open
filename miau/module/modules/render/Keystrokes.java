package miau.module.modules.render;

import java.util.ArrayDeque;
import java.util.Deque;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Keystrokes extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    public final IntProperty x = new IntProperty("x", 6, 0, 1000);
    public final IntProperty y = new IntProperty("y", 18, 0, 1000);
    public final IntProperty scale = new IntProperty("scale", 100, 50, 200);
    public final IntProperty opacity = new IntProperty("opacity", 102, 20, 255);
    public final BooleanProperty centerY = new BooleanProperty("center-y", true);
    public final BooleanProperty showSpace = new BooleanProperty("space-bar", true);
    public final BooleanProperty showMouse = new BooleanProperty("mouse-buttons", true);
    public final BooleanProperty showCPS = new BooleanProperty("cps", true, () -> this.showMouse.getValue());
    public final BooleanProperty showMouseMovement = new BooleanProperty("mouse-movement", true);
    private float mouseDotX = 0.0F;
    private float mouseDotY = 0.0F;

    public Keystrokes() {
        super("Keystrokes", false);
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        recordLeftClick();
    }

    public static void recordLeftClick() {
        Keystrokes.MiauKeystrokesHolder.INSTANCE.leftClicks.addLast(System.currentTimeMillis());
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        this.rightClicks.addLast(System.currentTimeMillis());
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled()) {
            long now = System.currentTimeMillis();
            this.prune(this.leftClicks, now);
            this.prune(this.rightClicks, now);
            ScaledResolution sr = new ScaledResolution(mc);
            float scaleValue = this.scale.getValue().intValue() / 100.0F;
            int baseX = this.x.getValue();
            int baseY = this.centerY.getValue() ? sr.func_78328_b() / 2 - this.y.getValue() : this.y.getValue();
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(scaleValue, scaleValue, 1.0F);
            baseX = (int)(baseX / scaleValue);
            baseY = (int)(baseY / scaleValue);
            this.drawKey(
                "W", baseX + 24, baseY, 22, 22, Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())
            );
            this.drawKey(
                "A", baseX, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i())
            );
            this.drawKey(
                "S", baseX + 24, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i())
            );
            this.drawKey(
                "D", baseX + 48, baseY + 24, 22, 22, Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i())
            );
            int currentY = baseY + 48;
            if (this.showSpace.getValue()) {
                this.drawSpaceKey(
                    baseX, currentY, 70, 14, Keyboard.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i())
                );
                currentY += 16;
            }

            if (this.showMouse.getValue()) {
                this.drawMouse("LMB", this.leftClicks.size(), baseX, currentY, 34, 22, Mouse.isButtonDown(0));
                this.drawMouse("RMB", this.rightClicks.size(), baseX + 36, currentY, 34, 22, Mouse.isButtonDown(1));
            }

            if (this.showMouseMovement.getValue()) {
                this.drawMouseMovementBox(baseX + 74, baseY, 46, 70);
            }

            GlStateManager.func_179121_F();
        }
    }

    private void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.removeFirst();
        }
    }

    private int background(boolean down) {
        int alpha = down ? 204 : this.opacity.getValue();
        return alpha << 24 | (down ? 16777215 : 0);
    }

    private void drawKey(String label, int x, int y, int w, int h, boolean down) {
        int fg = down ? -15658735 : -1;
        Gui.func_73734_a(x, y, x + w, y + h, this.background(down));
        mc.field_71466_p.func_175063_a(label, x + w / 2 - mc.field_71466_p.func_78256_a(label) / 2, y + 7, fg);
    }

    private void drawMouse(String label, int cps, int x, int y, int w, int h, boolean down) {
        int fg = down ? -15658735 : -1;
        Gui.func_73734_a(x, y, x + w, y + h, this.background(down));
        int labelY = this.showCPS.getValue() ? y + 3 : y + 7;
        mc.field_71466_p.func_175063_a(label, x + w / 2 - mc.field_71466_p.func_78256_a(label) / 2, labelY, fg);
        if (this.showCPS.getValue()) {
            String cpsText = cps + " CPS";
            mc.field_71466_p.func_175063_a(cpsText, x + w / 2 - mc.field_71466_p.func_78256_a(cpsText) / 2, y + 12, fg);
        }
    }

    private void drawSpaceKey(int x, int y, int w, int h, boolean down) {
        int fg = down ? -15658735 : -1;
        Gui.func_73734_a(x, y, x + w, y + h, this.background(down));
        int lineW = 28;
        int lineX = x + (w - lineW) / 2;
        int lineY = y + h / 2;
        Gui.func_73734_a(lineX, lineY, lineX + lineW, lineY + 2, fg);
    }

    private void drawMouseMovementBox(int x, int y, int w, int h) {
        Gui.func_73734_a(x, y, x + w, y + h, this.background(false));
        float dx = Mouse.getDX() * 0.25F;
        float dy = -Mouse.getDY() * 0.25F;
        this.mouseDotX = (this.mouseDotX + dx) * 0.75F;
        this.mouseDotY = (this.mouseDotY + dy) * 0.75F;
        int maxOffset = w / 2 - 4;
        float dotX = Math.max(-maxOffset, Math.min(maxOffset, this.mouseDotX));
        float dotY = Math.max(-maxOffset, Math.min(maxOffset, this.mouseDotY));
        int centerX = x + w / 2;
        int centerY = y + h / 2;
        int finalDotX = (int)(centerX + dotX);
        int finalDotY = (int)(centerY + dotY);
        Gui.func_73734_a(finalDotX - 2, finalDotY - 2, finalDotX + 3, finalDotY + 3, -16711800);
    }

    private static class MiauKeystrokesHolder {
        private static final Keystrokes INSTANCE = (Keystrokes)Miau.moduleManager.modules.get(Keystrokes.class);
    }
}
