package miau.module.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class SlinkNotifs extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final String blacklistedModules = "InvMove";
    private final List<SlinkNotifs.Notification> notifications = new ArrayList<>();
    private final Map<String, Boolean> lastStates = new HashMap<>();
    private final String[] themeOptions = new String[]{
        "Default",
        "Rainbow",
        "Aurora",
        "Cherry",
        "Cotton Candy",
        "Flare",
        "Flower",
        "Forest",
        "Frost",
        "Gold",
        "Grayscale",
        "Inferno",
        "Royal",
        "Sandstorm",
        "Sky",
        "Vine"
    };
    private final String[] disableThemeOptions = new String[]{
        "Disabled",
        "Rainbow",
        "Aurora",
        "Cherry",
        "Cotton Candy",
        "Flare",
        "Flower",
        "Forest",
        "Frost",
        "Gold",
        "Grayscale",
        "Inferno",
        "Royal",
        "Sandstorm",
        "Sky",
        "Vine"
    };
    private boolean mouseDown = false;
    private boolean lastMouseDown = false;
    private float mouseX = 0.0F;
    private float mouseY = 0.0F;
    private boolean notificationDragging = false;
    private float notificationDragX = 0.0F;
    private float notificationDragY = 0.0F;
    private long closeMs = 230L;
    private long lastEditPositionWarningMs = 0L;
    public final BooleanProperty startWithFont = new BooleanProperty("Start with {f}", true);
    public final BooleanProperty syncHud2 = new BooleanProperty("Sync hud2", true);
    public final ModeProperty theme = new ModeProperty("Theme", 0, this.themeOptions);
    public final ModeProperty disableTheme = new ModeProperty("Disable theme", 0, this.disableThemeOptions);
    public final IntProperty duration = new IntProperty("Duration", 3000, 1000, 7000);
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 2.0F);
    public final ModeProperty position = new ModeProperty(
        "Position", 0, new String[]{"Bottom Right", "Bottom Left", "Top Right", "Top Left"}
    );
    public final BooleanProperty editPosition = new BooleanProperty("Edit position", false);
    public final FloatProperty brOffsetX = new FloatProperty("BR X", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty brOffsetY = new FloatProperty("BR Y", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty blOffsetX = new FloatProperty("BL X", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty blOffsetY = new FloatProperty("BL Y", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty trOffsetX = new FloatProperty("TR X", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty trOffsetY = new FloatProperty("TR Y", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty tlOffsetX = new FloatProperty("TL X", 12.0F, 0.0F, 10000.0F);
    public final FloatProperty tlOffsetY = new FloatProperty("TL Y", 12.0F, 0.0F, 10000.0F);

    public SlinkNotifs() {
        super("SlinkNotifs", false, true);
    }

    @Override
    public void onEnabled() {
        this.notifications.clear();
        this.notificationDragging = false;
        this.lastMouseDown = false;
        this.mouseDown = false;
        this.lastEditPositionWarningMs = 0L;
        this.syncModuleStates();
    }

    @Override
    public void onDisabled() {
        this.notifications.clear();
        this.lastStates.clear();
        this.notificationDragging = false;
    }

    @EventTarget
    public void onRenderTick(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            this.checkModuleChanges();
            this.renderNotifications();
        }
    }

    private void checkModuleChanges() {
        this.printEditPositionWarningIfNeeded();
        int enabledCount = 0;
        int disabledCount = 0;
        String enabledName = "";
        String disabledName = "";

        for (Module module : Miau.moduleManager.modules.values()) {
            String moduleName = module.getName();
            if (moduleName != null && !moduleName.equalsIgnoreCase(this.getName())) {
                if (this.shouldIgnoreModule(moduleName)) {
                    this.lastStates.remove(moduleName);
                } else {
                    boolean current = module.isEnabled();
                    Boolean old = this.lastStates.get(moduleName);
                    if (old == null) {
                        this.lastStates.put(moduleName, current);
                    } else if (old != current) {
                        this.lastStates.put(moduleName, current);
                        if (current) {
                            if (++enabledCount == 1) {
                                enabledName = moduleName;
                            }
                        } else if (++disabledCount == 1) {
                            disabledName = moduleName;
                        }
                    }
                }
            }
        }

        if (enabledCount > 0) {
            this.pushNotification(this.buildMessage(enabledName, enabledCount, true), true);
        }

        if (disabledCount > 0) {
            this.pushNotification(this.buildMessage(disabledName, disabledCount, false), false);
        }
    }

    private void syncModuleStates() {
        this.lastStates.clear();

        for (Module module : Miau.moduleManager.modules.values()) {
            String moduleName = module.getName();
            if (moduleName != null
                && !moduleName.equalsIgnoreCase(this.getName())
                && !this.shouldIgnoreModule(moduleName)) {
                this.lastStates.put(moduleName, module.isEnabled());
            }
        }
    }

    private String buildMessage(String firstName, int count, boolean enabled) {
        String prefix = enabled ? "Enabled " : "Disabled ";
        return count == 1 ? prefix + this.aliasFor(firstName) : prefix + count + " mods";
    }

    private String aliasFor(String moduleName) {
        return !this.syncHud2.getValue() ? moduleName : moduleName;
    }

    private void pushNotification(String text, boolean enabled) {
        SlinkNotifs.Notification notification = new SlinkNotifs.Notification();
        notification.text = text;
        notification.enabled = enabled;
        notification.created = System.currentTimeMillis();
        this.notifications.add(0, notification);
        int openCount = 0;

        for (SlinkNotifs.Notification n : this.notifications) {
            if (!this.isNotificationClosing(n)) {
                openCount++;
            }
        }

        for (int i = this.notifications.size() - 1; openCount > 7 && i >= 0; i--) {
            SlinkNotifs.Notification oldest = this.notifications.get(i);
            if (!this.isNotificationClosing(oldest)) {
                this.startNotificationClose(oldest);
                openCount--;
            }
        }
    }

    private void updateMouse(ScaledResolution sr) {
        this.lastMouseDown = this.mouseDown;
        this.mouseDown = Mouse.isButtonDown(0);
        float guiScale = sr.func_78325_e();
        if (guiScale <= 0.0F) {
            guiScale = 1.0F;
        }

        float mpX = Mouse.getX();
        float mpY = Mouse.getY();
        this.mouseX = mpX / guiScale;
        this.mouseY = sr.func_78328_b() - mpY / guiScale;
    }

    private void renderNotifications() {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.func_78326_a();
        int screenHeight = sr.func_78328_b();
        long now = System.currentTimeMillis();
        boolean chatOpen = mc.field_71462_r instanceof GuiChat;
        boolean editPos = this.editPosition.getValue();
        boolean previewMode = chatOpen && editPos;
        if (previewMode) {
            this.updateMouse(sr);
        } else {
            this.notificationDragging = false;
            this.lastMouseDown = false;
            this.mouseDown = false;
        }

        if (!this.notifications.isEmpty()) {
            long durationMs = this.duration.getValue().intValue();

            for (int i = this.notifications.size() - 1; i >= 0; i--) {
                SlinkNotifs.Notification notification = this.notifications.get(i);
                if (this.isNotificationClosing(notification)) {
                    if (now - notification.closingAt > this.closeMs) {
                        this.notifications.remove(i);
                    }
                } else if (now - notification.created > durationMs) {
                    this.startNotificationClose(notification);
                }
            }
        }

        if (this.notifications.isEmpty() && !previewMode) {
            this.notificationDragging = false;
        } else {
            float uiScale = this.scale.getValue();
            boolean fontPrefix = this.useFontPrefix();
            float notifScale = 0.78F * uiScale;
            float tagScale = 0.7F * uiScale;
            float padding = 5.0F * uiScale;
            float height = 18.5F * uiScale;
            float tagHeight = 11.5F * uiScale;
            float spacing = height + 3.0F * uiScale;
            int corner = this.position.getValue();
            boolean right = corner == 0 || corner == 2;
            boolean bottom = corner == 0 || corner == 1;
            float qX1 = right ? screenWidth / 2.0F : 0.0F;
            float qX2 = right ? screenWidth : screenWidth / 2.0F;
            float qY1 = bottom ? screenHeight / 2.0F : 0.0F;
            float qY2 = bottom ? screenHeight : screenHeight / 2.0F;
            if (chatOpen && editPos) {
                RenderUtil.drawRect(qX1, qY1, qX2, qY2, 570425344);
                this.drawRectOutline(qX1 + 1.0F, qY1 + 1.0F, qX2 - 1.0F, qY2 - 1.0F, 1.0F, -1996488705);
            }

            int count = previewMode ? 1 : this.notifications.size();
            float stackHeight = height + (count - 1) * spacing;
            String tag = Miau.clientName;
            String tagText = this.fontText(tag, fontPrefix);
            float tagTextWidth = this.getFontWidth(tagText, fontPrefix);
            float tagFontHeight = this.getFontHeight(fontPrefix) * tagScale;
            float tagWidth = tagTextWidth * tagScale + 12.0F * uiScale;
            float tagRadius = Math.max(3.0F * uiScale, tagHeight * 0.34F);
            float innerGap = 2.5F * uiScale;
            float rightGap = 8.0F * uiScale;

            for (int i = 0; i < count; i++) {
                SlinkNotifs.Notification notification = null;
                String text = "Enabled Player ESP";
                boolean enabledNotification = true;
                long created = now;
                if (!previewMode) {
                    notification = this.notifications.get(i);
                    text = notification.text;
                    enabledNotification = notification.enabled;
                    created = notification.created;
                }

                long age = now - created;
                float inAnim = previewMode ? 1.0F : this.clamp((float)age / 210.0F, 0.0F, 1.0F);
                float outAnim = !previewMode && this.isNotificationClosing(notification)
                    ? 1.0F - this.clamp((float)(now - notification.closingAt) / (float)this.closeMs, 0.0F, 1.0F)
                    : 1.0F;
                float anim = this.easeOutCubic(Math.min(inAnim, outAnim));
                String messageText = this.fontText(text, fontPrefix);
                float messageScale = 0.6F * uiScale;
                float messageFontHeight = this.getFontHeight(fontPrefix) * messageScale;
                float messageTextWidth = this.getFontWidth(messageText, fontPrefix);
                float messageWidth = messageTextWidth * messageScale;
                float width = padding + tagWidth + innerGap + messageWidth + rightGap;
                float xOffset = this.clamp(
                    this.getNotificationOffsetX(corner), 2.0F, Math.max(2.0F, qX2 - qX1 - width - 2.0F)
                );
                float yOffset = this.clamp(
                    this.getNotificationOffsetY(corner), 2.0F, Math.max(2.0F, qY2 - qY1 - stackHeight - 2.0F)
                );
                float shownX = right ? qX2 - width - xOffset : qX1 + xOffset;
                float hiddenX = right ? screenWidth + width + 8.0F : -width - 8.0F;
                float x = hiddenX + (shownX - hiddenX) * anim;
                float baseY = bottom ? qY2 - yOffset - height : qY1 + yOffset;
                float y = bottom ? baseY - i * spacing : baseY + i * spacing;
                if (i == 0) {
                    this.updateNotificationDrag(
                        chatOpen && editPos, corner, qX1, qX2, qY1, qY2, shownX, baseY, width, height, stackHeight
                    );
                    xOffset = this.clamp(
                        this.getNotificationOffsetX(corner), 2.0F, Math.max(2.0F, qX2 - qX1 - width - 2.0F)
                    );
                    yOffset = this.clamp(
                        this.getNotificationOffsetY(corner), 2.0F, Math.max(2.0F, qY2 - qY1 - stackHeight - 2.0F)
                    );
                    shownX = right ? qX2 - width - xOffset : qX1 + xOffset;
                    x = hiddenX + (shownX - hiddenX) * anim;
                    baseY = bottom ? qY2 - yOffset - height : qY1 + yOffset;
                    y = baseY;
                }

                float tagX = x + padding;
                float tagY = y + (height - tagHeight) / 2.0F;
                float textX = tagX + tagWidth + innerGap;
                float textY = y + (height - messageFontHeight) / 2.0F + 0.45F * uiScale;
                float tagTextX = tagX + (tagWidth - tagTextWidth * tagScale) / 2.0F;
                float tagTextY = tagY + (tagHeight - tagFontHeight) / 2.0F;
                int accent = this.getThemeColor(enabledNotification ? this.resolveTheme() : this.resolveDisableTheme());
                int pillColor = this.getPillColor(accent);
                this.renderNotificationBackground(x, y, width, height, height / 2.0F, anim);
                RenderUtil.drawRoundedRectangle(
                    tagX, tagY, tagX + tagWidth, tagY + tagHeight, tagRadius, this.multiplyAlpha(pillColor, anim)
                );
                this.drawScaledText(tagText, tagTextX, tagTextY, tagScale, this.multiplyAlpha(accent, anim), fontPrefix);
                this.drawScaledText(messageText, textX, textY, messageScale, this.multiplyAlpha(-1, anim), fontPrefix);
            }
        }
    }

    private void renderNotificationBackground(float x, float y, float width, float height, float radius, float anim) {
        RenderUtil.drawRoundedRectangle(x, y, x + width, y + height, radius, this.multiplyAlpha(-267909110, anim));
    }

    private boolean isNotificationClosing(SlinkNotifs.Notification notification) {
        return notification != null && notification.closingAt != -1L;
    }

    private void startNotificationClose(SlinkNotifs.Notification notification) {
        if (notification != null && !this.isNotificationClosing(notification)) {
            notification.closingAt = System.currentTimeMillis();
        }
    }

    private String resolveTheme() {
        int idx = this.theme.getValue();
        if (idx == 0) {
            return "default";
        } else {
            return idx >= 1 && idx < this.themeOptions.length ? this.themeOptions[idx] : "white";
        }
    }

    private String resolveDisableTheme() {
        int idx = this.disableTheme.getValue();
        if (idx <= 0) {
            return this.resolveTheme();
        } else {
            return idx < this.disableThemeOptions.length ? this.disableThemeOptions[idx] : this.resolveTheme();
        }
    }

    private int getThemeColor(String name) {
        String lo = name == null ? "white" : name.toLowerCase().trim();
        if (lo.equals("default")) {
            Color c = Themes.getCurrentTheme().getFirstColor();
            return this.withAlpha(c.getRed() << 16 | c.getGreen() << 8 | c.getBlue(), 255);
        } else if (lo.equals("rainbow")) {
            return this.getRainbowColor();
        } else {
            double p = this.getWaveRatio();
            if (lo.equals("aurora")) {
                return this.lerpColor(-9240126, -15208271, p);
            } else if (lo.equals("cherry")) {
                return this.lerpColor(-2278039, -2051145, p);
            } else if (lo.equals("cotton candy")) {
                return this.lerpColor(-7152920, -1218376, p);
            } else if (lo.equals("flare")) {
                return this.lerpColor(-890090, -1792483, p);
            } else if (lo.equals("flower")) {
                return this.lerpColor(-3630376, -5482055, p);
            } else if (lo.equals("forest")) {
                return this.lerpColor(-14715369, -10443229, p);
            } else if (lo.equals("frost")) {
                return this.lerpColor(-2104349, -4405814, p);
            } else if (lo.equals("gold")) {
                return this.lerpColor(-1712336, -2434378, p);
            } else if (lo.equals("grayscale")) {
                return this.lerpColor(-10394776, -1578774, p);
            } else if (lo.equals("inferno")) {
                return this.lerpColor(-13303808, -4179694, p);
            } else if (lo.equals("royal")) {
                return this.lerpColor(-8011800, -14860921, p);
            } else if (lo.equals("sandstorm")) {
                return this.lerpColor(-6450327, -662604, p);
            } else if (lo.equals("sky")) {
                return this.lerpColor(-8262920, -15352621, p);
            } else {
                return lo.equals("vine") ? this.lerpColor(-14162887, -6621023, p) : -38374;
            }
        }
    }

    private int getRainbowColor() {
        long now = System.currentTimeMillis();
        float hue = (float)(now % 5000L) / 5000.0F;
        return Color.HSBtoRGB(hue, 1.0F, 1.0F);
    }

    private int getPillColor(int accent) {
        int r = accent >> 16 & 0xFF;
        int g = accent >> 8 & 0xFF;
        int b = accent & 0xFF;
        int darkR = this.clampInt((int)(r * 0.18), 0, 255);
        int darkG = this.clampInt((int)(g * 0.21), 0, 255);
        int darkB = this.clampInt((int)(b * 0.42), 0, 255);
        return this.withAlpha(darkR << 16 | darkG << 8 | darkB, 85);
    }

    private double getWaveRatio() {
        long now = System.currentTimeMillis();
        float time = (float)(now % 5000L) / 5000.0F;
        return time <= 0.5F ? time * 2.0 : 2.0 - time * 2.0;
    }

    private int lerpColor(int c1, int c2, double t) {
        int r = this.clampInt((int)((c1 >> 16 & 0xFF) + ((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * t), 0, 255);
        int g = this.clampInt((int)((c1 >> 8 & 0xFF) + ((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * t), 0, 255);
        int b = this.clampInt((int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t), 0, 255);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    private boolean shouldIgnoreModule(String moduleName) {
        if (moduleName != null && "InvMove" != null && !"InvMove".isEmpty()) {
            String[] parts = "InvMove".split(",");

            for (String part : parts) {
                if (part.trim().equalsIgnoreCase(moduleName)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean isEditingPosition() {
        return this.editPosition.getValue();
    }

    private void printEditPositionWarningIfNeeded() {
        if (!this.isEditingPosition()) {
            this.lastEditPositionWarningMs = 0L;
        } else {
            long now = System.currentTimeMillis();
            if (this.lastEditPositionWarningMs == 0L || now - this.lastEditPositionWarningMs >= 5000L) {
                this.lastEditPositionWarningMs = now;
                ChatUtil.display("&7[&dR&7] &b%ss&7: &c\"Edit position\" is enabled&7.", this.getName());
            }
        }
    }

    private float getNotificationOffsetX(int corner) {
        if (corner == 0) {
            return this.brOffsetX.getValue();
        } else if (corner == 1) {
            return this.blOffsetX.getValue();
        } else {
            return corner == 2 ? this.trOffsetX.getValue() : this.tlOffsetX.getValue();
        }
    }

    private float getNotificationOffsetY(int corner) {
        if (corner == 0) {
            return this.brOffsetY.getValue();
        } else if (corner == 1) {
            return this.blOffsetY.getValue();
        } else {
            return corner == 2 ? this.trOffsetY.getValue() : this.tlOffsetY.getValue();
        }
    }

    private void setNotificationOffsets(int corner, float x, float y) {
        if (corner == 0) {
            this.brOffsetX.setValue(x);
            this.brOffsetY.setValue(y);
        } else if (corner == 1) {
            this.blOffsetX.setValue(x);
            this.blOffsetY.setValue(y);
        } else if (corner == 2) {
            this.trOffsetX.setValue(x);
            this.trOffsetY.setValue(y);
        } else {
            this.tlOffsetX.setValue(x);
            this.tlOffsetY.setValue(y);
        }
    }

    private void updateNotificationDrag(
        boolean chatOpen,
        int corner,
        float qX1,
        float qX2,
        float qY1,
        float qY2,
        float shownX,
        float shownY,
        float width,
        float height,
        float stackHeight
    ) {
        if (chatOpen && this.mouseDown) {
            boolean right = corner == 0 || corner == 2;
            boolean bottom = corner == 0 || corner == 1;
            if (!this.notificationDragging
                && this.mouseDown
                && !this.lastMouseDown
                && this.isMouseInside(shownX, shownY, shownX + width, shownY + height)) {
                this.notificationDragging = true;
                this.notificationDragX = this.mouseX - shownX;
                this.notificationDragY = this.mouseY - shownY;
            }

            if (this.notificationDragging) {
                float targetX = this.clamp(this.mouseX - this.notificationDragX, qX1 + 2.0F, qX2 - width - 2.0F);
                float targetY;
                if (bottom) {
                    targetY = this.clamp(
                        this.mouseY - this.notificationDragY, qY1 + stackHeight - height + 2.0F, qY2 - height - 2.0F
                    );
                } else {
                    targetY = this.clamp(this.mouseY - this.notificationDragY, qY1 + 2.0F, qY2 - stackHeight - 2.0F);
                }

                float nextXOffset = right ? qX2 - width - targetX : targetX - qX1;
                float nextYOffset = bottom ? qY2 - height - targetY : targetY - qY1;
                nextXOffset = this.clamp(nextXOffset, 2.0F, Math.max(2.0F, qX2 - qX1 - width - 2.0F));
                nextYOffset = this.clamp(nextYOffset, 2.0F, Math.max(2.0F, qY2 - qY1 - stackHeight - 2.0F));
                this.setNotificationOffsets(corner, nextXOffset, nextYOffset);
            }
        } else {
            this.notificationDragging = false;
        }
    }

    private boolean isMouseInside(float x1, float y1, float x2, float y2) {
        return this.mouseX >= x1 && this.mouseX <= x2 && this.mouseY >= y1 && this.mouseY <= y2;
    }

    private void drawRectOutline(float x1, float y1, float x2, float y2, float thickness, int color) {
        RenderUtil.drawRect(x1, y1, x2, y1 + thickness, color);
        RenderUtil.drawRect(x1, y2 - thickness, x2, y2, color);
        RenderUtil.drawRect(x1, y1, x1 + thickness, y2, color);
        RenderUtil.drawRect(x2 - thickness, y1, x2, y2, color);
    }

    private boolean useFontPrefix() {
        return this.startWithFont.getValue();
    }

    private String fontText(String text, boolean usePrefix) {
        return text == null ? "" : text;
    }

    private Font getFont() {
        return FontRepository.getHudFont(18);
    }

    private float getFontWidth(String text, boolean custom) {
        return custom ? this.getFont().width(text) : mc.field_71466_p.func_78256_a(text);
    }

    private float getFontHeight(boolean custom) {
        return custom ? this.getFont().height() : mc.field_71466_p.field_78288_b;
    }

    private void drawScaledText(String text, float x, float y, float scale, int color, boolean custom) {
        RenderUtil.scaleStart(x, y, scale);
        if (custom) {
            this.getFont().drawWithShadow(text, x, y, color);
        } else {
            mc.field_71466_p.func_175063_a(text, x, y, color);
        }

        RenderUtil.scaleEnd();
    }

    private int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | color & 16777215;
    }

    private int multiplyAlpha(int color, float alpha) {
        int currentAlpha = color >> 24 & 0xFF;
        int nextAlpha = (int)(currentAlpha * this.clamp(alpha, 0.0F, 1.0F));
        return this.withAlpha(color, nextAlpha);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float easeOutCubic(float t) {
        float value = this.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - value;
        return 1.0F - inv * inv * inv;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.theme.getModeString()};
    }

    private static final class Notification {
        String text;
        boolean enabled;
        long created;
        long closingAt = -1L;

        private Notification() {
        }
    }
}
