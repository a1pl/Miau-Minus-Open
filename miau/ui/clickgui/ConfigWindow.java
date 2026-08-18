package miau.ui.clickgui;

import java.awt.Color;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import miau.Miau;
import miau.config.Config;
import miau.config.online.OnlineConfigApplier;
import miau.config.online.OnlineConfigClient;
import miau.config.online.OnlineConfigEntry;
import miau.module.modules.render.ClickGUI;
import miau.util.client.ChatUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.math.MathUtil;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.shader.RoundedUtils;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ConfigWindow {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final int TAB_LOCAL = 0;
    private static final int TAB_ONLINE = 1;
    private static final int TAB_USER = 2;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public float x;
    public float y;
    public float width;
    public float height;
    private boolean dragging;
    private float dragX;
    private float dragY;
    private boolean expanded = true;
    private float localScrollY;
    private float targetLocalScrollY;
    private float onlineScrollY;
    private float targetOnlineScrollY;
    private float userScrollY;
    private float targetUserScrollY;
    private int selectedTab = 0;
    private final List<File> localConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> onlineConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> userConfigs = new ArrayList<>();
    private String onlineStatus = "Loading...";
    private String userStatus = "Loading...";
    private boolean isTyping = false;
    private final StringBuilder typeText = new StringBuilder();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ConfigWindowThread");
        t.setDaemon(true);
        return t;
    });
    private final OnlineConfigClient onlineClient = new OnlineConfigClient();

    public ConfigWindow(float x, float y) {
        this.x = x;
        this.y = y;
        this.width = 360.0F;
        this.height = 250.0F;
        this.refreshLocalConfigs();
        this.refreshOnlineConfigs();
        this.refreshUserConfigs();
    }

    public void refreshLocalConfigs() {
        this.localConfigs.clear();
        File configDir = new File("./config/Miau/");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    this.localConfigs.add(file);
                }
            }
        }
    }

    private void refreshOnlineConfigs() {
        this.onlineStatus = "Fetching configs...";
        this.onlineConfigs.clear();
        EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = this.onlineClient.list();
                mc.func_152344_a(() -> {
                    if (entries.isEmpty()) {
                        this.onlineStatus = "No configs found.";
                    } else {
                        this.onlineConfigs.addAll(entries);
                        this.onlineStatus = "";
                    }
                });
            } catch (Exception e) {
                mc.func_152343_a(() -> this.onlineStatus = "Fetch failed!");
            }
        });
    }

    private void refreshUserConfigs() {
        this.userStatus = "Fetching user configs...";
        this.userConfigs.clear();
        EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = this.onlineClient.listUserConfigs();
                mc.func_152344_a(() -> {
                    if (entries.isEmpty()) {
                        this.userStatus = "No user configs found.";
                    } else {
                        this.userConfigs.addAll(entries);
                        this.userStatus = "";
                    }
                });
            } catch (Exception e) {
                mc.func_152343_a(() -> this.userStatus = "Fetch failed!");
            }
        });
    }

    private int getClickGuiMode() {
        try {
            ClickGUI guiMod = (ClickGUI)Miau.moduleManager.getModule(ClickGUI.class);
            if (guiMod != null && guiMod.style != null) {
                return guiMod.style.getValue();
            }
        } catch (Exception var2) {
        }

        return 0;
    }

    public void drawWindow(int mouseX, int mouseY, float delta) {
        if (this.dragging) {
            this.x = mouseX - this.dragX;
            this.y = mouseY - this.dragY;
        }

        this.localScrollY = MathUtil.lerp(this.localScrollY, this.targetLocalScrollY, 0.015F * delta);
        this.onlineScrollY = MathUtil.lerp(this.onlineScrollY, this.targetOnlineScrollY, 0.015F * delta);
        this.userScrollY = MathUtil.lerp(this.userScrollY, this.targetUserScrollY, 0.015F * delta);
        this.height = this.expanded ? 250.0F : 18.0F;
        Color themeColor1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.x, this.y));
        Color themeColor2 = Themes.getCurrentTheme()
            .getAccentColor(new Vector2d(this.x + this.width, this.y + this.height));
        int mode = this.getClickGuiMode();
        if (mode == 0) {
            RoundedUtils.drawGradientHorizontal(
                this.x - 1.0F, this.y - 1.0F, this.width + 2.0F, this.height + 2.0F, 6.0F, themeColor1, themeColor2
            );
            RoundedUtils.drawRound(this.x, this.y, this.width, this.height, 6.0F, new Color(20, 20, 20, 235));
        } else {
            RenderUtil.drawHorizontalGradientRect(
                this.x - 1.0F,
                this.y - 1.0F,
                this.x + this.width + 1.0F,
                this.y + this.height + 1.0F,
                themeColor1.getRGB(),
                themeColor2.getRGB()
            );
            RenderUtil.drawRect(
                this.x, this.y, this.x + this.width, this.y + this.height, new Color(25, 25, 25).getRGB()
            );
        }

        Font titleFont = FontRepository.getHudFont(15);
        Font regularFont = FontRepository.getHudFont(13);
        Font smallFont = FontRepository.getHudFont(11);
        titleFont.draw("config manager", this.x + 5.0F, this.y + 3.0F, -1);
        if (this.expanded) {
            float tabY = this.y + 18.0F;
            float tabWidth = (this.width - 16.0F) / 3.0F;
            this.drawTab("Local", this.x + 4.0F, tabY, tabWidth, this.selectedTab == 0, mouseX, mouseY, regularFont);
            this.drawTab(
                "Online", this.x + 6.0F + tabWidth, tabY, tabWidth, this.selectedTab == 1, mouseX, mouseY, regularFont
            );
            this.drawTab(
                "MiauUser",
                this.x + 8.0F + tabWidth * 2.0F,
                tabY,
                tabWidth,
                this.selectedTab == 2,
                mouseX,
                mouseY,
                regularFont
            );
            if (this.selectedTab == 0) {
                this.drawLocalTab(mouseX, mouseY, regularFont, smallFont);
            } else if (this.selectedTab == 1) {
                this.drawRemoteTab(
                    mouseX,
                    mouseY,
                    regularFont,
                    smallFont,
                    this.onlineConfigs,
                    this.onlineStatus,
                    this.onlineScrollY,
                    false
                );
            } else {
                this.drawRemoteTab(
                    mouseX, mouseY, regularFont, smallFont, this.userConfigs, this.userStatus, this.userScrollY, true
                );
            }
        }
    }

    private void drawTab(
        String text, float tabX, float tabY, float tabWidth, boolean selected, int mouseX, int mouseY, Font font
    ) {
        boolean hovered = this.isHovered(mouseX, mouseY, tabX, tabY, tabWidth, 16.0F);
        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(tabX, tabY));
        Color c2 = Themes.getCurrentTheme().getAccentColor(new Vector2d(tabX + tabWidth, tabY + 16.0F));
        int mode = this.getClickGuiMode();
        if (selected) {
            if (mode == 0) {
                RoundedUtils.drawRound(tabX, tabY, tabWidth, 16.0F, 4.0F, c1);
            } else {
                RenderUtil.drawHorizontalGradientRect(
                    tabX, tabY, tabX + tabWidth, tabY + 16.0F, c1.getRGB(), c2.getRGB()
                );
            }
        } else if (mode == 0) {
            RoundedUtils.drawRound(tabX, tabY, tabWidth, 16.0F, 4.0F, new Color(35, 35, 35));
            if (hovered) {
                RoundedUtils.drawRound(tabX, tabY, tabWidth, 16.0F, 4.0F, new Color(255, 255, 255, 30));
            }
        } else {
            RenderUtil.drawRect(tabX, tabY, tabX + tabWidth, tabY + 16.0F, new Color(36, 36, 36).getRGB());
            if (hovered) {
                RenderUtil.drawRect(tabX, tabY, tabX + tabWidth, tabY + 16.0F, new Color(255, 255, 255, 30).getRGB());
            }
        }

        int textColor = selected ? RenderUtil.getContrastTextColor(c1) : new Color(160, 160, 160).getRGB();
        font.draw(
            text.toLowerCase(), tabX + tabWidth / 2.0F - font.width(text.toLowerCase()) / 2, tabY + 3.0F, textColor
        );
    }

    private void drawLocalTab(int mouseX, int mouseY, Font regularFont, Font smallFont) {
        float startX = this.x + 6.0F;
        float inputY = this.y + 38.0F;
        float listY = inputY + 20.0F;
        float listWidth = this.width - 12.0F;
        Color c1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(startX, inputY));
        RenderUtil.drawOutLineRect(
            startX, inputY, listWidth, 16.0F, 1.0F, new Color(36, 36, 36), this.isTyping ? c1 : new Color(60, 60, 60)
        );
        String displayTxt = this.typeText.length() == 0 && !this.isTyping
            ? "create new..."
            : this.typeText.toString() + (this.isTyping && System.currentTimeMillis() % 1000L < 500L ? "_" : "");
        regularFont.draw(
            displayTxt.toLowerCase(),
            startX + 5.0F,
            inputY + 3.0F,
            this.isTyping ? -1 : new Color(140, 140, 140).getRGB()
        );
        GL11.glEnable(3089);
        this.scissor(this.x, listY, this.width, this.y + this.height - listY);
        float currentY = listY + this.localScrollY;
        int mode = this.getClickGuiMode();

        for (File file : this.localConfigs) {
            String name = this.removeJsonExtension(file.getName());
            boolean hovered = mouseX >= startX
                && mouseX <= startX + listWidth
                && mouseY >= currentY
                && mouseY <= currentY + 25.0F
                && mouseY > listY
                && mouseY < this.y + this.height;
            int bgColor = hovered ? new Color(48, 48, 48).getRGB() : new Color(36, 36, 36).getRGB();
            if (mode == 0) {
                RoundedUtils.drawRound(startX, currentY, listWidth, 25.0F, 4.0F, new Color(bgColor, true));
            } else {
                RenderUtil.drawRect(startX, currentY, startX + listWidth, currentY + 25.0F, bgColor);
            }

            regularFont.draw(name.toLowerCase(), startX + 5.0F, currentY + 3.0F, -1);
            smallFont.draw(
                "last used: " + this.formatLastUsed(file),
                startX + 5.0F,
                currentY + 14.0F,
                new Color(150, 150, 150).getRGB()
            );
            currentY += 27.0F;
        }

        GL11.glDisable(3089);
    }

    private void drawRemoteTab(
        int mouseX,
        int mouseY,
        Font regularFont,
        Font smallFont,
        List<OnlineConfigEntry> entries,
        String status,
        float scrollY,
        boolean userConfig
    ) {
        float startX = this.x + 6.0F;
        float listY = this.y + 38.0F;
        float listWidth = this.width - 12.0F;
        GL11.glEnable(3089);
        this.scissor(this.x, listY, this.width, this.y + this.height - listY);
        float currentY = listY + scrollY;
        int mode = this.getClickGuiMode();
        if (!status.isEmpty()) {
            regularFont.draw(status.toLowerCase(), startX + 5.0F, currentY + 5.0F, new Color(200, 200, 200).getRGB());
        } else {
            for (OnlineConfigEntry entry : entries) {
                boolean hovered = mouseX >= startX
                    && mouseX <= startX + listWidth
                    && mouseY >= currentY
                    && mouseY <= currentY + 25.0F
                    && mouseY > listY
                    && mouseY < this.y + this.height;
                int bgColor = hovered ? new Color(48, 48, 48).getRGB() : new Color(36, 36, 36).getRGB();
                if (mode == 0) {
                    RoundedUtils.drawRound(startX, currentY, listWidth, 25.0F, 4.0F, new Color(bgColor, true));
                } else {
                    RenderUtil.drawRect(startX, currentY, startX + listWidth, currentY + 25.0F, bgColor);
                }

                regularFont.draw(entry.getName().toLowerCase(), startX + 5.0F, currentY + 3.0F, -1);
                String meta = userConfig
                    ? "by " + entry.getAuthor() + " | " + entry.getLoadCount() + " loads"
                    : "by " + entry.getAuthor() + " | " + this.safe(entry.setting_type);
                smallFont.draw(meta.toLowerCase(), startX + 5.0F, currentY + 14.0F, new Color(150, 150, 150).getRGB());
                currentY += 27.0F;
            }
        }

        GL11.glDisable(3089);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!this.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return false;
        }

        if (mouseY <= this.y + 18.0F) {
            if (button == 1) {
                this.expanded = !this.expanded;
                return true;
            }

            if (button == 0) {
                this.dragging = true;
                this.dragX = mouseX - this.x;
                this.dragY = mouseY - this.y;
                return true;
            }
        }

        if (!this.expanded) {
            return false;
        } else if (this.handleTabClick(mouseX, mouseY)) {
            return true;
        } else {
            return this.selectedTab == 0
                ? this.handleLocalClick(mouseX, mouseY, button)
                : this.handleRemoteClick(mouseX, mouseY, button);
        }
    }

    private boolean handleTabClick(int mouseX, int mouseY) {
        float tabY = this.y + 18.0F;
        float tabWidth = (this.width - 16.0F) / 3.0F;
        if (this.isHovered(mouseX, mouseY, this.x + 4.0F, tabY, tabWidth, 16.0F)) {
            this.selectedTab = 0;
            this.isTyping = false;
            return true;
        } else if (this.isHovered(mouseX, mouseY, this.x + 6.0F + tabWidth, tabY, tabWidth, 16.0F)) {
            this.selectedTab = 1;
            this.isTyping = false;
            return true;
        } else if (this.isHovered(mouseX, mouseY, this.x + 8.0F + tabWidth * 2.0F, tabY, tabWidth, 16.0F)) {
            this.selectedTab = 2;
            this.isTyping = false;
            return true;
        } else {
            return false;
        }
    }

    private boolean handleLocalClick(int mouseX, int mouseY, int button) {
        float startX = this.x + 6.0F;
        float inputY = this.y + 38.0F;
        float listY = inputY + 20.0F;
        float listWidth = this.width - 12.0F;
        if (this.isHovered(mouseX, mouseY, startX, inputY, listWidth, 15.0F)) {
            this.isTyping = true;
            return true;
        }

        this.isTyping = false;
        if (mouseY > listY) {
            float currentY = listY + this.targetLocalScrollY;

            for (File file : this.localConfigs) {
                if (this.isHovered(mouseX, mouseY, startX, currentY, listWidth, 25.0F)) {
                    String configName = this.removeJsonExtension(file.getName());
                    if (button == 0) {
                        if (Keyboard.isKeyDown(42)) {
                            if (file.exists() && file.delete()) {
                                ChatUtil.display("Deleted config: &c" + configName);
                            }

                            this.refreshLocalConfigs();
                        } else {
                            new Config(configName, false).load();
                        }
                    } else if (button == 1) {
                        new Config(configName, false).save();
                        this.refreshLocalConfigs();
                    }

                    return true;
                }

                currentY += 27.0F;
            }
        }

        return true;
    }

    private boolean handleRemoteClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return true;
        }

        float startX = this.x + 6.0F;
        float listY = this.y + 38.0F;
        float listWidth = this.width - 12.0F;
        List<OnlineConfigEntry> entries = this.selectedTab == 1 ? this.onlineConfigs : this.userConfigs;
        float scroll = this.selectedTab == 1 ? this.targetOnlineScrollY : this.targetUserScrollY;
        boolean userConfig = this.selectedTab == 2;
        float currentY = listY + scroll;

        for (OnlineConfigEntry entry : entries) {
            if (this.isHovered(mouseX, mouseY, startX, currentY, listWidth, 25.0F)) {
                this.loadOnlineConfig(entry, userConfig);
                return true;
            }

            currentY += 27.0F;
        }

        return true;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = false;
    }

    public boolean onScroll(int wheel, int mouseX, int mouseY) {
        if (!this.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return false;
        }

        float scrollSpeed = 40.0F;
        if (this.selectedTab == 0) {
            this.targetLocalScrollY += wheel > 0 ? scrollSpeed : -scrollSpeed;
            float maxScroll = Math.max(0.0F, this.localConfigs.size() * 28 - (this.height - 70.0F));
            this.targetLocalScrollY = Math.max(-maxScroll, Math.min(0.0F, this.targetLocalScrollY));
        } else if (this.selectedTab == 1) {
            this.targetOnlineScrollY += wheel > 0 ? scrollSpeed : -scrollSpeed;
            float maxScroll = Math.max(0.0F, this.onlineConfigs.size() * 28 - (this.height - 48.0F));
            this.targetOnlineScrollY = Math.max(-maxScroll, Math.min(0.0F, this.targetOnlineScrollY));
        } else {
            this.targetUserScrollY += wheel > 0 ? scrollSpeed : -scrollSpeed;
            float maxScroll = Math.max(0.0F, this.userConfigs.size() * 28 - (this.height - 48.0F));
            this.targetUserScrollY = Math.max(-maxScroll, Math.min(0.0F, this.targetUserScrollY));
        }

        return true;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (this.isTyping) {
            if (keyCode == 1) {
                this.isTyping = false;
            } else if (keyCode == 28) {
                if (this.typeText.length() > 0) {
                    new Config(this.typeText.toString(), true).save();
                    this.typeText.setLength(0);
                    this.isTyping = false;
                    this.refreshLocalConfigs();
                }
            } else if (keyCode == 14) {
                if (this.typeText.length() > 0) {
                    this.typeText.setLength(this.typeText.length() - 1);
                }
            } else if (String.valueOf(typedChar).matches("[a-zA-Z0-9_-]") && this.typeText.length() < 16) {
                this.typeText.append(typedChar);
            }

            return true;
        } else {
            return false;
        }
    }

    private void loadOnlineConfig(OnlineConfigEntry entry, boolean userConfig) {
        EXECUTOR.execute(
            () -> {
                try {
                    String json = userConfig
                        ? this.onlineClient.loadUserConfig(entry.getId())
                        : this.onlineClient.load(entry.getId());
                    mc.func_152344_a(
                        () -> {
                            try {
                                this.showMetadata(entry, userConfig);
                                int applied = new OnlineConfigApplier().apply(json);
                                if (userConfig) {
                                    ChatUtil.display(
                                        "%sUser config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                                        entry.getName(),
                                        applied
                                    );
                                } else {
                                    ChatUtil.display(
                                        "%sOnline config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                                        entry.getName(),
                                        applied
                                    );
                                }
                            } catch (Exception ex) {
                                ChatUtil.display(
                                    Miau.clientName
                                        + "Failed to load "
                                        + (userConfig ? "user" : "online")
                                        + " config: &c"
                                        + ex.getMessage()
                                        + "&r"
                                );
                            }
                        }
                    );
                } catch (Exception e) {
                    mc.func_152344_a(
                        () -> ChatUtil.display(
                            Miau.clientName
                                + "Failed to load "
                                + (userConfig ? "user" : "online")
                                + " config: &c"
                                + e.getMessage()
                                + "&r"
                        )
                    );
                }
            }
        );
    }

    private void showMetadata(OnlineConfigEntry entry, boolean userConfig) {
        if (userConfig) {
            ChatUtil.display(Miau.clientName + "User config info:&r");
            ChatUtil.display("&fName: &a" + entry.getName() + "&r");
            ChatUtil.display("&fID: &b" + entry.getId() + "&r");
            ChatUtil.display("&fAuthor: &a" + entry.getAuthor() + "&r");
            ChatUtil.display("&fUpload time: &b" + this.safe(entry.date) + "&r");
            ChatUtil.display("&fLoads: &e" + entry.getLoadCount() + "&r");
            if (!entry.getVersion().isEmpty()) {
                ChatUtil.display("&fVersion: &e" + entry.getVersion() + "&r");
            }

            if (entry.description != null && !entry.description.trim().isEmpty()) {
                ChatUtil.display("&fNote: &7" + entry.description + "&r");
            }
        } else {
            ChatUtil.display(Miau.clientName + "Loading online config...&r");
            ChatUtil.display("&fName: &a" + entry.getName() + "&r");
            ChatUtil.display("&fUpload time: &b" + this.safe(entry.date) + "&r");
            ChatUtil.display("&fAuthor: &a" + entry.getAuthor() + "&r");
            ChatUtil.display("&fType: &b" + this.safe(entry.setting_type) + "&r");
            ChatUtil.display("&fStatus: &e" + this.safe(entry.status_type) + "&r");
            if (!entry.getVersion().isEmpty()) {
                ChatUtil.display("&fVersion: &e" + entry.getVersion() + "&r");
            }

            if (entry.description != null && !entry.description.trim().isEmpty()) {
                ChatUtil.display("&fDescription: &7" + entry.description + "&r");
            }
        }
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isTyping() {
        return this.isTyping;
    }

    private String removeJsonExtension(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private String formatLastUsed(File file) {
        return file.exists() ? DATE_FORMAT.format(new Date(file.lastModified())) : "unknown";
    }

    private String safe(String value) {
        return value != null && !value.trim().isEmpty() ? value : "unknown";
    }

    private void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (ClickGui.openingScale != 1.0F) {
            double scaleFactor = ClickGui.openingScale;
            double centerX = sr.func_78326_a() / 2.0;
            double centerY = sr.func_78328_b() / 2.0;
            x = centerX + (x - centerX) * scaleFactor;
            y = centerY + (y - centerY) * scaleFactor;
            width *= scaleFactor;
            height *= scaleFactor;
        }

        double scale = sr.func_78325_e();
        y = sr.func_78328_b() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        GL11.glScissor((int)x, (int)(y - height), (int)width, (int)height);
    }
}
