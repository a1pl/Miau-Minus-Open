package miau.module.modules.combat;

import java.util.function.Supplier;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.ITruePosition;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class ForwardTrack extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty espMode = new ModeProperty("ESP-Mode", 0, new String[]{"Box", "Model", "Wireframe"});
    public final FloatProperty wireframeWidth = new FloatProperty(
        "WireFrame-Width", 1.0F, 0.5F, 5.0F, () -> this.espMode.getModeString().equals("Wireframe")
    );
    public final ColorProperty espColor = new ColorProperty(
        "ESPColor", 65280, () -> !this.espMode.getModeString().equals("Model")
    );

    public ForwardTrack() {
        super("ForwardTrack", false);
    }

    private boolean isSelected(Entity entity) {
        return entity instanceof EntityLivingBase
            && entity != mc.field_71439_g
            && !entity.field_70128_L
            && entity.func_70089_S();
    }

    public void includeEntityTruePos(Entity entity, Supplier<Object> action) {
        if (this.isEnabled() && this.isSelected(entity)) {
            BackTrackUtil.runWithSimulatedPosition(entity, this.usePosition(entity), action);
        }
    }

    private Vec3 usePosition(Entity entity) {
        if (!mc.func_71387_A() && entity instanceof ITruePosition) {
            ITruePosition tp = (ITruePosition)entity;
            if (tp.isTruePos()) {
                return BackTrackUtil.getTrueInterpolatedPosition(
                    entity, tp, ((IAccessorMinecraft)mc).getTimer().field_74281_c
                );
            }
        }

        return new Vec3(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
    }

    private Vec3 renderPos() {
        IAccessorRenderManager accessor = (IAccessorRenderManager)mc.func_175598_ae();
        return new Vec3(accessor.getRenderPosX(), accessor.getRenderPosY(), accessor.getRenderPosZ());
    }

    private float lerpYaw(Entity entity, float partialTicks) {
        return entity.field_70126_B + (entity.field_70177_z - entity.field_70126_B) * partialTicks;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.field_71441_e != null) {
            Vec3 renderPos = this.renderPos();

            for (Entity target : mc.field_71441_e.field_72996_f) {
                if (this.isSelected(target)) {
                    Vec3 vec = this.usePosition(target);
                    double x = vec.field_72450_a - renderPos.field_72450_a;
                    double y = vec.field_72448_b - renderPos.field_72448_b;
                    double z = vec.field_72449_c - renderPos.field_72449_c;
                    String mode = this.espMode.getModeString();
                    int color = this.espColor.getValue();
                    if (mode.equals("Box")) {
                        AxisAlignedBB box = target.func_174813_aQ()
                            .func_72317_d(x - target.field_70165_t, y - target.field_70163_u, z - target.field_70161_v);
                        RenderUtil.drawBoundingBox(box, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 1.0F);
                    } else if (mode.equals("Model")) {
                        GlStateManager.func_179094_E();
                        GL11.glPushAttrib(1048575);
                        GlStateManager.func_179131_c(0.6F, 0.6F, 0.6F, 1.0F);
                        mc.func_175598_ae()
                            .func_147939_a(
                                target,
                                x,
                                y,
                                z,
                                this.lerpYaw(target, event.getPartialTicks()),
                                event.getPartialTicks(),
                                true
                            );
                        GL11.glPopAttrib();
                        GlStateManager.func_179121_F();
                    } else if (mode.equals("Wireframe")) {
                        GlStateManager.func_179094_E();
                        GL11.glPushAttrib(1048575);
                        GL11.glPolygonMode(1032, 6913);
                        GL11.glDisable(3553);
                        GL11.glDisable(2896);
                        GL11.glDisable(2929);
                        GL11.glEnable(2848);
                        GL11.glEnable(3042);
                        GL11.glBlendFunc(770, 771);
                        GL11.glLineWidth(this.wireframeWidth.getValue());
                        RenderUtil.glColor(color);
                        mc.func_175598_ae()
                            .func_147939_a(
                                target,
                                x,
                                y,
                                z,
                                this.lerpYaw(target, event.getPartialTicks()),
                                event.getPartialTicks(),
                                true
                            );
                        GL11.glPopAttrib();
                        GlStateManager.func_179121_F();
                    }
                }
            }
        }
    }
}
