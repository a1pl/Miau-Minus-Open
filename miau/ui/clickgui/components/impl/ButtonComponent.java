package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.ui.clickgui.components.Component;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import org.lwjgl.opengl.GL11;

public class ButtonComponent extends Component {
    private static final int ENABLED_COLOR = new Color(20, 255, 0).getRGB();
    private Module mod;
    public BooleanProperty property;
    private ModuleComponent moduleComponent;
    public float o;
    public float x;
    private float y;
    public float xOffset;
    private float toggleAnim = -1.0F;

    public ButtonComponent(Module mod, BooleanProperty op, ModuleComponent b, float o) {
        this.mod = mod;
        this.property = op;
        this.moduleComponent = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.yPos;
        this.o = o;
    }

    @Override
    public void render() {
        Font renderer = FontRepository.getClickGuiFont();
        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.moduleComponent.categoryComponent.getY() + this.o;
        float cw = this.moduleComponent.categoryComponent.getWidth();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        renderer.draw(this.property.getName(), (cx + 6.0F + this.xOffset / 2.0F) * 2.0F, (cy + 4.0F) * 2.0F, -1, false);
        GL11.glPopMatrix();
        boolean enabled = this.property.getValue();
        if (this.toggleAnim == -1.0F) {
            this.toggleAnim = enabled ? 1.0F : 0.0F;
        } else {
            this.toggleAnim = this.toggleAnim + ((enabled ? 1.0F : 0.0F) - this.toggleAnim) * 0.2F;
        }

        float switchW = 16.0F;
        float switchH = 8.0F;
        float switchX = cx + cw - switchW - 6.0F + this.xOffset / 2.0F;
        float switchY = cy + 2.0F;
        Color c1 = new Color(40, 40, 40);
        Color c2 = new Color(ENABLED_COLOR);
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * this.toggleAnim);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * this.toggleAnim);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * this.toggleAnim);
        int bgColor = new Color(r, g, b).getRGB();
        RenderUtil.drawRoundedRectangle(switchX, switchY, switchX + switchW, switchY + switchH, switchH / 2.0F, bgColor);
        float circleR = switchH / 2.0F - 1.0F;
        float minCircleX = switchX + circleR + 1.0F;
        float maxCircleX = switchX + switchW - circleR - 1.0F;
        float circleX = minCircleX + (maxCircleX - minCircleX) * this.toggleAnim;
        float circleY = switchY + switchH / 2.0F;
        RenderUtil.drawRoundedRectangle(
            circleX - circleR, circleY - circleR, circleX + circleR, circleY + circleR, circleR, -1
        );
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
        return this.property.isVisible();
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int b) {
        if (this.i(x, y) && b == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            this.property.setValue(!this.property.getValue());
            this.moduleComponent.reloadSettings();
            return true;
        } else {
            return false;
        }
    }

    public boolean i(int x, int y) {
        return x > this.x
            && x < this.x + this.moduleComponent.categoryComponent.getWidth()
            && y > this.y
            && y < this.y + 11.0F;
    }
}
