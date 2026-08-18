package miau.ui.nogui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import miau.Miau;
import miau.module.Module;
import miau.property.Property;
import miau.property.properties.DragProperty;
import miau.property.properties.ItemListProperty;
import miau.util.client.KeyBindUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class NoguiGui extends GuiScreen {
    private static final Color ACCENT_A = new Color(33, 212, 253);
    private static final Color ACCENT_B = new Color(123, 108, 255);
    private static final Color BG_COLOR = new Color(5, 7, 13);
    private static final Color PANEL_BG = new Color(10, 13, 22, 242);
    private static final Color CARD_BG = new Color(14, 20, 30);
    private static final Color CARD_BG_HOVER = new Color(18, 26, 39);
    private static final Color CARD_ON_TOP = new Color(10, 32, 47, 200);
    private static final Color CARD_ON_BOTTOM = new Color(12, 16, 34, 200);
    private static final Color BORDER = new Color(35, 45, 64);
    private static final Color BORDER_HOVER = new Color(52, 67, 92);
    private static final Color TEXT_MAIN = new Color(241, 245, 251);
    private static final Color TEXT_DIM = new Color(163, 174, 194);
    private static final Color TEXT_FAINT = new Color(94, 106, 128);
    private static final Color ROW_HOVER = new Color(255, 255, 255, 10);
    private static final Color SWITCH_OFF = new Color(38, 48, 71);
    private static final Color TRACK_BG = new Color(34, 42, 62);
    private static final float MARGIN = 16.0F;
    private static final float BAR_Y = 10.0F;
    private static final float BAR_H = 40.0F;
    private static final float GRID_Y = 72.0F;
    private static final float CARD_W = 152.0F;
    private static final float CARD_H = 52.0F;
    private static final float GAP_X = 10.0F;
    private static final float GAP_Y = 12.0F;
    private static final float PAD = 14.0F;
    private static final float ROW_H = 30.0F;
    private static final float HEADER_H = 58.0F;
    private static final float LOGO_W = 58.0F;
    private static final float CONFIG_PANEL_W = 420.0F;
    private static final float SETTINGS_PANEL_W = 420.0F;
    private static final float SETTINGS_PANEL_H = 520.0F;
    private final Minecraft mc = Minecraft.func_71410_x();
    private Font fontLogo;
    private Font fontTab;
    private Font fontCard;
    private Font fontBody;
    private Font fontValue;
    private Font fontSmall;
    private Font fontTitle;
    private Font fontBig;
    private Map<String, List<Module>> categories;
    private List<String> catNames;
    private String activeCategory;
    private String search = "";
    private boolean searchFocused = false;
    private String configSearch = "";
    private boolean configSearchFocused = false;
    private List<Module> visibleModules = new ArrayList<>();
    private final Map<Module, Float> hover = new HashMap<>();
    private final Map<Module, Float> entrance = new HashMap<>();
    private final Map<Property<?>, Float> knobs = new HashMap<>();
    private float tabProgress = 0.0F;
    private int tabTarget = -1;
    private float scroll = 0.0F;
    private float targetScroll = 0.0F;
    private float openAnim = 0.0F;
    private long lastMS = 0L;
    private int cachedEnabled = 0;
    private boolean configVisible = false;
    private Module configModule = null;
    private Property<?> draggingSlider = null;
    private Module bindingModule = null;
    private ScaledResolution currentSR;
    private float[] configRect;
    private final List<Object[]> configRows = new ArrayList<>();
    private List<NoguiGui.Card> cards = new ArrayList<>();
    private float[] bindsRect;

    public NoguiGui() {
        this.categories = Miau.moduleManager.getModulesByCategory();
        this.catNames = new ArrayList<>(this.categories.keySet());
        if (!this.catNames.isEmpty()) {
            this.activeCategory = this.catNames.get(0);
        }

        this.fontLogo = FontRepository.getFont("sfuidisplay-bold", 13.0F);
        this.fontTab = FontRepository.getFont("sfuidisplay-medium", 12.0F);
        this.fontCard = FontRepository.getFont("sfuidisplay-semibold", 13.0F);
        this.fontBody = FontRepository.getFont("sfuidisplay-regular", 12.0F);
        this.fontValue = FontRepository.getFont("sfuidisplay-medium", 12.0F);
        this.fontSmall = FontRepository.getFont("sfuidisplay-medium", 9.0F);
        this.fontTitle = FontRepository.getFont("sfuidisplay-bold", 17.0F);
        this.fontBig = FontRepository.getFont("sfuidisplay-bold", 30.0F);
        this.rebuildLayout();
    }

    private void rebuildLayout() {
        this.visibleModules.clear();
        if (this.search.isEmpty()) {
            List<Module> list = this.categories.get(this.activeCategory);
            if (list != null) {
                this.visibleModules.addAll(list);
            }
        } else {
            String q = this.search.toLowerCase();

            for (List<Module> list : this.categories.values()) {
                for (Module m : list) {
                    if (m.getName().toLowerCase().contains(q)) {
                        this.visibleModules.add(m);
                    }
                }
            }
        }

        this.cards.clear();
        float w = this.scaledWidth();
        float gridW = w - 32.0F;
        int cols = Math.max(1, (int)Math.floor((gridW + 10.0F) / 162.0F));
        float totalW = cols * 152.0F + (cols - 1) * 10.0F;
        float startX = 16.0F + (gridW - totalW) / 2.0F;

        for (int i = 0; i < this.visibleModules.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            float x = startX + col * 162.0F;
            float y = 72.0F + row * 64.0F;
            this.cards.add(new NoguiGui.Card(this.visibleModules.get(i), x, y, 152.0F, 52.0F));
        }

        Map<Module, Float> nh = new HashMap<>();

        for (Module m : this.visibleModules) {
            Float v = this.entrance.get(m);
            nh.put(m, v == null ? 0.0F : v);
        }

        this.entrance.clear();
        this.entrance.putAll(nh);
    }

    private float scaledWidth() {
        return this.currentSR != null ? this.currentSR.func_78326_a() : new ScaledResolution(this.mc).func_78326_a();
    }

    private float scaledHeight() {
        return this.currentSR != null ? this.currentSR.func_78328_b() : new ScaledResolution(this.mc).func_78328_b();
    }

    public boolean func_73868_f() {
        return false;
    }

    public void func_73866_w_() {
        this.lastMS = System.currentTimeMillis();
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        super.func_73863_a(mouseX, mouseY, partialTicks);
        long now = System.currentTimeMillis();
        float delta = Math.min((float)(now - this.lastMS) / 50.0F, 1.5F);
        this.lastMS = now;
        this.currentSR = new ScaledResolution(this.mc);
        this.cachedEnabled = this.countEnabled();
        this.openAnim = expApproach(this.openAnim, 1.0F, delta, 6.0F);
        if (this.tabTarget < 0) {
            for (int i = 0; i < this.catNames.size(); i++) {
                if (this.catNames.get(i).equals(this.activeCategory)) {
                    this.tabTarget = i;
                    break;
                }
            }
        }

        this.tabProgress = expApproach(this.tabProgress, this.tabTarget, delta, 6.0F);
        this.scroll = expApproach(this.scroll, this.targetScroll, delta, 7.0F);
        float w = this.scaledWidth();
        float h = this.scaledHeight();
        float alpha = easeOut(this.openAnim);
        RoundedUtils.drawRound(0.0F, 0.0F, w, h, 0.0F, withAlpha(BG_COLOR, (int)(235.0F * alpha)));
        this.drawAmbientGlow(w, h, alpha);
        float scale = 1.0F - (1.0F - this.openAnim) * 0.03F;
        GL11.glPushMatrix();
        GL11.glTranslatef(w / 2.0F, h / 2.0F, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glTranslatef(-w / 2.0F, -h / 2.0F, 0.0F);
        this.drawTopBar(mouseX, mouseY, w, alpha);
        this.drawGrid(mouseX, mouseY, w, h, alpha);
        this.drawBindsPanel(mouseX, mouseY, w, h, alpha);
        this.drawFooter(mouseX, mouseY, w, h, alpha);
        GL11.glPopMatrix();
        if (this.bindingModule != null) {
            this.drawBindingOverlay(w, h);
        }
    }

    private void drawAmbientGlow(float w, float h, float alpha) {
        RoundedUtils.drawRound(-140.0F, -160.0F, 440.0F, 440.0F, 220.0F, new Color(33, 212, 253, (int)(7.0F * alpha)));
        RoundedUtils.drawRound(
            w - 300.0F, h - 320.0F, 430.0F, 430.0F, 215.0F, new Color(123, 108, 255, (int)(6.0F * alpha))
        );
    }

    private void drawTopBar(int mx, int my, float w, float alpha) {
        float x = 16.0F;
        float y = 10.0F;
        float bw = w - 32.0F;
        RoundedUtils.drawRound(x, y, bw, 40.0F, 13.0F, PANEL_BG);
        RoundedUtils.drawRoundOutline(
            x, y, bw, 40.0F, 13.0F, 1.0F, new Color(0, 0, 0, 0), withAlpha(BORDER, (int)(255.0F * alpha))
        );
        float t = this.pulse();
        RoundedUtils.drawGradientHorizontal(
            x + 4.0F, y + 7.0F, 58.0F, 26.0F, 8.0F, this.accent(t), this.accent(t + 0.35F)
        );
        this.fontLogo
            .drawWithShadow(
                "MIAU",
                x + 4.0F + (58.0F - this.fontLogo.width("MIAU")) / 2.0F,
                y + 7.0F + (26.0F - this.fontLogo.height()) / 2.0F,
                new Color(4, 9, 17).getRGB()
            );
        float tx = x + 4.0F + 58.0F + 14.0F;
        float tabH = 26.0F;
        float tabY = y + (40.0F - tabH) / 2.0F;
        float[] tabXs = new float[this.catNames.size()];
        float[] tabWs = new float[this.catNames.size()];

        for (int i = 0; i < this.catNames.size(); i++) {
            float tw = this.fontTab.width(this.catNames.get(i)) + 18.0F;
            tabXs[i] = tx;
            tabWs[i] = tw;
            tx += tw + 6.0F;
        }

        if (!this.catNames.isEmpty()) {
            float slide = clamp(this.tabProgress, 0.0F, this.catNames.size() - 1.0F);
            int left = (int)Math.floor(slide);
            int right = Math.min(left + 1, this.catNames.size() - 1);
            float ft = slide - left;
            float moverX = lerp(tabXs[left], tabXs[right], ft);
            float moverW = lerp(tabWs[left], tabWs[right], ft);
            if (this.catNames.size() > 1) {
                RoundedUtils.drawGradientHorizontal(
                    moverX, tabY, moverW, tabH, 8.0F, this.accent(t), this.accent(t + 0.35F)
                );
            }
        }

        for (int i = 0; i < this.catNames.size(); i++) {
            boolean active = i == this.tabTarget;
            boolean hovered = mx >= tabXs[i] && mx <= tabXs[i] + tabWs[i] && my >= tabY && my <= tabY + tabH;
            Color text = active ? new Color(4, 9, 17) : (hovered ? TEXT_DIM : TEXT_FAINT);
            this.fontTab
                .draw(
                    this.catNames.get(i),
                    tabXs[i] + (tabWs[i] - this.fontTab.width(this.catNames.get(i))) / 2.0F,
                    tabY + (tabH - this.fontTab.height()) / 2.0F,
                    text.getRGB()
                );
        }

        float closeSize = 28.0F;
        float closeX = x + bw - closeSize - 6.0F;
        float closeY = y + (40.0F - closeSize) / 2.0F;
        boolean closeHover = mx >= closeX && mx <= closeX + closeSize && my >= closeY && my <= closeY + closeSize;
        RoundedUtils.drawRound(
            closeX,
            closeY,
            closeSize,
            closeSize,
            9.0F,
            closeHover ? new Color(226, 70, 90, 90) : new Color(255, 255, 255, 9)
        );
        this.drawCross(
            closeX + closeSize / 2.0F,
            closeY + closeSize / 2.0F,
            4.5F,
            1.6F,
            new Color(255, 255, 255, closeHover ? 255 : 180)
        );
        String stats = this.cachedEnabled + "/" + this.visibleModules.size();
        float statsW = this.fontValue.width(stats) + 22.0F;
        float statsX = closeX - statsW - 8.0F;
        RoundedUtils.drawRound(statsX, closeY, statsW, closeSize, 9.0F, new Color(255, 255, 255, 10));
        this.fontSmall
            .drawCentered(
                stats,
                statsX + statsW / 2.0F,
                closeY + (closeSize - this.fontSmall.height()) / 2.0F + 0.5F,
                TEXT_DIM.getRGB()
            );
        float searchW = 132.0F;
        float searchX = statsX - searchW - 10.0F;
        if (mx >= searchX && mx <= searchX + searchW && my >= closeY && my <= closeY + closeSize) {
            boolean searchHover = true;
        } else {
            boolean searchHover = false;
        }

        RoundedUtils.drawRound(searchX, closeY, searchW, closeSize, 9.0F, new Color(7, 10, 18, 210));
        RoundedUtils.drawRoundOutline(
            searchX,
            closeY,
            searchW,
            closeSize,
            9.0F,
            1.0F,
            new Color(0, 0, 0, 0),
            this.searchFocused ? this.accent(t) : withAlpha(BORDER, 255)
        );
        this.drawMagnifier(searchX + 10.0F, closeY + closeSize / 2.0F, withAlpha(TEXT_FAINT, 255));
        String shown = !this.searchFocused && this.search.isEmpty() ? "Search..." : this.search;
        int textColor = this.search.isEmpty() && !this.searchFocused ? TEXT_FAINT.getRGB() : TEXT_DIM.getRGB();
        String clipped = this.clipText(this.fontBody, shown, searchW - 32.0F);
        this.fontBody.draw(clipped, searchX + 21.0F, closeY + (closeSize - this.fontBody.height()) / 2.0F, textColor);
    }

    private void drawGrid(int mx, int my, float w, float h, float alpha) {
        float gridW = w - 32.0F;
        float bottom = h - 52.0F;
        float viewH = Math.max(40.0F, bottom - 72.0F);
        int cols = Math.max(1, (int)Math.floor((gridW + 10.0F) / 162.0F));
        int rows = (int)Math.ceil((float)this.cards.size() / cols);
        float totalH = rows * 64.0F - 12.0F;
        float maxScroll = Math.max(0.0F, totalH - viewH);
        this.targetScroll = clamp(this.targetScroll, 0.0F, maxScroll);
        if (mx >= 16.0F && mx <= 16.0F + gridW && my >= 72.0F && my <= bottom) {
            boolean overGrid = true;
        } else {
            boolean overGrid = false;
        }

        String label = this.search.isEmpty()
            ? this.activeCategory + "  ·  " + this.visibleModules.size()
            : "RESULTS  ·  " + this.visibleModules.size();
        this.fontSmall.drawWithShadow(label, 16.0, 58.0, TEXT_FAINT.getRGB());
        this.beginScissor(16.0F, 72.0F, gridW, viewH);
        float fade = easeOut(this.openAnim);

        for (NoguiGui.Card card : this.cards) {
            this.drawCard(card, mx, my, fade);
        }

        this.endScissor();
        if (maxScroll > 0.5F) {
            float barX = 16.0F + gridW - 3.5F;
            float thumbH = Math.max(22.0F, viewH * (viewH / totalH));
            float thumbY = 72.0F + (viewH - thumbH) * (this.scroll / maxScroll);
            RoundedUtils.drawRound(barX, 72.0F, 2.5F, viewH, 1.5F, new Color(255, 255, 255, 14));
            RoundedUtils.drawRound(barX, thumbY, 2.5F, thumbH, 1.5F, new Color(33, 212, 253, 170));
        }
    }

    private void drawCard(NoguiGui.Card card, int mx, int my, float alpha) {
        float t = this.pulse();
        boolean hovering = mx >= card.x
            && mx <= card.x + card.w
            && my >= card.y - this.scroll
            && my <= card.y - this.scroll + card.h;
        Float hov = this.hover.get(card.module);
        float h = hov == null ? 0.0F : hov;
        h = expApproach(h, hovering ? 1.0F : 0.0F, 0.3F, 5.0F);
        this.hover.put(card.module, h);
        Float ent = this.entrance.get(card.module);
        float e = ent == null ? 0.0F : ent;
        e = expApproach(e, 1.0F, 0.3F, 4.0F);
        this.entrance.put(card.module, e);
        if (!(e < 0.01F)) {
            float x = card.x;
            float y = card.y - this.scroll + (1.0F - easeOut(e)) * 14.0F;
            float w = card.w + h * 2.0F;
            float hh = card.h + h * 2.0F;
            x -= h;
            y -= h;
            boolean enabled = card.module.isEnabled();
            Color base = enabled ? new Color(0, 0, 0, 0) : (h > 0.4F ? CARD_BG_HOVER : CARD_BG);
            if (enabled) {
                RoundedUtils.drawRound(
                    x - 1.0F, y - 1.0F, w + 2.0F, hh + 2.0F, 11.0F, new Color(33, 212, 253, (int)(16.0F + 18.0F * h))
                );
                RoundedUtils.drawGradientVertical(x, y, w, hh, 10.0F, CARD_ON_TOP, CARD_ON_BOTTOM);
                RoundedUtils.drawRoundOutline(x, y, w, hh, 10.0F, 1.4F, new Color(0, 0, 0, 0), this.accent(t));
                RoundedUtils.drawGradientVertical(
                    x, y + 6.0F, 2.6F, hh - 12.0F, 1.3F, this.accent(t), this.accent(t + 0.35F)
                );
            } else {
                RoundedUtils.drawRound(x, y, w, hh, 10.0F, base);
                RoundedUtils.drawRoundOutline(
                    x, y, w, hh, 10.0F, 1.0F, new Color(0, 0, 0, 0), h > 0.4F ? withAlpha(this.accent(t), 70) : BORDER
                );
            }

            int nameColor = enabled
                ? TEXT_MAIN.getRGB()
                : (hovering ? TEXT_MAIN.getRGB() : new Color(198, 207, 222).getRGB());
            this.fontCard.drawWithShadow(card.module.getName(), x + 10.0F, y + 9.0F, nameColor);
            float dotY = y + 24.0F;
            RoundedUtils.drawRound(x + 10.0F, dotY + 4.0F, 3.0F, 3.0F, 1.5F, enabled ? this.accent(t) : BORDER_HOVER);
            String cat = card.module.getCategory();
            this.fontSmall
                .draw(
                    cat == null ? "" : this.clipText(this.fontSmall, cat, card.w - 70.0F),
                    x + 17.0F,
                    dotY,
                    TEXT_FAINT.getRGB()
                );
            String key = card.module.getKey() != 0 ? KeyBindUtil.getKeyName(card.module.getKey()) : "";
            if (!key.isEmpty()) {
                this.fontSmall
                    .drawWithShadow(
                        this.clipText(this.fontSmall, key, 40.0F),
                        x + w - 10.0F - this.fontSmall.width(this.clipText(this.fontSmall, key, 40.0F)),
                        dotY,
                        TEXT_FAINT.getRGB()
                    );
            }

            if (hovering && !enabled) {
                float cy = y + 10.0F;

                for (int i = 0; i < 3; i++) {
                    RoundedUtils.drawRound(
                        x + w - 34.0F + i * 7.0F, cy + 9.0F, 3.0F, 3.0F, 1.5F, withAlpha(this.accent(t), 160)
                    );
                }
            }

            float pillW = 36.0F;
            float pillH = 14.0F;
            float pillY = y + 6.0F;
            float pillX = x + w - pillW - 8.0F;
            if (enabled) {
                RoundedUtils.drawGradientHorizontal(
                    pillX, pillY, pillW, pillH, 7.0F, this.accent(t), this.accent(t + 0.35F)
                );
                this.fontSmall
                    .drawWithShadow(
                        "ON",
                        pillX + (pillW - this.fontSmall.width("ON")) / 2.0F,
                        pillY + (pillH - this.fontSmall.height()) / 2.0F + 0.5F,
                        new Color(4, 9, 17).getRGB()
                    );
            } else if (h > 0.4F) {
                RoundedUtils.drawRound(pillX, pillY, pillW, pillH, 7.0F, new Color(255, 255, 255, 10));
                this.fontSmall
                    .draw(
                        "OFF",
                        pillX + (pillW - this.fontSmall.width("OFF")) / 2.0F,
                        pillY + (pillH - this.fontSmall.height()) / 2.0F + 0.5F,
                        TEXT_FAINT.getRGB()
                    );
            }
        }
    }

    private void drawConfigPanel(int mx, int my, float w, float h, float alpha) {
        if (this.configVisible) {
            float fw = 420.0F;
            float fh = h - 40.0F;
            float fx = w - fw - 16.0F;
            float fy = 20.0F;
            this.configRect = new float[]{fx, fy, fw, fh};
            RoundedUtils.drawRound(fx, fy, fw, fh, 12.0F, new Color(11, 14, 24, 220));
            RoundedUtils.drawRoundOutline(fx, fy, fw, fh, 12.0F, 1.0F, new Color(0, 0, 0, 0), withAlpha(BORDER, 200));
            float t = this.pulse();
            RoundedUtils.drawGradientHorizontal(fx, fy, fw, 3.0F, 1.5F, ACCENT_A, ACCENT_B);
            this.fontTitle.drawWithShadow("CONFIG MANAGEMENT", fx + 14.0F, fy + 10.0F, TEXT_MAIN.getRGB());
            float searchW = fw - 28.0F;
            float searchX = fx + 14.0F;
            float searchY = fy + 50.0F;
            if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + 26.0F) {
                boolean searchHover = true;
            } else {
                boolean searchHover = false;
            }

            RoundedUtils.drawRound(searchX, searchY, searchW, 26.0F, 8.0F, new Color(7, 10, 18, 200));
            RoundedUtils.drawRoundOutline(
                searchX,
                searchY,
                searchW,
                26.0F,
                8.0F,
                1.0F,
                new Color(0, 0, 0, 0),
                this.configSearchFocused ? withAlpha(ACCENT_A, 220) : withAlpha(BORDER, 255)
            );
            this.drawMagnifier(searchX + 8.0F, searchY + 13.0F, withAlpha(TEXT_FAINT, 255));
            String shown = !this.configSearchFocused && this.configSearch.isEmpty()
                ? "Search configs..."
                : this.configSearch;
            int textColor = this.configSearch.isEmpty() && !this.configSearchFocused
                ? TEXT_FAINT.getRGB()
                : TEXT_DIM.getRGB();
            String clipped = this.clipText(this.fontBody, shown, searchW - 24.0F);
            this.fontBody.draw(clipped, searchX + 16.0F, searchY + 8.0F, textColor);
            float listY = searchY + 36.0F;
            float listH = fh - 100.0F;
            float listW = fw - 20.0F;
            boolean overList = mx >= searchX && mx <= searchX + listW && my >= listY && my <= listY + listH;
            this.beginScissor(searchX, listY, listW, listH);
            this.drawConfigList(searchX, listY, listW, listH, mx, my);
            this.endScissor();
            if (overList && Mouse.isButtonDown(0)) {
                this.configSearch = "";
                this.configModule = null;
            }

            float bottomY = fy + fh - 38.0F;
            float saveBtnX = fx + 14.0F;
            float saveBtnY = bottomY;
            float saveBtnW = 90.0F;
            float saveBtnH = 28.0F;
            boolean saveHover = mx >= saveBtnX
                && mx <= saveBtnX + saveBtnW
                && my >= saveBtnY
                && my <= saveBtnY + saveBtnH;
            RoundedUtils.drawRound(
                saveBtnX,
                saveBtnY,
                saveBtnW,
                saveBtnH,
                8.0F,
                saveHover ? withAlpha(ACCENT_A, 200) : new Color(7, 10, 18, 200)
            );
            this.fontBody
                .drawCentered("Save Config", saveBtnX + saveBtnW / 2.0F, saveBtnY + 14.0F, new Color(4, 9, 17).getRGB());
            float loadBtnX = saveBtnX + saveBtnW + 8.0F;
            boolean loadHover = mx >= loadBtnX
                && mx <= loadBtnX + saveBtnW
                && my >= saveBtnY
                && my <= saveBtnY + saveBtnH;
            RoundedUtils.drawRound(
                loadBtnX,
                saveBtnY,
                saveBtnW,
                saveBtnH,
                8.0F,
                loadHover ? withAlpha(ACCENT_B, 200) : new Color(7, 10, 18, 200)
            );
            this.fontBody.drawCentered("Load Config", loadBtnX + saveBtnW / 2.0F, saveBtnY + 14.0F, TEXT_MAIN.getRGB());
            float delBtnX = loadBtnX + saveBtnW + 8.0F;
            boolean delHover = mx >= delBtnX && mx <= delBtnX + saveBtnW && my >= saveBtnY && my <= saveBtnY + saveBtnH;
            RoundedUtils.drawRound(
                delBtnX,
                saveBtnY,
                saveBtnW,
                saveBtnH,
                8.0F,
                delHover ? new Color(226, 70, 90, 200) : new Color(7, 10, 18, 200)
            );
            this.fontBody.drawCentered("Delete", delBtnX + saveBtnW / 2.0F, saveBtnY + 14.0F, TEXT_MAIN.getRGB());
            if (Mouse.isButtonDown(0)) {
                if (saveHover) {
                    this.saveConfig();
                } else if (loadHover) {
                    this.loadConfig();
                } else if (delHover) {
                    this.deleteConfig();
                }
            }
        }
    }

    private void drawConfigList(float x, float y, float w, float h, int mx, int my) {
        this.configRows.clear();
        float rowH = 28.0F;

        for (Module m : Miau.moduleManager.modules.values()) {
            boolean rowHover = mx >= x && mx <= x + w && my >= y && my <= y + rowH;
            if (rowHover) {
                RoundedUtils.drawRound(x, y + 2.0F, w, rowH - 4.0F, 7.0F, ROW_HOVER);
            }

            String name = this.clipText(this.fontBody, m.getName(), w - 60.0F);
            int color = m.isEnabled() ? TEXT_DIM.getRGB() : TEXT_FAINT.getRGB();
            this.fontBody.draw(name, x + 8.0F, y + 8.0F, color);
            if (m.isEnabled()) {
                RoundedUtils.drawRound(x + w - 4.0F, y + 10.0F, 3.0F, 3.0F, 1.0F, ACCENT_A);
            }

            y += rowH;
        }
    }

    private void drawBindsPanel(int mx, int my, float w, float h, float alpha) {
        List<Module> bound = new ArrayList<>();

        for (Module m : Miau.moduleManager.modules.values()) {
            if (m.getKey() != 0) {
                bound.add(m);
            }
        }

        if (bound.isEmpty()) {
            this.bindsRect = null;
        } else {
            bound.sort(new Comparator<Module>() {
                public int compare(Module a, Module b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            int show = Math.min(bound.size(), 5);
            float pw = 190.0F;
            float ph = 34.0F + show * 19.0F;
            float px = w - pw - 16.0F;
            float py = h - ph - 10.0F;
            this.bindsRect = new float[]{px, py, pw, ph};
            RoundedUtils.drawRound(px, py, pw, ph, 11.0F, new Color(11, 15, 25, 215));
            RoundedUtils.drawRoundOutline(px, py, pw, ph, 11.0F, 1.0F, new Color(0, 0, 0, 0), withAlpha(BORDER, 200));
            this.fontSmall
                .drawWithShadow("KEYBINDS  (" + bound.size() + ")", px + 10.0F, py + 7.0F, TEXT_FAINT.getRGB());

            for (int i = 0; i < show; i++) {
                Module m = bound.get(i);
                float rowY = py + 24.0F + i * 19.0F;
                String name = this.clipText(this.fontBody, m.getName(), pw - 95.0F);
                this.fontBody.draw(name, px + 10.0F, rowY, m.isEnabled() ? TEXT_DIM.getRGB() : TEXT_FAINT.getRGB());
                if (m.isEnabled()) {
                    RoundedUtils.drawRound(px + 8.0F, rowY + 3.0F, 2.0F, 2.0F, 1.0F, ACCENT_A);
                }

                String key = KeyBindUtil.getKeyName(m.getKey());
                this.fontBody.draw(key, px + pw - 10.0F - this.fontBody.width(key), rowY, TEXT_MAIN.getRGB());
            }

            if (bound.size() > show) {
                String more = "+" + (bound.size() - show) + " more";
                this.fontSmall
                    .draw(more, px + pw - 10.0F - this.fontSmall.width(more), py + ph - 12.0F, TEXT_FAINT.getRGB());
            }
        }
    }

    private void drawFooter(int mx, int my, float w, float h, float alpha) {
        float y = h - 25.0F;
        this.fontSmall.draw("LMB  toggle     RMB  settings     ESC close", 16.0, y, new Color(70, 80, 100).getRGB());
        String version = "v" + Miau.version;
        this.fontSmall.drawRight(version, w - 16.0F, y, TEXT_FAINT.getRGB());
    }

    private void drawBindingOverlay(float w, float h) {
        RoundedUtils.drawRound(0.0F, 0.0F, w, h, 0.0F, new Color(4, 6, 12, 195));
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        RoundedUtils.drawRound(cx - 210.0F, cy - 58.0F, 420.0F, 116.0F, 16.0F, PANEL_BG);
        RoundedUtils.drawRoundOutline(
            cx - 210.0F, cy - 58.0F, 420.0F, 116.0F, 16.0F, 1.0F, new Color(0, 0, 0, 0), withAlpha(ACCENT_A, 180)
        );
        RoundedUtils.drawGradientHorizontal(cx - 210.0F, cy - 58.0F, 420.0F, 2.5F, 1.5F, ACCENT_A, ACCENT_B);
        this.fontTitle
            .drawCentered("Bind a key for " + this.bindingModule.getName(), cx, cy - 34.0F, TEXT_MAIN.getRGB());
        this.fontBody
            .drawCentered("Press any key to bind  ·  ESC cancel  ·  DELETE clear", cx, cy + 16.0F, TEXT_DIM.getRGB());
    }

    private void beginScissor(float x, float y, float w, float h) {
        float s = this.currentSR.func_78325_e();
        int sx = (int)(x * s);
        int sy = (int)(this.mc.field_71440_d - (y + h) * s);
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

    private void drawMagnifier(float cx, float cy, Color color) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        GL11.glLineWidth(1.4F);
        GL11.glBegin(2);

        for (int i = 0; i < 24; i++) {
            double ang = Math.toRadians(i * 15.0);
            GL11.glVertex2f((float)(cx + Math.cos(ang) * 4.0), (float)(cy + Math.sin(ang) * 4.0));
        }

        GL11.glEnd();
        GL11.glBegin(1);
        GL11.glVertex2f(cx + 3.0F, cy + 3.0F);
        GL11.glVertex2f(cx + 6.5F, cy + 6.5F);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    protected void func_73864_a(int mx, int my, int button) throws IOException {
        super.func_73864_a(mx, my, button);
        if (this.bindingModule != null) {
            this.bindingModule = null;
        } else {
            float w = this.scaledWidth();
            float h = this.scaledHeight();
            float x = 16.0F;
            float y = 10.0F;
            float bw = w - 32.0F;
            float closeSize = 28.0F;
            float closeX = x + bw - closeSize - 6.0F;
            float closeY = y + (40.0F - closeSize) / 2.0F;
            if (mx >= closeX && mx <= closeX + closeSize && my >= closeY && my <= closeY + closeSize) {
                this.mc.func_147108_a(null);
            } else {
                float tx = x + 4.0F + 58.0F + 14.0F;
                float tabH = 26.0F;
                float tabY = y + (40.0F - tabH) / 2.0F;

                for (int i = 0; i < this.catNames.size(); i++) {
                    float tw = this.fontTab.width(this.catNames.get(i)) + 18.0F;
                    if (mx >= tx && mx <= tx + tw && my >= tabY && my <= tabY + tabH) {
                        if (!this.catNames.get(i).equals(this.activeCategory) || !this.search.isEmpty()) {
                            this.activeCategory = this.catNames.get(i);
                            this.tabTarget = i;
                            this.search = "";
                            this.searchFocused = false;
                            this.targetScroll = 0.0F;
                            this.rebuildLayout();
                        }

                        return;
                    }

                    tx += tw + 6.0F;
                }

                float closeSize2 = 28.0F;
                float statsW = this.fontValue.width(this.cachedEnabled + "/" + this.visibleModules.size()) + 22.0F;
                float statsX = closeSize2 <= 0.0F ? 0.0F : 16.0F + w - 32.0F - closeSize2 - 6.0F - statsW - 8.0F;
                float searchW = 132.0F;
                float searchX = statsX - searchW - 10.0F;
                if (mx >= searchX && mx <= searchX + searchW && my >= closeY && my <= closeY + closeSize2) {
                    this.searchFocused = true;
                    this.configSearchFocused = false;
                } else if (this.configRect != null && contains(this.configRect, mx, my)) {
                    this.handleConfigClick(mx, my, button);
                } else {
                    if (button == 0 || button == 1) {
                        for (NoguiGui.Card card : this.cards) {
                            if (mx >= card.x
                                && mx <= card.x + card.w
                                && my >= card.y - this.scroll
                                && my <= card.y - this.scroll + card.h) {
                                if (button == 0) {
                                    card.module.toggle();
                                } else {
                                    this.mc.func_147108_a(new ModuleSettingsGui(card.module));
                                }

                                return;
                            }
                        }
                    }

                    if (this.bindsRect == null || !contains(this.bindsRect, mx, my)) {
                        this.searchFocused = false;
                    }
                }
            }
        }
    }

    private void handleConfigClick(int mx, int my, int button) {
        if (button == 0) {
            float w = this.scaledWidth();
            float fw = 420.0F;
            float fx = w - fw - 16.0F;
            float searchY = 88.0F;
            if (mx >= fx + 14.0F && mx <= fx + fw - 14.0F && my >= searchY && my <= searchY + 160.0F) {
                float rowH = 28.0F;
                int row = (int)((my - searchY) / rowH);
                if (row >= 0 && row < this.visibleModules.size()) {
                    this.configModule = this.visibleModules.get(row);
                    this.configVisible = true;
                }
            }
        }
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int mx = (int)(Mouse.getX() * this.scaledWidth() / this.mc.field_71443_c);
            int my = (int)(this.scaledHeight() - Mouse.getY() * this.scaledHeight() / this.mc.field_71440_d);
            if (this.configRect != null && contains(this.configRect, mx, my)) {
                float fw = 420.0F;
                float fx = this.scaledWidth() - fw - 16.0F;
                float searchY = 88.0F;
                if (mx >= fx + 14.0F && mx <= fx + fw - 14.0F && my >= searchY && my <= searchY + 280.0F) {
                    float rowH = 28.0F;
                    int row = (int)((my - searchY) / rowH);
                    if (row >= 0 && row < this.visibleModules.size()) {
                        this.configModule = this.visibleModules.get(row);
                        this.configVisible = true;
                    }
                }
            } else {
                this.targetScroll -= wheel / 120.0F * 40.0F;
                this.targetScroll = clamp(this.targetScroll, 0.0F, 99999.0F);
            }
        }
    }

    protected void func_146286_b(int mx, int my, int state) {
        super.func_146286_b(mx, my, state);
        if (state == 0) {
            this.draggingSlider = null;
        }
    }

    public void func_73876_c() {
        if (this.draggingSlider != null) {
            int mx = (int)(Mouse.getX() * this.scaledWidth() / this.mc.field_71443_c);
            int my = (int)(this.scaledHeight() - Mouse.getY() * this.scaledHeight() / this.mc.field_71440_d);
            if (!Mouse.isButtonDown(0)) {
                this.draggingSlider = null;
            }
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (this.bindingModule == null) {
            if (this.searchFocused) {
                if (keyCode == 1) {
                    this.searchFocused = false;
                } else if (keyCode == 14 && !this.search.isEmpty()) {
                    this.search = this.search.substring(0, this.search.length() - 1);
                    this.targetScroll = 0.0F;
                    this.rebuildLayout();
                } else if (typedChar >= ' ' && typedChar != 127) {
                    this.search = this.search + typedChar;
                    this.targetScroll = 0.0F;
                    this.rebuildLayout();
                }
            } else if (this.configSearchFocused) {
                if (keyCode == 1) {
                    this.configSearchFocused = false;
                } else if (keyCode == 14 && !this.configSearch.isEmpty()) {
                    this.configSearch = this.configSearch.substring(0, this.configSearch.length() - 1);
                } else if (typedChar >= ' ' && typedChar != 127) {
                    this.configSearch = this.configSearch + typedChar;
                }
            } else {
                if (keyCode == 1) {
                    this.mc.func_147108_a(null);
                }
            }
        } else {
            if (keyCode == 1) {
                this.bindingModule = null;
            } else if (keyCode != 211 && keyCode != 14) {
                this.bindingModule.setKey(keyCode);
                this.bindingModule = null;
            } else {
                this.bindingModule.setKey(0);
                this.bindingModule = null;
            }
        }
    }

    private void saveConfig() {
        try {
            File file = new File(this.mc.field_71412_D, "keystrokesconfig.json");
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
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(json, writer);
            } catch (Throwable var12) {
                try {
                    writer.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }

                throw var12;
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            File file = new File(this.mc.field_71412_D, "keystrokesconfig.json");
            if (!file.exists()) {
                return;
            }

            JsonObject json = new JsonParser().parse(new FileReader(file)).getAsJsonObject();

            for (Entry<String, JsonElement> entry : json.entrySet()) {
                String modName = entry.getKey();
                JsonObject modJson = entry.getValue().getAsJsonObject();
                boolean enabled = modJson.get("enabled").getAsBoolean();
                int key = modJson.get("key").getAsInt();
                Module m = Miau.moduleManager.getModule(modName);
                if (m != null) {
                    m.setEnabled(enabled);
                    m.setKey(key);
                    if (modJson.has("properties")) {
                        for (JsonElement propElement : modJson.getAsJsonArray("properties")) {
                            JsonObject propJson = propElement.getAsJsonObject();
                            String propName = propJson.get("name").getAsString();
                            if (propJson.has("value")) {
                                String value = propJson.get("value").getAsString();
                                Module mod = Miau.moduleManager.getModule(modName);
                                if (mod != null) {
                                    for (Property<?> p : mod.getValues()) {
                                        if (p.getName().equals(propName)) {
                                            p.parseString(value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteConfig() {
        try {
            File file = new File(this.mc.field_71412_D, "keystrokesconfig.json");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCustomConfig() {
        try {
            File file = new File(this.mc.field_71412_D, "miau_custom_config.json");
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
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(json, writer);
            } catch (Throwable var12) {
                try {
                    writer.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }

                throw var12;
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomConfig() {
        try {
            File file = new File(this.mc.field_71412_D, "miau_custom_config.json");
            if (!file.exists()) {
                return;
            }

            JsonObject json = new JsonParser().parse(new FileReader(file)).getAsJsonObject();

            for (Entry<String, JsonElement> entry : json.entrySet()) {
                String modName = entry.getKey();
                JsonObject modJson = entry.getValue().getAsJsonObject();
                boolean enabled = modJson.get("enabled").getAsBoolean();
                int key = modJson.get("key").getAsInt();
                Module m = Miau.moduleManager.getModule(modName);
                if (m != null) {
                    m.setEnabled(enabled);
                    m.setKey(key);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetCustomConfig() {
        for (Module m : Miau.moduleManager.modules.values()) {
            m.setEnabled(false);
            m.setKey(0);

            for (Property<?> p : m.getValues()) {
                p.parseString("");
            }
        }
    }

    private List<Property<?>> buildVisibleProps(Module module) {
        List<Property<?>> result = new ArrayList<>();

        for (Property<?> p : module.getValues()) {
            if (!(p instanceof DragProperty) && !(p instanceof ItemListProperty) && p.isVisible()) {
                result.add(p);
            }
        }

        return result;
    }

    private int countEnabled() {
        int n = 0;

        for (Module m : Miau.moduleManager.modules.values()) {
            if (m.isEnabled()) {
                n++;
            }
        }

        return n;
    }

    private static String stripCodes(String s) {
        return s == null ? "" : s.replaceAll("[&\\u00A7].", "");
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

    private static boolean contains(float[] rect, float mx, float my) {
        return mx >= rect[0] && mx <= rect[0] + rect[2] && my >= rect[1] && my <= rect[1] + rect[3];
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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

    private Color accent(float t) {
        float x = (float)(0.5 + 0.5 * Math.sin(t * Math.PI));
        return interpolate(ACCENT_A, ACCENT_B, x);
    }

    private float pulse() {
        return (float)(System.currentTimeMillis() % 7000L) / 7000.0F;
    }

    private static class Card {
        Module module;
        float x;
        float y;
        float w;
        float h;

        Card(Module module, float x, float y, float w, float h) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
