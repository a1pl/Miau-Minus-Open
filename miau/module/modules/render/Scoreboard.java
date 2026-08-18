package miau.module.modules.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import miau.Miau;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.IntProperty;
import miau.util.animation.Animation;
import miau.util.animation.Easing;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class Scoreboard extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final DragProperty drag = new DragProperty("Position", new Vector2d(0.0, 0.0), false, true);
    public float defaultX = 0.0F;
    public float defaultY = 0.0F;
    public final IntProperty yOffset = new IntProperty("Y Offset", 0, -250, 250);
    public final BooleanProperty customFont = new BooleanProperty("Custom Font", false);
    public final BooleanProperty textShadow = new BooleanProperty("Text Shadow", true);
    public final BooleanProperty redNumbers = new BooleanProperty("Red Numbers", false);
    public final BooleanProperty shaders = new BooleanProperty("Shaders", false);
    private final Animation autofitAnimation = new Animation(Easing.EASE_OUT_EXPO, 300L);

    public Scoreboard() {
        super("Scoreboard", true, false);
    }

    public void updateBounds(ScaledResolution scaledRes) {
        net.minecraft.scoreboard.Scoreboard sb = null;
        ScoreObjective objective = null;
        if (mc.field_71441_e != null) {
            sb = mc.field_71441_e.func_96441_U();
            if (sb != null) {
                objective = sb.func_96539_a(1);
            }
        }

        int size;
        int maxWidth;
        if (objective != null && sb != null) {
            Collection<Score> collection = sb.func_96534_i(objective);
            List<Score> list = new ArrayList<>();

            for (Score score : collection) {
                if (score.func_96653_e() != null && !score.func_96653_e().startsWith("#")) {
                    list.add(score);
                }
            }

            if (list.size() > 15) {
                list = list.subList(list.size() - 15, list.size());
            }

            size = list.size();
            maxWidth = mc.field_71466_p.func_78256_a(objective.func_96678_d());

            for (Score score : list) {
                ScorePlayerTeam team = sb.func_96509_i(score.func_96653_e());
                String name = ScorePlayerTeam.func_96667_a(team, score.func_96653_e()) + ": " + score.func_96652_c();
                maxWidth = Math.max(maxWidth, mc.field_71466_p.func_78256_a(name));
            }
        } else {
            size = 5;
            maxWidth = 80;
        }

        int padding = 8;
        int width = maxWidth + padding + 4;
        int height = size * mc.field_71466_p.field_78288_b + 14;
        float baseX = scaledRes.func_78326_a() - width - 2;
        float baseY = scaledRes.func_78328_b() / 2 - height / 3 + this.yOffset.getValue();
        float autofitOffset = 0.0F;
        HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
        if (hud != null && hud.isEnabled() && hud.posX.getValue() == 1) {
            float moduleListHeight = hud.getModuleListHeight();
            if (moduleListHeight > 0.0F) {
                float hudStartY;
                if (hud.posY.getValue() == 0) {
                    hudStartY = hud.offsetY.getValue().intValue();
                    if (hud.showWatermark.getValue()) {
                        hudStartY += hud.getFont().getFontHeight() + 6.0F;
                    }
                } else {
                    hudStartY = scaledRes.func_78328_b() - hud.offsetY.getValue() - moduleListHeight;
                }

                float hudBottom = hudStartY + moduleListHeight;
                if (hudBottom > baseY) {
                    autofitOffset = hudBottom - baseY + 4.0F;
                }
            }
        }

        this.defaultX = baseX;
        this.autofitAnimation.run(baseY + autofitOffset);
        this.defaultY = this.autofitAnimation.getValue();
        this.drag.position.x = baseX;
        this.drag.position.y = this.defaultY;
        this.drag.targetPosition.x = baseX;
        this.drag.targetPosition.y = this.defaultY;
        this.drag.scale.x = width;
        this.drag.scale.y = height;
    }
}
