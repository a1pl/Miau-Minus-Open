package miau.ui.clickgui.rise;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.module.Module;
import miau.module.ModuleManager;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.property.properties.TextProperty;
import miau.util.client.KeyBindUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RiseClickGui extends GuiScreen {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final Font FONT_REGULAR = FontRepository.getFont("Inter Regular", 18.0F);
    private static final Font FONT_SEMIBOLD = FontRepository.getFont("Inter SemiBold", 18.0F);
    private static final Font FONT_TITLE = FontRepository.getFont("augustus", 35.0F);
    private static final Font FONT_MOD_NAME = FontRepository.getFont("Inter SemiBold", 18.0F);
    private static final Color WIN_BG = new Color(8, 9, 15, 255);
    private static final Color SIDEBAR_BG = new Color(6, 7, 12, 255);
    private static final Color CONTENT_BG = new Color(10, 11, 18, 255);
    private static final Color DIVIDER = new Color(20, 22, 34, 180);
    private static final Color ACCENT = new Color(30, 120, 255, 255);
    private static final Color ACCENT_DIM = new Color(30, 120, 255, 150);
    private static final Color SRCH_BG = new Color(13, 14, 22, 255);
    private static final Color SRCH_BOR = new Color(32, 36, 52, 255);
    private static final Color SRCH_BOR_F = new Color(30, 120, 255, 190);
    private static final Color SRCH_HINT = new Color(66, 72, 96, 255);
    private static final Color MOD_BG = new Color(13, 14, 22, 255);
    private static final Color MOD_BG_H = new Color(19, 21, 33, 255);
    private static final Color MOD_NAME = new Color(200, 208, 230, 255);
    private static final Color MOD_NAME_ON = new Color(30, 120, 255, 255);
    private static final Color MOD_CAT_C = new Color(30, 120, 255, 175);
    private static final Color MOD_DESC = new Color(86, 93, 118, 255);
    private static final Color MOD_SEP = new Color(21, 23, 35, 210);
    private static final Color SET_BG = new Color(8, 9, 15, 255);
    private static final Color SET_LBL = new Color(178, 187, 210, 255);
    private static final Color SL_TRACK = new Color(22, 24, 38, 255);
    private static final Color SL_FILL = new Color(30, 120, 255, 255);
    private static final Color TOG_ON = new Color(30, 120, 255, 255);
    private static final Color TOG_OFF = new Color(36, 40, 55, 255);
    private static final Color SEP_C = new Color(22, 24, 38, 255);
    private static final Color KEY_C = new Color(128, 138, 160, 255);
    private static final Color KEY_H = new Color(30, 120, 255, 255);
    private static final Color OVR_DIM = new Color(0, 0, 0, 155);
    private static final Color OVR_BOX = new Color(13, 15, 24, 250);
    private static final Color CAT_HOV = new Color(255, 255, 255, 8);
    private static final Color CAT_TXT = new Color(128, 136, 160, 255);
    private static final Color CAT_TXT_A = new Color(255, 255, 255, 255);
    private static final Color CAT_PILL = new Color(18, 24, 46, 210);
    private static final float CR = 12.0F;
    private static final int SW = 105;
    private static final int HDR_H = 33;
    private static final int CAT_H = 22;
    private static final int CHDR_H = 30;
    private static final int MOD_H = 37;
    private static final int M_PAD = 5;
    private static final int SBW = 3;
    private static final int DEF_W = 500;
    private static final int DEF_H = 322;
    private static final int MIN_W = 336;
    private static final int MIN_H = 222;
    private static final float ANIM_SPD = 0.18F;
    private static final float SCROLL_FRIC = 0.82F;
    private static final String[] TARGET_NAMES = new String[]{"Players", "Mobs", "Animals", "Invisible", "Dead"};
    private final RiseClickGui.SavedState saved = new RiseClickGui.SavedState();
    private int gX = 0;
    private int gY = 0;
    private int gW = 500;
    private int gH = 322;
    private boolean dragging = false;
    private int dDX = 0;
    private int dDY = 0;
    private String selCat = "Combat";
    private boolean srchOn = false;
    private String srchTxt = "";
    private float mScroll = 0.0F;
    private int mMax = 0;
    private float scrollVel = 0.0F;
    private String openMod = null;
    private boolean showTargets = false;
    private boolean curOn = true;
    private long blinkT = 0L;
    private boolean clickL = false;
    private boolean prevML = false;
    private boolean clickR = false;
    private boolean prevMR = false;
    private final Map<String, Float> hovAnim = new HashMap<>();
    private final Map<String, Float> expandAnim = new HashMap<>();
    private final Map<String, Float> toggleAnim = new HashMap<>();
    private final Map<String, Float> modFadeAnim = new HashMap<>();
    private final Map<String, Float> catSelAnim = new HashMap<>();
    private float contentFade = 1.0F;
    private int contentFadeDir = 0;
    private String pendingCat = null;
    private Module bindMod = null;
    private long bindOverlayOpenTime = 0L;
    private final Map<String, Boolean> listExpanded = new HashMap<>();
    private Property<?> sliderHeld = null;
    private final Map<String, Boolean> toggleTargets = new HashMap<>();
    private long lastFrameMs = System.currentTimeMillis();

    public void func_73866_w_() {
        ScaledResolution sr = new ScaledResolution(mc);
        this.gX = (sr.func_78326_a() - this.gW) / 2;
        this.gY = (sr.func_78328_b() - this.gH) / 2;
        this.selCat = this.saved.selCat;
        this.srchTxt = this.saved.srchTxt;
        this.srchOn = this.saved.srchOn;
        this.mScroll = this.saved.scroll;
        this.expandAnim.putAll(this.saved.expandAnim);
        this.openMod = this.saved.openMod;
        this.showTargets = this.saved.showTargets;
        this.contentFade = 1.0F;
        this.contentFadeDir = 0;
        this.pendingCat = null;
        super.func_73866_w_();
    }

    public void func_146281_b() {
        this.saved.selCat = this.selCat;
        this.saved.srchTxt = this.srchTxt;
        this.saved.srchOn = this.srchOn;
        this.saved.scroll = this.mScroll;
        this.saved.expandAnim = new HashMap<>(this.expandAnim);
        this.saved.openMod = this.openMod;
        this.saved.showTargets = this.showTargets;
        super.func_146281_b();
    }

    public boolean func_73868_f() {
        return false;
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = (float)(now - this.lastFrameMs) / 16.67F;
        if (dt < 0.5F) {
            dt = 0.5F;
        }

        if (dt > 3.0F) {
            dt = 3.0F;
        }

        this.lastFrameMs = now;
        if (this.dragging) {
            this.gX = mouseX - this.dDX;
            this.gY = mouseY - this.dDY;
        }

        boolean ml = Mouse.isButtonDown(0);
        boolean mr = Mouse.isButtonDown(1);
        this.clickL = !this.prevML && ml;
        this.clickR = !this.prevMR && mr;
        this.prevML = ml;
        this.prevMR = mr;
        if (!ml) {
            this.dragging = false;
            this.sliderHeld = null;
        }

        if (now - this.blinkT > 530L) {
            this.curOn = !this.curOn;
            this.blinkT = now;
        }

        this.mScroll = this.mScroll + this.scrollVel * dt;
        this.mScroll = Math.max(0.0F, Math.min(this.mMax, this.mScroll));
        this.scrollVel = (float)(this.scrollVel * Math.pow(0.82F, dt));
        if (Math.abs(this.scrollVel) < 0.3F) {
            this.scrollVel = 0.0F;
        }

        if (this.contentFadeDir == -1) {
            this.contentFade -= 0.36F * dt;
            if (this.contentFade <= 0.0F) {
                if (this.pendingCat != null) {
                    this.selCat = this.pendingCat;
                    this.pendingCat = null;
                    this.showTargets = false;
                }

                this.contentFadeDir = 1;
                this.mScroll = 0.0F;
                this.scrollVel = 0.0F;
                this.openMod = null;
                this.modFadeAnim.clear();
            }
        } else if (this.contentFadeDir == 1) {
            this.contentFade += 0.36F * dt;
            if (this.contentFade >= 1.0F) {
                this.contentFadeDir = 0;
            }
        }

        RoundedUtils.drawRoundedRectRise(
            this.gX, this.gY, this.gW, this.gH, 12.0F, WIN_BG.getRGB(), true, true, true, true
        );
        RenderUtil.drawRect(this.gX + 12.0F, this.gY, this.gX + this.gW - 12.0F, this.gY + this.gH, WIN_BG.getRGB());
        RenderUtil.drawRect(this.gX, this.gY + 12.0F, this.gX + this.gW, this.gY + this.gH - 12.0F, WIN_BG.getRGB());
        this.drawSidebar(mouseX, mouseY);
        this.drawContent(mouseX, mouseY);
        if (this.bindMod != null) {
            this.drawKeybindOverlay();
        }

        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    private void drawSidebar(int mx, int my) {
        float x1 = this.gX;
        float y1 = this.gY;
        RoundedUtils.drawRoundedRectRise(x1, y1, 105.0F, this.gH, 12.0F, SIDEBAR_BG.getRGB(), true, false, false, true);
        String titleStr = "Miau Minus";
        String verStr = "b1";
        float titleW = FONT_TITLE.getStringWidth(titleStr);
        float blockW = titleW + 3.0F + FONT_TITLE.getStringWidth(verStr);
        float tX = x1 + (105.0F - blockW) / 2.0F;
        float tY = y1 + (33 - FONT_TITLE.getFontHeight()) / 2.0F - 1.5F;
        FONT_TITLE.draw(titleStr, tX, tY, Color.WHITE.getRGB());
        FONT_TITLE.draw(verStr, tX + titleW + 3.0F, tY - 2.0F, new Color(30, 120, 255, 255).getRGB());
        RenderUtil.drawRect(x1 + 12.0F, y1 + 33.0F, x1 + 105.0F - 12.0F, y1 + 33.0F + 1.0F, DIVIDER.getRGB());
        this.drawCatList(mx, my, x1, y1 + 33.0F + 1.0F, 105);
    }

    private List<String> getCategories() {
        List<String> result = new ArrayList<>();
        ModuleManager mm = Miau.moduleManager;
        if (mm != null && mm.modules != null) {
            for (Module mod : mm.modules.values()) {
                String cat = this.getCategoryName(mod);
                if (cat != null && !result.contains(cat)) {
                    result.add(cat);
                }
            }
        }

        return result;
    }

    private void drawCatList(int mx, int my, float sx, float startY, int sw) {
        float y = startY + 8.0F;
        float px1 = sx + 10.0F;
        float px2 = sx + sw - 10.0F;
        float rW = px2 - px1;
        float txX = px1 + 24.0F;
        boolean sAct = this.srchOn || this.selCat == null && !this.srchTxt.isEmpty();
        boolean sHov = this.inRect(mx, my, px1, y, rW, 22.0F);
        if (sAct) {
            RoundedUtils.drawRoundedRectRise(
                px1, y + 1.0F, rW, 20.0F, 6.0F, CAT_PILL.getRGB(), false, false, false, false
            );
            RenderUtil.drawRect(px1, y + 4.0F, px1 + 2.5F, y + 22.0F - 4.0F, new Color(80, 150, 255, 255).getRGB());
        } else if (sHov) {
            RoundedUtils.drawRoundedRectRise(
                px1, y + 1.0F, rW, 20.0F, 6.0F, CAT_HOV.getRGB(), false, false, false, false
            );
        }

        int sCol = sAct ? CAT_TXT_A.getRGB() : CAT_TXT.getRGB();
        FONT_REGULAR.draw("Search", txX, y + (22 - FONT_REGULAR.getFontHeight()) / 2.0F - 1.5F, sCol);
        if (this.clickL && sHov) {
            this.showTargets = false;
            this.selCat = null;
            this.srchOn = true;
            this.openMod = null;
            this.mScroll = 0.0F;
            this.scrollVel = 0.0F;
        }

        y += 26.0F;

        for (String cat : this.getCategories()) {
            boolean isAct = this.selCat != null && this.selCat.equals(cat) && !this.srchOn && this.srchTxt.isEmpty();
            boolean isHov = this.inRect(mx, my, px1, y, rW, 22.0F);
            float prev = this.catSelAnim.getOrDefault(cat, 0.0F);
            float newVal = prev + ((isAct ? 1.0F : 0.0F) - prev) * 0.18F * 2.0F * this.dt();
            this.catSelAnim.put(cat, newVal);
            float t = this.easeOutQ(newVal);
            if (t > 0.005F) {
                RoundedUtils.drawRoundedRectRise(
                    px1,
                    y + 1.0F,
                    rW,
                    20.0F,
                    6.0F,
                    new Color(18, 24, 46, (int)(210.0F * t)).getRGB(),
                    false,
                    false,
                    false,
                    false
                );
                RenderUtil.drawRect(px1, y + 4.0F, px1 + 2.5F, y + 22.0F - 4.0F, new Color(80, 150, 255, 255).getRGB());
            } else if (isHov) {
                RoundedUtils.drawRoundedRectRise(
                    px1, y + 1.0F, rW, 20.0F, 6.0F, CAT_HOV.getRGB(), false, false, false, false
                );
            }

            int col = this.lerpColor(CAT_TXT, CAT_TXT_A, t).getRGB();
            FONT_SEMIBOLD.draw(cat, txX, y + (22 - FONT_SEMIBOLD.getFontHeight()) / 2.0F - 1.5F, col);
            if (this.clickL && isHov && this.bindMod == null && (this.showTargets || !cat.equals(this.selCat))) {
                this.showTargets = false;
                this.pendingCat = cat;
                this.contentFadeDir = -1;
                this.contentFade = 1.0F;
                this.srchOn = false;
                this.srchTxt = "";
            }

            y += 26.0F;
        }

        y += 4.0F;
        RenderUtil.drawRect(px1 + 4.0F, y, px2 - 4.0F, y + 1.0F, DIVIDER.getRGB());
        y += 6.0F;
        boolean tHov = this.inRect(mx, my, px1, y, rW, 22.0F);
        if (this.showTargets) {
            RoundedUtils.drawRoundedRectRise(
                px1, y + 1.0F, rW, 20.0F, 6.0F, CAT_PILL.getRGB(), false, false, false, false
            );
            RenderUtil.drawRect(px1, y + 4.0F, px1 + 2.5F, y + 22.0F - 4.0F, new Color(80, 150, 255, 255).getRGB());
        } else if (tHov) {
            RoundedUtils.drawRoundedRectRise(
                px1, y + 1.0F, rW, 20.0F, 6.0F, CAT_HOV.getRGB(), false, false, false, false
            );
        }

        int tCol = this.showTargets ? CAT_TXT_A.getRGB() : CAT_TXT.getRGB();
        FONT_SEMIBOLD.draw("Targets", txX, y + (22 - FONT_SEMIBOLD.getFontHeight()) / 2.0F - 1.0F, tCol);
        if (this.clickL && tHov && this.bindMod == null && !this.showTargets) {
            this.showTargets = true;
            this.selCat = null;
            this.srchOn = false;
            this.srchTxt = "";
            this.contentFadeDir = -1;
            this.contentFade = 1.0F;
            this.openMod = null;
            this.mScroll = 0.0F;
            this.scrollVel = 0.0F;
        }
    }

    private void drawContent(int mx, int my) {
        float cx = this.gX + 105;
        float cy = this.gY;
        float cw = this.gW - 105;
        float ch = this.gH;
        RoundedUtils.drawRoundedRectRise(cx, cy, cw, ch, 12.0F, CONTENT_BG.getRGB(), false, true, true, false);
        this.drawSearchBar(mx, my, cx, cy, cw);
        RenderUtil.drawRect(cx + 8.0F, cy + 30.0F - 1.0F, cx + cw - 8.0F, cy + 30.0F, DIVIDER.getRGB());
        float listY = cy + 30.0F;
        float listH = ch - 30.0F;
        GL11.glEnable(3089);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.func_78325_e();
        GL11.glScissor(
            (int)(cx * sf), (int)((this.field_146295_m - (cy + ch)) * sf), (int)(cw * sf), (int)((ch - 30.0F) * sf)
        );
        GL11.glColor4f(1.0F, 1.0F, 1.0F, this.easeOutQ(this.contentFade));
        if (this.showTargets) {
            this.drawTargetsPanel(mx, my, cx, listY, cw, listH);
        } else {
            this.drawModList(mx, my, cx, listY, cw, listH);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3089);
        this.drawScrollbar(cx + cw - 3.0F - 3.0F, listY + 4.0F, listH - 8.0F, (int)this.mScroll, this.mMax);
    }

    private void drawSearchBar(int mx, int my, float cx, float cy, float cw) {
        float srH = 26.0F;
        float srW = cw * 0.55F;
        float srX = cx + cw - srW - 14.0F;
        float srY = cy + (30.0F - srH) / 2.0F;
        boolean hov = this.inRect(mx, my, srX, srY, srW, srH);
        Color bg = this.srchOn ? new Color(19, 21, 33, 255) : SRCH_BG;
        Color bor = this.srchOn ? SRCH_BOR_F : (hov ? new Color(42, 47, 66, 255) : SRCH_BOR);
        RoundedUtils.drawRound(srX, srY, srW, srH, 8.0F, bg);
        RoundedUtils.drawRoundOutline(srX, srY, srW, srH, 8.0F, 1.0F, bg, bor);
        float ty2 = srY + (srH - FONT_REGULAR.getFontHeight()) / 2.0F + 1.5F;
        float tx = srX + 19.0F;
        if (this.srchTxt.isEmpty() && !this.srchOn) {
            FONT_REGULAR.draw("Start typing to search...", tx, ty2, SRCH_HINT.getRGB());
        } else {
            FONT_REGULAR.draw(this.srchTxt, tx, ty2, Color.WHITE.getRGB());
        }

        if (this.srchOn && this.curOn) {
            float bx = tx + FONT_REGULAR.getStringWidth(this.srchTxt);
            RenderUtil.drawRect(bx, srY + 4.0F, bx + 1.0F, srY + srH - 4.0F, new Color(210, 218, 240, 190).getRGB());
        }

        if (this.clickL && this.bindMod == null) {
            boolean wasOn = this.srchOn;
            this.srchOn = hov;
            if (this.srchOn && !wasOn) {
                this.selCat = null;
            }
        }
    }

    private void drawModList(int mx, int my, float cx, float cy, float cw, float ch) {
        List<Module> mods = this.getMods();
        float iw = cw - 10.0F - 3.0F - 6.0F;
        float gap = 4.0F;

        for (Module mod : mods) {
            boolean isOpen = mod.getName().equals(this.openMod);
            String name = mod.getName();
            float prev = this.expandAnim.getOrDefault(name, 0.0F);
            float target = isOpen ? 1.0F : 0.0F;
            this.expandAnim.put(name, prev + (target - prev) * 0.18F * this.dt());
        }

        float totalH = 0.0F;

        for (Module mod : mods) {
            totalH += 37.0F + gap;
            float eT = this.easeOut(this.expandAnim.getOrDefault(mod.getName(), 0.0F));
            if (eT > 0.0F) {
                totalH += this.calcInlineH(mod) * eT;
            }
        }

        this.mMax = Math.max(0, (int)(totalH - (ch - 8.0F)));
        this.mScroll = Math.max(0.0F, Math.min(this.mMax, this.mScroll));
        float y = cy + 8.0F - this.mScroll;

        for (int idx = 0; idx < mods.size(); idx++) {
            Module mod = mods.get(idx);
            float ix = cx + 5.0F;
            float iy = y;
            float ix2 = ix + iw;
            float eT = this.easeOut(this.expandAnim.getOrDefault(mod.getName(), 0.0F));
            float animH = this.calcInlineH(mod) * eT;
            float bottom = iy + 37.0F + animH;
            if (!(bottom < cy) && !(iy > cy + ch)) {
                boolean inMod = this.inRect(mx, my, ix, iy, iw, 37.0F);
                String nameK = mod.getName();
                float prev = this.hovAnim.getOrDefault(nameK, 0.0F);
                this.hovAnim.put(nameK, prev + ((inMod ? 1.0F : 0.0F) - prev) * 0.18F * 1.5F * this.dt());
                Color bgC = this.lerpColor(MOD_BG, MOD_BG_H, this.easeOutQ(this.hovAnim.getOrDefault(nameK, 0.0F)));
                boolean isOpen = mod.getName().equals(this.openMod);
                float modR = 8.0F;
                float cardY = iy;
                RoundedUtils.drawRoundedRectRise(
                    ix, cardY, iw, 37.0F, modR, bgC.getRGB(), eT <= 0.01F, true, true, eT <= 0.01F
                );
                if (mod.isEnabled()) {
                    RoundedUtils.drawRoundedRectRise(
                        ix, cardY + 9.0F, 3.0F, 19.0F, 1.5F, ACCENT.getRGB(), false, false, false, false
                    );
                }

                float nX = ix + 14.0F;
                float nY = cardY + 9.0F;
                int nameColor = mod.isEnabled() ? MOD_NAME_ON.getRGB() : MOD_NAME.getRGB();
                FONT_MOD_NAME.draw(mod.getName(), nX, nY, nameColor);
                float nW = FONT_MOD_NAME.getStringWidth(mod.getName());
                float tgY = nY + (FONT_MOD_NAME.getFontHeight() - FONT_REGULAR.getFontHeight()) / 2.0F;
                FONT_REGULAR.draw("(" + this.getCategoryNameFor(mod) + ")", nX + nW + 5.0F, tgY, MOD_CAT_C.getRGB());
                if (this.clickL && inMod && this.bindMod == null) {
                    mod.toggle();
                }

                if (this.clickR && inMod && this.bindMod == null) {
                    this.openMod = isOpen ? null : mod.getName();
                }

                y += 37.0F;
                if (eT > 0.01F) {
                    float sh = this.calcInlineH(mod);
                    float panelTop = y;
                    float panelBottom = y + sh;
                    float clipTop = Math.max(panelTop, cy);
                    float clipBottom = Math.min(panelBottom, cy + ch);
                    float clipH = clipBottom - clipTop;
                    if (clipH > 0.0F) {
                        ScaledResolution sr = new ScaledResolution(mc);
                        int sf = sr.func_78325_e();
                        GL11.glScissor(
                            (int)(ix * sf),
                            (int)((this.field_146295_m - clipBottom) * sf),
                            (int)(iw * sf),
                            (int)(clipH * sf)
                        );
                        RoundedUtils.drawRoundedRectRise(ix, y, iw, sh, modR, SET_BG.getRGB(), false, false, true, true);
                        this.drawInlineSettings(mx, my, ix + 12.0F, y + 6.0F, iw - 24.0F, mod);
                        GL11.glScissor(
                            (int)(cx * sf),
                            (int)((this.field_146295_m - (cy + ch)) * sf),
                            (int)(cw * sf),
                            (int)((ch - 30.0F) * sf)
                        );
                    }

                    y += animH;
                }

                y += gap;
            } else {
                y += 37.0F + gap + animH;
            }
        }
    }

    private void drawInlineSettings(int mx, int my, float x, float startY, float w, Module mod) {
        float yy = startY;
        String kn = mod.getKey() == 0 ? "None" : KeyBindUtil.getKeyName(mod.getKey());
        String kTxt = "Keybind: " + kn;
        float kW = FONT_REGULAR.getStringWidth(kTxt);
        boolean kHov = this.inRect(mx, my, x, yy, kW, 12.0F);
        FONT_REGULAR.draw(kTxt, x, yy, !kHov && this.bindMod != mod ? KEY_C.getRGB() : KEY_H.getRGB());
        if (this.clickL && kHov && this.bindMod == null) {
            this.bindMod = mod;
            this.bindOverlayOpenTime = System.currentTimeMillis();
        }

        yy += 16.0F;
        RenderUtil.drawRect(x, yy, x + w, yy + 1.0F, SEP_C.getRGB());
        yy += 7.0F;

        for (Property<?> value : mod.getValues()) {
            if (value.isVisible() && !(value instanceof DragProperty)) {
                yy = this.drawValue(mx, my, value, x, yy, w) + 5.0F;
            }
        }
    }

    private float drawValue(int mx, int my, Property<?> v, float x, float y, float w) {
        if (v instanceof BooleanProperty) {
            return this.drawBoolean((BooleanProperty)v, mx, my, x, y, w);
        }

        if (v instanceof FloatProperty) {
            FloatProperty fp = (FloatProperty)v;
            return fp.isDoubleSlider()
                ? this.drawFloatRange(fp, mx, my, x, y, w)
                : this.drawFloatSlider(fp, mx, my, x, y, w);
        }

        if (v instanceof IntProperty) {
            return this.drawIntSlider((IntProperty)v, mx, my, x, y, w);
        }

        if (v instanceof PercentProperty) {
            return this.drawPercentSlider((PercentProperty)v, mx, my, x, y, w);
        }

        if (v instanceof ModeProperty) {
            return this.drawModeList((ModeProperty)v, mx, my, x, y, w);
        }

        if (v instanceof ColorProperty) {
            return this.drawColorValue((ColorProperty)v, x, y, w);
        }

        if (v instanceof TextProperty) {
            return this.drawTextValue((TextProperty)v, x, y, w);
        }

        FONT_REGULAR.draw(v.getName() + ": " + v.formatValue(), x, y, SET_LBL.getRGB());
        return y + 13.0F;
    }

    private float drawBoolean(BooleanProperty v, int mx, int my, float x, float y, float w) {
        FONT_SEMIBOLD.draw(v.getName(), x, y + 1.0F, SET_LBL.getRGB());
        float tw = 28.0F;
        float th = 13.0F;
        float tx = x + w - tw;
        float ty = y;
        String tKey = v.getName() + "_tog";
        float prev = this.toggleAnim.getOrDefault(tKey, v.getValue() ? 1.0F : 0.0F);
        float target = v.getValue() ? 1.0F : 0.0F;
        float newT = prev + (target - prev) * 0.18F * 2.0F * this.dt();
        this.toggleAnim.put(tKey, newT);
        float eT = this.easeOutQ(newT);
        RoundedUtils.drawRoundedRectRise(
            tx, ty, tw, th, th / 2.0F, this.lerpColor(TOG_OFF, TOG_ON, eT).getRGB(), false, false, false, false
        );
        float tr = th / 2.0F - 1.5F;
        float thmXOff = tw - tr * 2.0F - 3.0F;
        float dmX = tx + 1.5F + thmXOff * eT;
        RoundedUtils.drawRoundedRectRise(
            dmX, ty + 1.5F, tr * 2.0F, th - 3.0F, tr, Color.WHITE.getRGB(), false, false, false, false
        );
        if (this.clickL && this.inRect(mx, my, tx - 3.0F, ty - 3.0F, tw + 6.0F, th + 6.0F)) {
            v.setValue(!v.getValue());
        }

        return y + 15.0F;
    }

    private float drawFloatSlider(FloatProperty v, int mx, int my, float x, float y, float w) {
        float min = v.getMin();
        float max = v.getMax();
        float cur = v.getValue();
        FONT_REGULAR.draw(v.getName() + ": " + this.trim(cur), x, y, SET_LBL.getRGB());
        float sy = y + 11.0F;
        float fillX = x + w * this.clamp01((cur - min) / (max - min));
        this.drawSliderTrack(x, sy, w, fillX);
        if (Mouse.isButtonDown(0) && this.inSlider(mx, my, x, sy, w) || this.sliderHeld == v) {
            float nv = min + (max - min) * this.clamp01((mx - x) / w);
            v.setValue(nv);
            this.sliderHeld = v;
        }

        return sy + 10.0F;
    }

    private float drawIntSlider(IntProperty v, int mx, int my, float x, float y, float w) {
        int min = v.getMinimum();
        int max = v.getMaximum();
        if (max <= min) {
            return y + 13.0F;
        }

        FONT_REGULAR.draw(v.getName() + ": " + v.getValue(), x, y, SET_LBL.getRGB());
        float sy = y + 11.0F;
        float fill = x + w * this.clamp01((float)(v.getValue() - min) / (max - min));
        this.drawSliderTrack(x, sy, w, fill);
        if (Mouse.isButtonDown(0) && this.inSlider(mx, my, x, sy, w) || this.sliderHeld == v) {
            int nv = Math.round(min + (max - min) * this.clamp01((mx - x) / w));
            v.setValue(nv);
            this.sliderHeld = v;
        }

        return sy + 10.0F;
    }

    private float drawPercentSlider(PercentProperty v, int mx, int my, float x, float y, float w) {
        int min = v.getMinimum();
        int max = v.getMaximum();
        if (max <= min) {
            return y + 13.0F;
        }

        FONT_REGULAR.draw(v.getName() + ": " + v.getValue() + "%", x, y, SET_LBL.getRGB());
        float sy = y + 11.0F;
        float fill = x + w * this.clamp01((float)(v.getValue() - min) / (max - min));
        this.drawSliderTrack(x, sy, w, fill);
        if (Mouse.isButtonDown(0) && this.inSlider(mx, my, x, sy, w) || this.sliderHeld == v) {
            int nv = Math.round(min + (max - min) * this.clamp01((mx - x) / w));
            v.setValue(nv);
            this.sliderHeld = v;
        }

        return sy + 10.0F;
    }

    private float drawFloatRange(FloatProperty v, int mx, int my, float x, float y, float w) {
        float lo = v.getValue();
        float hi = v.getSecondValue();
        float min = v.getMin();
        float max = v.getMax();
        FONT_REGULAR.draw(v.getName() + ": " + this.trim(lo) + " - " + this.trim(hi), x, y, SET_LBL.getRGB());
        float sy = y + 11.0F;
        lo = Math.max(min, Math.min(max, lo));
        hi = Math.max(min, Math.min(max, hi));
        float x1 = x + w * this.clamp01((lo - min) / (max - min));
        float x2 = x + w * this.clamp01((hi - min) / (max - min));
        this.drawRangeTrack(x, sy, w, x1, x2);
        if (Mouse.isButtonDown(0) && this.inSlider(mx, my, x, sy, w)) {
            float nv = min + (max - min) * this.clamp01((mx - x) / w);
            float d1 = Math.abs(nv - lo);
            float d2 = Math.abs(nv - hi);
            boolean nearLo = hi == lo ? nv >= lo : d1 <= d2;
            if (this.sliderHeld != v) {
                this.sliderHeld = v;
            }

            if (nearLo) {
                if (nv <= hi) {
                    v.setValue(nv);
                }
            } else if (nv >= lo) {
                v.setSecondValue(nv);
            }
        }

        return sy + 10.0F;
    }

    private float drawModeList(ModeProperty v, int mx, int my, float x, float y, float w) {
        FONT_SEMIBOLD.draw(v.getName(), x, y, SET_LBL.getRGB());
        float rowH = 18.0F;
        float rowY = y + 13.0F;
        boolean isOpen = this.listExpanded.getOrDefault(v.getName(), false);
        boolean rowHov = this.inRect(mx, my, x, rowY, w, rowH);
        RoundedUtils.drawRoundedRectRise(
            x,
            rowY,
            w,
            rowH,
            4.0F,
            (rowHov ? new Color(28, 31, 46, 220) : new Color(20, 22, 34, 200)).getRGB(),
            false,
            false,
            false,
            false
        );
        FONT_REGULAR.draw(v.getValue() + ": " + v.getModeString(), x + 6.0F, rowY + 3.0F, CAT_TXT_A.getRGB());
        String arrow = isOpen ? "▴" : "▾";
        FONT_REGULAR.draw(arrow, x + w - FONT_REGULAR.getStringWidth(arrow) - 6.0F, rowY + 3.0F, CAT_TXT.getRGB());
        if (this.clickL && rowHov) {
            this.listExpanded.put(v.getName(), !isOpen);
        }

        float cy = rowY + rowH;
        return isOpen ? this.drawModeOptions(v, mx, my, x, cy, w) : cy + 2.0F;
    }

    private float drawModeOptions(ModeProperty v, int mx, int my, float x, float cy, float w) {
        String[] modes = v.getModes();

        for (int i = 0; i < modes.length; i++) {
            boolean sel = v.getValue() == i;
            boolean hov = this.inRect(mx, my, x, cy, w, 17.0F);
            Color bg = sel ? ACCENT_DIM : (hov ? CAT_HOV : new Color(16, 18, 27, 215));
            RoundedUtils.drawRoundedRectRise(x, cy, w, 17.0F, 4.0F, bg.getRGB(), false, false, false, false);
            FONT_REGULAR.draw(modes[i], x + 10.0F, cy + 2.5F, !sel && !hov ? CAT_TXT.getRGB() : CAT_TXT_A.getRGB());
            if (this.clickL && hov) {
                v.setValue(i);
                this.listExpanded.put(v.getName(), false);
                this.clickL = false;
            }

            cy += 18.0F;
        }

        return cy + 2.0F;
    }

    private float drawColorValue(ColorProperty v, float x, float y, float w) {
        FONT_SEMIBOLD.draw(v.getName(), x, y, SET_LBL.getRGB());
        float swX = x + w - 14.0F;
        int rgb = v.getValue();
        Color col = new Color(rgb);
        RoundedUtils.drawRound(swX, y - 1.0F, 12.0F, 11.0F, 3.0F, col);
        return y + 14.0F;
    }

    private float drawTextValue(TextProperty v, float x, float y, float w) {
        FONT_SEMIBOLD.draw(v.getName() + ":", x, y, SET_LBL.getRGB());
        float by = y + 11.0F;
        RoundedUtils.drawRoundedRectRise(x, by, w, 13.0F, 3.0F, SRCH_BG.getRGB(), false, false, false, false);
        RoundedUtils.drawRoundOutline(x, by, w, 13.0F, 3.0F, 1.0F, SRCH_BG, SRCH_BOR);
        String s = v.getValue();
        FONT_REGULAR.draw(
            s.isEmpty() ? "..." : s, x + 4.0F, by + 2.0F, s.isEmpty() ? SRCH_HINT.getRGB() : SET_LBL.getRGB()
        );
        return by + 15.0F;
    }

    private void drawSliderTrack(float x, float y, float w, float fillX) {
        RoundedUtils.drawRoundedRectRise(x, y, w, 4.0F, 2.0F, SL_TRACK.getRGB(), false, false, false, false);
        if (fillX > x) {
            RoundedUtils.drawRoundedRectRise(x, y, fillX - x, 4.0F, 2.0F, SL_FILL.getRGB(), false, false, false, false);
        }

        int cx = (int)fillX;
        float cy = y + 2.0F;
        RoundedUtils.drawRoundedRectRise(
            cx - 3.0F, cy - 3.5F, 7.0F, 7.0F, 3.5F, Color.WHITE.getRGB(), false, false, false, false
        );
        RoundedUtils.drawRoundedRectRise(
            cx - 2.0F, cy - 2.5F, 5.0F, 5.0F, 2.5F, SL_TRACK.getRGB(), false, false, false, false
        );
    }

    private void drawRangeTrack(float x, float y, float w, float x1, float x2) {
        RoundedUtils.drawRoundedRectRise(x, y, w, 4.0F, 2.0F, SL_TRACK.getRGB(), false, false, false, false);
        if (x2 > x1) {
            RoundedUtils.drawRoundedRectRise(x1, y, x2 - x1, 4.0F, 2.0F, SL_FILL.getRGB(), false, false, false, false);
        }

        float cy = y + 2.0F;
        this.drawKnob(x1, cy);
        this.drawKnob(x2, cy);
    }

    private void drawKnob(float kx, float cy) {
        int cx = (int)kx;
        RoundedUtils.drawRoundedRectRise(
            cx - 3.0F, cy - 3.5F, 7.0F, 7.0F, 3.5F, Color.WHITE.getRGB(), false, false, false, false
        );
        RoundedUtils.drawRoundedRectRise(
            cx - 2.0F, cy - 2.5F, 5.0F, 5.0F, 2.5F, SL_TRACK.getRGB(), false, false, false, false
        );
    }

    private void drawTargetsPanel(int mx, int my, float cx, float cy, float cw, float ch) {
        float x = cx + 14.0F;
        float yy = cy + 14.0F;
        float w = cw - 28.0F;
        FONT_SEMIBOLD.draw("Entity Targets", x, yy, Color.WHITE.getRGB());
        yy += FONT_SEMIBOLD.getFontHeight() + 8.0F;
        RenderUtil.drawRect(cx + 8.0F, yy, cx + cw - 8.0F, yy + 1.0F, DIVIDER.getRGB());
        yy += 10.0F;

        for (String target : TARGET_NAMES) {
            boolean isOn = this.isTargetOn(target);
            float tw = 22.0F;
            float th = 13.0F;
            float tx = x + w - tw;
            float ty = yy;
            RoundedUtils.drawRoundedRectRise(
                tx, ty, tw, th, th / 2.0F, isOn ? TOG_ON.getRGB() : TOG_OFF.getRGB(), false, false, false, false
            );
            float tr = th / 2.0F - 1.5F;
            float dmX = isOn ? tx + tw - tr * 2.0F - 1.5F : tx + 1.5F;
            RoundedUtils.drawRoundedRectRise(
                dmX, ty + 1.5F, tr * 2.0F, th - 3.0F, tr, Color.WHITE.getRGB(), false, false, false, false
            );
            FONT_REGULAR.draw(target, x, yy + 1.0F, SET_LBL.getRGB());
            if (this.clickL && this.inRect(mx, my, tx - 3.0F, ty - 3.0F, tw + 6.0F, th + 6.0F)) {
                this.toggleTargets.put(target, !isOn);
            }

            yy += 22.0F;
        }
    }

    private boolean isTargetOn(String name) {
        if (!this.toggleTargets.containsKey(name)) {
            this.toggleTargets.put(name, true);
            return true;
        } else {
            return this.toggleTargets.get(name);
        }
    }

    private void drawScrollbar(float x, float y, float h, int scroll, int max) {
        if (max > 0) {
            float sbX = x - 1.0F;
            RoundedUtils.drawRoundedRectRise(
                sbX, y, 3.0F, h, 1.5F, new Color(18, 20, 31, 120).getRGB(), false, false, false, false
            );
            float th = Math.max(20.0F, h * h / (h + max));
            float ty = y + (float)scroll / max * (h - th);
            RoundedUtils.drawRoundedRectRise(
                sbX, ty, 3.0F, th, 1.5F, new Color(60, 130, 255, 200).getRGB(), false, false, false, false
            );
        }
    }

    private void drawKeybindOverlay() {
        ScaledResolution sr = new ScaledResolution(mc);
        float sw = sr.func_78326_a();
        float sh = sr.func_78328_b();
        RenderUtil.drawRect(0.0F, 0.0F, sw, sh, OVR_DIM.getRGB());
        String modName = this.bindMod == null ? "" : this.bindMod.getName();
        int curKey = this.bindMod == null ? 0 : this.bindMod.getKey();
        boolean hasBind = curKey != 0;
        String line1 = "Binding: " + modName;
        String line2 = "Press a key  -  Click to clear  -  ESC to cancel";
        float line1W = FONT_MOD_NAME.getStringWidth(line1);
        float line2W = FONT_REGULAR.getStringWidth(line2);
        float boxW = Math.max(line1W, line2W) + 44.0F;
        float boxH = 58.0F;
        float bx = (sw - boxW) / 2.0F;
        float by = (sh - boxH) / 2.0F;
        RoundedUtils.drawRoundedRectRise(bx, by, boxW, boxH, 10.0F, OVR_BOX.getRGB(), false, false, false, false);
        RoundedUtils.drawRoundOutline(bx, by, boxW, boxH, 10.0F, 1.5F, OVR_BOX, ACCENT);
        if (hasBind) {
            String badgeTxt = "Currently: " + KeyBindUtil.getKeyName(curKey);
            float badgeW = FONT_REGULAR.getStringWidth(badgeTxt) + 10.0F;
            float badgeX = bx + boxW - badgeW - 10.0F;
            float badgeY = by + 7.0F;
            RoundedUtils.drawRoundedRectRise(
                badgeX, badgeY, badgeW, 12.0F, 4.0F, new Color(30, 60, 140, 160).getRGB(), false, false, false, false
            );
            FONT_REGULAR.draw(badgeTxt, badgeX + 5.0F, badgeY + 1.5F, new Color(140, 185, 255, 220).getRGB());
        }

        FONT_MOD_NAME.draw(line1, bx + (boxW - line1W) / 2.0F, by + 12.0F, CAT_TXT_A.getRGB());
        RenderUtil.drawRect(bx + 14.0F, by + 26.0F, bx + boxW - 14.0F, by + 27.0F, new Color(30, 38, 65, 200).getRGB());
        String part1 = "Press a key  -  ";
        String part2 = "Click to clear";
        String part3 = "  -  ESC to cancel";
        float p1W = FONT_REGULAR.getStringWidth(part1);
        float p2W = FONT_REGULAR.getStringWidth(part2);
        float totalW = FONT_REGULAR.getStringWidth(line2);
        float textX = bx + (boxW - totalW) / 2.0F;
        float textY = by + 32.0F;
        FONT_REGULAR.draw(part1, textX, textY, CAT_TXT.getRGB());
        FONT_REGULAR.draw(part2, textX + p1W, textY, ACCENT.getRGB());
        FONT_REGULAR.draw(part3, textX + p1W + p2W, textY, CAT_TXT.getRGB());
    }

    private String getCategoryNameFor(Module mod) {
        String cat = this.getCategoryName(mod);
        return cat == null ? "Misc" : cat;
    }

    private List<Module> getMods() {
        List<Module> result = new ArrayList<>();
        ModuleManager mm = Miau.moduleManager;
        if (mm != null && mm.modules != null) {
            for (Module mod : mm.modules.values()) {
                if ((this.selCat == null || this.getCategoryName(mod).equals(this.selCat))
                    && (this.srchTxt.isEmpty() || mod.getName().toLowerCase().contains(this.srchTxt.toLowerCase()))) {
                    result.add(mod);
                }
            }
        }

        return result;
    }

    private String getCategoryName(Module mod) {
        String pkg = mod.getClass().getPackage().getName();
        if (pkg.startsWith("miau.module.modules.")) {
            String cat = pkg.substring("miau.module.modules.".length());
            int dot = cat.indexOf(46);
            if (dot >= 0) {
                cat = cat.substring(0, dot);
            }

            return this.capitalize(cat);
        } else {
            return null;
        }
    }

    private String capitalize(String s) {
        return s != null && !s.isEmpty() ? s.substring(0, 1).toUpperCase() + s.substring(1) : s;
    }

    private float calcInlineH(Module mod) {
        float h = 37.0F;

        for (Property<?> v : mod.getValues()) {
            if (v.isVisible() && !(v instanceof DragProperty)) {
                if (v instanceof BooleanProperty) {
                    h += 20.0F;
                } else if (v instanceof FloatProperty) {
                    h += 27.0F;
                } else if (v instanceof IntProperty) {
                    h += 27.0F;
                } else if (v instanceof PercentProperty) {
                    h += 27.0F;
                } else if (v instanceof ModeProperty) {
                    float base = 38.0F;
                    if (this.listExpanded.getOrDefault(v.getName(), false)) {
                        base += ((ModeProperty)v).getModes().length * 18.0F;
                    }

                    h += base;
                } else if (v instanceof ColorProperty) {
                    h += 19.0F;
                } else if (v instanceof TextProperty) {
                    h += 31.0F;
                } else {
                    h += 18.0F;
                }
            }
        }

        return h;
    }

    private float dt() {
        long now = System.currentTimeMillis();
        float dt = (float)(now - this.lastFrameMs) / 16.67F;
        if (dt < 0.5F) {
            dt = 0.5F;
        }

        if (dt > 3.0F) {
            dt = 3.0F;
        }

        return dt;
    }

    private float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    private float easeOutQ(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private String trim(float v) {
        float r = Math.round(v * 100.0F) / 100.0F;
        return r == Math.floor(r) ? String.valueOf((long)r) : String.valueOf(r);
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl, 255);
    }

    private boolean inRect(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean inSlider(int mx, int my, float x, float y, float w) {
        return mx >= x - 6.0F && mx <= x + w + 6.0F && my >= y - 5.0F && my <= y + 9.0F;
    }

    public void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (this.bindMod != null) {
            if (keyCode == 1) {
                this.bindMod = null;
            } else if (keyCode == 14) {
                this.bindMod.setKey(0);
                this.bindMod = null;
            } else {
                this.bindMod.setKey(keyCode);
                this.bindMod = null;
            }

            this.bindOverlayOpenTime = 0L;
        } else if (keyCode == 1) {
            mc.func_147108_a(null);
        } else if (this.srchOn) {
            if (keyCode == 14) {
                if (!this.srchTxt.isEmpty()) {
                    this.srchTxt = this.srchTxt.substring(0, this.srchTxt.length() - 1);
                }
            } else if (keyCode == 28) {
                this.srchOn = false;
            } else if (typedChar >= ' ') {
                this.srchTxt = this.srchTxt + typedChar;
            }
        } else {
            super.func_73869_a(typedChar, keyCode);
        }
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.bindMod == null || mouseButton != 0 && mouseButton != 1) {
            if (mouseButton == 0
                && mouseX >= this.gX
                && mouseX <= this.gX + this.gW
                && mouseY >= this.gY
                && mouseY <= this.gY + 33) {
                this.dragging = true;
                this.dDX = mouseX - this.gX;
                this.dDY = mouseY - this.gY;
            } else {
                super.func_73864_a(mouseX, mouseY, mouseButton);
            }
        } else if (System.currentTimeMillis() - this.bindOverlayOpenTime >= 1000L && this.bindMod != null) {
            this.bindMod.setKey(0);
            this.bindMod = null;
        }
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int ex = Mouse.getEventX() * this.field_146294_l / mc.field_71443_c;
            int ey = this.field_146295_m - Mouse.getEventY() * this.field_146295_m / mc.field_71440_d - 1;
            if (ex >= this.gX + 105 && ex <= this.gX + this.gW && ey >= this.gY && ey <= this.gY + this.gH) {
                this.scrollVel += -(wheel / 120) * 22.0F;
            }
        }
    }

    private static class SavedState {
        String openMod = null;
        String selCat = "Combat";
        String srchTxt = "";
        boolean srchOn = false;
        float scroll = 0.0F;
        Map<String, Float> expandAnim = new HashMap<>();
        boolean showTargets = false;

        private SavedState() {
        }
    }
}
