package miau.module.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.PercentProperty;
import miau.util.render.RenderUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class Trajectories extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty projectiles = new BooleanProperty("projectiles", false);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);

    public Trajectories() {
        super("Trajectories", false, true);
    }

    // $VF: Unable to simplify switch-on-enum, as the enum class was not able to be found.
    // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.field_71439_g.func_70694_bm() != null && mc.field_71474_y.field_74320_O == 0) {
            Item item = mc.field_71439_g.func_70694_bm().func_77973_b();
            RenderManager renderManager = mc.func_175598_ae();
            boolean isBow = false;
            float velocityMultiplier = 1.5F;
            float drag = 0.99F;
            float gravity;
            float hitboxExpand;
            if (item instanceof ItemBow && this.bow.getValue()) {
                if (!mc.field_71439_g.func_71039_bw()) {
                    return;
                }

                isBow = true;
                gravity = 0.05F;
                hitboxExpand = 0.3F;
                float charge = mc.field_71439_g.func_71057_bx() / 20.0F;
                charge = (charge * charge + charge * 2.0F) / 3.0F;
                if (charge < 0.1F) {
                    return;
                }

                if (charge > 1.0F) {
                    charge = 1.0F;
                }

                velocityMultiplier = charge * 3.0F;
            } else if (item instanceof ItemFishingRod && this.projectiles.getValue()) {
                gravity = 0.04F;
                hitboxExpand = 0.25F;
                drag = 0.92F;
            } else if ((item instanceof ItemSnowball || item instanceof ItemEgg) && this.projectiles.getValue()) {
                gravity = 0.03F;
                hitboxExpand = 0.25F;
            } else {
                if (!(item instanceof ItemEnderPearl) || !this.pearls.getValue()) {
                    return;
                }

                gravity = 0.03F;
                hitboxExpand = 0.25F;
            }

            float yaw = mc.field_71439_g.field_70177_z;
            float pitch = mc.field_71439_g.field_70125_A;
            double x = ((IAccessorRenderManager)renderManager).getRenderPosX()
                - MathHelper.func_76134_b(yaw / 180.0F * (float) Math.PI) * 0.16;
            double y = ((IAccessorRenderManager)renderManager).getRenderPosY() + mc.field_71439_g.func_70047_e() - 0.1F;
            double z = ((IAccessorRenderManager)renderManager).getRenderPosZ()
                - MathHelper.func_76126_a(yaw / 180.0F * (float) Math.PI) * 0.16;
            double mx = MathHelper.func_76126_a(yaw / 180.0F * (float) Math.PI)
                    * MathHelper.func_76134_b(pitch / 180.0F * (float) Math.PI)
                * (isBow ? 1.0 : 0.4)
                * -1.0;
            double my = MathHelper.func_76126_a(pitch / 180.0F * (float) Math.PI) * (isBow ? 1.0 : 0.4) * -1.0;
            double mz = MathHelper.func_76134_b(yaw / 180.0F * (float) Math.PI)
                    * MathHelper.func_76134_b(pitch / 180.0F * (float) Math.PI)
                * (isBow ? 1.0 : 0.4);
            float mag = MathHelper.func_76133_a(mx * mx + my * my + mz * mz);
            mx /= mag;
            my /= mag;
            mz /= mag;
            mx *= velocityMultiplier;
            my *= velocityMultiplier;
            mz *= velocityMultiplier;
            MovingObjectPosition mop = null;
            boolean hasHitBlock = false;
            boolean hasHitEntity = false;
            WorldRenderer worldRenderer = Tessellator.func_178181_a().func_178180_c();
            ArrayList<Vec3> trajectoryPoints = new ArrayList<>();

            while (!hasHitBlock && y > 0.0) {
                Vec3 start = new Vec3(x, y, z);
                Vec3 end = new Vec3(x + mx, y + my, z + mz);
                mop = mc.field_71441_e.func_147447_a(start, end, false, true, false);
                start = new Vec3(x, y, z);
                end = new Vec3(x + mx, y + my, z + mz);
                if (mop != null) {
                    hasHitBlock = true;
                    end = new Vec3(
                        mop.field_72307_f.field_72450_a,
                        mop.field_72307_f.field_72448_b,
                        mop.field_72307_f.field_72449_c
                    );
                }

                AxisAlignedBB aabb = new AxisAlignedBB(
                        x - hitboxExpand,
                        y - hitboxExpand,
                        z - hitboxExpand,
                        x + hitboxExpand,
                        y + hitboxExpand,
                        z + hitboxExpand
                    )
                    .func_72321_a(mx, my, mz)
                    .func_72314_b(1.0, 1.0, 1.0);
                int minChunkX = MathHelper.func_76128_c((aabb.field_72340_a - 2.0) / 16.0);
                int maxChunkX = MathHelper.func_76128_c((aabb.field_72336_d + 2.0) / 16.0);
                int minChunkZ = MathHelper.func_76128_c((aabb.field_72339_c - 2.0) / 16.0);
                int maxChunkZ = MathHelper.func_76128_c((aabb.field_72334_f + 2.0) / 16.0);
                ArrayList<Entity> possibleEntities = new ArrayList<>();

                for (int x1 = minChunkX; x1 <= maxChunkX; x1++) {
                    for (int z1 = minChunkZ; z1 <= maxChunkZ; z1++) {
                        mc.field_71441_e
                            .func_72964_e(x1, z1)
                            .func_177414_a(mc.field_71439_g, aabb, possibleEntities, null);
                    }
                }

                for (Entity entity : possibleEntities) {
                    if (entity.func_70067_L() && entity != mc.field_71439_g) {
                        AxisAlignedBB entityBox = entity.func_174813_aQ()
                            .func_72314_b(hitboxExpand, hitboxExpand, hitboxExpand);
                        MovingObjectPosition intercept = entityBox.func_72327_a(start, end);
                        if (intercept != null) {
                            hasHitEntity = true;
                            hasHitBlock = true;
                            mop = intercept;
                        }
                    }
                }

                x += mx;
                y += my;
                z += mz;
                if (mc.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c().func_149688_o()
                    == Material.field_151586_h) {
                    mx *= 0.6;
                    my *= 0.6;
                    mz *= 0.6;
                } else {
                    mx *= drag;
                    my *= drag;
                    mz *= drag;
                }

                my -= gravity;
                trajectoryPoints.add(
                    new Vec3(
                        x - ((IAccessorRenderManager)renderManager).getRenderPosX(),
                        y - ((IAccessorRenderManager)renderManager).getRenderPosY(),
                        z - ((IAccessorRenderManager)renderManager).getRenderPosZ()
                    )
                );
            }

            if (trajectoryPoints.size() > 1) {
                RenderUtil.enableRenderState();
                RenderUtil.setColor(
                    new Color(
                            hasHitEntity ? 85 : 255,
                            255,
                            hasHitEntity ? 85 : 255,
                            (int)(this.opacity.getValue().floatValue() / 100.0F * 255.0F)
                        )
                        .getRGB()
                );
                GL11.glLineWidth(1.5F);
                GL11.glEnable(2848);
                GL11.glHint(3154, 4354);
                worldRenderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
                trajectoryPoints.forEach(
                    vec3 -> worldRenderer.func_181662_b(vec3.field_72450_a, vec3.field_72448_b, vec3.field_72449_c)
                        .func_181675_d()
                );
                Tessellator.func_178181_a().func_78381_a();
                GlStateManager.func_179094_E();
                GlStateManager.func_179137_b(
                    x - ((IAccessorRenderManager)renderManager).getRenderPosX(),
                    y - ((IAccessorRenderManager)renderManager).getRenderPosY(),
                    z - ((IAccessorRenderManager)renderManager).getRenderPosZ()
                );
                if (mop != null) {
                    switch (mop.field_178784_b.func_176740_k().ordinal()) {
                        case 0:
                            GlStateManager.func_179114_b(90.0F, 0.0F, 1.0F, 0.0F);
                            break;
                        case 1:
                            GlStateManager.func_179114_b(90.0F, 1.0F, 0.0F, 0.0F);
                    }

                    RenderUtil.drawLine(
                        -0.25F,
                        -0.25F,
                        0.25F,
                        0.25F,
                        1.5F,
                        new Color(
                                hasHitEntity ? 85 : 255,
                                255,
                                hasHitEntity ? 85 : 255,
                                (int)(this.opacity.getValue().floatValue() / 100.0F * 255.0F)
                            )
                            .getRGB()
                    );
                    RenderUtil.drawLine(
                        -0.25F,
                        0.25F,
                        0.25F,
                        -0.25F,
                        1.5F,
                        new Color(
                                hasHitEntity ? 85 : 255,
                                255,
                                hasHitEntity ? 85 : 255,
                                (int)(this.opacity.getValue().floatValue() / 100.0F * 255.0F)
                            )
                            .getRGB()
                    );
                }

                GlStateManager.func_179121_F();
                GL11.glDisable(2848);
                GL11.glLineWidth(2.0F);
                GlStateManager.func_179117_G();
                RenderUtil.disableRenderState();
            }
        }
    }
}
