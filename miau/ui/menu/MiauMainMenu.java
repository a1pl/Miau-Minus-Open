package miau.ui.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import me.ksyz.accountmanager.gui.GuiAccountManager;
import miau.Miau;
import miau.management.MiauAPI;
import miau.module.modules.render.HUD;
import miau.ui.GuiNotificationClient;
import miau.ui.GuiUpdateClient;
import miau.util.client.Mp3Util;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.font.impl.minecraft.MinecraftFontRenderer;
import miau.util.font.impl.rise.FontRenderer;
import miau.util.math.MathUtil;
import miau.util.render.ColorUtil;
import miau.util.render.MenuBackground;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class MiauMainMenu extends GuiScreen {
    private static boolean hasCheckedVersion = false;
    private static boolean hasPlayedWelcome = false;
    private static final int PARTICLE_COUNT = 55;
    private final float[] px = new float[55];
    private final float[] py = new float[55];
    private final float[] pSpeed = new float[55];
    private final float[] pSize = new float[55];
    private final float[] pAlpha = new float[55];
    private boolean particlesInit = false;
    private final Random rng = new Random();
    private float animProgress = 0.0F;
    private final float[] hoverAnim = new float[6];
    private long lastFrame = System.currentTimeMillis();
    private Font fontLogo;
    private Font fontSubtitle;
    private Font fontBtn;
    private Font fontMeta;

    static Font loadRiseFont(String filename, float size) {
        try {
            InputStream is = MiauMainMenu.class
                .getClassLoader()
                .getResourceAsStream("assets/keystrokesmod/fonts/" + filename);
            if (is == null) {
                return null;
            }

            java.awt.Font awt = java.awt.Font.createFont(0, is).deriveFont(size);
            return new FontRenderer(awt, true, true, false);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadFonts() {
        this.fontLogo = FontRepository.getMinecraftFont();
        this.fontSubtitle = FontRepository.getMinecraftFont();
        this.fontBtn = FontRepository.getMinecraftFont();
        this.fontMeta = FontRepository.getMinecraftFont();
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        this.field_146292_n.clear();
        this.loadFonts();
        this.animProgress = 0.0F;
        if (!hasPlayedWelcome) {
            hasPlayedWelcome = true;
            Mp3Util.play("miau/sound/welcome/welcome.mp3");
        }

        if (!this.particlesInit) {
            for (int i = 0; i < 55; i++) {
                this.respawnParticle(i, true);
            }

            this.particlesInit = true;
        }

        if (!hasCheckedVersion) {
            hasCheckedVersion = true;
            new Thread(
                    () -> {
                        try {
                            String resp = MiauAPI.getClientVersion();
                            JsonObject json = new JsonParser().parse(resp).getAsJsonObject();
                            if (json.has("status") && json.get("status").getAsString().equals("success")) {
                                String latest = json.get("version").getAsString();
                                String url = json.get("updateUrl").getAsString();
                                if (MiauAPI.isOutdated("1.1.0-beta", latest)) {
                                    Minecraft.func_71410_x()
                                        .func_152344_a(
                                            () -> Minecraft.func_71410_x()
                                                .func_147108_a(new GuiUpdateClient(this, "1.1.0-beta", latest, url))
                                        );
                                    return;
                                }
                            }

                            String notiResp = MiauAPI.getClientNotification();
                            JsonObject notiJson = new JsonParser().parse(notiResp).getAsJsonObject();
                            if (notiJson.has("status")
                                && notiJson.get("status").getAsString().equals("success")
                                && notiJson.has("hasNotification")
                                && notiJson.get("hasNotification").getAsBoolean()) {
                                JsonObject noti = notiJson.get("notification").getAsJsonObject();
                                String title = noti.has("title") ? noti.get("title").getAsString() : "Notification";
                                String desc = noti.has("desc") ? noti.get("desc").getAsString() : "";
                                String btn1Text = noti.has("btn1Text") ? noti.get("btn1Text").getAsString() : "OK";
                                String btn1Link = noti.has("btn1Link") ? noti.get("btn1Link").getAsString() : "";
                                String btn2Text = noti.has("btn2Text") ? noti.get("btn2Text").getAsString() : "";
                                String btn2Link = noti.has("btn2Link") ? noti.get("btn2Link").getAsString() : "";
                                int durationDays = noti.has("durationDays") ? noti.get("durationDays").getAsInt() : 0;
                                long updatedAt = noti.has("updatedAt") ? noti.get("updatedAt").getAsLong() : 0L;
                                long now = System.currentTimeMillis();
                                if (durationDays <= 0
                                    || updatedAt == 0L
                                    || now - updatedAt <= durationDays * 24L * 60L * 60L * 1000L) {
                                    Minecraft.func_71410_x()
                                        .func_152344_a(
                                            () -> Minecraft.func_71410_x()
                                                .func_147108_a(
                                                    new GuiNotificationClient(
                                                        this, title, desc, btn1Text, btn1Link, btn2Text, btn2Link
                                                    )
                                                )
                                        );
                                }
                            }
                        } catch (Exception var17) {
                        }
                    }
                )
                .start();
        }
    }

    private void respawnParticle(int i, boolean anywhere) {
        this.px[i] = this.rng.nextFloat() * Math.max(this.field_146294_l, 854);
        this.py[i] = anywhere
            ? this.rng.nextFloat() * Math.max(this.field_146295_m, 480)
            : Math.max(this.field_146295_m, 480) + 5;
        this.pSpeed[i] = 0.15F + this.rng.nextFloat() * 0.45F;
        this.pSize[i] = 1.0F + this.rng.nextFloat() * 2.2F;
        this.pAlpha[i] = 0.2F + this.rng.nextFloat() * 0.45F;
    }

    protected void func_146284_a(GuiButton button) throws IOException {
    }

    public void func_73864_a(int mouseX, int mouseY, int button) throws IOException {
        super.func_73864_a(mouseX, mouseY, button);
        if (button == 0) {
            int centerX = this.field_146294_l / 2;
            int centerY = this.field_146295_m / 2;
            int btnWidth = 160;
            int btnHeight = 22;
            int spacing = 5;
            int startY = centerY - 25;

            for (int i = 0; i < 3; i++) {
                int by = startY + i * (btnHeight + spacing);
                int bx = centerX - btnWidth / 2;
                if (mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= by && mouseY <= by + btnHeight) {
                    this.onButtonClick(i);
                    return;
                }
            }

            int bottomY = startY + (btnHeight + spacing) * 3;
            int smallW = (btnWidth - spacing * 2) / 3;
            int startSmallX = centerX - btnWidth / 2;

            for (int i = 0; i < 3; i++) {
                int bx = startSmallX + i * (smallW + spacing);
                if (mouseX >= bx && mouseX <= bx + smallW && mouseY >= bottomY && mouseY <= bottomY + btnHeight) {
                    this.onButtonClick(3 + i);
                    return;
                }
            }
        }
    }

    private void onButtonClick(int id) {
        HUD hud = null;

        try {
            if (Miau.moduleManager != null) {
                hud = (HUD)Miau.moduleManager.getModule(HUD.class);
            }
        } catch (Exception var5) {
        }

        switch (id) {
            case 0:
                this.field_146297_k.func_147108_a(new GuiSelectWorld(this));
                break;
            case 1:
                this.field_146297_k.func_147108_a(new GuiMultiplayer(this));
                break;
            case 2:
                this.field_146297_k.func_147108_a(new GuiAccountManager(this));
                break;
            case 3:
                this.field_146297_k.func_147108_a(new GuiOptions(this, this.field_146297_k.field_71474_y));
                break;
            case 4:
                if (hud != null) {
                    int current = hud.menuBackground.getValue();
                    int next = (current + 1) % MenuBackground.NAMES.length;
                    hud.menuBackground.setValue(next);
                }
                break;
            case 5:
                this.field_146297_k.func_71400_g();
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.loadFonts();
        long now = System.currentTimeMillis();
        float dt = Math.min((float)(now - this.lastFrame) / 1000.0F, 0.05F);
        this.lastFrame = now;
        this.animProgress = MathUtil.lerp(this.animProgress, 1.0F, 0.08F * (dt * 60.0F));
        this.drawBackground(now);
        this.drawParticles(dt, now);
        this.drawHeaderBlock(now);
        this.drawButtonBlock(mouseX, mouseY, dt, now);
        this.drawFooter();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawBackground(long now) {
        int shaderIndex = 0;

        try {
            if (Miau.moduleManager != null) {
                HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
                if (hud != null) {
                    shaderIndex = hud.menuBackground.getValue();
                }
            }
        } catch (Exception var5) {
        }

        MenuBackground.draw(this.field_146294_l, this.field_146295_m, shaderIndex);
    }

    private void drawParticles(float dt, long now) {
        Themes theme = Themes.getCurrentTheme();
        Color c1 = theme.getFirstColor();
        Color c2 = theme.getSecondColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GlStateManager.func_179090_x();
        GL11.glEnable(2832);
        GL11.glBegin(0);

        for (int i = 0; i < 55; i++) {
            this.py[i] = this.py[i] - this.pSpeed[i] * 18.0F * dt;
            this.px[i] = this.px[i] + (float)Math.sin(now / 1800.0 + i * 1.7F) * 0.1F;
            if (this.py[i] < -4.0F) {
                this.respawnParticle(i, false);
            }

            Color c = blendC(c1, c2, i / 55.0F);
            GL11.glPointSize(this.pSize[i]);
            GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, this.pAlpha[i]);
            GL11.glVertex2f(this.px[i], this.py[i]);
        }

        GL11.glEnd();
        GL11.glDisable(2832);
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179117_G();
    }

    private void drawHeaderBlock(long now) {
        if (this.fontLogo != null && this.fontSubtitle != null) {
            Themes theme = Themes.getCurrentTheme();
            Color accent = theme.getFirstColor();
            float centerX = this.field_146294_l / 2.0F;
            float centerY = this.field_146295_m / 2.0F;
            float titleY = centerY - 85.0F;
            float s = 1.0F + (1.0F - this.animProgress) * 0.2F;
            int alphaVal = (int)(255.0F * this.animProgress);
            int color = new Color(255, 255, 255, alphaVal).getRGB();
            int subColor = new Color(180, 180, 180, Math.max(0, alphaVal - 80)).getRGB();
            if (this.fontLogo instanceof MinecraftFontRenderer) {
                GlStateManager.func_179094_E();
                float titleScale = s * 3.0F;
                GlStateManager.func_179109_b(centerX, titleY, 0.0F);
                GlStateManager.func_179152_a(titleScale, titleScale, 1.0F);
                GlStateManager.func_179109_b(-centerX, -titleY, 0.0F);
                this.drawSkyTitle(this.fontLogo, "Miau Minus Client", centerX, titleY, alphaVal);
                GlStateManager.func_179121_F();
                GlStateManager.func_179094_E();
                float subScale = s * 1.2F;
                float subY = titleY + 27.0F + 4.0F;
                GlStateManager.func_179109_b(centerX, subY, 0.0F);
                GlStateManager.func_179152_a(subScale, subScale, 1.0F);
                GlStateManager.func_179109_b(-centerX, -subY, 0.0F);
                String ver = "v1.1.0-beta";
                this.fontSubtitle.drawCentered(ver, centerX, subY, subColor);
                GlStateManager.func_179121_F();
            } else {
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b(centerX, titleY, 0.0F);
                GlStateManager.func_179152_a(s, s, 1.0F);
                GlStateManager.func_179109_b(-centerX, -titleY, 0.0F);
                this.drawSkyTitle(this.fontLogo, "Miau Minus Client", centerX, titleY, alphaVal);
                String ver = "v1.1.0-beta";
                this.fontSubtitle.drawCentered(ver, centerX, titleY + this.fontLogo.height() + 4.0F, subColor);
                GlStateManager.func_179121_F();
            }
        }
    }

    private void drawSkyTitle(Font font, String text, float centerX, float y, int alphaVal) {
        float total = 0.0F;

        for (int i = 0; i < text.length(); i++) {
            total += font.width(String.valueOf(text.charAt(i))) + 0.5F;
        }

        float x = centerX - total / 2.0F;
        Color top = new Color(126, 213, 255, alphaVal);
        Color bottom = new Color(147, 112, 219, alphaVal);

        for (int i = 0; i < text.length(); i++) {
            float t = text.length() <= 1 ? 0.5F : (float)i / (text.length() - 1);
            Color c = ColorUtil.mixColors(bottom, top, t);
            font.drawCharacter(text.charAt(i), (int)x, (int)y, c);
            x += font.width(String.valueOf(text.charAt(i))) + 0.5F;
        }
    }

    private void drawButtonBlock(int mouseX, int mouseY, float dt, long now) {
        if (this.fontBtn != null) {
            int centerX = this.field_146294_l / 2;
            int centerY = this.field_146295_m / 2;
            int btnWidth = 160;
            int btnHeight = 22;
            int spacing = 5;
            int startY = centerY - 25;
            this.drawButton(
                0, centerX - btnWidth / 2, startY, btnWidth, btnHeight, "Singleplayer", mouseX, mouseY, dt, 0
            );
            this.drawButton(
                1,
                centerX - btnWidth / 2,
                startY + btnHeight + spacing,
                btnWidth,
                btnHeight,
                "Multiplayer",
                mouseX,
                mouseY,
                dt,
                1
            );
            this.drawButton(
                2,
                centerX - btnWidth / 2,
                startY + (btnHeight + spacing) * 2,
                btnWidth,
                btnHeight,
                "Alt Manager",
                mouseX,
                mouseY,
                dt,
                2
            );
            int bottomY = startY + (btnHeight + spacing) * 3;
            int smallW = (btnWidth - spacing * 2) / 3;
            int startSmallX = centerX - btnWidth / 2;
            this.drawButton(3, startSmallX, bottomY, smallW, btnHeight, "Settings", mouseX, mouseY, dt, 3);
            this.drawButton(
                4, startSmallX + smallW + spacing, bottomY, smallW, btnHeight, "Theme", mouseX, mouseY, dt, 4
            );
            this.drawButton(
                5, startSmallX + (smallW + spacing) * 2, bottomY, smallW, btnHeight, "Exit", mouseX, mouseY, dt, 5
            );
        }
    }

    private void drawButton(
        int id, int bx, int by, int bw, int bh, String label, int mouseX, int mouseY, float dt, int count
    ) {
        float btnAnim = Math.max(0.0F, Math.min(1.0F, (this.animProgress - count * 0.03F) * 2.5F));
        if (!(btnAnim <= 0.01F)) {
            boolean hov = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            float target = hov ? 1.0F : 0.0F;
            this.hoverAnim[id] = MathUtil.lerp(this.hoverAnim[id], target, 0.25F * (dt * 60.0F));
            float h = this.hoverAnim[id];
            GlStateManager.func_179094_E();
            float cx = bx + bw / 2.0F;
            float cy = by + bh / 2.0F;
            GlStateManager.func_179109_b(cx, cy, 0.0F);
            GlStateManager.func_179152_a(btnAnim, btnAnim, 1.0F);
            GlStateManager.func_179109_b(-cx, -cy, 0.0F);
            GlStateManager.func_179109_b(0.0F, (1.0F - btnAnim) * 5.0F, 0.0F);
            Color colorBgNormal = new Color(20, 20, 20, 120);
            Color colorBgHover = new Color(40, 40, 45, 200);
            Color colorOutlineNormal = new Color(255, 255, 255, 60);
            Color colorOutlineHover = new Color(255, 255, 255, 180);
            Color bg = blendC(colorBgNormal, colorBgHover, h);
            Color outline = blendC(colorOutlineNormal, colorOutlineHover, h);
            Color textCol = blendC(new Color(200, 200, 200), Color.WHITE, h);
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            RenderUtil.drawRoundedRectangle(bx, by, bx + bw, by + bh, 5.0F, bg.getRGB());
            RenderUtil.drawRoundedRectangleOutline(bx, by, bx + bw, by + bh, 5.0F, 1.0F, outline.getRGB());
            float fontHeight = this.fontBtn.height();
            if (this.fontBtn instanceof MinecraftFontRenderer) {
                fontHeight = 8.0F;
            }

            float fontY = by + (bh - fontHeight) / 2.0F;
            this.fontBtn.drawCentered(label, cx, fontY, textCol.getRGB());
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        }
    }

    private void drawFooter() {
        if (this.fontMeta != null) {
            Gui.func_73734_a(
                0,
                this.field_146295_m - 1,
                this.field_146294_l,
                this.field_146295_m,
                new Color(255, 255, 255, 10).getRGB()
            );
            float footerY = this.field_146295_m - this.fontMeta.height() - 5.0F;
            String right = "Credit: [Tirumdz-d, SPEEDBOY19 and ihatenocheat-bot based on Project-Miau]";
            int rw = this.fontMeta.width(right);
            this.fontMeta.draw(right, this.field_146294_l - rw - 5, footerY, -788529153, false);
        }
    }

    private void drawGlow(Font font, String text, int x, int y, Color color, long now) {
        double pulse = Math.sin(now / 1400.0) * 0.5 + 0.5;
        int baseAlpha = (int)(18.0 + pulse * 18.0);

        for (int off = 4; off >= 1; off--) {
            int a = Math.max(1, baseAlpha / off);
            int gc = alpha(color, a).getRGB();
            font.draw(text, x - off, y, gc, false);
            font.draw(text, x + off, y, gc, false);
            font.draw(text, x, y - off, gc, false);
            font.draw(text, x, y + off, gc, false);
        }
    }

    private void drawBlob(float cx, float cy, float radius, Color color, int alpha) {
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GlStateManager.func_179090_x();
        int steps = 20;

        for (int s = steps; s >= 0; s--) {
            float r = radius * ((float)s / steps);
            float a = alpha / 255.0F * (1.0F - (float)s / steps);
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, a);
            GL11.glBegin(6);
            GL11.glVertex2f(cx, cy);

            for (int seg = 0; seg <= 52; seg++) {
                double ang = seg * 0.1208304866765305;
                GL11.glVertex2f((float)(cx + Math.cos(ang) * r), (float)(cy + Math.sin(ang) * r));
            }

            GL11.glEnd();
        }

        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179117_G();
    }

    private static Color blendC(Color a, Color b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return new Color(
            (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public boolean func_73868_f() {
        return false;
    }
}
