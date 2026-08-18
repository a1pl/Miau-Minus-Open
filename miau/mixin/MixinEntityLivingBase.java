package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.JumpEvent;
import miau.event.impl.StrafeEvent;
import miau.management.RotationState;
import miau.module.modules.movement.Jesus;
import miau.module.modules.render.Animations;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityLivingBase.class, priority = 9999)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @ModifyVariable(method = "jump", at = @At("STORE"), ordinal = 0)
    private float jump(float float1) {
        if ((Entity)this instanceof EntityPlayerSP) {
            float yawDegrees = float1 * (float) (180.0 / Math.PI);
            JumpEvent event = new JumpEvent(RotationState.isActived() ? RotationState.getSmoothedYaw() : yawDegrees);
            EventManager.call(event);
            if (event.getJumpoff() != 0.0F) {
                this.field_70181_x = event.getJumpoff();
            }

            return event.getYaw() * (float) (Math.PI / 180.0);
        } else {
            return float1;
        }
    }

    @Redirect(
        method = "moveEntityWithHeading",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V")
    )
    private void moveEntityWithHeading(EntityLivingBase entityLivingBase, float float2, float float3, float float4) {
        if ((Entity)this instanceof EntityPlayerSP) {
            float originalYaw = this.field_70177_z;
            float movementYaw = RotationState.isActived() ? RotationState.getSmoothedYaw() : originalYaw;
            StrafeEvent event = new StrafeEvent(float2, float3, float4, movementYaw);
            EventManager.call(event);
            if (event.isCancelled()) {
                this.field_70177_z = movementYaw;
                return;
            }

            float2 = event.getStrafe();
            float3 = event.getForward();
            float4 = event.getFriction();
            this.field_70177_z = event.getYaw();
            entityLivingBase.func_70060_a(float2, float3, float4);
            this.field_70177_z = originalYaw;
        } else {
            entityLivingBase.func_70060_a(float2, float3, float4);
        }
    }

    @ModifyVariable(method = "moveEntityWithHeading", name = "f3", at = @At("STORE"))
    private float moveEntityWithHeading(float float1) {
        if ((EntityLivingBase)this instanceof EntityPlayerSP
            && float1 == EnchantmentHelper.func_180318_b((EntityLivingBase)this)) {
            if (Miau.moduleManager == null) {
                return float1;
            }

            Jesus jesus = (Jesus)Miau.moduleManager.modules.get(Jesus.class);
            if (jesus != null && jesus.isEnabled() && (!jesus.groundOnly.getValue() || this.field_70122_E)) {
                return Math.max(float1, jesus.speed.getValue());
            }
        }

        return float1;
    }

    @Shadow
    public abstract boolean func_70644_a(Potion var1);

    @Shadow
    public abstract PotionEffect func_70660_b(Potion var1);

    @Inject(method = "getArmSwingAnimationEnd", at = @At("RETURN"), cancellable = true)
    private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        cir.setReturnValue(Animations.getSwingAnimationEnd((EntityLivingBase)this, original));
    }
}
