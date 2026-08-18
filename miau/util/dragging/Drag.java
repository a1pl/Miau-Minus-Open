package miau.util.dragging;

import miau.util.time.TimerUtil;
import org.lwjgl.input.Mouse;

public class Drag {
    public float positionX;
    public float positionY;
    public float targetPositionX;
    public float targetPositionY;
    public float scaleX;
    public float scaleY;
    public float offsetX;
    public float offsetY;
    public boolean dragging = false;
    public TimerUtil stopWatch = new TimerUtil();
    private boolean wasButtonDown = false;

    public Drag(float initialX, float initialY, float scaleX, float scaleY) {
        this.positionX = this.targetPositionX = initialX;
        this.positionY = this.targetPositionY = initialY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void onClick(int mouseButton) {
        float[] mouse = MouseUtil.getMouse();
        float mouseX = mouse[0];
        float mouseY = mouse[1];
        if (this.mouseOver(mouseX, mouseY) && mouseButton == 0) {
            this.dragging = true;
            this.offsetX = this.targetPositionX - mouseX;
            this.offsetY = this.targetPositionY - mouseY;
        }
    }

    public void interpolate() {
        if (Math.abs(this.positionX - this.targetPositionX) > 0.01F
            || Math.abs(this.positionY - this.targetPositionY) > 0.01F) {
            long elapsed = this.stopWatch.getElapsedTime();

            for (int i = 0; i <= elapsed; i++) {
                this.positionX = (this.positionX * 38.0F + this.targetPositionX) / 39.0F;
                this.positionY = (this.positionY * 38.0F + this.targetPositionY) / 39.0F;
            }
        }

        this.stopWatch.reset();
    }

    public void render() {
        float[] mouse = MouseUtil.getMouse();
        float mouseX = mouse[0];
        float mouseY = mouse[1];
        boolean buttonDown = Mouse.isButtonDown(0);
        if (buttonDown && !this.wasButtonDown) {
            this.onClick(0);
        } else if (!buttonDown && this.wasButtonDown) {
            this.release();
        }

        this.wasButtonDown = buttonDown;
        if (this.dragging) {
            this.targetPositionX = mouseX + this.offsetX;
            this.targetPositionY = mouseY + this.offsetY;
        }

        this.interpolate();
    }

    public void release() {
        this.dragging = false;
    }

    public boolean mouseOver(float mouseX, float mouseY) {
        return mouseX >= this.positionX
            && mouseX <= this.positionX + this.scaleX
            && mouseY >= this.positionY
            && mouseY <= this.positionY + this.scaleY;
    }
}
