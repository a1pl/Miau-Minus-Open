package miau.module.modules.minigames.bedwarsutils.features;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;

public class EventTimersFeature implements BedwarsComponent {
    private final BedwarsUtils parent;
    public final BooleanProperty eventTimers = new BooleanProperty("Event Timers", false);
    public final DragProperty eventDrag = new DragProperty("Event Timers", new Vector2d(10.0, 60.0), true);
    public final FloatProperty eventScale = new FloatProperty(
        "Event Scale", 1.0F, 0.5F, 1.5F, this.eventTimers::getValue
    );
    public final BooleanProperty eventTime = new BooleanProperty("Events Enabled", true, this.eventTimers::getValue);
    public final BooleanProperty onlyNext = new BooleanProperty(
        "Show next events only", false, this.eventTimers::getValue
    );
    public final BooleanProperty romanNumerals = new BooleanProperty(
        "Roman numerals", false, this.eventTimers::getValue
    );
    public final BooleanProperty eventDynamicColor = new BooleanProperty(
        "Dynamic color", false, this.eventTimers::getValue
    );
    public final BooleanProperty diamondTimer = new BooleanProperty("Diamond Timer", true, this.eventTimers::getValue);
    public final BooleanProperty emeraldTimer = new BooleanProperty("Emerald Timer", true, this.eventTimers::getValue);
    public final BooleanProperty bedGoneTimer = new BooleanProperty("Bed Gone Timer", true, this.eventTimers::getValue);
    public final BooleanProperty suddenDeathTimer = new BooleanProperty(
        "Sudden Death Timer", true, this.eventTimers::getValue
    );
    public final BooleanProperty gameEndTimer = new BooleanProperty("Game End Timer", true, this.eventTimers::getValue);
    public final BooleanProperty emeraldTime = new BooleanProperty("Emeralds Enabled", true, this.eventTimers::getValue);
    public final DragProperty emeraldDrag = new DragProperty("Emerald Timers", new Vector2d(10.0, 110.0), true);
    public final BooleanProperty emeraldDynamicColor = new BooleanProperty(
        "Emerald Dynamic color", true, this.eventTimers::getValue
    );
    private static final EventTimersFeature.EmeraldEntry EIGHT_TEAMS_MODE_DATA = new EventTimersFeature.EmeraldEntry(
        65, 50, 35, 4
    );
    private static final EventTimersFeature.EmeraldEntry FOUR_TEAMS_MODE_DATA = new EventTimersFeature.EmeraldEntry(
        55, 40, 27, 2
    );
    private static final ItemStack EMERALD_ICON = new ItemStack(Items.field_151166_bC);
    private long gameStartTime = 0L;
    private boolean gameStarted = false;
    private EventTimersFeature.EmeraldEntry currentModeData = FOUR_TEAMS_MODE_DATA;

    public EventTimersFeature(BedwarsUtils parent) {
        this.parent = parent;
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> props = new ArrayList<>();
        props.add(this.eventTimers);
        props.add(this.eventDrag);
        props.add(this.eventScale);
        props.add(this.eventTime);
        props.add(this.onlyNext);
        props.add(this.romanNumerals);
        props.add(this.eventDynamicColor);
        props.add(this.diamondTimer);
        props.add(this.emeraldTimer);
        props.add(this.bedGoneTimer);
        props.add(this.suddenDeathTimer);
        props.add(this.gameEndTimer);
        props.add(this.emeraldTime);
        props.add(this.emeraldDrag);
        props.add(this.emeraldDynamicColor);
        return props;
    }

    @Override
    public void onReset() {
        this.gameStarted = false;
        this.gameStartTime = 0L;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat)event.getPacket();
            String formattedMsg = packet.func_148915_c().func_150254_d();
            if (formattedMsg.contains("§e§lProtect your bed and destroy the enemy bed")
                || formattedMsg.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                this.gameStartTime = System.currentTimeMillis();
                this.gameStarted = true;
                if (formattedMsg.contains("Protect your bed")) {
                    this.currentModeData = EIGHT_TEAMS_MODE_DATA;
                } else {
                    this.currentModeData = FOUR_TEAMS_MODE_DATA;
                }
            }
        }
    }

    private void renderItemIcon(Minecraft mc, ItemStack stack, float x, float y, float scale) {
        if (stack != null) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b(x, y, 0.0F);
            GlStateManager.func_179152_a(scale, scale, scale);
            GlStateManager.func_179091_B();
            RenderHelper.func_74520_c();
            mc.func_175599_af().func_180450_b(stack, 0, 0);
            mc.func_175599_af().func_180453_a(mc.field_71466_p, stack, 0, 0, null);
            RenderHelper.func_74518_a();
            GlStateManager.func_179101_C();
            GlStateManager.func_179121_F();
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private boolean shouldShow(EventTimersFeature.EventTypeTimer type) {
        switch (type) {
            case DIAMOND:
                return this.diamondTimer.getValue();
            case EMERALD:
                return this.emeraldTimer.getValue();
            case BED_GONE:
                return this.bedGoneTimer.getValue();
            case SUDDEN_DEATH:
                return this.suddenDeathTimer.getValue();
            case GAME_END:
                return this.gameEndTimer.getValue();
            default:
                return true;
        }
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.eventTimers.getValue() && this.gameStarted) {
                long now = System.currentTimeMillis();
                int elapsedSeconds = (int)Math.max(0L, (now - this.gameStartTime) / 1000L);
                Font font = FontRepository.getHudFont(18);
                if (this.eventTime.getValue()) {
                    float x = (float)this.eventDrag.position.x;
                    float y = (float)this.eventDrag.position.y;
                    float sc = this.eventScale.getValue();
                    float maxWidth = 0.0F;
                    float startY = y;
                    boolean diamondShown = false;
                    boolean emeraldShown = false;
                    int shown = 0;
                    String diamond2 = (
                            this.eventDynamicColor.getValue() ? EnumChatFormatting.AQUA : EnumChatFormatting.WHITE
                        )
                        + "Diamond "
                        + EnumChatFormatting.WHITE
                        + (this.romanNumerals.getValue() ? "II" : "2");
                    String diamond3 = (
                            this.eventDynamicColor.getValue() ? EnumChatFormatting.AQUA : EnumChatFormatting.WHITE
                        )
                        + "Diamond "
                        + EnumChatFormatting.WHITE
                        + (this.romanNumerals.getValue() ? "III" : "3");
                    String emerald2 = (
                            this.eventDynamicColor.getValue()
                                ? EnumChatFormatting.DARK_GREEN
                                : EnumChatFormatting.WHITE
                        )
                        + "Emerald "
                        + EnumChatFormatting.WHITE
                        + (this.romanNumerals.getValue() ? "II" : "2");
                    String emerald3 = (
                            this.eventDynamicColor.getValue()
                                ? EnumChatFormatting.DARK_GREEN
                                : EnumChatFormatting.WHITE
                        )
                        + "Emerald "
                        + EnumChatFormatting.WHITE
                        + (this.romanNumerals.getValue() ? "III" : "3");
                    String bedGone = (
                            this.eventDynamicColor.getValue() ? EnumChatFormatting.GOLD : EnumChatFormatting.WHITE
                        )
                        + "Bed Gone";
                    String suddenDeath = (
                            this.eventDynamicColor.getValue()
                                ? EnumChatFormatting.DARK_PURPLE
                                : EnumChatFormatting.WHITE
                        )
                        + "Sudden Death";
                    String gameEnd = (
                            this.eventDynamicColor.getValue() ? EnumChatFormatting.RED : EnumChatFormatting.WHITE
                        )
                        + "Game End";
                    EventTimersFeature.EventEntry[] schedule = new EventTimersFeature.EventEntry[]{
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Items.field_151045_i),
                            diamond2,
                            360,
                            EventTimersFeature.EventTypeTimer.DIAMOND
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Items.field_151166_bC),
                            emerald2,
                            720,
                            EventTimersFeature.EventTypeTimer.EMERALD
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Items.field_151045_i),
                            diamond3,
                            1080,
                            EventTimersFeature.EventTypeTimer.DIAMOND
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Items.field_151166_bC),
                            emerald3,
                            1440,
                            EventTimersFeature.EventTypeTimer.EMERALD
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Blocks.field_150324_C),
                            bedGone,
                            1800,
                            EventTimersFeature.EventTypeTimer.BED_GONE
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Blocks.field_150380_bt),
                            suddenDeath,
                            2400,
                            EventTimersFeature.EventTypeTimer.SUDDEN_DEATH
                        ),
                        new EventTimersFeature.EventEntry(
                            new ItemStack(Blocks.field_150427_aO),
                            gameEnd,
                            3000,
                            EventTimersFeature.EventTypeTimer.GAME_END
                        )
                    };

                    for (EventTimersFeature.EventEntry entry : schedule) {
                        if (this.shouldShow(entry.type)) {
                            int remainingSeconds = entry.targetSeconds - elapsedSeconds;
                            if (remainingSeconds > 0) {
                                if (entry.type == EventTimersFeature.EventTypeTimer.DIAMOND) {
                                    if (diamondShown) {
                                        continue;
                                    }

                                    diamondShown = true;
                                }

                                if (entry.type == EventTimersFeature.EventTypeTimer.EMERALD) {
                                    if (emeraldShown) {
                                        continue;
                                    }

                                    emeraldShown = true;
                                }

                                this.renderItemIcon(mc, entry.icon, x, y, sc);
                                font.drawWithShadow(entry.title, x + 18.0F * sc, y, -1);
                                font.drawWithShadow(
                                    EnumChatFormatting.GRAY + this.formatTime(remainingSeconds),
                                    x + 18.0F * sc,
                                    y + (font.getFontHeight() + 2) * sc,
                                    -1
                                );
                                y += Math.max((int)((font.getFontHeight() * 2 + 4) * sc), (int)(16.0F * sc))
                                    + (int)(4.0F * sc);
                                float w1 = font.getStringWidth(EnumChatFormatting.func_110646_a(entry.title)) * sc
                                    + 18.0F * sc;
                                float w2 = font.getStringWidth(
                                            EnumChatFormatting.func_110646_a(
                                                EnumChatFormatting.GRAY + this.formatTime(remainingSeconds)
                                            )
                                        )
                                        * sc
                                    + 18.0F * sc;
                                if (w1 > maxWidth) {
                                    maxWidth = w1;
                                }

                                if (w2 > maxWidth) {
                                    maxWidth = w2;
                                }

                                if (this.onlyNext.getValue()) {
                                    if (++shown >= 2) {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    this.eventDrag.setScale(new Vector2d(maxWidth, y - startY));
                }

                if (this.emeraldTime.getValue()) {
                    EventTimersFeature.EmeraldEntry modeData = this.currentModeData;
                    int nextSpawnTime = 31;
                    int totalEmeralds = 0;
                    int spawnTime = 31;

                    while (elapsedSeconds >= nextSpawnTime) {
                        nextSpawnTime += modeData.getSpawnInterval(nextSpawnTime);
                    }

                    while (elapsedSeconds >= spawnTime) {
                        totalEmeralds += modeData.emeraldsPerSpawn;
                        spawnTime += modeData.getSpawnInterval(spawnTime);
                    }

                    int nextEmeraldSpawn = Math.max(0, nextSpawnTime - elapsedSeconds);
                    float x = (float)this.emeraldDrag.position.x;
                    float y = (float)this.emeraldDrag.position.y;
                    float sc = this.eventScale.getValue();
                    EnumChatFormatting timeColor = !this.emeraldDynamicColor.getValue()
                        ? EnumChatFormatting.GRAY
                        : (
                            nextEmeraldSpawn < 3
                                ? EnumChatFormatting.DARK_RED
                                : (
                                    nextEmeraldSpawn < 5
                                        ? EnumChatFormatting.RED
                                        : (
                                            nextEmeraldSpawn < 9
                                                ? EnumChatFormatting.GOLD
                                                : (
                                                    nextEmeraldSpawn < 12
                                                        ? EnumChatFormatting.YELLOW
                                                        : EnumChatFormatting.DARK_GREEN
                                                )
                                        )
                                )
                        );
                    String mainText = "Next Emerald: " + timeColor + nextEmeraldSpawn + EnumChatFormatting.GRAY + " s";
                    String secondText = EnumChatFormatting.WHITE
                        + "Total: "
                        + EnumChatFormatting.DARK_GREEN
                        + totalEmeralds;
                    int textBlockHeight = (int)((font.getFontHeight() * 2 + 2) * sc);
                    int iconSize = (int)(16.0F * sc);
                    float iconY = y + Math.max(0.0F, (textBlockHeight - iconSize) / 2.0F);
                    this.renderItemIcon(mc, EMERALD_ICON, x, iconY, sc);
                    font.drawWithShadow(mainText, x + 18.0F * sc, y, -1);
                    font.drawWithShadow(secondText, x + 18.0F * sc, y + (font.getFontHeight() + 2) * sc, -1);
                    float w1 = font.getStringWidth(EnumChatFormatting.func_110646_a(mainText)) * sc + 18.0F * sc;
                    float w2 = font.getStringWidth(EnumChatFormatting.func_110646_a(secondText)) * sc + 18.0F * sc;
                    this.emeraldDrag.setScale(new Vector2d(Math.max(w1, w2), (font.getFontHeight() * 2 + 4) * sc));
                }
            }
        }
    }

    private static class EmeraldEntry {
        final int tierOneInterval;
        final int tierTwoInterval;
        final int tierThreeInterval;
        final int emeraldsPerSpawn;

        EmeraldEntry(int tierOneInterval, int tierTwoInterval, int tierThreeInterval, int emeraldsPerSpawn) {
            this.tierOneInterval = tierOneInterval;
            this.tierTwoInterval = tierTwoInterval;
            this.tierThreeInterval = tierThreeInterval;
            this.emeraldsPerSpawn = emeraldsPerSpawn;
        }

        int getSpawnInterval(int elapsedSeconds) {
            if (elapsedSeconds >= 1440) {
                return this.tierThreeInterval;
            } else {
                return elapsedSeconds >= 720 ? this.tierTwoInterval : this.tierOneInterval;
            }
        }
    }

    private static class EventEntry {
        final ItemStack icon;
        final String title;
        final int targetSeconds;
        final EventTimersFeature.EventTypeTimer type;

        private EventEntry(ItemStack icon, String title, int targetSeconds, EventTimersFeature.EventTypeTimer type) {
            this.icon = icon;
            this.title = title;
            this.targetSeconds = targetSeconds;
            this.type = type;
        }
    }

    private enum EventTypeTimer {
        DIAMOND,
        EMERALD,
        BED_GONE,
        SUDDEN_DEATH,
        GAME_END;
    }
}
