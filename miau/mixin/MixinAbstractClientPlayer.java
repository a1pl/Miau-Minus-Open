package miau.mixin;

import miau.Miau;
import miau.module.modules.movement.Sprint;
import miau.module.modules.render.Capes;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = AbstractClientPlayer.class, priority = 9999)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {
    @Redirect(
        method = "getFovModifier",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D"
        )
    )
    private double getFovModifier(IAttributeInstance iAttributeInstance) {
        double attributeValue = iAttributeInstance.func_111126_e();
        if ((Entity)this instanceof EntityPlayerSP && Miau.moduleManager != null) {
            Sprint sprint = (Sprint)Miau.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldApplyFovFix(iAttributeInstance)
                ? attributeValue * 1.300000011920929
                : attributeValue;
        } else {
            return attributeValue;
        }
    }

    @Inject(method = "getLocationCape", at = @At("RETURN"), cancellable = true)
    public void onGetLocationCape(CallbackInfoReturnable<ResourceLocation> cir) {
        if (Miau.moduleManager != null) {
            Capes capes = (Capes)Miau.moduleManager.modules.get(Capes.class);
            if (capes != null && capes.isEnabled()) {
                boolean isSelf = this instanceof EntityPlayerSP;
                if (capes.allPlayer.getValue() || isSelf) {
                    cir.setReturnValue(capes.getCape());
                }
            }
        }
    }
}
