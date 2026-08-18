package miau.ui.nogui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import miau.Miau;
import miau.module.Module;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ItemListProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.client.KeyBindUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ModuleSettingsGui extends GuiScreen {
    private static final Color ACCENT_A = new Color(33, 212, 253);
    private static final Color ACCENT_B = new Color(123, 108, 255);
    private static final Color BG_COLOR = new Color(5, 7, 13);
    private static final Color PANEL_BG = new Color(11, 14, 24, 246);
    private static final Color BORDER = new Color(35, 45, 64);
    private static final Color TEXT_MAIN = new Color(241, 245, 251);
    private static final Color TEXT_DIM = new Color(163, 174, 194);
    private static final Color TEXT_FAINT = new Color(94, 106, 128);
    private static final Color ROW_HOVER = new Color(255, 255, 255, 10);
    private static final Color SWITCH_OFF = new Color(38, 48, 71);
    private static final Color TRACK_BG = new Color(34, 42, 62);
    private static final float PAD = 14.0F;
    private static final float ROW_H = 30.0F;
    private static final float HEADER_H = 56.0F;
    private static final float TAB_H = 30.0F;
    private static final int T_SWITCH = 0;
    private static final int T_KEYBIND = 99;
    private static final int T_SLIDER = 1;
    private static final int T_MODE = 2;
    private static final int T_COLOR = 3;
    private static final int T_TEXT = 4;
    private final Module module;
    private final String[] tabs = new String[]{"Settings", "Bind", "Config"};
    private String tab = "Settings";
    private Font fontTitle;
    private Font fontBody;
    private Font fontValue;
    private Font fontSmall;
    private Font fontBig;
    private float scroll = 0.0F;
    private float targetScroll = 0.0F;
    private float maxScroll = 0.0F;
    private float openAnim = 0.0F;
    private long lastMS = 0L;
    private ScaledResolution sr;
    private boolean binding = false;
    private Property<?> dragging = null;
    private final Map<Property<?>, Float> knobs = new HashMap<>();
    private String status = "";

    public ModuleSettingsGui(Module module) {
        this.module = module;
        this.fontTitle = FontRepository.getFont("sfuidisplay-bold", 16.0F);
        this.fontBody = FontRepository.getFont("sfuidisplay-regular", 12.0F);
        this.fontValue = FontRepository.getFont("sfuidisplay-medium", 12.0F);
        this.fontSmall = FontRepository.getFont("sfuidisplay-medium", 9.0F);
        this.fontBig = FontRepository.getFont("sfuidisplay-bold", 26.0F);
    }

    public boolean func_73868_f() {
        return false;
    }

    public void func_73866_w_() {
        this.lastMS = System.currentTimeMillis();
    }

    private float sw() {
        return new ScaledResolution(this.field_146297_k).func_78326_a();
    }

    private float sh() {
        return new ScaledResolution(this.field_146297_k).func_78328_b();
    }

    private List<ModuleSettingsGui.Row> buildRows() {
        List<ModuleSettingsGui.Row> rows = new ArrayList<>();
        rows.add(new ModuleSettingsGui.Row(0, null));
        rows.add(new ModuleSettingsGui.Row(99, null));

        for (Property<?> p : this.module.getValues()) {
            if (!(p instanceof DragProperty) && !(p instanceof ItemListProperty) && p.isVisible()) {
                int t = 4;
                if (p instanceof BooleanProperty) {
                    t = 0;
                } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
                    t = 1;
                } else if (p instanceof ModeProperty) {
                    t = 2;
                } else if (p instanceof ColorProperty) {
                    t = 3;
                }

                rows.add(new ModuleSettingsGui.Row(t, p));
            }
        }

        return rows;
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        super.func_73863_a(mouseX, mouseY, partialTicks);
        long now = System.currentTimeMillis();
        float delta = Math.min((float)(now - this.lastMS) / 50.0F, 1.5F);
        this.lastMS = now;
        this.sr = new ScaledResolution(this.field_146297_k);
        this.openAnim = expApproach(this.openAnim, 1.0F, delta, 6.0F);
        this.maxScroll = this.computeMaxScroll();
        this.scroll = expApproach(this.scroll, clamp(this.targetScroll, 0.0F, this.maxScroll), delta, 6.0F);
        float w = this.sw();
        float h = this.sh();
        float alpha = easeOut(this.openAnim);
        RoundedUtils.drawRound(0.0F, 0.0F, w, h, 0.0F, withAlpha(BG_COLOR, (int)(225.0F * alpha)));
        RoundedUtils.drawRound(-140.0F, -160.0F, 440.0F, 440.0F, 220.0F, new Color(33, 212, 253, (int)(7.0F * alpha)));
        RoundedUtils.drawRound(
            w - 300.0F, h - 320.0F, 430.0F, 430.0F, 215.0F, new Color(123, 108, 255, (int)(6.0F * alpha))
        );
        float winW = Math.min(340.0F, w - 60.0F);
        float winH = Math.min(430.0F, h - 80.0F);
        float wx = (w - winW) / 2.0F;
        float wy = (h - winH) / 2.0F;
        float anim = easeOut(this.openAnim);
        GL11.glPushMatrix();
        GL11.glTranslatef(w / 2.0F, h / 2.0F, 0.0F);
        GL11.glScalef(0.97F + 0.03F * anim, 0.97F + 0.03F * anim, 1.0F);
        GL11.glTranslatef(-w / 2.0F, -h / 2.0F, 0.0F);
        RoundedUtils.drawRound(wx, wy, winW, winH, 14.0F, PANEL_BG);
        RoundedUtils.drawRoundOutline(wx, wy, winW, winH, 14.0F, 1.5F, new Color(0, 0, 0, 0), withAlpha(BORDER, 255));
        float t = this.pulse();
        RoundedUtils.drawGradientHorizontal(wx, wy, winW, 3.0F, 1.5F, this.accent(t), this.accent(t + 0.35F));
        RoundedUtils.drawRound(wx + 14.0F, wy + 10.0F, 34.0F, 34.0F, 9.0F, new Color(255, 255, 255, 10));
        String initial = this.module.getName().isEmpty() ? "?" : this.module.getName().substring(0, 1);
        this.fontBig
            .drawWithShadow(
                initial,
                wx + 14.0F + (34.0F - this.fontBig.width(initial)) / 2.0F,
                wy + 10.0F + (34.0F - this.fontBig.height()) / 2.0F - 1.0F,
                this.accent(t).getRGB()
            );
        this.fontTitle.drawWithShadow(this.module.getName(), wx + 14.0F + 42.0F, wy + 13.0F, TEXT_MAIN.getRGB());
        this.fontSmall
            .draw(
                this.module.getCategory(),
                wx + 14.0F + 42.0F,
                wy + 13.0F + this.fontTitle.height() + 3.0F,
                TEXT_FAINT.getRGB()
            );
        float closeX = wx + winW - 40.0F;
        float closeY = wy + 12.0F;
        boolean closeHover = mouseX >= closeX
            && mouseX <= closeX + 26.0F
            && mouseY >= closeY
            && mouseY <= closeY + 26.0F;
        RoundedUtils.drawRound(
            closeX, closeY, 26.0F, 26.0F, 8.0F, closeHover ? new Color(226, 70, 90, 90) : new Color(255, 255, 255, 9)
        );
        this.drawCross(closeX + 13.0F, closeY + 13.0F, 4.5F, 1.6F, new Color(255, 255, 255, closeHover ? 255 : 180));
        this.drawTabs(mouseX, mouseY, wx, wy, winW);
        float contentTop = wy + 56.0F + 30.0F + 10.0F;
        float contentBottom = wy + winH - 12.0F;
        if (this.tab.equals("Settings")) {
            this.drawSettingsTab(mouseX, mouseY, wx, contentTop, winW, contentBottom);
        } else if (this.tab.equals("Bind")) {
            this.drawBindTab(mouseX, mouseY, wx, contentTop, winW, contentBottom);
        } else {
            this.drawConfigTab(mouseX, mouseY, wx, contentTop, winW, contentBottom);
        }

        GL11.glPopMatrix();
        if (this.binding) {
            this.drawBindingOverlay(w, h, winW, winH, wx, wy);
        }
    }

    private float computeMaxScroll() {
        List<ModuleSettingsGui.Row> rows = this.buildRows();
        float totalH = rows.size() * 30.0F;
        float winH = Math.min(430.0F, this.sh() - 80.0F);
        float avail = winH - 56.0F - 30.0F - 10.0F - 12.0F;
        return Math.max(0.0F, totalH - avail);
    }

    private void drawTabs(int mx, int my, float wx, float wy, float winW) {
        float tabY = wy + 56.0F + 4.0F;
        float tx = wx + 14.0F;

        for (String name : this.tabs) {
            float tw = this.fontBody.width(name) + 26.0F;
            boolean active = name.equals(this.tab);
            boolean hovered = mx >= tx && mx <= tx + tw && my >= tabY && my <= tabY + 30.0F;
            Color bg = active
                ? withAlpha(ACCENT_A, 210)
                : (hovered ? new Color(33, 212, 253, 50) : new Color(255, 255, 255, 8));
            Color text = active ? new Color(4, 9, 17) : (hovered ? TEXT_DIM : TEXT_FAINT);
            RoundedUtils.drawRound(tx, tabY, tw, 30.0F, 8.0F, bg);
            this.fontBody
                .drawCentered(
                    name, tx + tw / 2.0F, tabY + (30.0F - this.fontBody.height()) / 2.0F + 0.5F, text.getRGB()
                );
            tx += tw + 6.0F;
        }
    }

    private void drawSettingsTab(int mx, int my, float wx, float contentTop, float winW, float contentBottom) {
        List<ModuleSettingsGui.Row> rows = this.buildRows();
        float innerH = contentBottom - contentTop;
        this.beginScissor(wx + 14.0F, contentTop, winW - 28.0F, innerH);

        for (int i = 0; i < rows.size(); i++) {
            ModuleSettingsGui.Row row = rows.get(i);
            float rowY = contentTop + i * 30.0F - this.scroll;
            if (!(rowY + 30.0F < contentTop) && !(rowY > contentBottom)) {
                float rx = wx + 14.0F;
                float rw = winW - 28.0F;
                boolean rowHover = mx >= rx && mx <= rx + rw && my >= rowY && my <= rowY + 30.0F;
                if (rowHover) {
                    RoundedUtils.drawRound(rx, rowY + 2.0F, rw, 26.0F, 8.0F, ROW_HOVER);
                }

                this.drawRow(row, rx, rowY, rw, 30.0F);
            }
        }

        this.endScissor();
        if (this.maxScroll > 0.5F) {
            float barX = wx + winW - 4.0F;
            float thumbH = Math.max(18.0F, innerH * (innerH / (rows.size() * 30.0F)));
            float thumbY = contentTop + (innerH - thumbH) * (this.scroll / this.maxScroll);
            RoundedUtils.drawRound(barX, contentTop, 2.5F, innerH, 1.5F, new Color(255, 255, 255, 14));
            RoundedUtils.drawRound(barX, thumbY, 2.5F, thumbH, 1.5F, new Color(123, 108, 255, 170));
        }
    }

    private void drawRow(ModuleSettingsGui.Row row, float rx, float rowY, float rw, float rh) {
        float rightX = rx + rw - 4.0F;
        float t = this.pulse();
        if (row.type == 99) {
            String name = "Keybind";
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            String key = this.module.getKey() != 0 ? KeyBindUtil.getKeyName(this.module.getKey()) : "None";
            float tw = this.fontValue.width(key) + 20.0F;
            float pillX = rightX - tw;
            float pillH = 18.0F;
            float pillY = rowY + (rh - pillH) / 2.0F;
            RoundedUtils.drawRound(pillX, pillY, tw, pillH, 9.0F, new Color(255, 255, 255, 12));
            this.fontValue
                .drawWithShadow(
                    key,
                    pillX + (tw - this.fontValue.width(key)) / 2.0F,
                    pillY + (pillH - this.fontValue.height()) / 2.0F,
                    TEXT_MAIN.getRGB()
                );
        } else if (row.prop == null) {
            String name = "Enabled";
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            this.drawSwitch(rx + rw - 38.0F, rowY + (rh - 16.0F) / 2.0F, 34.0F, 16.0F, 0.0F, this.module.isEnabled());
        } else if (row.type == 0) {
            boolean value = (Boolean)row.prop.getValue();
            String name = row.prop.getName();
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            float k = this.animateKnob(row.prop, value);
            this.drawSwitch(rx + rw - 38.0F, rowY + (rh - 16.0F) / 2.0F, 34.0F, 16.0F, k, value);
        } else if (row.type == 1) {
            float min = 0.0F;
            float max = 100.0F;
            float cur = 0.0F;
            if (row.prop instanceof FloatProperty) {
                FloatProperty fp = (FloatProperty)row.prop;
                min = fp.getMinimum();
                max = fp.getMaximum();
                cur = fp.getValue();
            } else if (row.prop instanceof IntProperty) {
                IntProperty ip = (IntProperty)row.prop;
                min = ip.getMinimum().intValue();
                max = ip.getMaximum().intValue();
                cur = ip.getValue().intValue();
            } else {
                PercentProperty pp = (PercentProperty)row.prop;
                min = pp.getMinimum().intValue();
                max = pp.getMaximum().intValue();
                cur = pp.getValue().intValue();
            }

            String display = stripCodes(row.prop.formatValue());
            float trackX = rx + 4.0F;
            float trackW = rw - 8.0F;
            float trackY = rowY + rh - 9.0F;
            float ratio = max - min <= 0.001F ? 0.0F : clamp((cur - min) / (max - min), 0.0F, 1.0F);
            RoundedUtils.drawRound(trackX, trackY, trackW, 3.0F, 1.5F, TRACK_BG);
            if (ratio > 0.01F) {
                RoundedUtils.drawGradientHorizontal(
                    trackX, trackY, trackW * ratio, 3.0F, 1.5F, this.accent(t), this.accent(t + 0.35F)
                );
            }

            float knobX = trackX + trackW * ratio;
            RoundedUtils.drawRound(knobX - 3.0F, trackY - 1.5F, 6.0F, 6.0F, 3.0F, withAlpha(TEXT_MAIN, 255));
            this.fontBody.drawWithShadow(row.prop.getName(), rx + 4.0F, rowY + 3.0F, TEXT_DIM.getRGB());
            String clippedVal = this.clipText(this.fontSmall, display, rw - 60.0F);
            this.fontSmall
                .drawWithShadow(clippedVal, rightX - this.fontSmall.width(clippedVal), rowY + 4.0F, TEXT_FAINT.getRGB());
        } else if (row.type == 2) {
            ModeProperty mp = (ModeProperty)row.prop;
            String mode = mp.getModeString();
            String name = row.prop.getName();
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            float tw = this.fontValue.width(mode) + 24.0F;
            float pillX = rightX - tw;
            float pillH = 18.0F;
            float pillY = rowY + (rh - pillH) / 2.0F;
            RoundedUtils.drawRound(pillX, pillY, tw, pillH, 9.0F, new Color(255, 255, 255, 12));
            this.fontValue
                .drawWithShadow(
                    mode,
                    pillX + (tw - this.fontValue.width(mode)) / 2.0F - 3.0F,
                    pillY + (pillH - this.fontValue.height()) / 2.0F,
                    TEXT_MAIN.getRGB()
                );
            this.fontSmall
                .draw(
                    ">",
                    pillX + tw - 10.0F,
                    pillY + (pillH - this.fontSmall.height()) / 2.0F + 0.5F,
                    TEXT_FAINT.getRGB()
                );
        } else if (row.type == 3) {
            String name = row.prop.getName();
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            int v = ((ColorProperty)row.prop).getValue();
            Color c = withAlpha(new Color(v, true), 255);
            float swW = 16.0F;
            float swX = rightX - swW;
            float swY = rowY + (rh - swW) / 2.0F;
            RoundedUtils.drawRound(swX, swY, swW, swW, 5.0F, c);
            RoundedUtils.drawRoundOutline(
                swX, swY, swW, swW, 5.0F, 1.0F, new Color(0, 0, 0, 0), new Color(255, 255, 255, 55)
            );
        } else {
            String name = row.prop.getName();
            this.fontBody
                .drawWithShadow(name, rx + 4.0F, rowY + (rh - this.fontBody.height()) / 2.0F, TEXT_DIM.getRGB());
            String val = this.clipText(
                this.fontValue, stripCodes(row.prop.formatValue()), rw - this.fontBody.width(name) - 24.0F
            );
            this.fontValue
                .draw(
                    val,
                    rightX - this.fontValue.width(val),
                    rowY + (rh - this.fontValue.height()) / 2.0F,
                    TEXT_FAINT.getRGB()
                );
        }
    }

    private void drawSwitch(float x, float y, float w, float h, float k, boolean on) {
        float t = this.pulse();
        if (on) {
            RoundedUtils.drawGradientHorizontal(x, y, w, h, h / 2.0F, this.accent(t), this.accent(t + 0.35F));
        } else {
            RoundedUtils.drawRound(x, y, w, h, h / 2.0F, SWITCH_OFF);
        }

        float knobX = x + 3.0F + k * (w - 16.0F);
        RoundedUtils.drawRound(
            knobX, y + 2.5F, 11.0F, 11.0F, 5.5F, on ? new Color(240, 247, 255) : new Color(120, 130, 152)
        );
    }

    private float animateKnob(Property<?> prop, boolean on) {
        Float f = this.knobs.get(prop);
        float k = f == null ? 0.0F : f;
        k = expApproach(k, on ? 1.0F : 0.0F, 0.3F, 4.0F);
        this.knobs.put(prop, k);
        return k;
    }

    private void drawBindTab(int mx, int my, float wx, float contentTop, float winW, float contentBottom) {
        float bx = wx + 14.0F;
        float bw = winW - 28.0F;
        float by = contentTop + 6.0F;
        float bh = 64.0F;
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        RoundedUtils.drawRound(
            bx, by, bw, bh, 10.0F, hovered ? new Color(33, 212, 253, 40) : new Color(255, 255, 255, 8)
        );
        RoundedUtils.drawRoundOutline(
            bx,
            by,
            bw,
            bh,
            10.0F,
            1.0F,
            new Color(0, 0, 0, 0),
            hovered ? withAlpha(ACCENT_A, 180) : withAlpha(BORDER, 255)
        );
        String key = this.module.getKey() != 0 ? KeyBindUtil.getKeyName(this.module.getKey()) : "None";
        this.fontValue
            .drawCentered(this.module.getName() + "  →  " + key, bx + bw / 2.0F, by + 18.0F, TEXT_MAIN.getRGB());
        this.fontSmall
            .drawCentered(
                hovered ? "Release mouse, then press any key..." : "Click here to bind a key",
                bx + bw / 2.0F,
                by + 40.0F,
                TEXT_FAINT.getRGB()
            );
        float clearW = 140.0F;
        float clearX = bx + bw / 2.0F - clearW / 2.0F;
        float clearY = by + bh + 14.0F;
        boolean clearHover = mx >= clearX && mx <= clearX + clearW && my >= clearY && my <= clearY + 30.0F;
        RoundedUtils.drawRound(
            clearX,
            clearY,
            clearW,
            30.0F,
            9.0F,
            clearHover ? new Color(226, 70, 90, 200) : new Color(255, 255, 255, 12)
        );
        this.fontBody.drawCentered("Clear Keybind", clearX + clearW / 2.0F, clearY + 15.0F, TEXT_MAIN.getRGB());
        this.fontSmall
            .drawCentered("ESC cancels binding", wx + winW / 2.0F, contentBottom - 12.0F, TEXT_FAINT.getRGB());
    }

    private void drawConfigTab(int mx, int my, float wx, float contentTop, float winW, float contentBottom) {
        float bx = wx + 14.0F;
        float bw = winW - 28.0F;
        String label = "GLOBAL CONFIG";
        this.fontSmall.drawWithShadow(label, bx, contentTop, TEXT_FAINT.getRGB());
        String path = (
                this.field_146297_k.field_71412_D == null ? "?" : this.field_146297_k.field_71412_D.getAbsolutePath()
            )
            + "\\MiauConfig.json";
        String clippedPath = this.clipText(this.fontSmall, path, bw);
        this.fontSmall.draw(clippedPath, bx, contentTop + 12.0F, new Color(60, 70, 88).getRGB());
        String[][] buttons = new String[][]{
            {"Save current settings", "Writes every module + property"},
            {"Load saved settings", "Restores from file"},
            {"Delete saved settings", "Removes the file"},
            {"Reset all modules", "Disables and clears binds"}
        };
        float y = contentTop + 34.0F;

        for (int i = 0; i < buttons.length; i++) {
            boolean hovered = mx >= bx && mx <= bx + bw && my >= y && my <= y + 36.0F;
            RoundedUtils.drawRound(
                bx, y, bw, 36.0F, 9.0F, hovered ? new Color(33, 212, 253, 50) : new Color(255, 255, 255, 8)
            );
            this.fontBody
                .drawWithShadow(buttons[i][0], bx + 12.0F, y + 10.0F, (hovered ? TEXT_MAIN : TEXT_DIM).getRGB());
            this.fontSmall
                .draw(
                    this.clipText(this.fontSmall, buttons[i][1], bw - 160.0F),
                    bx + 12.0F,
                    y + 22.0F,
                    TEXT_FAINT.getRGB()
                );
            y += 40.0F;
        }

        if (!this.status.isEmpty()) {
            this.fontSmall.drawCentered(this.status, wx + winW / 2.0F, contentBottom - 12.0F, ACCENT_A.getRGB());
        }
    }

    private void drawBindingOverlay(float w, float h, float winW, float winH, float wx, float wy) {
        RoundedUtils.drawRound(0.0F, 0.0F, w, h, 0.0F, new Color(4, 6, 12, 150));
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        RoundedUtils.drawRound(cx - 220.0F, cy - 46.0F, 440.0F, 92.0F, 14.0F, new Color(11, 14, 24, 240));
        RoundedUtils.drawRoundOutline(
            cx - 220.0F, cy - 46.0F, 440.0F, 92.0F, 14.0F, 1.0F, new Color(0, 0, 0, 0), withAlpha(ACCENT_A, 180)
        );
        RoundedUtils.drawGradientHorizontal(cx - 220.0F, cy - 46.0F, 440.0F, 2.5F, 1.5F, ACCENT_A, ACCENT_B);
        this.fontTitle.drawCentered("Press a key for " + this.module.getName(), cx, cy - 22.0F, TEXT_MAIN.getRGB());
        this.fontBody
            .drawCentered("Any key to bind  ·  ESC cancel  ·  DELETE clear", cx, cy + 20.0F, TEXT_DIM.getRGB());
    }

    protected void func_73864_a(int mx, int my, int button) throws IOException {
        super.func_73864_a(mx, my, button);
        if (this.binding) {
            this.binding = false;
        } else {
            float w = this.sw();
            float h = this.sh();
            float winW = Math.min(340.0F, w - 60.0F);
            float winH = Math.min(430.0F, h - 80.0F);
            float wx = (w - winW) / 2.0F;
            float wy = (h - winH) / 2.0F;
            float closeX = wx + winW - 40.0F;
            float closeY = wy + 12.0F;
            if (mx >= closeX && mx <= closeX + 26.0F && my >= closeY && my <= closeY + 26.0F) {
                this.field_146297_k.func_147108_a(new NoguiGui());
            } else {
                float tabY = wy + 56.0F + 4.0F;
                float tx = wx + 14.0F;

                for (String name : this.tabs) {
                    float tw = this.fontBody.width(name) + 26.0F;
                    if (mx >= tx && mx <= tx + tw && my >= tabY && my <= tabY + 30.0F) {
                        this.tab = name;
                        this.targetScroll = 0.0F;
                        return;
                    }

                    tx += tw + 6.0F;
                }

                if (this.tab.equals("Settings")) {
                    float contentTop = wy + 56.0F + 30.0F + 10.0F;
                    float contentBottom = wy + winH - 12.0F;
                    List<ModuleSettingsGui.Row> rows = this.buildRows();

                    for (int i = 0; i < rows.size(); i++) {
                        float rowY = contentTop + i * 30.0F - this.scroll;
                        if (!(rowY + 30.0F < contentTop) && !(rowY > contentBottom)) {
                            float rx = wx + 14.0F;
                            float rw = winW - 28.0F;
                            if (mx >= rx && mx <= rx + rw && my >= rowY && my <= rowY + 30.0F) {
                                this.handleRowClick(rows.get(i), mx, rx, rw);
                                return;
                            }
                        }
                    }
                } else if (this.tab.equals("Bind")) {
                    float bx = wx + 14.0F;
                    float bw = winW - 28.0F;
                    float by = wy + 56.0F + 30.0F + 16.0F;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + 64.0F) {
                        this.binding = true;
                    } else {
                        float clearW = 140.0F;
                        float clearX = bx + bw / 2.0F - clearW / 2.0F;
                        float clearY = by + 64.0F + 14.0F;
                        if (mx >= clearX && mx <= clearX + clearW && my >= clearY && my <= clearY + 30.0F) {
                            this.module.setKey(0);
                        }
                    }
                } else if (this.tab.equals("Config")) {
                    float bx = wx + 14.0F;
                    float bw = winW - 28.0F;
                    float y = wy + 56.0F + 30.0F + 10.0F + 34.0F;

                    for (int i = 0; i < 4; i++) {
                        if (mx >= bx && mx <= bx + bw && my >= y && my <= y + 36.0F) {
                            if (i == 0) {
                                this.saveConfig();
                            } else if (i == 1) {
                                this.loadConfig();
                            } else if (i == 2) {
                                this.deleteConfig();
                            } else {
                                this.resetAll();
                            }

                            return;
                        }

                        y += 40.0F;
                    }
                }
            }
        }
    }

    private void handleRowClick(ModuleSettingsGui.Row row, int mx, float rx, float rw) {
        if (row.type == 99) {
            this.binding = true;
        } else if (row.type == 0 && row.prop == null) {
            this.module.toggle();
        } else if (row.type == 0) {
            row.prop.setValue(!(Boolean)row.prop.getValue());
        } else if (row.type == 1) {
            this.dragging = row.prop;
            this.updateSlider(row.prop, mx, rx, rw);
        } else if (row.type == 2) {
            ((ModeProperty)row.prop).nextMode();
        }
    }

    private void updateSlider(Property<?> prop, float mx, float rx, float rw) {
        float min = 0.0F;
        float max = 100.0F;
        float trackX = rx + 4.0F;
        float trackW = rw - 8.0F;
        float ratio = clamp((mx - trackX) / trackW, 0.0F, 1.0F);
        if (prop instanceof FloatProperty) {
            FloatProperty fp = (FloatProperty)prop;
            min = fp.getMinimum();
            max = fp.getMaximum();
            fp.setValue(min + (max - min) * ratio);
        } else if (prop instanceof IntProperty) {
            IntProperty ip = (IntProperty)prop;
            min = ip.getMinimum().intValue();
            max = ip.getMaximum().intValue();
            ip.setValue(Math.round(min + (max - min) * ratio));
        } else {
            PercentProperty pp = (PercentProperty)prop;
            min = pp.getMinimum().intValue();
            max = pp.getMaximum().intValue();
            pp.setValue(Math.round(min + (max - min) * ratio));
        }
    }

    public void func_73876_c() {
        if (this.dragging != null) {
            if (!Mouse.isButtonDown(0)) {
                this.dragging = null;
            } else {
                float w = this.sw();
                float h = this.sh();
                float winW = Math.min(340.0F, w - 60.0F);
                float winH = Math.min(430.0F, h - 80.0F);
                float wx = (w - winW) / 2.0F;
                float wy = (h - winH) / 2.0F;
                float contentTop = wy + 56.0F + 30.0F + 10.0F;
                int mx = (int)(Mouse.getX() * this.sw() / this.field_146297_k.field_71443_c);
                List<ModuleSettingsGui.Row> rows = this.buildRows();

                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).prop == this.dragging) {
                        float rowY = contentTop + i * 30.0F - this.scroll;
                        this.updateSlider(this.dragging, mx, wx + 14.0F, winW - 28.0F);
                        break;
                    }
                }
            }
        }
    }

    protected void func_146286_b(int mx, int my, int state) {
        super.func_146286_b(mx, my, state);
        if (state == 0) {
            this.dragging = null;
        }
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheel = Mouse.getDWheel();
        if (wheel != 0 && this.tab.equals("Settings")) {
            int mx = (int)(Mouse.getX() * this.sw() / this.field_146297_k.field_71443_c);
            int my = (int)(this.sh() - Mouse.getY() * this.sh() / this.field_146297_k.field_71440_d);
            float w = this.sw();
            float h = this.sh();
            float wx = (w - Math.min(340.0F, w - 60.0F)) / 2.0F;
            float wy = (h - Math.min(430.0F, h - 80.0F)) / 2.0F;
            float contentTop = wy + 56.0F + 30.0F + 10.0F;
            float contentBottom = wy + Math.min(430.0F, h - 80.0F) - 12.0F;
            if (mx >= wx + 14.0F
                && mx <= wx + Math.min(340.0F, w - 60.0F) - 14.0F
                && my >= contentTop
                && my <= contentBottom) {
                this.targetScroll -= wheel / 120.0F * 34.0F;
                this.targetScroll = clamp(this.targetScroll, 0.0F, this.maxScroll);
            }
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (!this.binding) {
            if (keyCode == 1) {
                this.field_146297_k.func_147108_a(new NoguiGui());
            }
        } else {
            if (keyCode == 1) {
                this.binding = false;
            } else if (keyCode != 211 && keyCode != 14) {
                this.module.setKey(keyCode);
                this.binding = false;
            } else {
                this.module.setKey(0);
                this.binding = false;
            }
        }
    }

    private void saveConfig() {
        try {
            File file = new File(this.field_146297_k.field_71412_D, "MiauConfig.json");
            JsonObject json = new JsonObject();

            for (Module m : Miau.moduleManager.modules.values()) {
                JsonObject modJson = new JsonObject();
                modJson.addProperty("enabled", m.isEnabled());
                modJson.addProperty("key", m.getKey());
                JsonArray propsJson = new JsonArray();

                for (Property<?> p : m.getValues()) {
                    JsonObject propJson = new JsonObject();
                    propJson.addProperty("name", p.getName());
                    String formatted = p.formatValue();
                    if (formatted != null && !formatted.isEmpty()) {
                        propJson.addProperty("value", formatted);
                    }

                    propsJson.add(propJson);
                }

                modJson.add("properties", propsJson);
                json.add(m.getName(), modJson);
            }

            FileWriter writer = new FileWriter(file);

            try {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            } finally {
                writer.close();
            }

            this.status = "Settings saved";
        } catch (Exception e) {
            this.status = "Save failed";
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            File file = new File(this.field_146297_k.field_71412_D, "MiauConfig.json");
            if (!file.exists()) {
                this.status = "No saved config found";
                return;
            }

            JsonObject json = new JsonParser()
                .parse(
                    Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)
                        .stream()
                        .collect(Collectors.joining("\n"))
                )
                .getAsJsonObject();

            for (Entry<String, JsonElement> entry : json.entrySet()) {
                String modName = entry.getKey();
                JsonObject modJson = entry.getValue().getAsJsonObject();
                Module m = Miau.moduleManager.getModule(modName);
                if (m != null) {
                    if (modJson.has("enabled")) {
                        m.setEnabled(modJson.get("enabled").getAsBoolean());
                    }

                    if (modJson.has("key")) {
                        m.setKey(modJson.get("key").getAsInt());
                    }

                    if (modJson.has("properties")) {
                        for (JsonElement propElement : modJson.getAsJsonArray("properties")) {
                            JsonObject propJson = propElement.getAsJsonObject();
                            String propName = propJson.get("name").getAsString();
                            if (propJson.has("value")) {
                                String value = propJson.get("value").getAsString();

                                for (Property<?> p : m.getValues()) {
                                    if (p.getName().equals(propName)) {
                                        p.parseString(value);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.status = "Settings loaded";
        } catch (Exception e) {
            this.status = "Load failed";
            e.printStackTrace();
        }
    }

    private void deleteConfig() {
        try {
            File file = new File(this.field_146297_k.field_71412_D, "MiauConfig.json");
            if (file.exists()) {
                file.delete();
                this.status = "Config deleted";
            } else {
                this.status = "No config to delete";
            }
        } catch (Exception e) {
            this.status = "Delete failed";
            e.printStackTrace();
        }
    }

    private void resetAll() {
        for (Module m : Miau.moduleManager.modules.values()) {
            m.setEnabled(false);
            m.setKey(0);

            for (Property<?> p : m.getValues()) {
                p.parseString("");
            }
        }

        this.status = "All modules reset";
    }

    private void beginScissor(float x, float y, float w, float h) {
        float s = this.sr.func_78325_e();
        int sx = (int)(x * s);
        int sy = (int)(this.field_146297_k.field_71440_d - (y + h) * s);
        GL11.glEnable(3089);
        GL11.glScissor(sx, sy, (int)(w * s), (int)(h * s));
    }

    private void endScissor() {
        GL11.glDisable(3089);
    }

    private void drawCross(float cx, float cy, float r, float thick, Color color) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        GL11.glLineWidth(thick);
        GL11.glBegin(1);
        GL11.glVertex2f(cx - r, cy - r);
        GL11.glVertex2f(cx + r, cy + r);
        GL11.glVertex2f(cx - r, cy + r);
        GL11.glVertex2f(cx + r, cy - r);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    private static String stripCodes(String s) {
        return s == null ? "" : s.replaceAll("[&\\u00A7].", "");
    }

    private Color accent(float t) {
        float x = (float)(0.5 + 0.5 * Math.sin(t * Math.PI));
        return interpolate(ACCENT_A, ACCENT_B, x);
    }

    private float pulse() {
        return (float)(System.currentTimeMillis() % 7000L) / 7000.0F;
    }

    private String clipText(Font font, String text, float maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String clipped = text;

        while (!clipped.isEmpty() && font.width(clipped + "...") > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }

        return clipped + "...";
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static float expApproach(float a, float b, float delta, float tau) {
        float f = 1.0F - (float)Math.exp(-delta / tau);
        return a + (b - a) * f;
    }

    private static float easeOut(float x) {
        return 1.0F - (float)Math.pow(1.0 - x, 3.0);
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static Color interpolate(Color a, Color b, float t) {
        float x = clamp(t, 0.0F, 1.0F);
        return new Color(
            (int)(a.getRed() + (b.getRed() - a.getRed()) * x),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * x),
            (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * x),
            (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * x)
        );
    }

    private static class Row {
        int type;
        Property<?> prop;

        Row(int type, Property<?> prop) {
            this.type = type;
            this.prop = prop;
        }
    }
}
