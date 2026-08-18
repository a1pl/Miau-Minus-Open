package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.ui.clickgui.components.Component;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.vector.Vector2d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComponent extends Component {
    private static final String EYE_ICON_PATH = "/assets/keystrokesmod/textures/gui/eye.png";
    private static final String EYE_OFF_ICON_PATH = "/assets/keystrokesmod/textures/gui/eye_off.png";
    private static final int EYE_ICON_PADDING = 2;
    public boolean isBinding;
    public ModuleComponent moduleComponent;
    public float o;
    public float x;
    private float y;
    public float xOffset;

    public BindComponent(ModuleComponent moduleComponent, float o) {
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.o = o;
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return true;
    }

    @Override
    public void render() {
        Font renderer = FontRepository.getClickGuiFont();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        this.drawString(renderer, this.isBinding ? "Press a key..." : "Current bind: '§e" + this.getKeyAsStr() + "§r'");
        GL11.glPopMatrix();
        int iconSize = this.getEyeIconSize();
        float iconX = this.getEyeIconX(iconSize);
        float textHeight = renderer.getFontHeight() * 0.5F;
        float iconY = this.getRenderTextY() + (textHeight - iconSize) / 2.0F;
        int themeColor = !this.moduleComponent.mod.isHidden()
            ? Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y)).getRGB()
            : Color.GRAY.getRGB();
        String iconPath = this.moduleComponent.mod.isHidden()
            ? "/assets/keystrokesmod/textures/gui/eye_off.png"
            : "/assets/keystrokesmod/textures/gui/eye.png";
        RenderUtil.drawIcon(RenderUtil.getIcon(iconPath), iconX, iconY, iconSize, themeColor);
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int button) {
        if (!this.overSetting(x, y) || !this.moduleComponent.isOpened || !this.moduleComponent.isVisible(this)) {
            return false;
        } else if (button == 0 && this.overEyeIcon(x, y)) {
            this.moduleComponent.mod.setHidden(!this.moduleComponent.mod.isHidden());
            return true;
        } else if (button == 0 && this.overBindText(x, y)) {
            this.isBinding = !this.isBinding;
            return true;
        } else if (button > 1 && this.isBinding) {
            this.moduleComponent.mod.setKey(button + 1000);
            this.isBinding = false;
            return true;
        } else {
            return false;
        }
    }

    private boolean overEyeIcon(int x, int y) {
        int iconSize = this.getEyeIconSize();
        float iconX = this.getEyeIconX(iconSize);
        float iconY = this.getEyeIconY(iconSize);
        return x >= iconX && x < iconX + iconSize && y >= iconY && y < iconY + iconSize;
    }

    private float getBindTextX() {
        return this.moduleComponent.categoryComponent.getX() + 4.0F + this.xOffset * 0.5F;
    }

    private float getBindTextY() {
        return this.moduleComponent.categoryComponent.getModuleY() + this.o + 3.0F;
    }

    private float getRenderTextY() {
        return this.moduleComponent.categoryComponent.getY() + this.o + 3.0F;
    }

    private String getBindDisplayString() {
        return this.isBinding ? "Press a key..." : "Current bind: '§e" + this.getKeyAsStr() + "§r'";
    }

    private boolean overBindText(int mouseX, int mouseY) {
        String text = this.getBindDisplayString();
        Font renderer = FontRepository.getClickGuiFont();
        float left = this.getBindTextX();
        float top = this.getBindTextY();
        float width = renderer.getStringWidth(text) * 0.5F;
        float height = renderer.getFontHeight() * 0.5F;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private int getEyeIconSize() {
        int fontH = Math.round(FontRepository.getClickGuiFont().getFontHeight() * 0.5F);
        return Math.max(6, fontH - 1);
    }

    private float getEyeIconX(int iconSize) {
        return this.moduleComponent.categoryComponent.getX()
            + this.moduleComponent.categoryComponent.getWidth()
            - iconSize
            - 2.0F;
    }

    private float getEyeIconY(int iconSize) {
        float textY = this.getBindTextY();
        float textHeight = FontRepository.getClickGuiFont().getFontHeight() * 0.5F;
        return textY + (textHeight - iconSize) / 2.0F;
    }

    @Override
    public void onScroll(int scroll) {
        if (this.isBinding && scroll != 0) {
            this.moduleComponent.mod.setKey(scroll > 0 ? 1069 : 1070);
            this.isBinding = false;
        }
    }

    @Override
    public void keyTyped(char t, int keybind) {
        if (this.isBinding) {
            if (keybind != 11 && keybind != 1) {
                this.moduleComponent.mod.setKey(keybind);
            } else {
                this.moduleComponent.mod.setKey(0);
            }

            this.isBinding = false;
        }
    }

    public boolean overSetting(int mouseX, int mouseY) {
        float rowX = this.moduleComponent.categoryComponent.getX();
        float rowY = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        float rowW = this.moduleComponent.categoryComponent.getWidth();
        return mouseX > rowX && mouseX < rowX + rowW && mouseY > rowY - 1.0F && mouseY < rowY + 12.0F;
    }

    public String getKeyAsStr() {
        int key = this.moduleComponent.mod.getKey();
        return key >= 1000
            ? (key != 1069 && key != 1070 ? "M" + (key - 1000) : this.getScroll(key))
            : Keyboard.getKeyName(key);
    }

    public String getScroll(int key) {
        if (key == 1069) {
            return "MScrollUp";
        } else {
            return key == 1070 ? "MScrollDown" : "&cERROR";
        }
    }

    @Override
    public float getHeightF() {
        return 16.0F;
    }

    @Override
    public int getHeight() {
        return Math.round(this.getHeightF());
    }

    private void drawString(Font renderer, String s) {
        int color = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y)).getRGB();
        renderer.draw(
            s,
            (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset,
            (this.moduleComponent.categoryComponent.getY() + this.o + 3.0F) * 2.0F,
            color,
            true
        );
    }

    @Override
    public void onGuiClosed() {
        this.isBinding = false;
    }
}
