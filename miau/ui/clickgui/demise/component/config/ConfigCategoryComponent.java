package miau.ui.clickgui.demise.component.config;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import miau.config.online.OnlineConfigClient;
import miau.config.online.OnlineConfigEntry;
import miau.ui.clickgui.demise.IComponent;
import miau.ui.clickgui.demise.PanelGui;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ConfigCategoryComponent implements IComponent {
    private float x;
    private float y;
    private boolean isHovered;
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private float scrollOffset = 0.0F;
    private float targetScrollOffset = 0.0F;
    private float maxScroll = 0.0F;
    private String name = "Configs";
    private final List<ConfigComponent> configs = new ArrayList<>();
    private int selectedTab = 0;
    private boolean localHovered;
    private boolean onlineHovered;
    private boolean userHovered;
    private final List<File> localConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> onlineConfigs = new ArrayList<>();
    private final List<OnlineConfigEntry> userConfigs = new ArrayList<>();
    private String onlineStatus = "Loading...";
    private String userStatus = "Loading...";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final OnlineConfigClient onlineClient = new OnlineConfigClient();

    public ConfigCategoryComponent(float x, float y) {
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;
        this.refreshLocalConfigs();
        this.refreshOnlineConfigs();
        this.refreshUserConfigs();
        this.buildComponents();
    }

    private void refreshLocalConfigs() {
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
                Minecraft.func_71410_x().func_152344_a(() -> {
                    if (entries.isEmpty()) {
                        this.onlineStatus = "No configs found.";
                    } else {
                        this.onlineConfigs.addAll(entries);
                        this.onlineStatus = "";
                        if (this.selectedTab == 1) {
                            this.buildComponents();
                        }
                    }
                });
            } catch (Exception e) {
                Minecraft.func_71410_x().func_152343_a(() -> this.onlineStatus = "Fetch failed!");
            }
        });
    }

    private void refreshUserConfigs() {
        this.userStatus = "Fetching user configs...";
        this.userConfigs.clear();
        EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = this.onlineClient.listUserConfigs();
                Minecraft.func_71410_x().func_152344_a(() -> {
                    if (entries.isEmpty()) {
                        this.userStatus = "No user configs found.";
                    } else {
                        this.userConfigs.addAll(entries);
                        this.userStatus = "";
                        if (this.selectedTab == 2) {
                            this.buildComponents();
                        }
                    }
                });
            } catch (Exception e) {
                Minecraft.func_71410_x().func_152343_a(() -> this.userStatus = "Fetch failed!");
            }
        });
    }

    private void buildComponents() {
        this.configs.clear();
        if (this.selectedTab == 0) {
            for (File file : this.localConfigs) {
                String n = file.getName();
                if (n.endsWith(".json")) {
                    n = n.substring(0, n.length() - 5);
                }

                this.configs.add(new ConfigComponent(n));
            }
        } else if (this.selectedTab == 1) {
            for (OnlineConfigEntry entry : this.onlineConfigs) {
                this.configs.add(new ConfigComponent(entry, false));
            }
        } else if (this.selectedTab == 2) {
            for (OnlineConfigEntry entry : this.userConfigs) {
                this.configs.add(new ConfigComponent(entry, true));
            }
        }
    }

    public void initCategory() {
        this.refreshLocalConfigs();
        this.buildComponents();

        for (ConfigComponent cc : this.configs) {
            cc.initCategory();
        }
    }

    public void initGui() {
        this.refreshLocalConfigs();
        this.buildComponents();
    }

    public void render(boolean shader) {
        float x = this.x;
        if (this.isSelected) {
            x += 3.0F;
            float width = FontRepository.getFont("Inter Regular", 18.0F).getStringWidth(this.name);
            this.interpolatedLineWidth = this.animate(this.interpolatedLineWidth, width, 0.05F);
        } else {
            this.interpolatedLineWidth = this.animate(this.interpolatedLineWidth, 0.0F, 0.05F);
        }

        if (this.isHovered) {
            x += 2.5F;
        }

        if (!PanelGui.dragging) {
            this.interpolatedX = this.animate(this.interpolatedX, x, 0.15F);
        } else {
            this.interpolatedX = x;
        }

        if (!shader) {
            FontRepository.getFont("Inter Regular", 18.0F)
                .draw(this.name, this.interpolatedX, this.y, Color.white.getRGB());
            RenderUtil.drawRect(
                this.interpolatedX,
                this.y + FontRepository.getFont("Inter Regular", 18.0F).height() - 2.6F,
                this.interpolatedX + this.interpolatedLineWidth,
                this.y + FontRepository.getFont("Inter Regular", 18.0F).height() - 2.6F + 0.5F,
                Color.white.getRGB()
            );
        }

        if (this.isSelected) {
            this.handleScroll();
            float componentStartY = PanelGui.posY + 65.0F;
            float viewHeight = 250.0F;
            if (!shader) {
                float tabX = PanelGui.posX + 105.0F;
                float tabY = componentStartY - 25.0F;
                float localW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("Local");
                float onlineW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("Online");
                float userW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("MiauUser");
                Color c1 = this.selectedTab == 0 ? new Color(255, 255, 255) : new Color(150, 150, 150);
                Color c2 = this.selectedTab == 1 ? new Color(255, 255, 255) : new Color(150, 150, 150);
                Color c3 = this.selectedTab == 2 ? new Color(255, 255, 255) : new Color(150, 150, 150);
                if (this.localHovered && this.selectedTab != 0) {
                    c1 = new Color(200, 200, 200);
                }

                if (this.onlineHovered && this.selectedTab != 1) {
                    c2 = new Color(200, 200, 200);
                }

                if (this.userHovered && this.selectedTab != 2) {
                    c3 = new Color(200, 200, 200);
                }

                FontRepository.getFont("Inter Regular", 16.0F).draw("Local", tabX, tabY, c1.getRGB());
                FontRepository.getFont("Inter Regular", 16.0F).draw("Online", tabX + localW + 15.0F, tabY, c2.getRGB());
                FontRepository.getFont("Inter Regular", 16.0F)
                    .draw("MiauUser", tabX + localW + 15.0F + onlineW + 15.0F, tabY, c3.getRGB());
            }

            float totalHeight = 0.0F;

            for (int i = 0; i < this.configs.size(); i++) {
                totalHeight += 40.0F;
            }

            this.maxScroll = Math.max(0.0F, totalHeight - viewHeight);
            this.scrollOffset = this.animate(this.scrollOffset, this.targetScrollOffset, 0.1F);
            RenderUtil.scissor(
                0.0F, componentStartY - 2.0F, PanelGui.posX + 450.0F, viewHeight, PanelGui.interpolatedScale
            );
            GL11.glEnable(3089);
            if (!shader && this.selectedTab == 1 && !this.onlineStatus.isEmpty()) {
                FontRepository.getFont("Inter Regular", 16.0F)
                    .draw(
                        this.onlineStatus,
                        PanelGui.posX + 105.0F,
                        componentStartY + 10.0F,
                        new Color(200, 200, 200).getRGB()
                    );
            } else if (!shader && this.selectedTab == 2 && !this.userStatus.isEmpty()) {
                FontRepository.getFont("Inter Regular", 16.0F)
                    .draw(
                        this.userStatus,
                        PanelGui.posX + 105.0F,
                        componentStartY + 10.0F,
                        new Color(200, 200, 200).getRGB()
                    );
            } else {
                float componentOffsetY = componentStartY;

                for (ConfigComponent config : this.configs) {
                    float moduleY = componentOffsetY - this.scrollOffset;
                    config.setX(PanelGui.posX + 105.0F);
                    config.setY(moduleY);
                    config.render(shader);
                    config.setVisible(moduleY + 35.0F >= componentStartY && moduleY <= componentStartY + viewHeight);
                    componentOffsetY += 35.0F;
                }
            }

            GL11.glDisable(3089);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        if (this.isSelected) {
            float componentStartY = PanelGui.posY + 65.0F;
            float tabX = PanelGui.posX + 105.0F;
            float tabY = componentStartY - 25.0F;
            float localW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("Local");
            float onlineW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("Online");
            float userW = FontRepository.getFont("Inter Regular", 16.0F).getStringWidth("MiauUser");
            this.localHovered = PanelGui.isHovered(tabX, tabY, localW, 16.0F, mouseX, mouseY);
            this.onlineHovered = PanelGui.isHovered(tabX + localW + 15.0F, tabY, onlineW, 16.0F, mouseX, mouseY);
            this.userHovered = PanelGui.isHovered(
                tabX + localW + 15.0F + onlineW + 15.0F, tabY, userW, 16.0F, mouseX, mouseY
            );
            float viewHeight = 250.0F;

            for (ConfigComponent cc : this.configs) {
                if (cc.getY() + 35.0F >= componentStartY && cc.getY() <= componentStartY + viewHeight) {
                    cc.drawScreen(mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.isSelected && mouseButton == 0) {
            if (this.localHovered) {
                this.selectedTab = 0;
                this.targetScrollOffset = 0.0F;
                this.buildComponents();
                return;
            }

            if (this.onlineHovered) {
                this.selectedTab = 1;
                this.targetScrollOffset = 0.0F;
                this.buildComponents();
                return;
            }

            if (this.userHovered) {
                this.selectedTab = 2;
                this.targetScrollOffset = 0.0F;
                this.buildComponents();
                return;
            }
        }

        if (this.isSelected) {
            float componentStartY = PanelGui.posY + 65.0F;
            float viewHeight = 250.0F;

            for (ConfigComponent cc : this.configs) {
                if (cc.getY() + 35.0F >= componentStartY && cc.getY() <= componentStartY + viewHeight) {
                    cc.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.isSelected) {
            for (ConfigComponent cc : this.configs) {
                cc.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.isSelected) {
            for (ConfigComponent cc : this.configs) {
                cc.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25.0F : 25.0F;
            this.targetScrollOffset = MathHelper.func_76131_a(
                this.targetScrollOffset + scrollAmount, 0.0F, this.maxScroll
            );
        }
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

    public boolean isHovered() {
        return this.isHovered;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }
}
