package miau.module.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector4d;
import miau.Miau;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.ResizeEvent;
import miau.event.impl.TickEvent;
import miau.mixin.IAccessorEntityRenderer;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.TeamUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class ESP extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean shadersAvailable = true;
    private Framebuffer framebuffer = null;
    private boolean outline = true;
    private boolean glow = true;
    public final ModeProperty mode = new ModeProperty(
        "mode", 2, new String[]{"NONE", "2D", "3D", "OUTLINE", "FAKECORNER", "FAKE2D"}
    );
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final ModeProperty healthBar = new ModeProperty("health-bar", 0, new String[]{"NONE", "2D", "RAVEN"});
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);
    private final List<EntityPlayer> cachedEntities = new ArrayList<>();

    private boolean shouldRenderPlayer(EntityPlayer entityPlayer) {
        if (entityPlayer.field_70725_aQ > 0) {
            return false;
        }

        if (mc.func_175606_aa().func_70032_d(entityPlayer) > 512.0F) {
            return false;
        }

        if (entityPlayer != mc.field_71439_g && entityPlayer != mc.func_175606_aa()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.bots.getValue();
            } else {
                return TeamUtil.isFriend(entityPlayer)
                    ? this.friends.getValue()
                    : TeamUtil.isTarget(entityPlayer) ? this.enemies.getValue() : this.players.getValue();
            }
        } else {
            return this.self.getValue() && mc.field_71474_y.field_74320_O != 0;
        }
    }

    private int getEntityColorInt(EntityPlayer entityPlayer) {
        if (TeamUtil.isFriend(entityPlayer)) {
            return Miau.friendManager.getColor().getRGB();
        }

        if (TeamUtil.isTarget(entityPlayer)) {
            return Miau.targetManager.getColor().getRGB();
        }

        switch (this.color.getValue()) {
            case 0:
                return TeamUtil.getTeamColor(entityPlayer, 1.0F).getRGB();
            case 1:
                return TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
            case 2:
                return ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
            default:
                return -1;
        }
    }

    public ESP() {
        super("ESP", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null) {
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (this.shouldRenderPlayer(player)) {
                    player.field_70158_ak = true;
                }
            }
        }
    }

    public boolean isOutlineEnabled() {
        return this.outline;
    }

    public boolean isGlowEnabled() {
        return this.glow;
    }

    @EventTarget
    public void onResize(ResizeEvent event) {
        if (this.framebuffer != null) {
            this.framebuffer.func_147608_a();
        }

        this.framebuffer = new Framebuffer(mc.field_71443_c, mc.field_71440_d, false);
    }

    @EventTarget(1)
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()
            && (this.mode.getValue() == 1 || this.mode.getValue() == 3 || this.healthBar.getValue() == 1)) {
            this.cachedEntities.clear();

            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer)entity)) {
                    this.cachedEntities.add((EntityPlayer)entity);
                }
            }

            if (!this.cachedEntities.isEmpty()) {
                if (this.mode.getValue() == 3) {
                }

                if (this.mode.getValue() == 1 || this.healthBar.getValue() == 1) {
                    RenderUtil.enableRenderState();
                    double scaleFactor = new ScaledResolution(mc).func_78325_e();
                    double scale = 1.0 / scaleFactor;
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179139_a(scale, scale, scale);

                    for (EntityPlayer player : this.cachedEntities) {
                        ((IAccessorEntityRenderer)mc.field_71460_t)
                            .callSetupCameraTransform(event.getPartialTicks(), 0);
                        Vector4d screenPosition = RenderUtil.projectToScreen(player, scaleFactor);
                        mc.field_71460_t.func_78478_c();
                        if (screenPosition != null) {
                            float x = (float)screenPosition.x;
                            float y = (float)screenPosition.y;
                            float z = (float)screenPosition.z;
                            float w = (float)screenPosition.w;
                            if (this.mode.getValue() == 1) {
                                int color = this.getEntityColorInt(player);
                                RenderUtil.drawOutlineRect(
                                    x, y, z, w, 3.0F, 0, (color & 16579836) >> 2 | color & 0xFF000000
                                );
                                RenderUtil.drawOutlineRect(x, y, z, w, 1.5F, 0, color);
                            }

                            if (this.healthBar.getValue() == 1) {
                                float heal = player.func_110143_aJ() + player.func_110139_bj();
                                float percent = Math.min(Math.max(heal / player.func_110138_aP(), 0.0F), 1.0F);
                                float box = (z - x) * 0.08F;
                                Color healthColor = ColorUtil.getHealthBlend(percent);
                                RenderUtil.drawLine(
                                    x - box, y, x - box, w, 3.0F, ColorUtil.darker(healthColor, 0.2F).getRGB()
                                );
                                RenderUtil.drawLine(
                                    x - box, w, x - box, w + (y - w) * percent, 1.5F, healthColor.getRGB()
                                );
                            }
                        }
                    }

                    GlStateManager.func_179121_F();
                    RenderUtil.disableRenderState();
                }
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()
            && (
                this.mode.getValue() == 2
                    || this.mode.getValue() == 4
                    || this.mode.getValue() == 5
                    || this.healthBar.getValue() == 2
            )) {
            RenderUtil.enableRenderState();
            this.cachedEntities.clear();

            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer)entity)) {
                    this.cachedEntities.add((EntityPlayer)entity);
                }
            }

            for (EntityPlayer player : this.cachedEntities) {
                if (this.mode.getValue() == 2) {
                    int color = this.getEntityColorInt(player);
                    RenderUtil.drawEntityBoundingBox(
                        player, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF, 1.5F, 0.1F
                    );
                    GlStateManager.func_179117_G();
                }

                if (this.mode.getValue() == 4) {
                    int color = this.getEntityColorInt(player);
                    RenderUtil.drawCornerESP(
                        player, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F
                    );
                }

                if (this.mode.getValue() == 5) {
                    int color = this.getEntityColorInt(player);
                    RenderUtil.drawFake2DESP(
                        player, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F
                    );
                }

                if (this.healthBar.getValue() == 2) {
                    double x = RenderUtil.lerpDouble(
                            player.field_70165_t, player.field_70142_S, event.getPartialTicks()
                        )
                        - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
                    double y = RenderUtil.lerpDouble(
                            player.field_70163_u, player.field_70137_T, event.getPartialTicks()
                        )
                        - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
                        - 0.1F;
                    double z = RenderUtil.lerpDouble(
                            player.field_70161_v, player.field_70136_U, event.getPartialTicks()
                        )
                        - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179137_b(x, y, z);
                    GlStateManager.func_179114_b(mc.func_175598_ae().field_78735_i * -1.0F, 0.0F, 1.0F, 0.0F);
                    float heal = player.func_110143_aJ() + player.func_110139_bj();
                    float percent = Math.min(Math.max(heal / player.func_110138_aP(), 0.0F), 1.0F);
                    Color healthColor = ColorUtil.getHealthBlend(percent);
                    float height = player.field_70131_O + 0.2F;
                    RenderUtil.drawRect3D(
                        0.57250005F, -0.027500002F, 0.7275F, height + 0.027500002F, Color.black.getRGB()
                    );
                    RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height, Color.darkGray.getRGB());
                    RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height * percent, healthColor.getRGB());
                    GlStateManager.func_179121_F();
                }
            }

            RenderUtil.disableRenderState();
        }
    }
}
