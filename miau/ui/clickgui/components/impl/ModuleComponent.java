package miau.ui.clickgui.components.impl;

import java.awt.Color;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
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
import miau.property.properties.TextProperty;
import miau.ui.clickgui.ClickGui;
import miau.ui.clickgui.components.Component;
import miau.util.animation.AnimationTimer;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ModuleComponent extends Component {
    public Module mod;
    public CategoryComponent categoryComponent;
    public float yPos;
    public ArrayList<Component> settings;
    public boolean isOpened;
    private boolean hovering;
    private AnimationTimer hoverTimer;
    private boolean hoverStarted;
    private AnimationTimer smoothTimer;
    private float enableAlpha = 0.0F;
    private float smoothingY = 16.0F;
    private float animationStartY = 16.0F;
    private float animationTargetY = 16.0F;
    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer(16);
    private static final int ORIGINAL_HOVER_ALPHA = 120;
    private static final int HOVER_COLOR = new Color(0, 0, 0, 120).getRGB();
    private static final int ENABLED_COLOR = new Color(24, 154, 255).getRGB();
    private static final int DISABLED_COLOR = new Color(192, 192, 192).getRGB();
    private final boolean categoryManager;
    private static final int MAX_SCISSOR_DEPTH = 4;
    private final int[][] scissorStack = new int[4][5];
    private int scissorDepth = 0;

    public ModuleComponent(Module mod, CategoryComponent p, float yPos) {
        this(mod, p, yPos, false);
    }

    public ModuleComponent(Module mod, CategoryComponent p, float yPos, boolean categoryManager) {
        this.mod = mod;
        this.categoryComponent = p;
        this.yPos = yPos;
        this.settings = new ArrayList<>();
        this.categoryManager = categoryManager;
        this.isOpened = categoryManager;
        float collapsedHeight = this.getCollapsedHeight();
        this.smoothingY = collapsedHeight;
        this.animationStartY = collapsedHeight;
        this.animationTargetY = collapsedHeight;
        this.rebuildSettingsList();
    }

    private void rebuildSettingsList() {
        this.settings = new ArrayList<>();
        float y = this.yPos + this.getSettingStartOffset();
        if (this.mod != null) {
            ArrayList<Property<?>> props = Miau.propertyManager.properties.get(this.mod.getClass());
            if (props != null) {
                for (int i = 0; i < props.size(); i++) {
                    Property<?> v = props.get(i);
                    if (v.isVisible()) {
                        if (v instanceof BooleanProperty && v.getName().startsWith("target-")) {
                            List<BooleanProperty> groupProps = new ArrayList<>();
                            groupProps.add((BooleanProperty)v);

                            for (int j = i + 1; j < props.size(); i = j++) {
                                Property<?> next = props.get(j);
                                if (!(next instanceof BooleanProperty)
                                    || !next.getName().startsWith("target-")
                                    || !next.isVisible()) {
                                    break;
                                }

                                groupProps.add((BooleanProperty)next);
                            }

                            for (BooleanProperty prop : groupProps) {
                                ButtonComponent c = new ButtonComponent(this.mod, prop, this, y);
                                this.settings.add(c);
                                y += 12.0F;
                            }
                        } else if (v instanceof BooleanProperty) {
                            ButtonComponent c = new ButtonComponent(this.mod, (BooleanProperty)v, this, y);
                            this.settings.add(c);
                            y += 12.0F;
                        } else if (v instanceof FloatProperty
                            || v instanceof IntProperty
                            || v instanceof PercentProperty
                            || v instanceof ModeProperty) {
                            SliderComponent s = new SliderComponent(v, this, y);
                            this.settings.add(s);
                            y += 12.0F;
                        } else if (v instanceof ColorProperty) {
                            ColorComponent cc = new ColorComponent((ColorProperty)v, this, y);
                            this.settings.add(cc);
                            y += 12.0F;
                        } else if (v instanceof TextProperty) {
                            StringComponent sc = new StringComponent((TextProperty)v, this, y);
                            this.settings.add(sc);
                            y += 12.0F;
                        }
                    }
                }
            }
        }

        if (!this.categoryManager) {
            this.settings.add(new BindComponent(this, y));
        }
    }

    public void reloadSettings() {
        boolean wasOpened = this.isOpened;
        Map<String, Boolean> groupExpandedStates = new HashMap<>();
        Map<Property<?>, Boolean> sliderHeldStates = new HashMap<>();
        Map<Property<?>, Boolean> sliderMinStates = new HashMap<>();
        Map<Property<?>, Boolean> sliderMaxStates = new HashMap<>();
        Map<Property<?>, Boolean> colorExpandedStates = new HashMap<>();
        Map<Property<?>, Boolean> modeExpandedStates = new HashMap<>();

        for (Component component : this.settings) {
            if (component instanceof GroupComponent) {
                groupExpandedStates.put(
                    ((GroupComponent)component).getGroupName(), ((GroupComponent)component).isExpanded()
                );
            } else if (component instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent)component;
                sliderHeldStates.put(sliderComponent.property, sliderComponent.heldDown);
                sliderMinStates.put(sliderComponent.property, sliderComponent.draggingMin);
                sliderMaxStates.put(sliderComponent.property, sliderComponent.draggingMax);
                if (sliderComponent.property instanceof ModeProperty) {
                    modeExpandedStates.put(sliderComponent.property, sliderComponent.isExpanded);
                }
            } else if (component instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent)component;
                colorExpandedStates.put(colorComponent.property, colorComponent.expanded);
            }
        }

        this.rebuildSettingsList();

        for (Component component : this.settings) {
            if (component instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent)component;
                Boolean wasHeldDown = sliderHeldStates.get(sliderComponent.property);
                if (wasHeldDown != null) {
                    sliderComponent.heldDown = wasHeldDown;
                }

                Boolean wasDraggingMin = sliderMinStates.get(sliderComponent.property);
                if (wasDraggingMin != null) {
                    sliderComponent.draggingMin = wasDraggingMin;
                }

                Boolean wasDraggingMax = sliderMaxStates.get(sliderComponent.property);
                if (wasDraggingMax != null) {
                    sliderComponent.draggingMax = wasDraggingMax;
                }

                if (sliderComponent.property instanceof ModeProperty) {
                    Boolean wasExpanded = modeExpandedStates.get(sliderComponent.property);
                    if (wasExpanded != null) {
                        sliderComponent.restoreModeDropdownState(wasExpanded);
                    }
                }
            } else if (component instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent)component;
                Boolean wasExpanded = colorExpandedStates.get(colorComponent.property);
                if (wasExpanded != null) {
                    colorComponent.restoreExpandedState(wasExpanded);
                }
            } else if (component instanceof GroupComponent) {
                GroupComponent groupComponent = (GroupComponent)component;
                Boolean wasExpanded = groupExpandedStates.get(groupComponent.getGroupName());
                if (wasExpanded != null) {
                    groupComponent.restoreExpandedState(wasExpanded);
                }
            }
        }

        this.restoreOpenState(wasOpened);
        this.updateSettingPositions();
    }

    public void restoreOpenState(boolean opened) {
        this.isOpened = this.categoryManager || opened;
        this.smoothTimer = null;
        float height = this.isOpened ? this.getHeightF() : this.getCollapsedHeight();
        this.smoothingY = height;
        this.animationStartY = height;
        this.animationTargetY = height;
    }

    public void updateAnimationState() {
        if (this.smoothTimer != null) {
            if (System.currentTimeMillis() - this.smoothTimer.last >= 280L) {
                this.smoothTimer = null;
                this.smoothingY = this.animationTargetY;
                this.animationStartY = this.animationTargetY;
            } else {
                this.smoothingY = this.smoothTimer.getValueFloat(this.animationStartY, this.animationTargetY, 1);
                if (this.smoothingY == this.animationTargetY) {
                    this.smoothTimer = null;
                    this.animationStartY = this.animationTargetY;
                }
            }
        }
    }

    @Override
    public void updateHeight(float newY) {
        this.yPos = newY;
        float y = this.yPos + this.getCollapsedHeight();

        for (Component co : this.settings) {
            if (this.isVisibleBase(co)) {
                co.updateHeight(y);
                if (co instanceof SliderComponent) {
                    ((SliderComponent)co).xOffset = 0.0F;
                } else if (co instanceof ButtonComponent) {
                    ((ButtonComponent)co).xOffset = 0.0F;
                } else if (co instanceof BindComponent) {
                    ((BindComponent)co).xOffset = 0.0F;
                } else if (co instanceof ColorComponent) {
                    ((ColorComponent)co).xOffset = 0.0F;
                } else if (co instanceof GroupComponent) {
                    ((GroupComponent)co).xOffset = 0.0F;
                }

                y += this.getBaseComponentHeightF(co);
            }
        }
    }

    @Override
    public void render() {
        boolean isEnabled = this.mod != null && this.mod.isEnabled();
        this.enableAlpha = isEnabled ? 1.0F : 0.0F;
        if (this.hasModuleHeader() && (this.hovering || this.hoverTimer != null)) {
            double hoverAlpha = this.hovering && this.hoverTimer != null
                ? this.hoverTimer.getValueFloat(0.0F, 120.0F, 1)
                : (
                    this.hoverTimer != null && !this.hovering
                        ? 120.0F - this.hoverTimer.getValueFloat(0.0F, 120.0F, 1)
                        : 120.0
                );
            if (hoverAlpha == 0.0) {
                this.hoverTimer = null;
            }
        }

        if (this.hasModuleHeader() && this.enableAlpha > 0.01F) {
        }

        int r = (int)(192.0F + 63.0F * this.enableAlpha);
        int g = (int)(192.0F + 63.0F * this.enableAlpha);
        int b = (int)(192.0F + 63.0F * this.enableAlpha);
        int button_rgb = new Color(r, g, b).getRGB();
        Font titleRenderer = FontRepository.getClickGuiFont();
        if (this.hasModuleHeader()) {
            float textX = this.categoryComponent.getX() + 6.0F;
            float textY = this.categoryComponent.getY() + this.yPos + 5.0F;
            titleRenderer.draw(this.mod.getName(), textX, textY, button_rgb, true);
        }

        boolean scissorRequired = this.smoothTimer != null || this.isOpened;
        if (scissorRequired) {
            ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
            int scale = sr.func_78325_e();
            double guiScale = ClickGui.getActiveRenderScale();
            float scrollOffset = this.categoryComponent.getModuleY() - this.categoryComponent.getY();
            double sx = this.categoryComponent.getX() - 2.0F;
            double sy = this.categoryComponent.getY() + this.yPos + scrollOffset;
            double sw = this.categoryComponent.getWidth() + 4.0F;
            double sh = this.smoothingY;
            if (ClickGui.openingScale != 1.0F) {
                double scaleFactor = ClickGui.openingScale;
                double centerX = sr.func_78326_a() / 2.0;
                double centerY = sr.func_78328_b() / 2.0;
                sx = centerX + (sx - centerX) * scaleFactor;
                sy = centerY + (sy - centerY) * scaleFactor;
                sw *= scaleFactor;
                sh *= scaleFactor;
            }

            int scissorX = (int)Math.floor(sx * guiScale * scale);
            int scissorY = (int)Math.floor((sr.func_78328_b() - (sy + sh) * guiScale) * scale);
            int scissorW = (int)Math.ceil(sw * guiScale * scale);
            int scissorH = (int)Math.ceil(sh * guiScale * scale);
            this.pushScissor(scissorX, scissorY, scissorW, scissorH);
        }

        if (this.isOpened || this.smoothTimer != null) {
            this.renderSettings();
        }

        if (scissorRequired) {
            this.popScissor();
        }
    }

    private void renderSettings() {
        for (Component c : this.settings) {
            if (this.isVisibleBase(c)) {
                c.render();
            }
        }
    }

    public void renderOverlays() {
        for (Component c : this.settings) {
            if (c instanceof SliderComponent && this.isVisibleBase(c)) {
                SliderComponent slider = (SliderComponent)c;
                if (slider.isModeDropdownActive()) {
                    ClickGui.activeModeDropdown = slider;
                }
            }
        }
    }

    @Override
    public float getHeightF() {
        if (this.smoothTimer != null) {
            return this.smoothingY;
        }

        if (!this.isOpened) {
            return this.getCollapsedHeight();
        }

        float h = this.getCollapsedHeight();

        for (Component c : this.settings) {
            h += this.getAnimatedComponentHeightF(c);
        }

        return h;
    }

    @Override
    public int getHeight() {
        return Math.round(this.getHeightF());
    }

    public void onSliderChange() {
        for (Component c : this.settings) {
            if (c instanceof SliderComponent) {
                ((SliderComponent)c).onSliderChange();
            }
        }
    }

    @Override
    public float getScrollExtentHeightF() {
        if (this.isOpened || this.smoothTimer != null && this.animationTargetY > 16.0F) {
            float h = this.getCollapsedHeight();

            for (Component c : this.settings) {
                if (this.isVisibleBase(c)) {
                    h += this.getBaseComponentHeightF(c);
                }
            }

            return h;
        } else {
            return this.getHeightF();
        }
    }

    @Override
    public void drawScreen(int x, int y) {
        for (Component c : this.settings) {
            c.drawScreen(x, y);
        }

        if (this.hasModuleHeader() && this.overModuleName(x, y) && this.categoryComponent.opened) {
            this.hovering = true;
            if (this.hoverTimer == null) {
                (this.hoverTimer = new AnimationTimer(75.0F)).start();
                this.hoverStarted = true;
            }
        } else {
            if (this.hovering && this.hoverStarted) {
                (this.hoverTimer = new AnimationTimer(75.0F)).start();
            }

            this.hoverStarted = false;
            this.hovering = false;
        }
    }

    @Override
    public boolean onClick(int x, int y, int mouse) {
        if (this.hasModuleHeader() && this.overModuleName(x, y) && mouse == 0) {
            this.mod.toggle();
            return true;
        }

        if (this.hasModuleHeader() && this.overModuleName(x, y) && mouse == 1) {
            float currentHeight = this.smoothTimer != null
                ? this.smoothingY
                : (this.isOpened ? this.getHeightF() : 18.0F);
            this.animationStartY = currentHeight;
            this.isOpened = !this.isOpened;
            float targetHeight;
            if (this.isOpened) {
                float h = this.getCollapsedHeight();

                for (Component c : this.settings) {
                    h += this.getAnimatedComponentHeightF(c);
                }

                targetHeight = h;
            } else {
                targetHeight = this.getCollapsedHeight();
            }

            this.animationTargetY = targetHeight;
            (this.smoothTimer = new AnimationTimer(250.0F)).start();
            return true;
        } else {
            SliderComponent activeDropdown = this.getActiveModeDropdown();
            if (activeDropdown != null) {
                if (activeDropdown.onClick(x, y, mouse)) {
                    return true;
                }

                if (!activeDropdown.isMouseOverModeDropdown(x, y)) {
                    activeDropdown.collapseModeDropdown();
                }

                return true;
            } else {
                for (Component settingComponent : this.settings) {
                    if (settingComponent.onClick(x, y, mouse)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public SliderComponent getActiveModeDropdown() {
        for (int i = this.settings.size() - 1; i >= 0; i--) {
            Component component = this.settings.get(i);
            if (component instanceof SliderComponent && this.isVisibleBase(component)) {
                SliderComponent slider = (SliderComponent)component;
                if (slider.isModeDropdownActive()) {
                    return slider;
                }
            }
        }

        return null;
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    @Override
    public void onScroll(int scroll) {
        for (Component component : this.settings) {
            component.onScroll(scroll);
        }
    }

    @Override
    public void onGuiClosed() {
        for (Component c : this.settings) {
            c.onGuiClosed();
        }

        this.smoothTimer = null;
        this.hoverTimer = null;
        float finalHeight = this.isOpened ? this.getHeightF() : this.getCollapsedHeight();
        this.smoothingY = finalHeight;
        this.animationStartY = finalHeight;
        this.animationTargetY = finalHeight;
    }

    public boolean overModuleName(int x, int y) {
        return !this.hasModuleHeader()
            ? false
            : x > this.categoryComponent.getX()
                && x < this.categoryComponent.getX() + this.categoryComponent.getWidth()
                && y > this.categoryComponent.getModuleY() + this.yPos
                && y < this.categoryComponent.getModuleY() + 18.0F + this.yPos;
    }

    public void updateSettingPositions() {
        this.categoryComponent.updateHeight();
    }

    public boolean isVisible(Component component) {
        return this.isVisibleBase(component);
    }

    private float getBaseComponentHeightF(Component component) {
        if (component instanceof SliderComponent) {
            return 16.0F;
        } else if (component instanceof ColorComponent) {
            ColorComponent cc = (ColorComponent)component;
            float progress = cc.getAnimationProgress();
            return 12.0F + (cc.getExpandedHeight() - 12.0F) * progress;
        } else if (component instanceof GroupComponent) {
            GroupComponent gc = (GroupComponent)component;
            return 14.0F + gc.getSubCount() * 12.0F * gc.getAnimationProgress();
        } else {
            return 12.0F;
        }
    }

    private float getAnimatedComponentHeightF(Component component) {
        return !this.isVisibleBase(component) ? 0.0F : this.getBaseComponentHeightF(component);
    }

    private void pushScissor(int x, int y, int w, int h) {
        boolean wasEnabled = GL11.glIsEnabled(3089);
        int[] saved = this.scissorStack[this.scissorDepth++];
        if (wasEnabled) {
            ((Buffer)SCISSOR_BOX).clear();
            GL11.glGetInteger(3088, SCISSOR_BOX);
            saved[0] = 1;
            saved[1] = SCISSOR_BOX.get(0);
            saved[2] = SCISSOR_BOX.get(1);
            saved[3] = SCISSOR_BOX.get(2);
            saved[4] = SCISSOR_BOX.get(3);
            int ix = Math.max(saved[1], x);
            int iy = Math.max(saved[2], y);
            int iw = Math.max(0, Math.min(saved[1] + saved[3], x + w) - ix);
            int ih = Math.max(0, Math.min(saved[2] + saved[4], y + h) - iy);
            GL11.glScissor(ix, iy, iw, ih);
        } else {
            saved[0] = 0;
            GL11.glEnable(3089);
            GL11.glScissor(x, y, w, h);
        }
    }

    private void popScissor() {
        int[] saved = this.scissorStack[--this.scissorDepth];
        if (saved[0] == 1) {
            GL11.glScissor(saved[1], saved[2], saved[3], saved[4]);
        } else {
            GL11.glDisable(3089);
        }
    }

    private boolean isVisibleBase(Component component) {
        return component.isBaseVisible();
    }

    private boolean hasModuleHeader() {
        return !this.categoryManager;
    }

    private float getCollapsedHeight() {
        return this.hasModuleHeader() ? 18.0F : 0.0F;
    }

    private float getSettingStartOffset() {
        return this.hasModuleHeader() ? 12.0F : 0.0F;
    }

    private boolean hasActualProperties() {
        if (this.mod == null) {
            return false;
        }

        ArrayList<Property<?>> props = Miau.propertyManager.properties.get(this.mod.getClass());
        if (props != null && !props.isEmpty()) {
            for (Property<?> p : props) {
                if (p.isVisible()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }
}
