package miau.module.modules.render;

import java.awt.Color;
import java.util.stream.Collectors;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class Indicators extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty offset = new FloatProperty("offset", 50.0F, 0.0F, 255.0F);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty fireballs = new BooleanProperty("fireballs", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);
    public final BooleanProperty egg = new BooleanProperty("egg", true);
    public final BooleanProperty snowball = new BooleanProperty("snowball", true);

    private boolean shouldRender(Entity entity) {
        double d = (entity.field_70165_t - entity.field_70142_S)
                * (mc.field_71439_g.field_70165_t - entity.field_70165_t)
            + (entity.field_70163_u - entity.field_70137_T)
                * (
                    mc.field_71439_g.field_70163_u
                        + mc.field_71439_g.func_70047_e()
                        - entity.field_70163_u
                        - entity.field_70131_O / 2.0
                )
            + (entity.field_70161_v - entity.field_70136_U) * (mc.field_71439_g.field_70161_v - entity.field_70161_v);
        if (d == 0.0) {
            return false;
        } else if (d < 0.0 && this.directionCheck.getValue()) {
            return false;
        } else if (this.fireballs.getValue() && entity instanceof EntityFireball) {
            return true;
        } else if (this.pearls.getValue() && entity instanceof EntityEnderPearl) {
            return true;
        } else if (this.arrows.getValue() && entity instanceof EntityArrow) {
            return true;
        } else {
            return this.egg.getValue() && entity instanceof EntityEgg
                ? true
                : this.snowball.getValue() && entity instanceof EntitySnowball;
        }
    }

    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.field_151059_bz;
        } else if (entity instanceof EntityEnderPearl) {
            return Items.field_151079_bi;
        } else if (entity instanceof EntityArrow) {
            return Items.field_151032_g;
        } else if (entity instanceof EntityEgg) {
            return Items.field_151110_aK;
        } else {
            return entity instanceof EntitySnowball ? Items.field_151126_ay : new Item();
        }
    }

    private Color getIndicatorColor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return new Color(12676363);
        } else if (entity instanceof EntityEnderPearl) {
            return new Color(2458740);
        } else {
            return entity instanceof EntityArrow ? new Color(9868950) : new Color(-1);
        }
    }

    public Indicators() {
        super("Indicators", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent render2DEvent) {
        if (this.isEnabled()) {
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()
                .stream()
                .filter(this::shouldRender)
                .collect(Collectors.toList())) {
                float offset = 10.0F + this.offset.getValue();
                float yawBetween = RotationUtil.getYawBetween(
                    RenderUtil.lerpDouble(
                        mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70169_q, render2DEvent.getPartialTicks()
                    ),
                    RenderUtil.lerpDouble(
                        mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70166_s, render2DEvent.getPartialTicks()
                    ),
                    RenderUtil.lerpDouble(entity.field_70165_t, entity.field_70169_q, render2DEvent.getPartialTicks()),
                    RenderUtil.lerpDouble(entity.field_70161_v, entity.field_70166_s, render2DEvent.getPartialTicks())
                );
                if (mc.field_71474_y.field_74320_O == 2) {
                    yawBetween += 180.0F;
                }

                float x = (float)Math.sin(Math.toRadians(yawBetween));
                float z = (float)Math.cos(Math.toRadians(yawBetween)) * -1.0F;
                GlStateManager.func_179094_E();
                GlStateManager.func_179097_i();
                GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);
                GlStateManager.func_179109_b(
                    new ScaledResolution(mc).func_78326_a() / 2.0F / this.scale.getValue(),
                    new ScaledResolution(mc).func_78328_b() / 2.0F / this.scale.getValue(),
                    0.0F
                );
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b((offset + 0.0F) * x - 8.0F, (offset + 0.0F) * z - 8.0F, -300.0F);
                mc.func_175599_af().func_180450_b(new ItemStack(this.getIndicatorItem(entity)), 0, 0);
                GlStateManager.func_179121_F();
                String string = String.format("%dm", (int)mc.field_71439_g.func_70032_d(entity));
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b(
                    (offset + 0.0F) * x - mc.field_71466_p.func_78256_a(string) / 2.0F + 1.0F,
                    (offset + 0.0F) * z + 1.0F,
                    -100.0F
                );
                mc.field_71466_p
                    .func_175063_a(string, 0.0F, 0.0F, ChatColors.GRAY.toAwtColor() & 16777215 | -1090519040);
                GlStateManager.func_179121_F();
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b((offset + 15.0F) * x + 1.0F, (offset + 15.0F) * z + 1.0F, -100.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawArrow(
                    0.0F,
                    0.0F,
                    (float)(Math.atan2(z, x) + Math.PI),
                    7.5F,
                    1.5F,
                    this.getIndicatorColor(entity).getRGB()
                );
                RenderUtil.disableRenderState();
                GlStateManager.func_179121_F();
                GlStateManager.func_179126_j();
                GlStateManager.func_179121_F();
            }
        }
    }
}
