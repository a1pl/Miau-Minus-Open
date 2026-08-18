package miau.ui.clickgui.demise;

import java.awt.Color;
import miau.util.render.RenderUtil;

public class Component implements IComponent {
    private float x;
    private float y;
    private float width;
    private float height;
    private Color color = new Color(5025616);
    private int colorRGB = this.color.getRGB();

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return this.width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getColorRGB() {
        return this.colorRGB;
    }

    public void setColorRGB(int colorRGB) {
        this.colorRGB = colorRGB;
    }

    public void drawBackground(Color color) {
        RenderUtil.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, color.getRGB());
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + height;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isChild() {
        return false;
    }
}
