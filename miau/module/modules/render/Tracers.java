package miau.module.modules.render;

import java.awt.Color;
import java.util.stream.Collectors;
import miau.Miau;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Tracers extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final BooleanProperty drawLines = new BooleanProperty("lines", true);
    public final BooleanProperty drawArrows = new BooleanProperty("arrows", false);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final IntProperty distance = new IntProperty("distance", 512, 0, 512);
    public final BooleanProperty showPlayers = new BooleanProperty("players", true);
    public final BooleanProperty showFriends = new BooleanProperty("friends", true);
    public final BooleanProperty showEnemies = new BooleanProperty("enemies", true);
    public final BooleanProperty showBots = new BooleanProperty("bots", false);

    private boolean shouldRender(EntityPlayer entityPlayer) {
        if (entityPlayer.field_70725_aQ > 0) {
            return false;
        } else if (mc.func_175606_aa().func_70032_d(entityPlayer) > this.distance.getValue().intValue()) {
            return false;
        } else if (entityPlayer == mc.field_71439_g || entityPlayer == mc.func_175606_aa()) {
            return false;
        } else if (TeamUtil.isBot(entityPlayer)) {
            return this.showBots.getValue();
        } else {
            return TeamUtil.isFriend(entityPlayer)
                ? this.showFriends.getValue()
                : TeamUtil.isTarget(entityPlayer) ? this.showEnemies.getValue() : this.showPlayers.getValue();
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer, float alpha) {
        if (TeamUtil.isFriend(entityPlayer)) {
            Color color = Miau.friendManager.getColor();
            return new Color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
        }

        if (TeamUtil.isTarget(entityPlayer)) {
            Color color = Miau.targetManager.getColor();
            return new Color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
        }

        switch (this.colorMode.getValue()) {
            case 0:
                return TeamUtil.getTeamColor(entityPlayer, alpha);
            case 1:
                int teamColor = TeamUtil.isSameTeam(entityPlayer)
                    ? ChatColors.BLUE.toAwtColor()
                    : ChatColors.RED.toAwtColor();
                return new Color(teamColor & Color.WHITE.getRGB() | (int)(alpha * 255.0F) << 24, true);
            case 2:
                int color = ((HUD)Miau.moduleManager.modules.get(HUD.class))
                    .getColor(System.currentTimeMillis())
                    .getRGB();
                return new Color(color & Color.WHITE.getRGB() | (int)(alpha * 255.0F) << 24, true);
            default:
                return new Color(1.0F, 1.0F, 1.0F, alpha);
        }
    }

    public Tracers() {
        super("Tracers", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.drawLines.getValue()) {
            RenderUtil.enableRenderState();
            Vec3 position;
            if (mc.field_71474_y.field_74320_O == 0) {
                position = new Vec3(0.0, 0.0, 1.0)
                    .func_178789_a(
                        (float)(
                            -Math.toRadians(
                                RenderUtil.lerpFloat(
                                    mc.func_175606_aa().field_70125_A,
                                    mc.func_175606_aa().field_70127_C,
                                    ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                )
                            )
                        )
                    )
                    .func_178785_b(
                        (float)(
                            -Math.toRadians(
                                RenderUtil.lerpFloat(
                                    mc.func_175606_aa().field_70177_z,
                                    mc.func_175606_aa().field_70126_B,
                                    ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                )
                            )
                        )
                    );
            } else {
                position = new Vec3(0.0, 0.0, 0.0)
                    .func_178789_a(
                        (float)(
                            -Math.toRadians(
                                RenderUtil.lerpFloat(
                                    mc.field_71439_g.field_70726_aT,
                                    mc.field_71439_g.field_70727_aS,
                                    ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                )
                            )
                        )
                    )
                    .func_178785_b(
                        (float)(
                            -Math.toRadians(
                                RenderUtil.lerpFloat(
                                    mc.field_71439_g.field_71109_bG,
                                    mc.field_71439_g.field_71107_bF,
                                    ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                )
                            )
                        )
                    );
            }

            position = new Vec3(
                position.field_72450_a,
                position.field_72448_b + mc.func_175606_aa().func_70047_e(),
                position.field_72449_c
            );

            for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted()
                .stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRender((EntityPlayer)entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList())) {
                Color color = this.getEntityColor(player, this.opacity.getValue().intValue() / 100.0F);
                double x = RenderUtil.lerpDouble(player.field_70165_t, player.field_70142_S, event.getPartialTicks());
                double y = RenderUtil.lerpDouble(player.field_70163_u, player.field_70137_T, event.getPartialTicks())
                    - (player.func_70093_af() ? 0.125 : 0.0);
                double z = RenderUtil.lerpDouble(player.field_70161_v, player.field_70136_U, event.getPartialTicks());
                RenderUtil.drawLine3D(
                    position,
                    x,
                    y + player.func_70047_e(),
                    z,
                    color.getRed() / 255.0F,
                    color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F,
                    color.getAlpha() / 255.0F,
                    1.5F
                );
            }

            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.drawArrows.getValue()) {
            for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted()
                .stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRender((EntityPlayer)entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList())) {
                float yawBetween = RotationUtil.getYawBetween(
                    RenderUtil.lerpDouble(
                        mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70169_q, event.getPartialTicks()
                    ),
                    RenderUtil.lerpDouble(
                        mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70166_s, event.getPartialTicks()
                    ),
                    RenderUtil.lerpDouble(player.field_70165_t, player.field_70169_q, event.getPartialTicks()),
                    RenderUtil.lerpDouble(player.field_70161_v, player.field_70166_s, event.getPartialTicks())
                );
                if (mc.field_71474_y.field_74320_O == 2) {
                    yawBetween += 180.0F;
                }

                float arrowDirX = (float)Math.sin(Math.toRadians(yawBetween));
                float arrowDirY = (float)Math.cos(Math.toRadians(yawBetween)) * -1.0F;
                float opacity = this.opacity.getValue().floatValue() / 100.0F;
                yawBetween = Math.abs(MathHelper.func_76142_g(yawBetween));
                if (yawBetween < 30.0F) {
                    opacity = 0.0F;
                } else if (yawBetween < 60.0F) {
                    opacity *= (yawBetween - 30.0F) / 30.0F;
                }

                HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(hud.scale.getValue(), hud.scale.getValue(), 0.0F);
                GlStateManager.func_179109_b(
                    new ScaledResolution(mc).func_78326_a() / 2.0F / hud.scale.getValue(),
                    new ScaledResolution(mc).func_78328_b() / 2.0F / hud.scale.getValue(),
                    0.0F
                );
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b(55.0F * arrowDirX + 1.0F, 55.0F * arrowDirY + 1.0F, -100.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawTriangle(
                    0.0F,
                    0.0F,
                    (float)(Math.atan2(arrowDirY, arrowDirX) + Math.PI),
                    10.0F,
                    this.getEntityColor(player, opacity).getRGB()
                );
                RenderUtil.disableRenderState();
                GlStateManager.func_179121_F();
                GlStateManager.func_179121_F();
            }
        }
    }
}
