package miau.ui.clickgui.demise.component.config;

import java.awt.Color;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import miau.Miau;
import miau.config.Config;
import miau.config.online.OnlineConfigApplier;
import miau.config.online.OnlineConfigClient;
import miau.config.online.OnlineConfigEntry;
import miau.ui.clickgui.demise.IComponent;
import miau.util.client.ChatUtil;
import miau.util.demise.RoundedUtils;
import miau.util.font.FontRepository;
import net.minecraft.client.Minecraft;

public class ConfigComponent implements IComponent {
    private float x;
    private float y;
    private boolean isHovered;
    private boolean saveHovered;
    private boolean deleteHovered;
    private Color interpolatedColor = new Color(20, 20, 20, 150);
    private Color interpolatedColor1 = new Color(0, 0, 0, 0);
    public boolean visible;
    private float slideProgress = 0.0F;
    private String name;
    private boolean isLocal = true;
    private boolean isUser = false;
    private OnlineConfigEntry onlineEntry = null;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final OnlineConfigClient onlineClient = new OnlineConfigClient();

    public ConfigComponent(String name) {
        this.name = name;
    }

    public ConfigComponent(OnlineConfigEntry entry, boolean isUser) {
        this.name = entry.getName();
        this.isLocal = false;
        this.isUser = isUser;
        this.onlineEntry = entry;
    }

    public void initCategory() {
        this.slideProgress = 0.0F;
    }

    public void render(boolean shader) {
        float width = 330.0F;
        this.slideProgress = this.animate(this.slideProgress, this.visible ? 1.0F : 0.0F, 0.1F);
        float slideOffset = width / 4.0F * (1.0F - this.slideProgress);
        if (!shader) {
            if (this.isHovered) {
                this.interpolatedColor = this.interpolateColorC(
                    this.interpolatedColor, new Color(35, 35, 35, 190), 0.1F
                );
            } else {
                this.interpolatedColor = this.interpolateColorC(
                    this.interpolatedColor, new Color(20, 20, 20, 150), 0.1F
                );
            }

            String currentConfig = "default";
            if (Objects.equals(currentConfig, this.name)) {
                this.interpolatedColor1 = this.interpolateColorC(
                    this.interpolatedColor1, new Color(50, 50, 50, 150), 0.1F
                );
            } else {
                this.interpolatedColor1 = this.interpolateColorC(this.interpolatedColor1, new Color(0, 0, 0, 0), 0.1F);
            }

            RoundedUtils.drawRound(this.x + slideOffset, this.y, width, 30.0F, 8.0F, this.interpolatedColor);
            RoundedUtils.drawRound(this.x + slideOffset, this.y, width, 30.0F, 8.0F, this.interpolatedColor1);
            FontRepository.getFont("Inter Regular", 20.0F)
                .draw(this.name, this.x + 7.0F + slideOffset, this.y + 11.0F, Color.white.getRGB());
            if (this.isLocal) {
                float saveWidth = FontRepository.getFont("Inter Regular", 14.0F).getStringWidth("save");
                float deleteWidth = FontRepository.getFont("Inter Regular", 14.0F).getStringWidth("delete");
                Color saveColor = this.saveHovered ? new Color(255, 255, 255) : new Color(179, 179, 179);
                Color deleteColor = this.deleteHovered ? new Color(255, 255, 255) : new Color(179, 179, 179);
                FontRepository.getFont("Inter Regular", 14.0F)
                    .draw(
                        "save",
                        this.x + width - 10.0F - deleteWidth - 5.0F - saveWidth + slideOffset,
                        this.y + 13.0F,
                        saveColor.getRGB()
                    );
                FontRepository.getFont("Inter Regular", 14.0F)
                    .draw(
                        "delete",
                        this.x + width - 10.0F - deleteWidth + slideOffset,
                        this.y + 13.0F,
                        deleteColor.getRGB()
                    );
            } else {
                String meta = "by " + this.onlineEntry.getAuthor();
                float metaWidth = FontRepository.getFont("Inter Regular", 14.0F).getStringWidth(meta);
                FontRepository.getFont("Inter Regular", 14.0F)
                    .draw(
                        meta,
                        this.x + width - 10.0F - metaWidth + slideOffset,
                        this.y + 13.0F,
                        new Color(179, 179, 179).getRGB()
                    );
            }
        } else {
            RoundedUtils.drawRound(this.x + slideOffset, this.y, width, 30.0F, 8.0F, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float width = 330.0F;
        float slideOffset = width / 4.0F * (1.0F - this.slideProgress);
        float saveWidth = FontRepository.getFont("Inter Regular", 14.0F).getStringWidth("save");
        float deleteWidth = FontRepository.getFont("Inter Regular", 14.0F).getStringWidth("delete");
        if (this.isLocal) {
            this.isHovered = this.isHovered(
                this.x + slideOffset, this.y, width - 15.0F - deleteWidth - 15.0F - saveWidth, 30.0F, mouseX, mouseY
            );
            this.saveHovered = this.isHovered(
                this.x + width - 10.0F - deleteWidth - 5.0F - saveWidth + slideOffset,
                this.y,
                saveWidth,
                30.0F,
                mouseX,
                mouseY
            );
            this.deleteHovered = this.isHovered(
                this.x + width - 10.0F - deleteWidth + slideOffset, this.y, deleteWidth, 30.0F, mouseX, mouseY
            );
        } else {
            this.isHovered = this.isHovered(this.x + slideOffset, this.y, width, 30.0F, mouseX, mouseY);
            this.saveHovered = false;
            this.deleteHovered = false;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.visible && mouseButton == 0) {
            if (this.isLocal) {
                if (this.isHovered) {
                    Config config = new Config(this.name, true);
                    if (config.file.exists()) {
                        config.load();
                        ChatUtil.display("Loaded config " + this.name);
                    } else {
                        ChatUtil.display("Failed to load config " + this.name + "!");
                    }
                } else if (this.saveHovered) {
                    Config config = new Config(this.name, true);
                    config.save();
                    ChatUtil.display("Saved config " + this.name);
                } else if (this.deleteHovered) {
                    File configFile = new File("config/Miau", this.name + ".json");
                    if (!configFile.exists()) {
                        ChatUtil.display("Config does not exist: " + this.name);
                        return;
                    }

                    String message = configFile.delete()
                        ? "Removed config: " + this.name
                        : "Failed to remove config: " + this.name;
                    ChatUtil.display(message);
                }
            } else if (this.isHovered) {
                this.loadOnlineConfig(this.onlineEntry, this.isUser);
            }
        }
    }

    private void loadOnlineConfig(OnlineConfigEntry entry, boolean userConfig) {
        EXECUTOR.execute(
            () -> {
                try {
                    String json = userConfig
                        ? onlineClient.loadUserConfig(entry.getId())
                        : onlineClient.load(entry.getId());
                    Minecraft.func_71410_x()
                        .func_152344_a(
                            () -> {
                                try {
                                    int applied = new OnlineConfigApplier().apply(json);
                                    ChatUtil.display(
                                        "%s config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                                        userConfig ? "User" : "Online",
                                        entry.getName(),
                                        applied
                                    );
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
                    Minecraft.func_71410_x()
                        .func_152344_a(
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

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        return new Color(
            (int)(color1.getRed() + (color2.getRed() - color1.getRed()) * amount),
            (int)(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * amount),
            (int)(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * amount),
            (int)(color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * amount)
        );
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }

    private boolean isHovered(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
