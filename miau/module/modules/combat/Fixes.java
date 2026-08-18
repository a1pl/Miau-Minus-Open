package miau.module.modules.combat;

import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.mixin.IAccessorEntityLivingBase;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.module.modules.misc.MouseRawInput;
import miau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class Fixes extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BooleanProperty noClickDelay = new BooleanProperty("NoClickDelay", true);
    private final BooleanProperty noRightClickDelay = new BooleanProperty("NoRightClickDelay", true);
    private final BooleanProperty noBlockHitDelay = new BooleanProperty("NoBlockHitDelay", false);
    private final BooleanProperty rawMouseInput = new BooleanProperty("RawMouseInput(EnableAgainToApply)", true);
    private final BooleanProperty booster = new BooleanProperty("Booster", false);
    private final BooleanProperty fpsBoost = new BooleanProperty("FPSBoost", true);
    private final BooleanProperty renderOptimization = new BooleanProperty("RenderOptimization", true);
    private final BooleanProperty entityOptimization = new BooleanProperty("EntityOptimization", true);
    private final BooleanProperty noJumpDelay = new BooleanProperty("NoJumpDelay", true);

    public Fixes() {
        super("Fixes", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.noClickDelay.getValue()) {
            ((IAccessorMinecraft)mc).setLeftClickCounter(0);
        }

        if (this.noBlockHitDelay.getValue()) {
            ((IAccessorPlayerControllerMP)mc.field_71442_b).setBlockHitDelay(0);
        }

        if (this.noRightClickDelay.getValue()) {
            ((IAccessorMinecraft)mc).setRightClickDelayTimer(0);
        }

        if (this.booster.getValue() && this.entityOptimization.getValue()) {
            this.optimizeEntities();
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.noJumpDelay.getValue()) {
            ((IAccessorEntityLivingBase)mc.field_71439_g).setJumpTicks(0);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.booster.getValue() && this.renderOptimization.getValue()) {
            this.optimizeRendering();
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        if (this.booster.getValue() && this.entityOptimization.getValue()) {
            this.clearEntityCache();
        }
    }

    @Override
    public void onEnabled() {
        if (this.rawMouseInput.getValue()) {
            Module raw = Miau.moduleManager.modules.get(MouseRawInput.class);
            if (raw != null && !raw.isEnabled()) {
                raw.toggle();
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.rawMouseInput.getValue()) {
            Module raw = Miau.moduleManager.modules.get(MouseRawInput.class);
            if (raw != null && raw.isEnabled()) {
                raw.toggle();
            }
        }
    }

    private void optimizeEntities() {
        World world = mc.field_71441_e;
        if (world != null) {
            Entity player = mc.field_71439_g;
            if (player != null) {
                for (Entity entity : world.field_72996_f) {
                    if (entity != player) {
                        double distance = player.func_70032_d(entity);
                        if (distance > 64.0) {
                            entity.func_82142_c(true);
                        } else if (distance > 32.0) {
                            entity.func_82142_c(false);
                        }
                    }
                }
            }
        }
    }

    private void optimizeRendering() {
        GlStateManager.func_179118_c();
        GlStateManager.func_179141_d();
        if (mc.field_71446_o != null) {
            GlStateManager.func_179144_i(0);
        }

        if (this.fpsBoost.getValue()) {
            mc.field_71460_t.func_175072_h();
            mc.field_71460_t.func_180436_i();
        }
    }

    private void clearEntityCache() {
        try {
            World world = mc.field_71441_e;
            if (world == null) {
                return;
            }

            List<Entity> entityList = world.field_72996_f;
            if (entityList.size() > 100) {
                world.field_72996_f
                    .removeIf(
                        entity -> entity.field_70128_L
                            || entity.field_70153_n == null
                                && entity.field_70154_o == null
                                && entity.field_70173_aa > 600
                    );
            }
        } catch (Exception var3) {
        }
    }
}
