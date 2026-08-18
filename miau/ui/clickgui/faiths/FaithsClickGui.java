package miau.ui.clickgui.faiths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import miau.Miau;
import miau.module.Module;
import miau.module.modules.render.ClickGUI;
import miau.ui.clickgui.ConfigWindow;
import miau.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class FaithsClickGui extends GuiScreen {
    private List<FaithsWindow> windows = new ArrayList<>();
    private FaithsThemeWindow themeWindow;
    private ConfigWindow configWindow;

    private void refreshWindows() {
        this.windows.clear();
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        float screenWidth = sr.func_78326_a();
        float xPos = 15.0F;
        float yPos = 20.0F;
        LinkedHashMap<String, List<Module>> cats = Miau.moduleManager.getModulesByCategory();

        for (String catName : cats.keySet()) {
            if (xPos + 115.0F > screenWidth) {
                xPos = 15.0F;
                yPos += 210.0F;
            }

            FaithsWindow window = new FaithsWindow(catName, xPos, yPos);
            this.windows.add(window);
            xPos += 125.0F;
        }

        if (xPos + 115.0F > screenWidth) {
            xPos = 15.0F;
            yPos += 210.0F;
        }

        this.themeWindow = new FaithsThemeWindow(xPos, yPos);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        ClickGUI guiModule = (ClickGUI)Miau.moduleManager.modules.get(ClickGUI.class);
        if (guiModule != null) {
            guiModule.checkModeSwitch();
        }

        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        RenderUtil.drawRect(0.0F, 0.0F, sr.func_78326_a(), sr.func_78328_b(), -1778384896);
        FaithsCharacterRenderer.renderCharacter(1.0F);

        for (FaithsWindow window : this.windows) {
            window.renderWindow(mouseX, mouseY);
        }

        if (this.themeWindow != null) {
            this.themeWindow.renderWindow(mouseX, mouseY);
        }

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

        if (this.themeWindow != null) {
            this.themeWindow.mouseReleased(mouseX, mouseY, state);
        }

        for (FaithsWindow window : this.windows) {
            window.mouseReleased(mouseX, mouseY, state);
        }

        super.func_146286_b(mouseX, mouseY, state);
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            ScaledResolution sr = new ScaledResolution(this.field_146297_k);
            int mouseX = Mouse.getEventX() * sr.func_78326_a() / this.field_146297_k.field_71443_c;
            int mouseY = sr.func_78328_b()
                - Mouse.getEventY() * sr.func_78328_b() / this.field_146297_k.field_71440_d
                - 1;
            if (this.configWindow != null && this.configWindow.onScroll(wheelInput, mouseX, mouseY)) {
                return;
            }

            for (FaithsWindow window : this.windows) {
                if (window.onScroll(wheelInput, mouseX, mouseY)) {
                    return;
                }
            }

            if (this.themeWindow != null) {
                this.themeWindow.onScroll(wheelInput, mouseX, mouseY);
            }
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (this.configWindow == null || !this.configWindow.keyTyped(typedChar, keyCode)) {
            super.func_73869_a(typedChar, keyCode);
        }
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        FaithsCharacterRenderer.resetAnimation();
        if (this.windows.isEmpty()) {
            this.refreshWindows();
        }

        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        if (this.configWindow == null) {
            this.configWindow = new ConfigWindow(sr.func_78326_a() - 350, sr.func_78328_b() - 250);
        } else {
            this.configWindow.refreshLocalConfigs();
        }
    }

    public boolean func_73868_f() {
        return false;
    }
}
