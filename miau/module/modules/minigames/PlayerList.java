package miau.module.modules.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.event.impl.TickEvent;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.module.modules.misc.CheatDetector;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.math.MathUtil;
import miau.util.player.TeamUtil;
import miau.util.render.ShapeUtil;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

public class PlayerList extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 2.0F);
    public final DragProperty pos = new DragProperty("playerList", new Vector2d(4.0, 30.0));
    private final List<EntityPlayer> cachedPlayers = new ArrayList<>();
    private final Map<String, String> teamCache = new HashMap<>();
    public final BooleanProperty showTeam = new BooleanProperty("ShowTeam", true);

    private boolean shouldShowTeam() {
        if (!this.showTeam.getValue()) {
            return false;
        }

        if (mc.field_71441_e == null) {
            return true;
        }

        Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
        if (scoreboard != null) {
            ScoreObjective objective = scoreboard.func_96539_a(1);
            if (objective != null) {
                String title = EnumChatFormatting.func_110646_a(objective.func_96678_d());
                if (title != null && title.toUpperCase().contains("SKYWARS")) {
                    return false;
                }
            }
        }

        return true;
    }

    public PlayerList() {
        super("PlayerList", false, false);
    }

    private String getTeamName(EntityPlayer player) {
        ScorePlayerTeam team = (ScorePlayerTeam)player.func_96124_cp();
        String teamName = "None";
        if (team != null) {
            String prefix = FontRenderer.func_78282_e(team.func_96668_e());
            if (prefix.length() >= 2) {
                char colorChar = prefix.charAt(1);
                switch (colorChar) {
                    case '7':
                        teamName = "Gray";
                        break;
                    case '9':
                        teamName = "Blue";
                        break;
                    case 'a':
                        teamName = "Green";
                        break;
                    case 'b':
                        teamName = "Aqua";
                        break;
                    case 'c':
                        teamName = "Red";
                        break;
                    case 'd':
                        teamName = "Pink";
                        break;
                    case 'e':
                        teamName = "Yellow";
                        break;
                    case 'f':
                        teamName = "White";
                }
            }
        }

        String name = player.func_70005_c_();
        if (!teamName.equals("Gray") && !teamName.equals("None")) {
            this.teamCache.put(name, teamName);
        } else if (this.teamCache.containsKey(name)) {
            return this.teamCache.get(name);
        }

        return teamName;
    }

    @EventTarget
    public void onTickEvent(TickEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (mc.field_71439_g.field_70173_aa < 5) {
                this.teamCache.clear();
            }

            this.cachedPlayers.clear();

            for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                if (p != null && !p.field_70128_L && !AntiBot.isBot(p)) {
                    this.cachedPlayers.add(p);
                }
            }

            this.cachedPlayers.sort(Comparator.comparing(this::getTeamName));
        } else {
            this.teamCache.clear();
        }
    }

    @EventTarget
    public void onRender2DEvent(Render2DEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            Font font16 = FontRepository.getHudFont(16);
            Font font18 = FontRepository.getHudFont(18);
            float rowHeight = font16.getFontHeight() + 3;
            float headerHeight = 14.0F;
            float height = headerHeight + this.cachedPlayers.size() * rowHeight;
            float width = 220.0F;
            float scaleValue = this.scale.getValue();
            this.pos.scale.x = width * scaleValue;
            this.pos.scale.y = height * scaleValue;
            float x = (float)this.pos.position.x / scaleValue;
            float y = (float)this.pos.position.y / scaleValue;
            int ix = (int)x;
            int iy = (int)y;
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(scaleValue, scaleValue, 1.0F);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            Color clientColor = hud.getColor(0L);
            ShapeUtil.drawRect(ix, iy, ix + width, iy + height, new Color(0, 0, 0, 100).getRGB());
            ShapeUtil.drawRect(ix, iy, ix + width, iy + 1, clientColor.getRGB());
            boolean showTeamCol = this.shouldShowTeam();
            font18.draw("Players §7" + this.cachedPlayers.size(), ix + 16, iy + 3, -1, true);
            font18.draw("Dist", ix + 105, iy + 3, -1, true);
            font18.draw("HP", ix + 140, iy + 3, -1, true);
            if (showTeamCol) {
                font18.draw("Team", ix + 175, iy + 3, -1, true);
            }

            float currentY = iy + headerHeight;

            for (int i = 0; i < this.cachedPlayers.size(); i++) {
                EntityPlayer player = this.cachedPlayers.get(i);
                this.renderPlayer(player, ix, (int)currentY, font16, font18, clientColor, showTeamCol);
                currentY += rowHeight;
            }

            GlStateManager.func_179121_F();
        }
    }

    private void renderPlayer(
        EntityPlayer player, int x, int y, Font font16, Font font18, Color clientColor, boolean showTeamCol
    ) {
        float rowHeight = font16.getFontHeight() + 3;
        CheatDetector cheatDetector = (CheatDetector)Miau.moduleManager.modules.get(CheatDetector.class);
        if (cheatDetector != null && cheatDetector.isEnabled() && cheatDetector.isCheater(player)) {
            ShapeUtil.drawRect(x, y, x + 220, y + rowHeight, new Color(255, 0, 0, 60).getRGB());
        }

        int headWH = 10;
        int headX = x + 3;
        int headY = y + (int)(rowHeight / 2.0F - headWH / 2.0F);
        GlStateManager.func_179094_E();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        mc.func_110434_K().func_110577_a(((AbstractClientPlayer)player).func_110306_p());
        Gui.func_152125_a(headX, headY, 8.0F, 8.0F, 8, 8, headWH, headWH, 64.0F, 64.0F);
        GlStateManager.func_179121_F();
        int textY = y + (int)((rowHeight - font16.getFontHeight()) / 2.0F);
        Color nameColor = TeamUtil.getTeamColor(player, 1.0F);
        font16.drawWithShadow(player.func_70005_c_(), x + 16, textY, nameColor.getRGB());
        int dist = (int)MathUtil.round(mc.field_71439_g.func_70032_d(player), 0);
        font16.drawWithShadow(dist + "m", x + 105, textY, -5592406);
        float healthPercent = Math.min(player.func_110143_aJ() / player.func_110138_aP(), 1.0F);
        int hp = (int)MathUtil.round(healthPercent * 100.0F, 0);
        Color healthColor = healthPercent > 0.75
            ? new Color(66, 246, 123)
            : (
                healthPercent > 0.5
                    ? new Color(228, 255, 105)
                    : (healthPercent > 0.35 ? new Color(236, 100, 64) : new Color(255, 65, 68))
            );
        font16.drawWithShadow(hp + "%", x + 140, textY, healthColor.getRGB());
        if (showTeamCol) {
            String teamName = this.getTeamName(player);
            font16.drawWithShadow(teamName, x + 175, textY, nameColor.getRGB());
        }
    }
}
