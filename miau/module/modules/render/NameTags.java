package miau.module.modules.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.vecmath.Vector4d;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.TickEvent;
import miau.mixin.IAccessorEntityRenderer;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class NameTags extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final Map<String, Integer> nameWidths = new HashMap<>();
    public final BooleanProperty showTargets = new BooleanProperty("targets", false);
    public final BooleanProperty player = new BooleanProperty("player", true, () -> !this.showTargets.getValue());
    public final BooleanProperty invisibles = new BooleanProperty(
        "invisibles", false, () -> !this.showTargets.getValue()
    );
    public final BooleanProperty animals = new BooleanProperty("animals", false, () -> !this.showTargets.getValue());
    public final BooleanProperty mobs = new BooleanProperty("mobs", false, () -> !this.showTargets.getValue());
    public final BooleanProperty teams = new BooleanProperty(
        "player-teammates", true, () -> !this.showTargets.getValue()
    );
    public final BooleanProperty showTeam = new BooleanProperty("show-team-tag", false);
    public final BooleanProperty showTarget = new BooleanProperty("show-target-tag", false);
    public final BooleanProperty showFriendTag = new BooleanProperty("show-friend-tag", false);
    public final BooleanProperty shortenedTags = new BooleanProperty("shortened-tags", false);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);

    public NameTags() {
        super("NameTags", false);
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.nameWidths.clear();
    }

    public float getWidth(String name, FontRenderer font) {
        String id = name + font.hashCode();
        if (!this.nameWidths.containsKey(id)) {
            this.nameWidths.put(id, font.func_78256_a(name));
        }

        return this.nameWidths.get(id).intValue();
    }

    public boolean shouldRenderTags(EntityLivingBase entity) {
        if (entity == null || mc.field_71441_e == null || mc.field_71439_g == null) {
            return false;
        } else if (entity.field_70725_aQ > 0 || entity.field_70128_L) {
            return false;
        } else if (entity == mc.field_71439_g) {
            return this.player.getValue() && mc.field_71474_y.field_74320_O != 0 && !this.showTargets.getValue();
        } else if (mc.func_175606_aa() == null || mc.func_175606_aa().func_70032_d(entity) > 512.0F) {
            return false;
        } else if (entity.func_82150_aj() && !this.showTargets.getValue() && !this.invisibles.getValue()) {
            return false;
        } else if (this.showTargets.getValue()) {
            return !(entity instanceof EntityPlayer)
                ? false
                : Miau.targetManager != null && Miau.targetManager.isFriend(entity.func_70005_c_());
        } else if (entity instanceof EntityPlayer) {
            return !this.player.getValue()
                ? false
                : this.teams.getValue() || !TeamUtil.isSameTeam((EntityPlayer)entity);
        } else {
            return !(entity instanceof EntityAnimal)
                    && !(entity instanceof EntityBat)
                    && !(entity instanceof EntitySquid)
                    && !(entity instanceof EntityVillager)
                ? this.mobs.getValue()
                : this.animals.getValue();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase && this.shouldRenderTags((EntityLivingBase)entity)) {
                    entity.field_70158_ak = true;
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null) {
            double scaleFactor = new ScaledResolution(mc).func_78325_e();
            double scale = scaleFactor / Math.pow(scaleFactor, 2.0);
            GlStateManager.func_179094_E();
            GlStateManager.func_179139_a(scale, scale, scale);

            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase living = (EntityLivingBase)entity;
                    if (this.shouldRenderTags(living)) {
                        ((IAccessorEntityRenderer)mc.field_71460_t)
                            .callSetupCameraTransform(event.getPartialTicks(), 0);
                        Vector4d position = RenderUtil.projectToScreen(living, scaleFactor);
                        mc.field_71460_t.func_78478_c();
                        if (position != null) {
                            String nametag = living.func_145748_c_().func_150254_d()
                                + " §7[§4❤"
                                + Math.round(living.func_110143_aJ())
                                + "§7]";
                            if (this.showTeam.getValue()
                                && living instanceof EntityPlayer
                                && TeamUtil.isSameTeam((EntityPlayer)living)) {
                                nametag = "§a§l"
                                    + (this.shortenedTags.getValue() ? "[TM]" : "[TEAM]")
                                    + "§r "
                                    + nametag;
                            }

                            if (this.showTarget.getValue()
                                && living instanceof EntityPlayer
                                && Miau.targetManager != null
                                && Miau.targetManager.isFriend(living.func_70005_c_())) {
                                nametag = "§4§l"
                                    + (this.shortenedTags.getValue() ? "[T]" : "[TARGET]")
                                    + "§r "
                                    + nametag;
                            }

                            if (this.showFriendTag.getValue()
                                && living instanceof EntityPlayer
                                && Miau.friendManager != null
                                && Miau.friendManager.isFriend(living.func_70005_c_())) {
                                nametag = "§b§l"
                                    + (this.shortenedTags.getValue() ? "[F]" : "[FRIEND]")
                                    + "§r "
                                    + nametag;
                            }

                            float padding = 2.0F;
                            float height = 8.0F;
                            float width = this.getWidth(nametag, mc.field_71466_p);
                            float posX = (float)(position.x + (position.z - position.x) / 2.0);
                            float posY = (float)position.y - height;
                            GlStateManager.func_179094_E();
                            GlStateManager.func_179109_b(posX, posY, 0.0F);
                            float scaleVal = this.scale.getValue();
                            GlStateManager.func_179152_a(scaleVal, scaleVal, 1.0F);
                            float x1 = -width / 2.0F - padding;
                            float y1 = -padding - 3.0F;
                            float x2 = x1 + width + padding * 2.0F;
                            float y2 = y1 + height + padding * 2.0F;
                            RenderUtil.enableRenderState();
                            RenderUtil.drawRect(x1, y1, x2, y2, Themes.getBackgroundShade().getRGB());
                            RenderUtil.disableRenderState();
                            float centeredPosX = -(width / 2.0F);
                            mc.field_71466_p
                                .func_175065_a(nametag, centeredPosX + 0.5F, -2.0F, Color.WHITE.getRGB(), true);
                            GlStateManager.func_179121_F();
                        }
                    }
                }
            }

            GlStateManager.func_179121_F();
        }
    }
}
