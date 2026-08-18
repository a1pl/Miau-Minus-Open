package miau.ui;

import java.awt.Desktop;
import java.net.URI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class GuiNotificationClient extends GuiScreen {
    private static final int BG_OVERLAY = -1342177280;
    private static final int PANEL_BG = -401205728;
    private static final int PANEL_BORDER = 1090519039;
    private static final int DIVIDER = 352321535;
    private static final int DOT_GRID = 352321535;
    private static final int TEXT_1 = -855306;
    private static final int TEXT_2 = -6513488;
    private static final int TEXT_3 = -9737344;
    private static final int PINK = -633718;
    private static final int PINK_SOFT = -1949831;
    private static final int PINK_GLOW = 1089885322;
    private static final int PANEL_RADIUS = 12;
    private static final int BUTTON_RADIUS = 7;
    private final GuiScreen parent;
    private final String title;
    private final String desc;
    private final String btn1Text;
    private final String btn1Link;
    private final String btn2Text;
    private final String btn2Link;

    public GuiNotificationClient(
        GuiScreen parent, String title, String desc, String btn1Text, String btn1Link, String btn2Text, String btn2Link
    ) {
        this.parent = parent;
        this.title = title;
        this.desc = desc;
        this.btn1Text = btn1Text;
        this.btn1Link = btn1Link;
        this.btn2Text = btn2Text;
        this.btn2Link = btn2Link;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        int centerX = this.field_146294_l / 2;
        int centerY = this.field_146295_m / 2;
        int y = centerY + 52;
        if (this.btn2Text != null && !this.btn2Text.isEmpty()) {
            this.field_146292_n
                .add(new GuiNotificationClient.StyledButton(0, centerX - 121, y, 116, 24, this.btn1Text, true));
            this.field_146292_n
                .add(new GuiNotificationClient.StyledButton(1, centerX + 5, y, 116, 24, this.btn2Text, false));
        } else {
            this.field_146292_n
                .add(new GuiNotificationClient.StyledButton(0, centerX - 58, y, 116, 24, this.btn1Text, true));
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button.field_146127_k == 0) {
            this.open(this.btn1Link);
        } else if (button.field_146127_k == 1) {
            this.open(this.btn2Link);
        }
    }

    private void open(String link) {
        if (link != null && !link.isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (Exception var3) {
            }
        } else {
            this.field_146297_k.func_147108_a(this.parent);
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        if (this.parent != null) {
            this.parent.func_73863_a(0, 0, partialTicks);
        } else {
            this.func_146276_q_();
        }

        func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1342177280);
        this.drawDotGrid();
        int centerX = this.field_146294_l / 2;
        int centerY = this.field_146295_m / 2;
        int panelWidth = 246;
        int panelHeight = 156;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2 - 8;
        int right = centerX + panelWidth / 2;
        int bottom = centerY + panelHeight / 2 + 8;
        this.drawPanel(left, top, right, bottom, 12);
        int contentX = left + 16;
        int y = top + 16;
        this.drawScaledString(this.title, contentX, y, 1.1F, -855306, true);
        y += 17;
        func_73734_a(left + 16, y, right - 16, y + 1, 352321535);
        y += 14;
        this.field_146289_q.func_78279_b(this.desc, contentX, y, panelWidth - 32, -6513488);
        y += 40;
        func_73734_a(left + 16, y, right - 16, y + 1, 352321535);
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(int left, int top, int right, int bottom, int radius) {
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        drawRoundedRect(left - 1, top - 1, right + 1, bottom + 1, radius + 1, 1090519039);
        drawRoundedRect(left, top, right, bottom, radius, -401205728);
        GlStateManager.func_179084_k();
    }

    private void drawDotGrid() {
        int step = 26;

        for (int x = 0; x < this.field_146294_l; x += step) {
            for (int y = 0; y < this.field_146295_m; y += step) {
                func_73734_a(x, y, x + 1, y + 1, 352321535);
            }
        }
    }

    private void drawScaledString(String text, int x, int y, float scale, int color, boolean bold) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y, 0.0F);
        GlStateManager.func_179152_a(scale, scale, 1.0F);
        this.field_146289_q.func_78276_b(bold ? "§l" + text : text, 0, 0, color);
        GlStateManager.func_179121_F();
    }

    private static void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color) {
        drawRoundedGradientRect(left, top, right, bottom, radius, color, color);
    }

    private static void drawRoundedGradientRect(
        int left, int top, int right, int bottom, int radius, int colorTop, int colorBottom
    ) {
        int height = bottom - top;
        int width = right - left;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        float aTop = (colorTop >> 24 & 0xFF) / 255.0F;
        float aBot = (colorBottom >> 24 & 0xFF) / 255.0F;
        float rTop = (colorTop >> 16 & 0xFF) / 255.0F;
        float rBot = (colorBottom >> 16 & 0xFF) / 255.0F;
        float gTop = (colorTop >> 8 & 0xFF) / 255.0F;
        float gBot = (colorBottom >> 8 & 0xFF) / 255.0F;
        float bTop = (colorTop & 0xFF) / 255.0F;
        float bBot = (colorBottom & 0xFF) / 255.0F;

        for (int i = 0; i < height; i++) {
            float t = height <= 1 ? 0.0F : (float)i / (height - 1);
            int a = (int)((aTop + (aBot - aTop) * t) * 255.0F);
            int rr = (int)((rTop + (rBot - rTop) * t) * 255.0F);
            int gg = (int)((gTop + (gBot - gTop) * t) * 255.0F);
            int bb = (int)((bTop + (bBot - bTop) * t) * 255.0F);
            int color = a << 24 | rr << 16 | gg << 8 | bb;
            int inset = 0;
            int distFromEdge = Math.min(i, height - 1 - i);
            if (distFromEdge < r) {
                int dy = r - distFromEdge - 1;
                inset = r - (int)Math.sqrt(Math.max(0, r * r - dy * dy));
            }

            func_73734_a(left + inset, top + i, right - inset, top + i + 1, color);
        }
    }

    private static class StyledButton extends GuiButton {
        private final boolean primary;

        StyledButton(int id, int x, int y, int w, int h, String text, boolean primary) {
            super(id, x, y, w, h, text);
            this.primary = primary;
        }

        public void func_146112_a(Minecraft mc, int mouseX, int mouseY) {
            if (this.field_146125_m) {
                this.field_146123_n = mouseX >= this.field_146128_h
                    && mouseY >= this.field_146129_i
                    && mouseX < this.field_146128_h + this.field_146120_f
                    && mouseY < this.field_146129_i + this.field_146121_g;
                GlStateManager.func_179147_l();
                GlStateManager.func_179112_b(770, 771);
                if (this.primary && this.field_146123_n) {
                    GuiNotificationClient.drawRoundedRect(
                        this.field_146128_h - 3,
                        this.field_146129_i - 3,
                        this.field_146128_h + this.field_146120_f + 3,
                        this.field_146129_i + this.field_146121_g + 3,
                        10,
                        1089885322
                    );
                }

                if (this.primary) {
                    int top = this.field_146123_n ? -633718 : -1949831;
                    int bottom = this.field_146123_n ? -1949831 : -633718;
                    GuiNotificationClient.drawRoundedGradientRect(
                        this.field_146128_h,
                        this.field_146129_i,
                        this.field_146128_h + this.field_146120_f,
                        this.field_146129_i + this.field_146121_g,
                        7,
                        top,
                        bottom
                    );
                } else {
                    int border = this.field_146123_n ? 1358954495 : 822083583;
                    int fill = this.field_146123_n ? 452984831 : 285212671;
                    GuiNotificationClient.drawRoundedRect(
                        this.field_146128_h,
                        this.field_146129_i,
                        this.field_146128_h + this.field_146120_f,
                        this.field_146129_i + this.field_146121_g,
                        7,
                        border
                    );
                    GuiNotificationClient.drawRoundedRect(
                        this.field_146128_h + 1,
                        this.field_146129_i + 1,
                        this.field_146128_h + this.field_146120_f - 1,
                        this.field_146129_i + this.field_146121_g - 1,
                        Math.max(0, 6),
                        fill
                    );
                }

                int textColor = this.primary ? -1 : (this.field_146123_n ? -855306 : -6513488);
                int strWidth = mc.field_71466_p.func_78256_a(this.field_146126_j);
                mc.field_71466_p
                    .func_78276_b(
                        this.field_146126_j,
                        this.field_146128_h + (this.field_146120_f - strWidth) / 2,
                        this.field_146129_i + (this.field_146121_g - 8) / 2,
                        textColor
                    );
                GlStateManager.func_179084_k();
            }
        }
    }
}
