package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.Render2DEvent;
import miau.module.modules.misc.NickHider;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = GuiIngameForge.class, priority = 9999)
public abstract class MixinGuiIngameForge {
    @Inject(
        method = "renderGameOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V",
            shift = At.Shift.AFTER,
            remap = false
        )
    )
    private void renderGameOverlay(float float1, CallbackInfo callbackInfo) {
        EventManager.call(new Render2DEvent(float1));
    }

    @Redirect(
        method = "renderExperience",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;experience:F")
    )
    private float renderExperience(EntityPlayerSP entityPlayerSP) {
        if (Miau.moduleManager == null) {
            return entityPlayerSP.field_71106_cc;
        }

        NickHider event = (NickHider)Miau.moduleManager.modules.get(NickHider.class);
        return event != null && event.isEnabled() && event.level.getValue() ? 0.0F : entityPlayerSP.field_71106_cc;
    }

    @Redirect(
        method = "renderExperience",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;experienceLevel:I")
    )
    private int renderExperienceLevel(EntityPlayerSP entityPlayerSP) {
        if (Miau.moduleManager == null) {
            return entityPlayerSP.field_71068_ca;
        }

        NickHider event = (NickHider)Miau.moduleManager.modules.get(NickHider.class);
        return event != null && event.isEnabled() && event.level.getValue() ? 0 : entityPlayerSP.field_71068_ca;
    }
}
