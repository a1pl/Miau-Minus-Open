package miau.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.module.modules.render.HUD;
import miau.util.animation.Animation;
import miau.util.animation.Easing;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

public final class NotificationRenderer {
    private static final Font ICON_FONT = FontRepository.getFont("materialicons-regular", 24.0F);
    private static final Font TITLE_FONT = FontRepository.getFont("productsans-bold", 14.0F);
    private static final Font DESCRIPTION_FONT = FontRepository.getFont("productsans-medium", 13.0F);
    private final Map<Notification, Animation> animations = new HashMap<>();
    private static final NotificationRenderer INSTANCE = new NotificationRenderer();

    public static NotificationRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderAll(ScaledResolution sr) {
        INSTANCE.render(sr);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71441_e != null && mc.field_71439_g != null) {
            if (mc.field_71462_r == null || mc.field_71462_r instanceof GuiChat) {
                this.render(new ScaledResolution(mc));
            }
        }
    }

    public void render(ScaledResolution sr) {
        List<Notification> notifications = Miau.notificationManager.getNotifications();
        float padding = 5.0F;
        float height = 21.0F;
        float iconSize = 14.0F;
        float iconOffset = 19.0F;
        float scaledWidth = sr.func_78326_a();
        float scaledHeight = sr.func_78328_b();
        float potionOffset = 0.0F;
        Minecraft mc = Minecraft.func_71410_x();
        Font hudFont = TITLE_FONT;
        if (mc.field_71439_g != null) {
            HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
            if (hud != null) {
                hudFont = hud.getFont();
                if (hud.isEnabled() && !mc.field_71474_y.field_74330_P) {
                    int effectsCount = mc.field_71439_g.func_70651_bq().size();
                    if (effectsCount > 0) {
                        potionOffset = effectsCount * (hudFont.height() + 1.5F);
                    }
                }
            }
        }

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            Animation animation = this.animations.get(notification);
            if (animation == null) {
                animation = new Animation(Easing.EASE_OUT_EXPO, 400L);
                this.animations.put(notification, animation);
            }

            float width = Math.min(
                scaledWidth - 10.0F,
                Math.max(
                    100.0F,
                    19.0F
                        + Math.max(
                            hudFont.getStringWidth(notification.getTitle()) + 20.0F,
                            hudFont.getStringWidth(notification.getDescription())
                        )
                )
            );
            float endX = scaledWidth - width - 5.0F;
            if (!notification.hasExpired()) {
                animation.setStartValue(scaledWidth);
            }

            animation.run(notification.hasExpired() ? scaledWidth : endX);
            float x = animation.getValue();
            float y = scaledHeight - 10.0F - (i + 1) * 26.0F - potionOffset;
            float progress = (float)notification.getTime() / notification.getDuration();
            int iconColor = notification.getType().getIconColor();
            RoundedUtils.drawRound(x, y, width, 21.0F, 4.0F, -2146891511);
            float barWidth = (width - 0.5F) * progress;
            if (barWidth > 0.0F) {
                RoundedUtils.drawRoundedRectRise(
                    x + 0.5F,
                    y + 21.0F - 1.5F,
                    barWidth,
                    1.5F,
                    1.0F,
                    this.applyOpacity(iconColor, 0.25F),
                    false,
                    false,
                    progress > 0.95F,
                    true
                );
            }

            RoundedUtils.drawRound(
                x + 5.0F - 0.5F, y + 1.0F, 19.0F, 19.0F, 2.75F, this.applyOpacity(this.darker(iconColor, 0.6F), 0.5F)
            );
            float iconDrawY = y + 7.5F;
            ICON_FONT.draw(notification.getType().getIcon(), x + 5.0F + 2.25F, iconDrawY, iconColor);
            hudFont.draw(notification.getTitle(), x + 10.0F + 19.0F, y + 4.0F, -1);
            hudFont.draw(notification.getDescription(), x + 10.0F + 19.0F, y + 12.0F, -5592406);
            if (notification.hasExpired() && animation.getValue() == scaledWidth) {
                notifications.remove(notification);
                this.animations.remove(notification);
                i--;
            }
        }
    }

    private int darker(int color, float factor) {
        float f = 1.0F - factor;
        int r = (int)((color >> 16 & 0xFF) * f);
        int g = (int)((color >> 8 & 0xFF) * f);
        int b = (int)((color & 0xFF) * f);
        int a = color >> 24 & 0xFF;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF | (a & 0xFF) << 24;
    }

    private int applyOpacity(int color, float opacityFactor) {
        opacityFactor = Math.min(1.0F, Math.max(0.0F, opacityFactor));
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = (int)(opacityFactor * 255.0F);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
