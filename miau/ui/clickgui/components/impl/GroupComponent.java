package miau.ui.clickgui.components.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import miau.property.properties.BooleanProperty;
import miau.ui.clickgui.components.Component;
import miau.util.animation.AnimationTimer;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import org.lwjgl.opengl.GL11;

public class GroupComponent extends Component {
    public static final float GROUP_HEADER_HEIGHT = 14.0F;
    private static final float SUB_PROP_HEIGHT = 12.0F;
    private static final long ANIMATION_DURATION = 180L;
    private final String groupName;
    private final ModuleComponent moduleComponent;
    private final List<BooleanProperty> subProperties;
    public float o;
    public float x;
    private float y;
    public float xOffset;
    private boolean expanded = false;
    private AnimationTimer smoothTimer;
    private float animationProgress = 0.0F;
    private float animationStart = 0.0F;
    private float animationTarget = 0.0F;

    public String getGroupName() {
        return this.groupName;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public GroupComponent(
        String groupName, ModuleComponent moduleComponent, float o, List<BooleanProperty> subProperties
    ) {
        this.groupName = groupName;
        this.moduleComponent = moduleComponent;
        this.o = o;
        this.subProperties = new ArrayList<>(subProperties);
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
    }

    public int getSubCount() {
        return this.subProperties.size();
    }

    public float getFullHeight() {
        return 14.0F + this.subProperties.size() * 12.0F;
    }

    public float getAnimationProgress() {
        if (this.smoothTimer != null) {
            if (System.currentTimeMillis() - this.smoothTimer.last >= 210L) {
                this.smoothTimer = null;
                this.animationProgress = this.animationTarget;
                this.animationStart = this.animationTarget;
            } else {
                this.animationProgress = this.smoothTimer.getValueFloat(this.animationStart, this.animationTarget, 1);
                if (this.animationProgress == this.animationTarget) {
                    this.smoothTimer = null;
                    this.animationStart = this.animationTarget;
                }
            }
        }

        return this.animationProgress;
    }

    public void setExpanded(boolean expanded) {
        float current = this.getAnimationProgress();
        this.animationStart = current;
        this.expanded = expanded;
        this.animationTarget = expanded ? 1.0F : 0.0F;
        (this.smoothTimer = new AnimationTimer(180.0F)).start();
        this.moduleComponent.updateSettingPositions();
    }

    public void restoreExpandedState(boolean expanded) {
        this.expanded = expanded;
        this.smoothTimer = null;
        this.animationProgress = expanded ? 1.0F : 0.0F;
        this.animationStart = this.animationProgress;
        this.animationTarget = this.animationProgress;
    }

    private void toggleExpanded() {
        this.setExpanded(!this.expanded);
    }

    @Override
    public void render() {
        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.moduleComponent.categoryComponent.getY() + this.o;
        float cw = this.moduleComponent.categoryComponent.getWidth();
        float left = cx + 4.0F + this.xOffset / 2.0F;
        float top = cy + 1.0F;
        float right = cx + cw - 4.0F + this.xOffset / 2.0F;
        float bottom = top + 14.0F;
        RenderUtil.drawRoundedRectangle(left, top, right, bottom, 4.0F, new Color(22, 22, 28, 185).getRGB());
        Font font = FontRepository.getClickGuiFont();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        font.draw(this.groupName, (left + 5.0F) * 2.0F, (top + 4.0F) * 2.0F, new Color(200, 200, 215).getRGB(), true);
        GL11.glPopMatrix();
        float progress = this.getAnimationProgress();
        float arrowCenterX = right - 10.0F;
        float arrowCenterY = top + 7.0F;
        GL11.glPushMatrix();
        GL11.glTranslatef(arrowCenterX, arrowCenterY, 0.0F);
        GL11.glRotatef(progress * 180.0F, 0.0F, 0.0F, 1.0F);
        RenderUtil.drawRect(-3.0F, -1.0F, 0.0F, 2.0F, new Color(220, 220, 228).getRGB());
        RenderUtil.drawRect(0.0F, -1.0F, 3.0F, 2.0F, new Color(220, 220, 228).getRGB());
        GL11.glPopMatrix();
        if (progress > 0.01F) {
            float subTop = top + 14.0F;
            float subFullHeight = this.subProperties.size() * 12.0F;
            float shownHeight = subFullHeight * progress;
            RenderUtil.drawRoundedRectangle(
                left, subTop, right, subTop + shownHeight, 4.0F, new Color(15, 15, 22, 235).getRGB()
            );
            RenderUtil.scissorPushGui(left, subTop, right - left, shownHeight);

            for (int i = 0; i < this.subProperties.size(); i++) {
                BooleanProperty prop = this.subProperties.get(i);
                if (prop.isVisible()) {
                    float rowTop = subTop + i * 12.0F;
                    String propName = prop.getName().replace("target-", "");
                    GL11.glPushMatrix();
                    GL11.glScaled(0.5, 0.5, 0.5);
                    font.draw(
                        propName, (left + 8.0F) * 2.0F, (rowTop + 3.0F) * 2.0F, new Color(210, 210, 220).getRGB(), true
                    );
                    GL11.glPopMatrix();
                    boolean enabled = prop.getValue();
                    float switchW = 16.0F;
                    float switchH = 8.0F;
                    float switchX = right - switchW - 6.0F + this.xOffset / 2.0F;
                    float switchY = rowTop + 2.0F;
                    Color c1 = new Color(40, 40, 40);
                    Color c2 = new Color(20, 255, 0);
                    float toggleAnim = enabled ? 1.0F : 0.0F;
                    int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * toggleAnim);
                    int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * toggleAnim);
                    int bl = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * toggleAnim);
                    int bgColor = new Color(r, g, bl).getRGB();
                    RenderUtil.drawRoundedRectangle(
                        switchX, switchY, switchX + switchW, switchY + switchH, switchH / 2.0F, bgColor
                    );
                    float circleR = switchH / 2.0F - 1.0F;
                    float minCircleX = switchX + circleR + 1.0F;
                    float maxCircleX = switchX + switchW - circleR - 1.0F;
                    float circleX = minCircleX + (maxCircleX - minCircleX) * toggleAnim;
                    float circleY = switchY + switchH / 2.0F;
                    RenderUtil.drawRoundedRectangle(
                        circleX - circleR, circleY - circleR, circleX + circleR, circleY + circleR, circleR, -1
                    );
                }
            }

            RenderUtil.scissorPop();
        }
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int button) {
        if (button == 0 && this.moduleComponent.isOpened) {
            float cx = this.moduleComponent.categoryComponent.getX();
            float cy = this.moduleComponent.categoryComponent.getModuleY() + this.o;
            float cw = this.moduleComponent.categoryComponent.getWidth();
            float left = cx + 4.0F + this.xOffset / 2.0F;
            float top = cy + 1.0F;
            float right = cx + cw - 4.0F + this.xOffset / 2.0F;
            boolean overHeader = x > left && x < right && y > top && y < top + 14.0F;
            if (overHeader) {
                this.toggleExpanded();
                return true;
            }

            float progress = this.getAnimationProgress();
            if (progress > 0.01F) {
                float subTop = top + 14.0F;

                for (int i = 0; i < this.subProperties.size(); i++) {
                    BooleanProperty prop = this.subProperties.get(i);
                    if (prop.isVisible()) {
                        float rowTop = subTop + i * 12.0F;
                        float rowBottom = rowTop + 12.0F;
                        if (x > left && x < right && y > rowTop && y < rowBottom) {
                            prop.setValue(!prop.getValue());
                            this.moduleComponent.reloadSettings();
                            return true;
                        }
                    }
                }
            }

            return false;
        } else {
            return false;
        }
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
        for (BooleanProperty prop : this.subProperties) {
            if (prop.isVisible()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onGuiClosed() {
        this.expanded = false;
        this.smoothTimer = null;
        this.animationProgress = 0.0F;
        this.animationStart = 0.0F;
        this.animationTarget = 0.0F;
    }
}
