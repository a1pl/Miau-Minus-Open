package miau.ui.clickgui.faiths;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.module.Module;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
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

public class FaithsWindow {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final DecimalFormat FLOAT_POINT_FORMAT = new DecimalFormat("0.00");
    private final String category;
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
    private String draggingPropertyName = null;
    private static final int PANEL_WIDTH = 100;
    private static final int TITLE_HEIGHT = 13;
    private static final int MODULE_HEIGHT = 11;
    private static final int VALUE_HEIGHT = 11;
    private static final Color ACCENT_COLOR = new Color(164, 53, 144);
    private static final Color BG_COLOR = new Color(25, 25, 25);
    private static final Color MODULE_BG = new Color(36, 36, 36);
    private static final Color EXPANDED_BG = new Color(17, 17, 17);
    private static final Map<String, Boolean> faithsExpandedState = new HashMap<>();

    public FaithsWindow(String category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
    }

    private boolean mouseHovered(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isInPanelBounds(float localY, float elementHeight) {
        return localY + elementHeight > 13.0F && localY < this.lastRenderHeight;
    }

    private void drawRect(float x, float y, float w, float h, Color color) {
        RenderUtil.drawRect(x, y, x + w, y + h, color.getRGB());
    }

    private void drawRect(float x, float y, float w, float h, int color) {
        RenderUtil.drawRect(x, y, x + w, y + h, color);
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
        if (!Mouse.isButtonDown(0)) {
            this.draggingPropertyName = null;
        }

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
        List<Module> modules = Miau.moduleManager.getModulesByCategory().get(this.category);
        float height = 15.0F;
        if (this.expand && modules != null) {
            for (Module module : modules) {
                height += 11.0F;
                if (this.isModuleExpanded(module)) {
                    for (Property<?> value : module.getValues()) {
                        if (value.isVisible()) {
                            height += 11.0F;
                        }
                    }
                }
            }
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
        titleFont.draw(this.category.toLowerCase(), 5.0, 3.0, -1);
        GL11.glEnable(3089);
        this.scissor(this.x, this.y + 13.0F, 100.0, renderHeight - 13.0F);
        float moduleY = 11.0F + this.scrollY;
        if (modules != null && this.expand) {
            for (Module module : modules) {
                boolean expanded = this.isModuleExpanded(module);
                Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + moduleY));
                Color c2 = Themes.getCurrentTheme()
                    .getAccentColor(new Vector2d(this.x + 100.0F, this.y + moduleY + 11.0F));
                if (!expanded) {
                    this.drawRect(3.0F, moduleY, 95.0F, 11.0F, MODULE_BG);
                    if (module.isEnabled()) {
                        RenderUtil.drawHorizontalGradientRect(
                            3.0F, moduleY, 98.0F, moduleY + 11.0F, c1.getRGB(), c2.getRGB()
                        );
                    }
                }

                if (this.isInPanelBounds(moduleY, 11.0F)
                    && this.mouseHovered(this.x, this.y + moduleY, 100.0F, 11.0F, mouseX, mouseY)) {
                    if (!expanded && !module.isEnabled()) {
                        this.drawRect(3.0F, moduleY, 95.0F, 11.0F, new Color(255, 255, 255, 50));
                    }

                    if (Mouse.isButtonDown(1)) {
                        if (!this.rightMouseClicked) {
                            if (!module.getValues().isEmpty()) {
                                this.toggleModuleExpanded(module);
                            }

                            this.rightMouseClicked = true;
                        }
                    } else {
                        this.rightMouseClicked = false;
                    }

                    if (Mouse.isButtonDown(0)) {
                        if (!this.leftMouseClicked) {
                            module.toggle();
                            this.leftMouseClicked = true;
                        }
                    } else {
                        this.leftMouseClicked = false;
                    }
                }

                Font moduleFont = FontRepository.getHudFont(13);
                int textColor;
                if (module.isEnabled()) {
                    textColor = expanded ? c1.getRGB() : RenderUtil.getContrastTextColor(c1);
                } else {
                    textColor = new Color(160, 160, 160).getRGB();
                }

                moduleFont.draw(
                    module.getName().toLowerCase(),
                    97 - moduleFont.width(module.getName().toLowerCase()),
                    moduleY + 2.0F,
                    textColor
                );
                if (expanded) {
                    for (Property<?> value : module.getValues()) {
                        if (value.isVisible()) {
                            moduleY += 11.0F;
                            this.renderValue(value, moduleY, mouseX, mouseY);
                        }
                    }
                }

                moduleY += 11.0F;
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

    private void renderValue(Property<?> value, float moduleY, int mouseX, int mouseY) {
        if (value.isVisible()) {
            if (value instanceof BooleanProperty) {
                this.renderBoolean((BooleanProperty)value, moduleY, mouseX, mouseY);
            } else if (value instanceof ModeProperty) {
                this.renderMode((ModeProperty)value, moduleY, mouseX, mouseY);
            } else if (value instanceof FloatProperty) {
                this.renderFloat((FloatProperty)value, moduleY, mouseX, mouseY);
            } else if (value instanceof IntProperty) {
                this.renderInt((IntProperty)value, moduleY, mouseX, mouseY);
            } else if (value instanceof PercentProperty) {
                this.renderPercent((PercentProperty)value, moduleY, mouseX, mouseY);
            } else if (value instanceof ColorProperty) {
                this.renderColor((ColorProperty)value, moduleY, mouseX, mouseY);
            }
        }
    }

    private void renderBoolean(BooleanProperty prop, float moduleY, int mouseX, int mouseY) {
        if (this.isInPanelBounds(moduleY, 11.0F)
            && this.mouseHovered(this.x, this.y + moduleY, 100.0F, 11.0F, mouseX, mouseY)) {
            if (Mouse.isButtonDown(0)) {
                if (!this.leftMouseClicked) {
                    prop.setValue(!prop.getValue());
                    this.leftMouseClicked = true;
                }
            } else {
                this.leftMouseClicked = false;
            }
        }

        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + moduleY));
        if (prop.getValue()) {
            Color c2 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x + 100.0F, this.y + moduleY + 11.0F));
            RenderUtil.drawHorizontalGradientRect(3.0F, moduleY, 98.0F, moduleY + 11.0F, c1.getRGB(), c2.getRGB());
        }

        Font font = FontRepository.getHudFont(13);
        int textColor = prop.getValue() ? RenderUtil.getContrastTextColor(c1) : -1;
        font.draw(prop.getName(), 5.0, moduleY + 2.0F, textColor);
    }

    private void renderMode(ModeProperty prop, float moduleY, int mouseX, int mouseY) {
        if (this.isInPanelBounds(moduleY, 11.0F)
            && this.mouseHovered(this.x, this.y + moduleY, 100.0F, 11.0F, mouseX, mouseY)) {
            if (Mouse.isButtonDown(0)) {
                if (!this.leftMouseClicked) {
                    prop.nextMode();
                    this.leftMouseClicked = true;
                }
            } else {
                this.leftMouseClicked = false;
            }
        }

        Font font = FontRepository.getHudFont(13);
        font.draw(prop.getName(), 5.0, moduleY + 2.0F, -1);
        font.draw(prop.getModeString(), 95 - font.width(prop.getModeString()), moduleY + 2.0F, -1);
    }

    private void renderFloat(FloatProperty prop, float moduleY, int mouseX, int mouseY) {
        Font font = FontRepository.getHudFont(13);
        float valueWidth = 95.0F;
        float ratio = (prop.getValue() - prop.getMinimum()) / (prop.getMaximum() - prop.getMinimum());
        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + moduleY));
        Color c2 = Themes.getCurrentTheme()
            .getAccentColor(new Vector2d(this.x + valueWidth * ratio, this.y + moduleY + 11.0F));
        boolean hovered = this.isInPanelBounds(moduleY, 11.0F)
            && this.mouseHovered(this.x, this.y + moduleY, valueWidth, 11.0F, mouseX, mouseY);
        if (hovered && Mouse.isButtonDown(0) && this.draggingPropertyName == null) {
            this.draggingPropertyName = prop.getName();
        }

        if (prop.getName().equals(this.draggingPropertyName) && Mouse.isButtonDown(0)) {
            float newRatio = Math.max(0.0F, Math.min(1.0F, (mouseX - this.x) / valueWidth));
            float newVal = prop.getMinimum() + newRatio * (prop.getMaximum() - prop.getMinimum());
            prop.setValue(newVal);
            ratio = newRatio;
        }

        RenderUtil.drawHorizontalGradientRect(
            3.0F, moduleY, 3.0F + valueWidth * ratio, moduleY + 11.0F, c1.getRGB(), c2.getRGB()
        );
        int textColor = ratio > 0.3F ? RenderUtil.getContrastTextColor(c1) : -1;
        font.draw(prop.getName(), 5.0, moduleY + 2.0F, textColor);
        font.drawCentered(FLOAT_POINT_FORMAT.format(prop.getValue()), valueWidth, moduleY + 2.0F, textColor);
    }

    private void renderInt(IntProperty prop, float moduleY, int mouseX, int mouseY) {
        Font font = FontRepository.getHudFont(13);
        float valueWidth = 95.0F;
        float ratio = (float)(prop.getValue() - prop.getMinimum()) / (prop.getMaximum() - prop.getMinimum());
        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + moduleY));
        Color c2 = Themes.getCurrentTheme()
            .getAccentColor(new Vector2d(this.x + valueWidth * ratio, this.y + moduleY + 11.0F));
        boolean hovered = this.isInPanelBounds(moduleY, 11.0F)
            && this.mouseHovered(this.x, this.y + moduleY, valueWidth, 11.0F, mouseX, mouseY);
        if (hovered && Mouse.isButtonDown(0) && this.draggingPropertyName == null) {
            this.draggingPropertyName = prop.getName();
        }

        if (prop.getName().equals(this.draggingPropertyName) && Mouse.isButtonDown(0)) {
            float newRatio = Math.max(0.0F, Math.min(1.0F, (mouseX - this.x) / valueWidth));
            int newVal = Math.round(prop.getMinimum().intValue() + newRatio * (prop.getMaximum() - prop.getMinimum()));
            prop.setValue(newVal);
            ratio = newRatio;
        }

        RenderUtil.drawHorizontalGradientRect(
            3.0F, moduleY, 3.0F + valueWidth * ratio, moduleY + 11.0F, c1.getRGB(), c2.getRGB()
        );
        int textColor = ratio > 0.3F ? RenderUtil.getContrastTextColor(c1) : -1;
        font.draw(prop.getName(), 5.0, moduleY + 2.0F, textColor);
        font.drawCentered(String.valueOf(prop.getValue()), valueWidth, moduleY + 2.0F, textColor);
    }

    private void renderPercent(PercentProperty prop, float moduleY, int mouseX, int mouseY) {
        Font font = FontRepository.getHudFont(13);
        float valueWidth = 95.0F;
        float ratio = prop.getValue().intValue() / 100.0F;
        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y + moduleY));
        Color c2 = Themes.getCurrentTheme()
            .getAccentColor(new Vector2d(this.x + valueWidth * ratio, this.y + moduleY + 11.0F));
        boolean hovered = this.isInPanelBounds(moduleY, 11.0F)
            && this.mouseHovered(this.x, this.y + moduleY, valueWidth, 11.0F, mouseX, mouseY);
        if (hovered && Mouse.isButtonDown(0) && this.draggingPropertyName == null) {
            this.draggingPropertyName = prop.getName();
        }

        if (prop.getName().equals(this.draggingPropertyName) && Mouse.isButtonDown(0)) {
            float newRatio = Math.max(0.0F, Math.min(1.0F, (mouseX - this.x) / valueWidth));
            prop.setValue(newRatio * 100.0F);
            ratio = newRatio;
        }

        RenderUtil.drawHorizontalGradientRect(
            3.0F, moduleY, 3.0F + valueWidth * ratio, moduleY + 11.0F, c1.getRGB(), c2.getRGB()
        );
        int textColor = ratio > 0.3F ? RenderUtil.getContrastTextColor(c1) : -1;
        font.draw(prop.getName(), 5.0, moduleY + 2.0F, textColor);
        font.drawCentered(prop.getValue() + "%", valueWidth, moduleY + 2.0F, textColor);
    }

    private void renderColor(ColorProperty prop, float moduleY, int mouseX, int mouseY) {
        Font font = FontRepository.getHudFont(13);
        this.drawRect(88.0F, moduleY, 9.0F, 9.0F, new Color(prop.getValue()));
        font.draw(prop.getName(), 5.0, moduleY + 1.0F, -1);
    }

    private boolean isModuleExpanded(Module module) {
        String key = "faiths_expanded_" + module.getName();
        Map<String, Boolean> state = this.getExpandedState();
        return state.containsKey(key) && state.get(key);
    }

    private void toggleModuleExpanded(Module module) {
        String key = "faiths_expanded_" + module.getName();
        Map<String, Boolean> state = this.getExpandedState();
        state.put(key, !state.getOrDefault(key, false));
    }

    private Map<String, Boolean> getExpandedState() {
        return faithsExpandedState;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = false;
        this.draggingPropertyName = null;
    }

    public String getCategory() {
        return this.category;
    }
}
