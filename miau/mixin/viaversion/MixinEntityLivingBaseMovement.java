package miau.mixin.viaversion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseMovement {
    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 0.005))
    private double viaversion_movementThreshold(double constant) {
        return ViaLoadingBase.getInstance().getTargetVersion().getVersion() >= 107 ? 0.003 : 0.005;
    }
}
