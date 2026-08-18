package miau.mixin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import miau.Miau;
import miau.module.modules.render.HUD;
import miau.module.modules.render.Scoreboard;
import miau.util.shader.BlurUtils;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngameScoreboard {
    private static final Minecraft mc = Minecraft.func_71410_x();

    @Shadow
    protected abstract void func_180475_a(ScoreObjective var1, ScaledResolution var2);

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardPre(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Miau.moduleManager != null) {
            Scoreboard scoreboardMod = (Scoreboard)Miau.moduleManager.getModule(Scoreboard.class);
            if (scoreboardMod != null && scoreboardMod.isEnabled()) {
                scoreboardMod.updateBounds(scaledRes);
                if (scoreboardMod.shaders.getValue()) {
                    float cardX = scoreboardMod.defaultX;
                    float cardY = scoreboardMod.defaultY;
                    float cardWidth = (float)scoreboardMod.drag.scale.x;
                    float cardHeight = (float)scoreboardMod.drag.scale.y;
                    HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
                    boolean hudShaders = hud != null && hud.isEnabled() && hud.shaders.getValue();
                    if (hudShaders) {
                        BlurUtils.prepareBloom();
                        RoundedUtils.drawRound(
                            cardX - 1.0F,
                            cardY - 1.0F,
                            cardWidth + 2.0F,
                            cardHeight + 2.0F,
                            2.0F,
                            true,
                            new Color(81, 99, 149, 80)
                        );
                        BlurUtils.bloomEnd(3, 2.0F);
                        BlurUtils.prepareBlur();
                        RoundedUtils.drawRound(cardX, cardY, cardWidth, cardHeight, 2.0F, true, new Color(0, 0, 0, 150));
                        BlurUtils.blurEnd(2, 3.0F);
                    }
                }

                this.renderOpalScoreboard(objective, scaledRes, scoreboardMod, false);
                ci.cancel();
            }
        }
    }

    private void renderOpalScoreboard(
        ScoreObjective objective, ScaledResolution scaledRes, Scoreboard scoreboardMod, boolean transparencyOnly
    ) {
        net.minecraft.scoreboard.Scoreboard scoreboard = objective.func_96682_a();
        Collection<Score> collection = scoreboard.func_96534_i(objective);
        List<Score> list = new ArrayList<>();

        for (Score score : collection) {
            if (score.func_96653_e() != null && !score.func_96653_e().startsWith("#")) {
                list.add(score);
            }
        }

        if (list.size() > 15) {
            list = list.subList(list.size() - 15, list.size());
        }

        float cardX = scoreboardMod.defaultX;
        float cardY = scoreboardMod.defaultY;
        float cardWidth = (float)scoreboardMod.drag.scale.x;
        float cardHeight = (float)scoreboardMod.drag.scale.y;
        float radius = 2.0F;
        boolean shadow = scoreboardMod.textShadow.getValue();
        if (!transparencyOnly) {
            RoundedUtils.drawRound(cardX, cardY, cardWidth, cardHeight, radius, new Color(128, 9, 9, 9));
        }

        String title = objective.func_96678_d();
        float titleX = cardX + cardWidth / 2.0F - this.getStringWidth(title) / 2.0F;
        float titleY = cardY + 2.0F;
        this.drawString(title, titleX, titleY, -1, shadow);
        int lineCount = 0;

        for (Score score : list) {
            lineCount++;
            ScorePlayerTeam team = scoreboard.func_96509_i(score.func_96653_e());
            String playerName = ScorePlayerTeam.func_96667_a(team, score.func_96653_e());
            float entryY = cardY + mc.field_71466_p.field_78288_b + 2.0F + lineCount * mc.field_71466_p.field_78288_b;
            float leftPad = cardX + 4.0F;
            this.drawString(playerName, leftPad, entryY, -1, shadow);
            String scoreText;
            if (scoreboardMod.redNumbers.getValue()) {
                scoreText = EnumChatFormatting.RED + "" + score.func_96652_c();
            } else {
                scoreText = " " + score.func_96652_c();
            }

            float rightEdge = cardX + cardWidth - 4.0F;
            this.drawString(scoreText, rightEdge - this.getStringWidth(scoreText), entryY, -1, shadow);
        }
    }

    private int getStringWidth(String text) {
        return mc.field_71466_p.func_78256_a(text);
    }

    private void drawString(String text, double x, double y, int color, boolean shadow) {
        if (shadow) {
            mc.field_71466_p.func_175063_a(text, (float)x, (float)y, color);
        } else {
            mc.field_71466_p.func_78276_b(text, (int)x, (int)y, color);
        }
    }
}
