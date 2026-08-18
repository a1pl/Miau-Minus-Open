package miau.module.modules.minigames.bedwarsutils.features;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.types.EventType;
import miau.module.modules.minigames.BedwarsUtils;
import miau.module.modules.minigames.bedwarsutils.BedwarsComponent;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;

public class UpgradeHUDFeature implements BedwarsComponent {
    private final BedwarsUtils parent;
    public final BooleanProperty upgradeHud = new BooleanProperty("Upgrade HUD", false);
    public final DragProperty drag = new DragProperty("Upgrade HUD", new Vector2d(10.0, 10.0), true);
    public final BooleanProperty shortNames = new BooleanProperty("Short names", false, this.upgradeHud::getValue);
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 1.5F, this.upgradeHud::getValue);
    public final BooleanProperty showSharpness = new BooleanProperty("Show Sharpness", true, this.upgradeHud::getValue);
    public final BooleanProperty showProtection = new BooleanProperty(
        "Show Protection", true, this.upgradeHud::getValue
    );
    public final BooleanProperty showTraps = new BooleanProperty("Show Traps", true, this.upgradeHud::getValue);
    public final BooleanProperty showFeatherFalling = new BooleanProperty(
        "Show Feather Falling", true, this.upgradeHud::getValue
    );
    public final BooleanProperty showHealPool = new BooleanProperty("Show Heal Pool", true, this.upgradeHud::getValue);
    public final BooleanProperty showForge = new BooleanProperty("Show Forge", true, this.upgradeHud::getValue);
    private final Queue<String> TRAP_QUEUE = new ArrayDeque<>();
    private final String FALSE_ICON = EnumChatFormatting.RED + "✗";
    private int sharpnessLevel = 0;
    private int sharpnessLevelCached = 0;
    private int protectionLevel = 0;
    private int protectionLevelCached = 0;
    private String trapName = "";
    private String trapNameCached = "";
    private int featherFallingLevel = 0;
    private int featherFallingLevelCached = 0;
    private boolean healPoolEnabled = false;
    private boolean healPoolEnabledCached = false;
    private String forgeLevel = "";
    private String forgeLevelCached = "";

    public UpgradeHUDFeature(BedwarsUtils parent) {
        this.parent = parent;
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> props = new ArrayList<>();
        props.add(this.upgradeHud);
        props.add(this.drag);
        props.add(this.shortNames);
        props.add(this.scale);
        props.add(this.showSharpness);
        props.add(this.showProtection);
        props.add(this.showTraps);
        props.add(this.showFeatherFalling);
        props.add(this.showHealPool);
        props.add(this.showForge);
        return props;
    }

    @Override
    public void onReset() {
        this.sharpnessLevel = 0;
        this.sharpnessLevelCached = 0;
        this.protectionLevel = 0;
        this.protectionLevelCached = 0;
        this.trapName = "";
        this.trapNameCached = "";
        this.featherFallingLevel = 0;
        this.featherFallingLevelCached = 0;
        this.healPoolEnabled = false;
        this.healPoolEnabledCached = false;
        this.forgeLevel = "";
        this.forgeLevelCached = "";
        this.TRAP_QUEUE.clear();
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat)event.getPacket();
            String msg = packet.func_148915_c().func_150260_c();
            if (msg.equals("You will respawn because you still have a bed!")) {
                this.sharpnessLevel = this.sharpnessLevelCached;
                this.protectionLevel = this.protectionLevelCached;
                this.trapName = this.trapNameCached;
                this.featherFallingLevel = this.featherFallingLevelCached;
                this.healPoolEnabled = this.healPoolEnabledCached;
                this.forgeLevel = this.forgeLevelCached;
            }

            if (msg.contains("purchased") && !msg.contains(":")) {
                if (msg.contains("Sharpened Swords")) {
                    if (msg.contains("II")) {
                        this.sharpnessLevel = 2;
                        this.sharpnessLevelCached = 2;
                    } else {
                        this.sharpnessLevel = 1;
                        this.sharpnessLevelCached = 1;
                    }
                }

                if (msg.contains("Reinforced Armor")) {
                    if (msg.contains("IV")) {
                        this.protectionLevel = 4;
                        this.protectionLevelCached = 4;
                    } else if (msg.contains("III")) {
                        this.protectionLevel = 3;
                        this.protectionLevelCached = 3;
                    } else if (msg.contains("II")) {
                        this.protectionLevel = 2;
                        this.protectionLevelCached = 2;
                    } else if (msg.contains("I")) {
                        this.protectionLevel = 1;
                        this.protectionLevelCached = 1;
                    }
                }

                if (msg.contains("Trap")) {
                    if (msg.contains("Miner Fatigue")) {
                        this.addTrap("Miner Fatigue");
                    } else if (msg.contains("Blindness")) {
                        this.addTrap("Blindness");
                    } else if (msg.contains("Reveal")) {
                        this.addTrap("Reveal");
                    } else if (msg.contains("Counter-Offensive")) {
                        this.addTrap("Counter-Offensive");
                    }
                }

                if (msg.contains("Cushioned Boots")) {
                    if (msg.contains("II")) {
                        this.featherFallingLevel = 2;
                        this.featherFallingLevelCached = 2;
                    } else if (msg.contains("I")) {
                        this.featherFallingLevel = 1;
                        this.featherFallingLevelCached = 1;
                    }
                }

                if (msg.contains("Heal Pool")) {
                    this.healPoolEnabled = true;
                    this.healPoolEnabledCached = true;
                }

                if (msg.contains("Forge")) {
                    if (msg.contains("Iron")) {
                        this.forgeLevel = "Iron";
                        this.forgeLevelCached = "Iron";
                    } else if (msg.contains("Golden")) {
                        this.forgeLevel = "Golden";
                        this.forgeLevelCached = "Golden";
                    } else if (msg.contains("Emerald")) {
                        this.forgeLevel = "Emerald";
                        this.forgeLevelCached = "Emerald";
                    } else if (msg.contains("Molten")) {
                        this.forgeLevel = "Molten";
                        this.forgeLevelCached = "Molten";
                    }
                }
            }

            if (msg.contains("Trap was set off!") || msg.contains("Your Bed was destroyed")) {
                this.trapName = this.TRAP_QUEUE.poll();
                this.trapNameCached = this.trapName;
                if (this.trapName == null) {
                    this.trapName = "";
                    this.trapNameCached = "";
                }
            }
        }
    }

    private void addTrap(String trap) {
        if (this.trapName.isEmpty()) {
            this.trapName = trap;
            this.trapNameCached = trap;
        } else {
            this.TRAP_QUEUE.offer(trap);
        }
    }

    private String getForgeLevel(String forgeLvl) {
        if (forgeLvl.equals("Iron")) {
            return EnumChatFormatting.GRAY + forgeLvl;
        } else if (forgeLvl.equals("Golden")) {
            return EnumChatFormatting.GOLD + forgeLvl;
        } else if (forgeLvl.equals("Emerald")) {
            return EnumChatFormatting.DARK_GREEN + forgeLvl;
        } else {
            return forgeLvl.equals("Molten") ? EnumChatFormatting.DARK_RED + forgeLvl : forgeLvl;
        }
    }

    private String formatUpgradeName(String upgradeName) {
        if (this.shortNames.getValue()) {
            if (upgradeName.equals("Sharpness: ")) {
                return "Sharp: ";
            }

            if (upgradeName.equals("Protection: ")) {
                return "Prot: ";
            }

            if (upgradeName.equals("Feather Falling: ")) {
                return "Feather: ";
            }

            if (upgradeName.equals("Heal Pool: ")) {
                return "Heal: ";
            }
        }

        return upgradeName;
    }

    private List<String> getDisplayString() {
        List<String> lines = new ArrayList<>();
        if (this.showSharpness.getValue()) {
            lines.add(
                this.formatUpgradeName("Sharpness: ")
                    + (
                        this.sharpnessLevel > 0
                            ? EnumChatFormatting.GREEN + String.valueOf(this.sharpnessLevel)
                            : this.FALSE_ICON
                    )
            );
        }

        if (this.showProtection.getValue()) {
            lines.add(
                this.formatUpgradeName("Protection: ")
                    + (
                        this.protectionLevel > 0
                            ? EnumChatFormatting.GREEN + String.valueOf(this.protectionLevel)
                            : this.FALSE_ICON
                    )
            );
        }

        if (this.showTraps.getValue()) {
            lines.add(
                this.formatUpgradeName("Trap: ")
                    + (this.trapName.isEmpty() ? this.FALSE_ICON : EnumChatFormatting.GREEN + this.trapName)
            );
        }

        if (this.showFeatherFalling.getValue()) {
            lines.add(
                this.formatUpgradeName("Feather Falling: ")
                    + (
                        this.featherFallingLevel > 0
                            ? EnumChatFormatting.GREEN + String.valueOf(this.featherFallingLevel)
                            : this.FALSE_ICON
                    )
            );
        }

        if (this.showHealPool.getValue()) {
            lines.add(
                this.formatUpgradeName("Heal Pool: ")
                    + (this.healPoolEnabled ? EnumChatFormatting.GREEN + "✓" : this.FALSE_ICON)
            );
        }

        if (this.showForge.getValue()) {
            lines.add(
                this.formatUpgradeName("Forge: ")
                    + (this.forgeLevel.isEmpty() ? this.FALSE_ICON : this.getForgeLevel(this.forgeLevel))
            );
        }

        return lines;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.upgradeHud.getValue()) {
                Font font = FontRepository.getHudFont(18);
                List<String> lines = this.getDisplayString();
                lines.sort(Comparator.comparingInt(s -> font.getStringWidth(EnumChatFormatting.func_110646_a(s))));
                Collections.reverse(lines);
                float x = (float)this.drag.position.x;
                float y = (float)this.drag.position.y;
                float sc = this.scale.getValue();
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(sc, sc, sc);
                float maxWidth = 0.0F;
                float startY = y;

                for (String line : lines) {
                    font.drawWithShadow(line, x / sc, y / sc, -1);
                    float w = font.getStringWidth(EnumChatFormatting.func_110646_a(line)) * sc;
                    if (w > maxWidth) {
                        maxWidth = w;
                    }

                    y += font.getFontHeight() * sc + 2.0F * sc;
                }

                this.drag.setScale(new Vector2d(maxWidth, y - startY));
                GlStateManager.func_179121_F();
            }
        }
    }
}
