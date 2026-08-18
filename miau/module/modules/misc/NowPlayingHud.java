package miau.module.modules.misc;

import java.awt.Color;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class NowPlayingHud extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static boolean active = false;
    public static String currentSong = "Track Name";
    public static String timeInfo = "01:41 / 03:30";
    public static String platform = "YouTube";
    public final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    public final FloatProperty hudScale = new FloatProperty("HUD Scale", 1.0F, 0.5F, 2.0F);
    public final BooleanProperty showSpectrum = new BooleanProperty("Show Spectrum", true);
    private NowPlayingWatcher watcher;

    public NowPlayingHud() {
        super("NowPlayingHud", false);
    }

    @Override
    public void onEnabled() {
        this.watcher = new NowPlayingWatcher();
        this.watcher.start();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisabled() {
        if (this.watcher != null) {
            this.watcher.stop();
            this.watcher = null;
        }

        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRender2D(Pre event) {
        if (this.enabled.getValue()) {
            if (active) {
                if (mc.field_71441_e != null && mc.field_71439_g != null) {
                    this.renderHud();
                }
            }
        }
    }

    public void renderHud() {
        ScaledResolution sr = new ScaledResolution(mc);
        int scaledWidth = sr.func_78326_a();
        int scaledHeight = sr.func_78328_b();
        float scale = this.hudScale.getValue();
        float baseX = 10.0F;
        float baseY = scaledHeight - 40.0F;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0F);
        String prefix = "Now Playing : ";
        String trackText = currentSong + " | " + timeInfo + " [" + platform + "]";
        Font fontSmall = FontRepository.getFont("Inter Regular", 18.0F);
        Font fontMedium = FontRepository.getFont("Inter Medium", 18.0F);
        float prefixWidth = fontMedium.getStringWidth(prefix);
        float trackWidth = fontSmall.getStringWidth(trackText);
        float totalTextWidth = prefixWidth + trackWidth;
        int barCount = 4;
        float barWidth = 3.0F;
        float barSpacing = 2.0F;
        float spectrumWidth = barCount * barWidth + (barCount - 1) * barSpacing;
        float paddingRight = 8.0F;
        float paddingLeft = 10.0F;
        float paddingTop = 6.0F;
        float paddingBottom = 6.0F;
        float totalWidth = paddingLeft + totalTextWidth + spectrumWidth + paddingRight;
        float textHeight = Math.max(fontMedium.getFontHeight(), fontSmall.getFontHeight());
        float totalHeight = paddingTop + textHeight + paddingBottom;
        float x = baseX;
        float y = baseY;
        if (x + totalWidth > scaledWidth) {
            x = scaledWidth - totalWidth - 5.0F;
        }

        if (y - totalHeight < 0.0F) {
            y = totalHeight + 5.0F;
        }

        Color bgColor = new Color(10, 14, 20, 180);
        Color outlineColor = new Color(255, 255, 255, 20);
        RoundedUtils.drawRound(x, y - totalHeight, totalWidth, totalHeight, 5.0F, bgColor.getRGB());
        RoundedUtils.drawRoundOutline(x, y - totalHeight, totalWidth, totalHeight, 5.0F, 1.0F, bgColor, outlineColor);
        float textY = y - totalHeight + paddingTop + (textHeight - fontMedium.getFontHeight()) / 2.0F;
        float textX = x + paddingLeft;
        fontMedium.draw(prefix, textX, textY, new Color(0, 190, 245).getRGB(), false);
        textX += prefixWidth;
        fontSmall.draw(trackText, textX, textY, Color.WHITE.getRGB(), false);
        if (this.showSpectrum.getValue()) {
            float spectrumX = x + paddingLeft + totalTextWidth + 6.0F;
            float spectrumY = y - totalHeight + paddingTop;
            float maxBarHeight = textHeight;
            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < barCount; i++) {
                double angle = currentTime * 0.003 + i * 0.8;
                float sineValue = (float)Math.sin(angle);
                float normalizedHeight = (sineValue + 1.0F) / 2.0F;
                float barHeight = 4.0F + normalizedHeight * (maxBarHeight - 4.0F);
                float barX = spectrumX + i * (barWidth + barSpacing);
                float barY = spectrumY + (maxBarHeight - barHeight);
                Color barColor = new Color(0, 190, 245, 220);
                RenderUtil.drawRoundedRectangle(barX, barY, barX + barWidth, barY + barHeight, 1.5F, barColor.getRGB());
                Color highlightColor = new Color(0, 220, 255, 180);
                RenderUtil.drawRect(barX, barY, barX + barWidth, barY + 1.0F, highlightColor.getRGB());
            }
        }

        GL11.glPopMatrix();
    }

    public static void setTrackInfo(String song, String currentTime, String totalTime, String platformName) {
        currentSong = song != null ? song : "Unknown";
        timeInfo = (currentTime != null ? currentTime : "00:00") + " / " + (totalTime != null ? totalTime : "00:00");
        platform = platformName != null ? platformName : "Unknown";
        active = true;
    }

    public static void clearTrack() {
        active = false;
        currentSong = "Track Name";
        timeInfo = "01:41 / 03:30";
        platform = "YouTube";
    }

    public static void setActive(boolean isActive) {
        active = isActive;
    }
}
