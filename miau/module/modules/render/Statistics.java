package miau.module.modules.render;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.ColorProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.ModeProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.shader.RoundedUtils;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.StringUtils;

public class Statistics extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static int wins;
    public static int killCount;
    public static long startTime = System.currentTimeMillis();
    public static final String[] KILL_TRIGGERS = new String[]{"by *", "para *", "fue destrozado a manos de *"};
    public final DragProperty dragging = new DragProperty("SessionStats", new Vector2d(5.0, 150.0));
    public final ModeProperty colorMode = new ModeProperty("Color Mode", 0, new String[]{"HUD", "Custom"});
    public final ColorProperty customColor = new ColorProperty(
        "Custom Color", new Color(255, 105, 180).getRGB(), () -> this.colorMode.getValue() == 1
    );
    private float width;
    private float height;
    private String timeString = "0 seconds";

    public Statistics() {
        super("Statistics", false, true);
    }

    private Color getAccentColor() {
        if (this.colorMode.getValue() == 0) {
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            return hud.getColor(System.currentTimeMillis(), 0L);
        } else {
            return new Color(this.customColor.getValue());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (this.isEnabled()) {
            if (mc.field_71439_g != null && mc.field_71439_g.field_70173_aa % 20 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                long hours = TimeUnit.MILLISECONDS.toHours(elapsed);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60L;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60L;
                String base = "";
                if (hours > 0L) {
                    base = base + hours + " " + (hours == 1L ? "hour" : "hours") + (minutes == 0L ? "" : " ");
                }

                if (minutes > 0L) {
                    base = base
                        + minutes
                        + " "
                        + (minutes == 1L ? "minute" : "minutes")
                        + (seconds != 0L && hours <= 0L ? " " : "");
                }

                if (seconds > 0L && hours == 0L) {
                    base = base + seconds + " " + (seconds == 1L ? "second" : "seconds");
                }

                if (base.isEmpty()) {
                    base = "0 seconds";
                }

                this.timeString = base;
            }

            float x = (float)this.dragging.position.x;
            float y = (float)this.dragging.position.y;
            this.width = 150.0F;
            this.height = 82.0F;
            this.dragging.scale.x = this.width;
            this.dragging.scale.y = this.height;
            Color c1 = this.applyOpacity(this.getAccentColor(), 0.8F);
            RoundedUtils.drawRoundOutline(x, y, this.width, this.height, 6.0F, 1.0F, new Color(0, 0, 0, 100), c1);
            Font font22 = FontRepository.getHudFont(22);
            Font font18 = FontRepository.getHudFont(18);
            double padding = 8.0;
            String title = "Session Stats";
            font22.draw(
                title,
                x + this.width / 2.0F - font22.getStringWidth(title) / 2.0F,
                (float)(y + padding),
                this.getAccentColor().getRGB()
            );
            font18.draw(
                this.timeString,
                x + this.width / 2.0F - font18.getStringWidth(this.timeString) / 2.0F,
                (float)(y + padding + 16.0),
                new Color(255, 255, 255, 200).getRGB()
            );
            String serverIp = mc.func_71387_A()
                ? "Singleplayer"
                : (mc.func_147104_D() != null ? mc.func_147104_D().field_78845_b : "None");
            String serverText = "Server: " + serverIp;
            font18.draw(
                serverText,
                x + this.width / 2.0F - font18.getStringWidth(serverText) / 2.0F,
                (float)(y + padding + 28.0),
                new Color(255, 255, 255, 200).getRGB()
            );
            long activeModules = Miau.moduleManager.modules.values().stream().filter(Module::isEnabled).count();
            String modulesText = "Modules: " + activeModules;
            font18.draw(
                modulesText,
                x + this.width / 2.0F - font18.getStringWidth(modulesText) / 2.0F,
                (float)(y + padding + 40.0),
                new Color(255, 255, 255, 200).getRGB()
            );
            String killsText = "kills " + killCount;
            String winsText = "wins " + wins;
            font18.draw(killsText, x + 15.0F, (float)(y + padding + 57.0), new Color(255, 255, 255, 200).getRGB());
            font18.draw(
                winsText,
                x + this.width - 15.0F - font18.getStringWidth(winsText),
                (float)(y + padding + 57.0),
                new Color(255, 255, 255, 200).getRGB()
            );
        }
    }

    private Color applyOpacity(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255.0F));
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof S02PacketChat) {
                S02PacketChat packet = (S02PacketChat)event.getPacket();
                if (mc.field_71439_g == null) {
                    return;
                }

                String message = packet.func_148915_c().func_150260_c();
                String strippedMessage = StringUtils.func_76338_a(message);
                if (!strippedMessage.contains(":")
                    && Arrays.stream(KILL_TRIGGERS)
                        .anyMatch(strippedMessage.replace(mc.field_71439_g.func_70005_c_(), "*")::contains)) {
                    killCount++;
                }
            } else if (event.getPacket() instanceof S45PacketTitle) {
                S45PacketTitle packet = (S45PacketTitle)event.getPacket();
                if (packet.func_179805_b() != null) {
                    String text = StringUtils.func_76338_a(packet.func_179805_b().func_150260_c());
                    if (text.equals("VICTORY!")) {
                        wins++;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.field_71462_r instanceof GuiMainMenu
            || mc.field_71462_r instanceof GuiMultiplayer
            || mc.field_71462_r instanceof GuiDisconnected) {
            resetStats();
        }
    }

    public static void resetStats() {
        startTime = System.currentTimeMillis();
        wins = 0;
        killCount = 0;
    }
}
