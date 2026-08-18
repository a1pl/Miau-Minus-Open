package miau.ui.clickgui.demise.component;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import miau.module.Module;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.ui.clickgui.demise.Component;
import miau.ui.clickgui.demise.IComponent;
import miau.ui.clickgui.demise.PanelGui;
import miau.ui.clickgui.demise.component.impl.BooleanComponent;
import miau.ui.clickgui.demise.component.impl.ColorPickerComponent;
import miau.ui.clickgui.demise.component.impl.ModeComponent;
import miau.ui.clickgui.demise.component.impl.SliderComponent;
import miau.ui.clickgui.demise.component.impl.StringComponent;
import miau.util.demise.RoundedUtils;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ModuleComponent implements IComponent {
    private Module module;
    private float x;
    private float y;
    private boolean isHovered;
    private boolean isExpanded;
    private float height;
    private Color interpolatedColor = new Color(20, 20, 20, 150);
    private Color interpolatedColor1 = new Color(0, 0, 0, 0);
    public boolean visible;
    private boolean visibleSetting;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private float slideProgress = 0.0F;

    public ModuleComponent(Module module) {
        this.module = module;
        this.height = 35.0F;

        for (Property<?> value : module.getValues()) {
            if (value instanceof BooleanProperty) {
                BooleanProperty bp = (BooleanProperty)value;
                this.settings.add(new BooleanComponent(bp));
            } else if (value instanceof ColorProperty) {
                ColorProperty cp = (ColorProperty)value;
                this.settings.add(new ColorPickerComponent(cp));
            } else if (value instanceof FloatProperty) {
                FloatProperty fp = (FloatProperty)value;
                this.settings.add(new SliderComponent(fp));
            } else if (value instanceof ModeProperty) {
                ModeProperty mp = (ModeProperty)value;
                this.settings.add(new ModeComponent(mp));
            } else if (value instanceof TextProperty) {
                TextProperty tp = (TextProperty)value;
                this.settings.add(new StringComponent(tp));
            }
        }
    }

    public void initCategory() {
        this.slideProgress = 0.0F;
    }

    public void render(boolean shader) {
        if (this.visible) {
            float width = 375.0F;
            this.slideProgress = this.animate(this.slideProgress, this.visibleSetting ? 1.0F : 0.0F, 0.1F);
            float slideOffset = width / 4.0F * (1.0F - this.slideProgress);
            if (!shader) {
                if (this.isHovered) {
                    this.interpolatedColor = this.interpolateColorC(
                        this.interpolatedColor, new Color(35, 35, 35, 190), 0.1F
                    );
                } else {
                    this.interpolatedColor = this.interpolateColorC(
                        this.interpolatedColor, new Color(20, 20, 20, 150), 0.1F
                    );
                }

                if (this.module.isEnabled()) {
                    this.interpolatedColor1 = this.interpolateColorC(
                        this.interpolatedColor1, new Color(50, 50, 50, 150), 0.1F
                    );
                } else {
                    this.interpolatedColor1 = this.interpolateColorC(
                        this.interpolatedColor1, new Color(0, 0, 0, 0), 0.1F
                    );
                }

                RoundedUtils.drawRound(this.x + slideOffset, this.y, width, this.height, 8.0F, this.interpolatedColor);
                RoundedUtils.drawRound(this.x + slideOffset, this.y, width, this.height, 8.0F, this.interpolatedColor1);
                FontRepository.getFont("Inter Regular", 18.0F)
                    .draw(this.module.getName(), this.x + 7.0F + slideOffset, this.y + 9.0F, Color.white.getRGB());
                FontRepository.getFont("Inter Regular", 14.0F)
                    .draw(
                        this.module.getName() + " module",
                        this.x + 7.0F + slideOffset,
                        this.y + 21.0F,
                        new Color(200, 200, 200).getRGB()
                    );
                String keyName = this.module.getKey() == 0 ? "None" : this.getKeyName(this.module.getKey());
                FontRepository.getFont("Inter Regular", 14.0F)
                    .draw(
                        keyName,
                        this.x
                            + width
                            - 8.0F
                            - FontRepository.getFont("Inter Regular", 14.0F).getStringWidth(keyName)
                            + slideOffset,
                        this.y + 10.0F,
                        new Color(150, 150, 150, 150).getRGB()
                    );
            } else {
                RoundedUtils.drawRound(this.x + slideOffset, this.y, width, this.height, 8.0F, Color.black);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = PanelGui.isHovered(this.x, this.y, 375.0F, 35.0F, mouseX, mouseY);
        float yOffset = 35.0F;
        float width = 375.0F;
        float openOutput = this.isExpanded ? 1.0F : 0.0F;
        RenderUtil.scissor(this.x, PanelGui.posY + 45.0F, width, 255.0F, PanelGui.interpolatedScale);
        GL11.glEnable(3089);
        float slideOffset = width / 4.0F * (1.0F - this.slideProgress);

        for (Component component : this.settings) {
            if (component.isVisible()) {
                component.setX((component.isChild() ? this.x + 5.0F : this.x) + slideOffset);
                component.setY(this.y + yOffset * openOutput + 1.0F);
                component.setWidth(component.isChild() ? width - 5.0F : width);
                if (openOutput > 0.7F) {
                    component.drawScreen(mouseX, mouseY);
                    if (component.isChild()) {
                        RenderUtil.drawRect(
                            this.x + 3.5F + slideOffset,
                            component.getY() - 2.8F,
                            this.x + 3.5F + slideOffset + 1.0F,
                            component.getY() - 2.8F + component.getHeight(),
                            Color.gray.getRGB()
                        );
                    }
                }

                yOffset += component.getHeight() * openOutput;
                this.height = yOffset;
            }
        }

        GL11.glDisable(3089);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovered) {
            if (this.visible && mouseY > PanelGui.posY + 45.0F) {
                if (mouseButton == 0) {
                    this.module.toggle();
                } else if (mouseButton == 1) {
                    this.isExpanded = !this.isExpanded;
                }
            }
        } else if (this.isExpanded) {
            for (Component setting : this.settings) {
                setting.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.isExpanded && !this.isHovered) {
            for (Component setting : this.settings) {
                setting.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.isExpanded && !this.isHovered) {
            for (Component setting : this.settings) {
                setting.keyTyped(typedChar, keyCode);
            }
        }
    }

    public Module getModule() {
        return this.module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

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

    public boolean isHovered() {
        return this.isHovered;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public boolean isExpanded() {
        return this.isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisibleSetting() {
        return this.visibleSetting;
    }

    public void setVisibleSetting(boolean visibleSetting) {
        this.visibleSetting = visibleSetting;
    }

    private Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        return new Color(
            (int)(color1.getRed() + (color2.getRed() - color1.getRed()) * amount),
            (int)(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * amount),
            (int)(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * amount),
            (int)(color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * amount)
        );
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }

    private String getKeyName(int key) {
        try {
            return Keyboard.getKeyName(key);
        } catch (Exception e) {
            return "None";
        }
    }
}
