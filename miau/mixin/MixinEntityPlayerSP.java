package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.modules.movement.NoSlow;
import miau.module.modules.player.AntiDebuff;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityPlayerSP.class, priority = 9999)
public abstract class MixinEntityPlayerSP extends MixinEntityPlayer {
    @Unique
    private float overrideYaw = Float.NaN;
    @Unique
    private float overridePitch = Float.NaN;
    @Unique
    private float pendingYaw;
    @Unique
    private float pendingPitch;
    @Shadow
    private float field_175164_bL;
    @Shadow
    private float field_175165_bM;
    @Shadow
    public float field_71154_f;
    @Shadow
    public float field_71163_h;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onUpdate(CallbackInfo callbackInfo) {
        if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
            UpdateEvent event = new UpdateEvent(
                EventType.PRE, this.field_175164_bL, this.field_175165_bM, this.field_70177_z, this.field_70125_A
            );
            EventManager.call(event);
            RotationState.applyState(
                event.isRotated() && !this.func_70115_ae(),
                event.getNewYaw(),
                event.getNewPitch(),
                event.getPreYaw(),
                event.isRotating()
            );
            if (event.isRotated()) {
                this.pendingYaw = this.field_70177_z;
                this.pendingPitch = this.field_70125_A;
                this.overrideYaw = event.getNewYaw();
                this.overridePitch = event.getNewPitch();
            } else {
                this.pendingYaw = Float.NaN;
                this.pendingPitch = Float.NaN;
                this.overrideYaw = Float.NaN;
                this.overridePitch = Float.NaN;
            }
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void postUpdate(CallbackInfo callbackInfo) {
        if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
            if (!Float.isNaN(this.pendingYaw) && !Float.isNaN(this.pendingPitch)) {
                this.field_175164_bL = this.field_70177_z;
                this.field_175165_bM = this.field_70125_A;
                this.field_70177_z = this.field_70177_z + MathHelper.func_76142_g(this.pendingYaw - this.field_70177_z);
                this.field_70125_A = this.pendingPitch;
                this.field_70126_B = this.field_70177_z;
                this.field_70127_C = this.field_70125_A;
                this.field_71163_h = this.field_70177_z - (this.field_71154_f - this.field_71163_h) * 2.0F;
                this.field_71154_f = this.field_70177_z;
            }

            EventManager.call(
                new UpdateEvent(
                    EventType.POST, this.field_175164_bL, this.field_175165_bM, this.field_70177_z, this.field_70125_A
                )
            );
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRiding()Z")
    )
    private boolean onRidding(EntityPlayerSP entityPlayerSP) {
        if (!Float.isNaN(this.overrideYaw) && !Float.isNaN(this.overridePitch)) {
            this.field_70177_z = this.overrideYaw;
            this.field_70125_A = this.overridePitch;
        }

        return entityPlayerSP.func_70115_ae();
    }

    @Inject(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V")
    )
    private void onMotionUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new PlayerUpdateEvent());
    }

    @Inject(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V")
    )
    private void onLivingUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new LivingUpdateEvent());
    }

    @Inject(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",
            shift = At.Shift.AFTER
        )
    )
    private void updateMove(CallbackInfo callbackInfo) {
        EventManager.call(new MoveInputEvent());
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z")
    )
    private boolean isUsing(EntityPlayerSP entityPlayerSP) {
        NoSlow noSlow = (NoSlow)Miau.moduleManager.modules.get(NoSlow.class);
        return noSlow != null && noSlow.isEnabled() && noSlow.isAnyActive()
            ? !noSlow.shouldCancelSlowdown()
            : entityPlayerSP.func_71039_bw();
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
        )
    )
    private boolean checkPotion(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.field_76431_k && Miau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff)Miau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }

        return ((IAccessorEntityLivingBase)entityPlayerSP).getActivePotionsMap().containsKey(potion.field_76415_H);
    }
}
