package miau.ui.clickgui.components.impl;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import miau.property.Property;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.ui.clickgui.components.Component;
import miau.util.animation.AnimationTimer;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import org.lwjgl.opengl.GL11;

public class SliderComponent extends Component {
    public Property<?> property;
    private ModuleComponent moduleComponent;
    public float o;
    public float x;
    private float y;
    public boolean heldDown = false;
    private double width;
    public float xOffset;
    private double targetValue;
    private double displayedValue;
    public boolean draggingMin = false;
    public boolean draggingMax = false;
    private double targetSecondValue;
    private double displayedSecondValue;
    public boolean isExpanded = false;
    private AnimationTimer dropdownTimer;
    private float dropdownProgress = 0.0F;
    private float dropdownStartProgress = 0.0F;
    private float dropdownTargetProgress = 0.0F;
    private static final double SLIDER_SPEED = 0.6;
    private static final float MODE_HEADER_HEIGHT = 14.0F;
    private static final float MODE_OPTION_HEIGHT = 12.0F;
    private static final float MODE_DROPDOWN_GAP = 2.0F;
    private static final float MODE_ANIMATION_DURATION = 180.0F;

    public SliderComponent(Property<?> property, ModuleComponent moduleComponent, float o) {
        this.property = property;
        this.moduleComponent = moduleComponent;
        this.o = o;
        double initial = this.getValue();
        this.targetValue = initial;
        this.displayedValue = initial;
        if (this.isDouble()) {
            double second = this.getSecondValue();
            this.targetSecondValue = second;
            this.displayedSecondValue = second;
        }

        double range = this.getMax() - this.getMin();
        this.width = range == 0.0
            ? 0.0
            : (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (initial - this.getMin()) / range;
    }

    public boolean isDouble() {
        return this.property instanceof FloatProperty && ((FloatProperty)this.property).isDoubleSlider();
    }

    public double getValue() {
        if (this.property instanceof FloatProperty) {
            return ((FloatProperty)this.property).getValue().doubleValue();
        } else if (this.property instanceof IntProperty) {
            return ((IntProperty)this.property).getValue().doubleValue();
        } else if (this.property instanceof PercentProperty) {
            return ((PercentProperty)this.property).getValue().doubleValue();
        } else {
            return this.property instanceof ModeProperty ? ((ModeProperty)this.property).getValue().doubleValue() : 0.0;
        }
    }

    public double getSecondValue() {
        return this.isDouble() ? ((FloatProperty)this.property).getSecondValue().doubleValue() : this.getValue();
    }

    public double getMin() {
        if (this.property instanceof FloatProperty) {
            return ((FloatProperty)this.property).getMinimum().doubleValue();
        } else if (this.property instanceof IntProperty) {
            return ((IntProperty)this.property).getMinimum().doubleValue();
        } else {
            return this.property instanceof PercentProperty
                ? ((PercentProperty)this.property).getMinimum().doubleValue()
                : 0.0;
        }
    }

    public double getMax() {
        if (this.property instanceof FloatProperty) {
            return ((FloatProperty)this.property).getMaximum().doubleValue();
        } else if (this.property instanceof IntProperty) {
            return ((IntProperty)this.property).getMaximum().doubleValue();
        } else if (this.property instanceof PercentProperty) {
            return ((PercentProperty)this.property).getMaximum().doubleValue();
        } else {
            return this.property instanceof ModeProperty ? ((ModeProperty)this.property).getModes().length - 1 : 1.0;
        }
    }

    public void setValue(double newValue) {
        newValue = Math.max(this.getMin(), Math.min(this.getMax(), newValue));
        if (this.isDouble() && newValue > this.getSecondValue()) {
            newValue = this.getSecondValue();
        }

        Object prevValue = this.property.getValue();
        if (this.property instanceof FloatProperty) {
            this.property.setValue((float)newValue);
        } else if (this.property instanceof IntProperty) {
            this.property.setValue((int)Math.round(newValue));
        } else if (this.property instanceof PercentProperty) {
            this.property.setValue((int)Math.round(newValue));
        } else if (this.property instanceof ModeProperty) {
            this.property.setValue((int)Math.round(newValue));
        }

        if (prevValue != null && !prevValue.equals(this.property.getValue())) {
            this.moduleComponent.reloadSettings();
        }
    }

    public void setSecondValue(double newValue) {
        newValue = Math.max(this.getMin(), Math.min(this.getMax(), newValue));
        if (this.isDouble() && newValue < this.getValue()) {
            newValue = this.getValue();
        }

        Object prevValue = this.getSecondValue();
        if (this.isDouble()) {
            ((FloatProperty)this.property).setSecondValue((float)newValue);
        }

        if (prevValue != null && !prevValue.equals(this.getSecondValue())) {
            this.moduleComponent.reloadSettings();
        }
    }

    public boolean isString() {
        return this.property instanceof ModeProperty;
    }

    public String[] getOptions() {
        return this.property instanceof ModeProperty ? ((ModeProperty)this.property).getModes() : null;
    }

    public String getSuffix() {
        return this.property instanceof PercentProperty ? "%" : "";
    }

    @Override
    public void render() {
        if (this.isString()) {
            this.renderModeHeader();
        } else {
            float cx = this.moduleComponent.categoryComponent.getX();
            float cy = this.moduleComponent.categoryComponent.getY() + this.o;
            float cw = this.moduleComponent.categoryComponent.getWidth();
            double input = this.getValue();
            String suffix = this.getSuffix();
            String valueText;
            if (this.property instanceof IntProperty || this.property instanceof PercentProperty) {
                valueText = String.valueOf((int)Math.round(input));
            } else if (this.isDouble()) {
                valueText = String.format("%.1f - %.1f", input, this.getSecondValue());
            } else {
                valueText = String.format("%.2f", input);
            }

            GL11.glPushMatrix();
            GL11.glScaled(0.5, 0.5, 0.5);
            float labelX = (cx + 6.0F + this.xOffset / 2.0F) * 2.0F;
            float labelY = (cy + 4.0F) * 2.0F;
            FontRepository.getClickGuiFont()
                .draw(this.property.getName() + ": " + valueText + suffix, labelX, labelY, -1, true);
            GL11.glPopMatrix();
            float trackLeft = cx + 6.0F + this.xOffset / 2.0F;
            float trackRight = cx + cw - 6.0F + this.xOffset / 2.0F;
            float trackY = cy + 13.0F;
            float trackHeight = 2.5F;
            RenderUtil.drawRoundedRectangle(
                trackLeft, trackY, trackRight, trackY + trackHeight, trackHeight / 2.0F, new Color(40, 40, 40).getRGB()
            );
            double range = this.getMax() - this.getMin();
            double fraction = range == 0.0 ? 0.0 : (this.displayedValue - this.getMin()) / range;
            float actualFillRight = trackLeft + (float)((trackRight - trackLeft) * fraction);
            int accentColor = Color.getHSBColor((float)(System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F)
                .getRGB();
            if (this.isDouble()) {
                double fraction2 = range == 0.0 ? 0.0 : (this.displayedSecondValue - this.getMin()) / range;
                float actualFillRight2 = trackLeft + (float)((trackRight - trackLeft) * fraction2);
                RenderUtil.drawRoundedRectangle(
                    actualFillRight, trackY, actualFillRight2, trackY + trackHeight, trackHeight / 2.0F, accentColor
                );
                float thumbRadius = 2.5F;
                RenderUtil.drawRoundedRectangle(
                    actualFillRight - thumbRadius,
                    trackY + trackHeight / 2.0F - thumbRadius,
                    actualFillRight + thumbRadius,
                    trackY + trackHeight / 2.0F + thumbRadius,
                    thumbRadius,
                    -1
                );
                RenderUtil.drawRoundedRectangle(
                    actualFillRight2 - thumbRadius,
                    trackY + trackHeight / 2.0F - thumbRadius,
                    actualFillRight2 + thumbRadius,
                    trackY + trackHeight / 2.0F + thumbRadius,
                    thumbRadius,
                    -1
                );
            } else {
                RenderUtil.drawRoundedRectangle(
                    trackLeft, trackY, actualFillRight, trackY + trackHeight, trackHeight / 2.0F, accentColor
                );
                float thumbRadius = 2.5F;
                RenderUtil.drawRoundedRectangle(
                    actualFillRight - thumbRadius,
                    trackY + trackHeight / 2.0F - thumbRadius,
                    actualFillRight + thumbRadius,
                    trackY + trackHeight / 2.0F + thumbRadius,
                    thumbRadius,
                    -1
                );
            }
        }
    }

    private void renderModeHeader() {
        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.moduleComponent.categoryComponent.getY() + this.o;
        float cw = this.moduleComponent.categoryComponent.getWidth();
        float left = cx + 4.0F + this.xOffset / 2.0F;
        float top = cy + 1.0F;
        float right = cx + cw - 4.0F + this.xOffset / 2.0F;
        float bottom = top + 14.0F;
        RenderUtil.drawRoundedRectangle(left, top, right, bottom, 4.0F, new Color(22, 22, 28, 185).getRGB());
        Font font = FontRepository.getClickGuiFont();
        String valueText = this.getModeText();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        font.draw(
            this.property.getName(), (left + 5.0F) * 2.0F, (top + 4.0F) * 2.0F, new Color(235, 235, 240).getRGB(), true
        );
        float valueWidth = font.getStringWidth(valueText) / 2.0F;
        font.draw(
            valueText,
            (right - 17.0F - valueWidth) * 2.0F,
            (top + 4.0F) * 2.0F,
            new Color(160, 205, 255).getRGB(),
            true
        );
        GL11.glPopMatrix();
        this.drawArrow(right - 10.0F, top + 7.0F, this.getDropdownProgress());
    }

    public void renderModeDropdownOverlay(int mouseX, int mouseY) {
        if (this.isString()
            && !(this.getDropdownProgress() <= 0.01F)
            && this.moduleComponent.isOpened
            && this.moduleComponent.isVisible(this)) {
            String[] options = this.getOptions();
            if (options != null && options.length != 0) {
                float progress = this.getDropdownProgress();
                float cx = this.moduleComponent.categoryComponent.getX();
                float cy = this.y;
                float cw = this.moduleComponent.categoryComponent.getWidth();
                float left = cx + 4.0F + this.xOffset / 2.0F;
                float top = cy + 1.0F + 14.0F + 2.0F;
                float right = cx + cw - 4.0F + this.xOffset / 2.0F;
                float fullHeight = options.length * 12.0F + 6.0F;
                float shownHeight = fullHeight * progress;
                GL11.glPushMatrix();
                GL11.glDisable(3089);
                RenderUtil.drawRoundedRectangle(
                    left, top, right, top + shownHeight, 5.0F, new Color(15, 15, 22, 235).getRGB()
                );
                RenderUtil.scissorPushGui(left, top, right - left, shownHeight);
                Font font = FontRepository.getClickGuiFont();
                int selected = (int)Math.round(this.getValue());
                int accentColor = Color.getHSBColor(
                        (float)(System.currentTimeMillis() % 11000L) / 11000.0F, 0.65F, 0.95F
                    )
                    .getRGB();

                for (int i = 0; i < options.length; i++) {
                    float rowTop = top + 3.0F + i * 12.0F;
                    float rowBottom = rowTop + 12.0F - 1.0F;
                    boolean hovered = mouseX >= left + 3.0F
                        && mouseX <= right - 3.0F
                        && mouseY >= rowTop
                        && mouseY <= rowBottom;
                    if (hovered || i == selected) {
                        int rowColor = hovered
                            ? new Color(255, 255, 255, 24).getRGB()
                            : new Color(255, 255, 255, 14).getRGB();
                        RenderUtil.drawRoundedRectangle(left + 3.0F, rowTop, right - 3.0F, rowBottom, 3.0F, rowColor);
                    }

                    if (i == selected) {
                        RenderUtil.drawRoundedRectangle(
                            left + 5.0F, rowTop + 3.0F, left + 7.0F, rowBottom - 3.0F, 1.0F, accentColor
                        );
                    }

                    GL11.glPushMatrix();
                    GL11.glScaled(0.5, 0.5, 0.5);
                    int textColor = i == selected
                        ? new Color(230, 245, 255).getRGB()
                        : new Color(205, 205, 214).getRGB();
                    font.draw(options[i], (left + 12.0F) * 2.0F, (rowTop + 3.5F) * 2.0F, textColor, true);
                    GL11.glPopMatrix();
                }

                RenderUtil.scissorPop();
                GL11.glPopMatrix();
            }
        }
    }

    private void drawArrow(float centerX, float centerY, float progress) {
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0.0F);
        GL11.glRotatef(progress * 180.0F, 0.0F, 0.0F, 1.0F);
        RenderUtil.drawRect(-3.0F, -1.0F, 0.0F, 2.0F, new Color(220, 220, 228).getRGB());
        RenderUtil.drawRect(0.0F, -1.0F, 3.0F, 2.0F, new Color(220, 220, 228).getRGB());
        GL11.glPopMatrix();
    }

    private String getModeText() {
        String[] opts = this.getOptions();
        if (opts != null && opts.length != 0) {
            int idx = (int)Math.round(this.getValue());
            idx = Math.max(0, Math.min(idx, opts.length - 1));
            return opts[idx];
        } else {
            return "";
        }
    }

    public float getDropdownProgress() {
        if (this.dropdownTimer != null) {
            if ((float)(System.currentTimeMillis() - this.dropdownTimer.last) >= 210.0F) {
                this.dropdownTimer = null;
                this.dropdownProgress = this.dropdownTargetProgress;
                this.dropdownStartProgress = this.dropdownTargetProgress;
            } else {
                this.dropdownProgress = this.dropdownTimer
                    .getValueFloat(this.dropdownStartProgress, this.dropdownTargetProgress, 1);
                if (this.dropdownProgress == this.dropdownTargetProgress) {
                    this.dropdownTimer = null;
                    this.dropdownStartProgress = this.dropdownTargetProgress;
                }
            }
        }

        return this.dropdownProgress;
    }

    private void setExpanded(boolean expanded) {
        float currentProgress = this.getDropdownProgress();
        this.dropdownStartProgress = currentProgress;
        this.isExpanded = expanded;
        this.dropdownTargetProgress = expanded ? 1.0F : 0.0F;
        (this.dropdownTimer = new AnimationTimer(180.0F)).start();
    }

    public boolean isModeDropdownActive() {
        return this.isString() && (this.isExpanded || this.getDropdownProgress() > 0.01F);
    }

    public boolean isMouseOverModeDropdown(int mouseX, int mouseY) {
        if (!this.isString()) {
            return false;
        }

        String[] options = this.getOptions();
        if (options == null) {
            return false;
        }

        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.y;
        float cw = this.moduleComponent.categoryComponent.getWidth();
        float left = cx + 4.0F + this.xOffset / 2.0F;
        float top = cy + 1.0F + 14.0F + 2.0F;
        float right = cx + cw - 4.0F + this.xOffset / 2.0F;
        float bottom = top + options.length * 12.0F + 6.0F;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    public void collapseModeDropdown() {
        if (this.isString() && this.isExpanded) {
            this.setExpanded(false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
        if (this.isString()) {
            this.getDropdownProgress();
        } else {
            if (this.heldDown || this.draggingMin || this.draggingMax) {
                float trackLeft = this.x + 6.0F + this.xOffset / 2.0F;
                float trackRight = this.x
                    + this.moduleComponent.categoryComponent.getWidth()
                    - 6.0F
                    + this.xOffset / 2.0F;
                float trackWidth = trackRight - trackLeft;
                double d = Math.min(trackWidth, Math.max(0.0F, mouseX - trackLeft));
                double range = this.getMax() - this.getMin();
                double n = this.getMin() + d / trackWidth * range;
                if (this.isDouble()) {
                    if (this.draggingMin) {
                        this.targetValue = roundToInterval(n, 4);
                        this.displayedValue = this.displayedValue + (this.targetValue - this.displayedValue) * 0.6;
                        this.setValue(this.targetValue);
                    } else if (this.draggingMax) {
                        this.targetSecondValue = roundToInterval(n, 4);
                        this.displayedSecondValue = this.displayedSecondValue
                            + (this.targetSecondValue - this.displayedSecondValue) * 0.6;
                        this.setSecondValue(this.targetSecondValue);
                    }
                } else {
                    this.targetValue = roundToInterval(n, 4);
                    this.displayedValue = this.displayedValue + (this.targetValue - this.displayedValue) * 0.6;
                    this.setValue(this.targetValue);
                    if (range == 0.0) {
                        this.width = 0.0;
                    } else {
                        double fraction = (this.displayedValue - this.getMin()) / range;
                        this.width = (this.moduleComponent.categoryComponent.getWidth() - 12.0F) * fraction;
                    }
                }
            }
        }
    }

    public void onSliderChange() {
        double initial = this.getValue();
        this.targetValue = initial;
        this.displayedValue = initial;
        if (this.isDouble()) {
            double second = this.getSecondValue();
            this.targetSecondValue = second;
            this.displayedSecondValue = second;
        }

        double range = this.getMax() - this.getMin();
        this.width = range == 0.0
            ? 0.0
            : (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (initial - this.getMin()) / range;
    }

    private static double roundToInterval(double value, int places) {
        if (places < 0) {
            return 0.0;
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.isString()) {
            return this.onModeClick(mouseX, mouseY, button);
        }

        if ((this.u(mouseX, mouseY) || this.i(mouseX, mouseY))
            && button == 0
            && this.moduleComponent.isOpened
            && this.moduleComponent.isVisible(this)) {
            if (this.isDouble()) {
                float cx = this.moduleComponent.categoryComponent.getX();
                float cw = this.moduleComponent.categoryComponent.getWidth();
                float trackLeft = cx + 6.0F + this.xOffset / 2.0F;
                float trackRight = cx + cw - 6.0F + this.xOffset / 2.0F;
                double range = this.getMax() - this.getMin();
                double fraction1 = range == 0.0 ? 0.0 : (this.getValue() - this.getMin()) / range;
                double fraction2 = range == 0.0 ? 0.0 : (this.getSecondValue() - this.getMin()) / range;
                float thumb1X = trackLeft + (float)((trackRight - trackLeft) * fraction1);
                float thumb2X = trackLeft + (float)((trackRight - trackLeft) * fraction2);
                if (Math.abs(mouseX - thumb1X) < Math.abs(mouseX - thumb2X)) {
                    this.draggingMin = true;
                } else if (Math.abs(mouseX - thumb1X) > Math.abs(mouseX - thumb2X)) {
                    this.draggingMax = true;
                } else {
                    if (Math.abs(this.getValue() - this.getMax()) < 0.01
                        && Math.abs(this.getSecondValue() - this.getMax()) < 0.01) {
                        boolean bothAtMax = true;
                    } else {
                        boolean bothAtMax = false;
                    }

                    boolean bothAtMin = Math.abs(this.getValue() - this.getMin()) < 0.01
                        && Math.abs(this.getSecondValue() - this.getMin()) < 0.01;
                    if (bothAtMin) {
                        this.draggingMax = true;
                    } else {
                        this.draggingMin = true;
                    }
                }
            } else {
                this.heldDown = true;
            }
        }

        return false;
    }

    private boolean onModeClick(int mouseX, int mouseY, int button) {
        if (button == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            float cw = this.moduleComponent.categoryComponent.getWidth();
            boolean overHeader = mouseX > this.x + 4.0F + this.xOffset / 2.0F
                && mouseX < this.x + cw - 4.0F + this.xOffset / 2.0F
                && mouseY > this.y + 1.0F
                && mouseY < this.y + 1.0F + 14.0F;
            if (overHeader) {
                this.setExpanded(!this.isExpanded);
                return true;
            }

            if (this.isModeDropdownActive() && this.isMouseOverModeDropdown(mouseX, mouseY)) {
                String[] options = this.getOptions();
                if (options != null) {
                    float cx = this.moduleComponent.categoryComponent.getX();
                    float top = this.y + 1.0F + 14.0F + 2.0F;
                    int optionIndex = (int)((mouseY - top - 3.0F) / 12.0F);
                    if (optionIndex >= 0 && optionIndex < options.length) {
                        this.property.setValue(optionIndex);
                        this.setExpanded(false);
                        this.moduleComponent.reloadSettings();
                    }

                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.heldDown = false;
        this.draggingMin = false;
        this.draggingMax = false;
    }

    public boolean u(int mouseX, int mouseY) {
        return mouseX > this.x
            && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F + 1.0F
            && mouseY > this.y
            && mouseY < this.y + 16.0F;
    }

    public boolean i(int mouseX, int mouseY) {
        return mouseX > this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F
            && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth()
            && mouseY > this.y
            && mouseY < this.y + 16.0F;
    }

    @Override
    public void onGuiClosed() {
        this.heldDown = false;
        this.draggingMin = false;
        this.draggingMax = false;
        this.isExpanded = false;
        this.dropdownTimer = null;
        this.dropdownProgress = 0.0F;
        this.dropdownStartProgress = 0.0F;
        this.dropdownTargetProgress = 0.0F;
    }

    public void restoreModeDropdownState(boolean expanded) {
        this.isExpanded = expanded;
        this.dropdownTimer = null;
        this.dropdownProgress = expanded ? 1.0F : 0.0F;
        this.dropdownStartProgress = this.dropdownProgress;
        this.dropdownTargetProgress = this.dropdownProgress;
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
}
