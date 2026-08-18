package miau.mixin;

import java.util.List;
import miau.Miau;
import miau.data.Box;
import miau.event.EventManager;
import miau.event.impl.PickEvent;
import miau.event.impl.PostRaytraceEvent;
import miau.event.impl.RaytraceEvent;
import miau.event.impl.Render3DEvent;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.Piercing;
import miau.module.modules.player.AntiDebuff;
import miau.module.modules.player.AutoBlockIn;
import miau.module.modules.player.GhostHand;
import miau.module.modules.render.FreeLook;
import miau.module.modules.render.NoHurtCam;
import miau.module.modules.render.ViewClip;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityRenderer.class, priority = 9999)
public abstract class MixinEntityRenderer {
    @Unique
    private Box<Integer> slot = null;
    @Unique
    private Box<ItemStack> using = null;
    @Unique
    private Box<Integer> useCount = null;
    @Shadow
    private Minecraft field_78531_r;
    @Shadow
    private float field_78490_B;
    @Shadow
    private float field_78491_C;
    @Shadow
    private ShaderGroup field_147707_d;
    @Unique
    private boolean miau$freeLookRestoreRotation;
    @Unique
    private float miau$freeLookYaw;
    @Unique
    private float miau$freeLookPitch;
    @Unique
    private float miau$freeLookPrevYaw;
    @Unique
    private float miau$freeLookPrevPitch;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void updateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.field_78531_r.field_71439_g != null) {
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            if (killAura != null && killAura.isEnabled() && killAura.isBlocking()) {
                this.using = new Box<>(((IAccessorEntityPlayer)this.field_78531_r.field_71439_g).getItemInUse());
                ((IAccessorEntityPlayer)this.field_78531_r.field_71439_g)
                    .setItemInUse(this.field_78531_r.field_71439_g.field_71071_by.func_70448_g());
                this.useCount = new Box<>(((IAccessorEntityPlayer)this.field_78531_r.field_71439_g).getItemInUseCount());
                ((IAccessorEntityPlayer)this.field_78531_r.field_71439_g).setItemInUseCount(69000);
            }
        }
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void postUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = this.slot.value;
            this.slot = null;
        }

        if (this.using != null) {
            ((IAccessorEntityPlayer)this.field_78531_r.field_71439_g).setItemInUse(this.using.value);
            this.using = null;
        }

        if (this.useCount != null) {
            ((IAccessorEntityPlayer)this.field_78531_r.field_71439_g).setItemInUseCount(this.useCount.value);
            this.useCount = null;
        }
    }

    @Inject(method = "updateRenderer", at = @At("HEAD"))
    private void updateRenderer(CallbackInfo callbackInfo) {
        AutoBlockIn autoBlockIn = (AutoBlockIn)Miau.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn != null && autoBlockIn.isEnabled() && autoBlockIn.itemSpoof.getValue()) {
            int slot = autoBlockIn.getSlot();
            if (slot >= 0) {
                this.slot = new Box<>(this.field_78531_r.field_71439_g.field_71071_by.field_70461_c);
                this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = slot;
            }
        }
    }

    @Inject(method = "updateRenderer", at = @At("RETURN"))
    private void postUpdateRenderer(CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = this.slot.value;
            this.slot = null;
        }
    }

    @Inject(
        method = "renderWorldPass",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z",
            shift = At.Shift.BEFORE
        )
    )
    private void renderWorldPass(int integer, float float2, long long3, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(float2));
    }

    @ModifyConstant(method = "hurtCameraEffect", constant = @Constant(floatValue = 14.0F, ordinal = 0))
    private float hurtCameraEffect(float float1) {
        if (Miau.moduleManager == null) {
            return float1;
        }

        NoHurtCam noHurtCam = (NoHurtCam)Miau.moduleManager.modules.get(NoHurtCam.class);
        return noHurtCam.isEnabled() ? float1 * noHurtCam.multiplier.getValue().intValue() / 100.0F : float1;
    }

    @ModifyConstant(method = "getMouseOver", constant = @Constant(doubleValue = 3.0, ordinal = 1))
    private double getMouseOver(double range) {
        PickEvent event = new PickEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @ModifyVariable(method = "getMouseOver", at = @At("STORE"), name = "d0")
    private double storeMouseOver(double range) {
        RaytraceEvent event = new RaytraceEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @Inject(
        method = "getMouseOver",
        at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void a(
        float float1,
        CallbackInfo callbackInfo,
        Entity entity,
        double double4,
        double double5,
        Vec3 vec36,
        boolean boolean7,
        int integer8,
        Vec3 vec39,
        Vec3 vec310,
        Vec3 vec311,
        float float12,
        List<Entity> list,
        double double14,
        int integer15
    ) {
        if (Miau.moduleManager != null) {
            GhostHand ghostHand = (GhostHand)Miau.moduleManager.modules.get(GhostHand.class);
            if (ghostHand != null && ghostHand.isEnabled()) {
                list.removeIf(ghostHand::shouldSkip);
            }
        }
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void onGetMouseOverReturn(float partialTicks, CallbackInfo ci) {
        if (Miau.moduleManager != null) {
            Piercing piercing = (Piercing)Miau.moduleManager.modules.get(Piercing.class);
            if (piercing != null && piercing.isEnabled()) {
                piercing.modifyMouseOver(partialTicks);
            }
        }

        PostRaytraceEvent event = new PostRaytraceEvent(partialTicks);
        EventManager.call(event);
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void orientFreeLook(float partialTicks, CallbackInfo callbackInfo) {
        if (Miau.moduleManager != null && this.field_78531_r.field_71439_g != null) {
            FreeLook freeLook = (FreeLook)Miau.moduleManager.modules.get(FreeLook.class);
            if (freeLook != null && freeLook.isFreeLooking()) {
                EntityPlayerSP player = this.field_78531_r.field_71439_g;
                this.miau$freeLookYaw = player.field_70177_z;
                this.miau$freeLookPitch = player.field_70125_A;
                this.miau$freeLookPrevYaw = player.field_70126_B;
                this.miau$freeLookPrevPitch = player.field_70127_C;
                this.miau$freeLookRestoreRotation = true;
                player.field_70126_B = player.field_70177_z = freeLook.getCameraYaw();
                player.field_70127_C = player.field_70125_A = MathHelper.func_76131_a(
                    freeLook.getCameraPitch(), -90.0F, 90.0F
                );
                if (this.field_78531_r.field_71474_y.field_74320_O == 0) {
                    this.field_78531_r.field_71474_y.field_74320_O = 1;
                }

                this.field_78491_C = this.field_78490_B;
            }
        }
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void restoreFreeLook(float partialTicks, CallbackInfo callbackInfo) {
        if (this.miau$freeLookRestoreRotation && this.field_78531_r.field_71439_g != null) {
            EntityPlayerSP player = this.field_78531_r.field_71439_g;
            player.field_70177_z = this.miau$freeLookYaw;
            player.field_70125_A = this.miau$freeLookPitch;
            player.field_70126_B = this.miau$freeLookPrevYaw;
            player.field_70127_C = this.miau$freeLookPrevPitch;
            this.miau$freeLookRestoreRotation = false;
        }
    }

    @Redirect(
        method = "orientCamera",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D")
    )
    private double v(Vec3 vec31, Vec3 vec32) {
        if (Miau.moduleManager == null) {
            return vec31.func_72438_d(vec32);
        }

        ViewClip viewClip = (ViewClip)Miau.moduleManager.modules.get(ViewClip.class);
        return viewClip != null && viewClip.isEnabled() ? this.field_78490_B : vec31.func_72438_d(vec32);
    }

    @Redirect(
        method = "setupFog",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
        )
    )
    private Material x(Block block) {
        if (Miau.moduleManager == null) {
            return block.func_149688_o();
        }

        ViewClip viewClip = (ViewClip)Miau.moduleManager.modules.get(ViewClip.class);
        return viewClip != null && viewClip.isEnabled() ? Material.field_151579_a : block.func_149688_o();
    }

    @Redirect(
        method = "updateFogColor",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
        )
    )
    private boolean y(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.field_76440_q && Miau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff)Miau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }

        return ((IAccessorEntityLivingBase)entityLivingBase).getActivePotionsMap().containsKey(potion.field_76415_H);
    }

    @Redirect(
        method = "setupFog",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
        )
    )
    private boolean q(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.field_76440_q && Miau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff)Miau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }

        return ((IAccessorEntityLivingBase)entityLivingBase).getActivePotionsMap().containsKey(potion.field_76415_H);
    }

    @Redirect(
        method = "setupCameraTransform",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
        )
    )
    private boolean c(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.field_76431_k && Miau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff)Miau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }

        return ((IAccessorEntityLivingBase)entityPlayerSP).getActivePotionsMap().containsKey(potion.field_76415_H);
    }
}
