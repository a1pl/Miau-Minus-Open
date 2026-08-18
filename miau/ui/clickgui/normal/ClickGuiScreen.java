package miau.ui.clickgui.normal;

import java.io.IOException;
import miau.Miau;
import miau.module.modules.render.ClickGUI;
import miau.ui.clickgui.ConfigWindow;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class ClickGuiScreen extends GuiScreen {
    private static ClickGuiScreen instance;
    private ConfigWindow configWindow;

    public static ClickGuiScreen getInstance() {
        if (instance == null) {
            instance = new ClickGuiScreen();
        }

        return instance;
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        if (this.configWindow == null) {
            this.configWindow = new ConfigWindow(sr.func_78326_a() - 350, sr.func_78328_b() - 250);
        } else {
            this.configWindow.refreshLocalConfigs();
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        ClickGUI guiModule = (ClickGUI)Miau.moduleManager.modules.get(ClickGUI.class);
        if (guiModule != null) {
            guiModule.checkModeSwitch();
        }

        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        func_73734_a(0, 0, sr.func_78326_a(), sr.func_78328_b(), -1879048192);
        this.field_146289_q
            .func_175063_a("Normal ClickGUI - TODO", sr.func_78326_a() / 2 - 55, sr.func_78328_b() / 2, -1);
        if (this.configWindow != null) {
            this.configWindow.drawWindow(mouseX, mouseY, 16.0F);
        }

        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.configWindow == null || !this.configWindow.mouseClicked(mouseX, mouseY, mouseButton)) {
            super.func_73864_a(mouseX, mouseY, mouseButton);
        }
    }

    protected void func_146286_b(int mouseX, int mouseY, int state) {
        if (this.configWindow != null) {
            this.configWindow.mouseReleased(mouseX, mouseY, state);
        }

        super.func_146286_b(mouseX, mouseY, state);
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            ScaledResolution sr = new ScaledResolution(this.field_146297_k);
            int mouseX = Mouse.getEventX() * sr.func_78326_a() / this.field_146297_k.field_71443_c;
            int mouseY = sr.func_78328_b()
                - Mouse.getEventY() * sr.func_78328_b() / this.field_146297_k.field_71440_d
                - 1;
            if (this.configWindow != null) {
                this.configWindow.onScroll(wheel, mouseX, mouseY);
            }
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (this.configWindow == null || !this.configWindow.keyTyped(typedChar, keyCode)) {
            super.func_73869_a(typedChar, keyCode);
        }
    }

    public boolean func_73868_f() {
        return false;
    }
}
