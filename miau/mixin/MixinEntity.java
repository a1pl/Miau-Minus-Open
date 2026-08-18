package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.KnockbackEvent;
import miau.event.impl.SafeWalkEvent;
import miau.event.impl.WebSlowDownEvent;
import miau.module.modules.render.Chams;
import miau.module.modules.render.ESP;
import miau.module.modules.render.FreeLook;
import miau.module.modules.render.NameTags;
import miau.util.misc.ITruePosition;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = Entity.class, priority = 9999)
public abstract class MixinEntity implements ITruePosition {
    @Shadow
    public World field_70170_p;
    @Shadow
    public double field_70165_t;
    @Shadow
    public double field_70163_u;
    @Shadow
    public double field_70161_v;
    @Shadow
    public double field_70159_w;
    @Shadow
    public double field_70181_x;
    @Shadow
    public double field_70179_y;
    @Shadow
    public float field_70177_z;
    @Shadow
    public float field_70125_A;
    @Shadow
    public float field_70126_B;
    @Shadow
    public float field_70127_C;
    @Shadow
    public boolean field_70122_E;
    @Shadow
    public int field_70118_ct;
    @Shadow
    public int field_70117_cu;
    @Shadow
    public int field_70116_cv;
    @Unique
    private double trueX;
    @Unique
    private double trueY;
    @Unique
    private double trueZ;
    @Unique
    private boolean truePos;
    @Unique
    private WebSlowDownEvent pendingWebSlowDownEvent;

    @Shadow
    public boolean func_70115_ae() {
        return false;
    }

    @Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
    private void setVelocity(double double1, double double2, double double3, CallbackInfo callbackInfo) {
        if ((Entity)this instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(double1, double2, double3);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                this.field_70159_w = event.getX();
                this.field_70181_x = event.getY();
                this.field_70179_y = event.getZ();
            }
        }
    }

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void setAngles(float yaw, float pitch, CallbackInfo callbackInfo) {
        if ((Entity)this instanceof EntityPlayerSP) {
            if (Miau.moduleManager != null) {
                FreeLook freeLook = (FreeLook)Miau.moduleManager.modules.get(FreeLook.class);
                if (freeLook != null && freeLook.isFreeLooking()) {
                    freeLook.updateCamera(yaw, pitch);
                    callbackInfo.cancel();
                    return;
                }
            }

            if (Miau.rotationManager != null && Miau.rotationManager.isRotated()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initTruePosition(World world, CallbackInfo callbackInfo) {
        this.updateTruePositionFromCurrent();
    }

    @Inject(method = "setPosition", at = @At("RETURN"))
    private void setPositionTrue(double x, double y, double z, CallbackInfo callbackInfo) {
        this.updateTruePositionFromCurrent();
    }

    @Inject(method = "setPositionAndRotation", at = @At("RETURN"))
    private void setPositionAndRotationTrue(
        double x, double y, double z, float yaw, float pitch, CallbackInfo callbackInfo
    ) {
        this.updateTruePositionFromCurrent();
    }

    @Inject(method = "setPositionAndRotation2", at = @At("HEAD"))
    private void setPositionAndRotation2True(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int increments,
        boolean teleport,
        CallbackInfo callbackInfo
    ) {
        this.trueX = x;
        this.trueY = y;
        this.trueZ = z;
        this.truePos = true;
    }

    @Unique
    private void updateTruePositionFromCurrent() {
        this.trueX = this.field_70165_t;
        this.trueY = this.field_70163_u;
        this.trueZ = this.field_70161_v;
        this.truePos = true;
    }

    @Override
    public double getTrueX() {
        return this.trueX;
    }

    @Override
    public double getTrueY() {
        return this.trueY;
    }

    @Override
    public double getTrueZ() {
        return this.trueZ;
    }

    @Override
    public void setTrueX(double trueX) {
        this.trueX = trueX;
    }

    @Override
    public void setTrueY(double trueY) {
        this.trueY = trueY;
    }

    @Override
    public void setTrueZ(double trueZ) {
        this.trueZ = trueZ;
    }

    @Override
    public boolean isTruePos() {
        return this.truePos;
    }

    @Override
    public void setTruePos(boolean truePos) {
        this.truePos = truePos;
    }

    @Inject(method = "isInRangeToRenderDist", at = @At("HEAD"), cancellable = true)
    private void isInRangeToRenderDist(double distance, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Entity entity = (Entity)this;
        if (Miau.moduleManager != null && entity instanceof EntityLivingBase) {
            ESP esp = (ESP)Miau.moduleManager.modules.get(ESP.class);
            NameTags nameTags = (NameTags)Miau.moduleManager.modules.get(NameTags.class);
            Chams chams = (Chams)Miau.moduleManager.modules.get(Chams.class);
            EntityLivingBase living = (EntityLivingBase)entity;
            boolean forceRender = false;
            if (esp != null && esp.isEnabled() && entity instanceof EntityPlayer) {
                forceRender = true;
            }

            if (nameTags != null && nameTags.isEnabled() && nameTags.shouldRenderTags(living)) {
                forceRender = true;
            }

            if (chams != null && chams.isEnabled()) {
                forceRender = true;
            }

            if (forceRender) {
                entity.field_70158_ak = true;
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }

    @ModifyVariable(method = "moveEntity", ordinal = 0, at = @At("STORE"), name = "flag")
    private boolean moveEntity(boolean boolean1) {
        if ((Entity)this instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(boolean1);
            EventManager.call(event);
            return event.isSafeWalk();
        } else {
            return boolean1;
        }
    }

    @Redirect(
        method = "moveEntity",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;isInWeb:Z", ordinal = 0)
    )
    private boolean redirectIsInWeb(Entity entity) {
        if (entity instanceof EntityPlayerSP) {
            EntityPlayerSP player = (EntityPlayerSP)entity;
            IAccessorEntity accessor = (IAccessorEntity)player;
            if (accessor.getIsInWeb()) {
                WebSlowDownEvent event = new WebSlowDownEvent(0.0, 0.0, 0.0);
                EventManager.call(event);
                if (event.isCancelled()) {
                    accessor.setIsInWeb(false);
                    return false;
                }

                this.pendingWebSlowDownEvent = event;
            }
        }

        return ((IAccessorEntity)entity).getIsInWeb();
    }

    @Redirect(
        method = "moveEntity",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;isInWeb:Z", ordinal = 1)
    )
    private void redirectSetIsInWeb(Entity entity, boolean value) {
        ((IAccessorEntity)entity).setIsInWeb(value);
    }

    @Inject(
        method = "moveEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0)
    )
    private void afterWebSlowDown(double x, double y, double z, CallbackInfo ci) {
        if ((Entity)this instanceof EntityPlayerSP && this.pendingWebSlowDownEvent != null) {
            this.field_70159_w = this.pendingWebSlowDownEvent.getMotionX();
            this.field_70181_x = this.pendingWebSlowDownEvent.getMotionY();
            this.field_70179_y = this.pendingWebSlowDownEvent.getMotionZ();
            this.pendingWebSlowDownEvent = null;
        }
    }
}
