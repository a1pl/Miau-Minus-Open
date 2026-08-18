package miau.ui.clickgui.faiths;

import java.awt.Color;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.math.MathUtil;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class FaithsThemeWindow {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private float x;
    private float y;
    private int prevMouseX;
    private int prevMouseY;
    private boolean leftMouseClicked = false;
    private boolean rightMouseClicked = false;
    private boolean expand = true;
    private boolean dragging = false;
    private float scrollY = 0.0F;
    private float targetScrollY = 0.0F;
    private float lastRenderHeight = 200.0F;
    private static final int PANEL_WIDTH = 100;
    private static final int TITLE_HEIGHT = 13;
    private static final int ITEM_HEIGHT = 11;
    private static final Color BG_COLOR = new Color(25, 25, 25);
    private static final Color ITEM_BG = new Color(36, 36, 36);

    public FaithsThemeWindow(float x, float y) {
        this.x = x;
        this.y = y;
    }

    private boolean mouseHovered(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean onScroll(int dWheel, int mouseX, int mouseY) {
        if (this.mouseHovered(this.x, this.y, 100.0F, this.lastRenderHeight, mouseX, mouseY)) {
            if (dWheel > 0) {
                this.targetScrollY += 25.0F;
            } else if (dWheel < 0) {
                this.targetScrollY -= 25.0F;
            }

            return true;
        } else {
            return false;
        }
    }

    private void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(mc);
        double scale = sr.func_78325_e();
        y = sr.func_78328_b() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        GL11.glScissor((int)x, (int)(y - height), (int)width, (int)height);
    }

    protected void renderWindow(int mouseX, int mouseY) {
        if (Mouse.isButtonDown(0)) {
            if (this.dragging) {
                this.x = this.x + (mouseX - this.prevMouseX);
                this.y = this.y + (mouseY - this.prevMouseY);
            } else if (this.mouseHovered(this.x, this.y, 100.0F, 13.0F, mouseX, mouseY)) {
                this.dragging = true;
            }
        } else {
            this.dragging = false;
        }

        this.prevMouseX = mouseX;
        this.prevMouseY = mouseY;
        GL11.glPushMatrix();
        GL11.glTranslatef(this.x, this.y, 0.0F);
        Themes[] themesList = Themes.values();
        float height = 15.0F;
        if (this.expand) {
            height += themesList.length * 11;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float maxWindowHeight = Math.min(220.0F, Math.max(100.0F, sr.func_78328_b() - this.y - 10.0F));
        float renderHeight = Math.min(height, maxWindowHeight);
        this.lastRenderHeight = renderHeight;
        float maxScroll = Math.min(0.0F, -(height - renderHeight));
        if (this.targetScrollY > 0.0F) {
            this.targetScrollY = 0.0F;
        }

        if (this.targetScrollY < maxScroll) {
            this.targetScrollY = maxScroll;
        }

        this.scrollY = MathUtil.lerp(this.scrollY, this.targetScrollY, 0.2F);
        if (this.scrollY > 0.0F) {
            this.scrollY = 0.0F;
        }

        if (this.scrollY < maxScroll) {
            this.scrollY = maxScroll;
        }

        Color themeAccent = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y));
        RenderUtil.drawOutLineRect(0.0F, 0.0F, 100.0F, renderHeight, 1.0F, BG_COLOR, themeAccent);
        Font titleFont = FontRepository.getHudFont(15);
        titleFont.draw("themes", 5.0, 3.0, -1);
        GL11.glEnable(3089);
        this.scissor(this.x, this.y + 13.0F, 100.0, renderHeight - 13.0F);
        float itemY = 11.0F + this.scrollY;
        if (this.expand) {
            for (Themes theme : themesList) {
                boolean selected = Themes.getCurrentTheme() == theme;
                Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + itemY));
                Color c2 = Themes.getCurrentTheme()
                    .getAccentColor(new Vector2d(this.x + 100.0F, this.y + itemY + 11.0F));
                RenderUtil.drawRect(3.0F, itemY, 98.0F, itemY + 11.0F, ITEM_BG.getRGB());
                if (selected) {
                    RenderUtil.drawHorizontalGradientRect(3.0F, itemY, 98.0F, itemY + 11.0F, c1.getRGB(), c2.getRGB());
                }

                if (this.mouseHovered(this.x, this.y + itemY, 100.0F, 11.0F, mouseX, mouseY)) {
                    if (!selected) {
                        RenderUtil.drawRect(3.0F, itemY, 98.0F, itemY + 11.0F, new Color(255, 255, 255, 50).getRGB());
                    }

                    if (Mouse.isButtonDown(0)) {
                        if (!this.leftMouseClicked) {
                            Themes.setCurrentTheme(theme);
                            this.leftMouseClicked = true;
                        }
                    } else {
                        this.leftMouseClicked = false;
                    }
                }

                Font font = FontRepository.getHudFont(13);
                int textColor = selected ? RenderUtil.getContrastTextColor(c1) : new Color(160, 160, 160).getRGB();
                font.draw(
                    theme.getThemeName().toLowerCase(),
                    97 - font.width(theme.getThemeName().toLowerCase()),
                    itemY + 2.0F,
                    textColor
                );
                itemY += 11.0F;
            }
        }

        GL11.glDisable(3089);
        if (this.mouseHovered(this.x, this.y, 100.0F, 13.0F, mouseX, mouseY)) {
            if (Mouse.isButtonDown(1)) {
                if (!this.rightMouseClicked) {
                    this.rightMouseClicked = true;
                    this.expand = !this.expand;
                }
            } else {
                this.rightMouseClicked = false;
            }
        }

        GL11.glPopMatrix();
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = false;
    }
}
