package miau.mixin;

import miau.interfaces.IMixinItemRenderer;
import miau.module.modules.render.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements IMixinItemRenderer {
    @Shadow
    private float field_78451_d;
    @Shadow
    private float field_78454_c;
    @Shadow
    @Final
    private Minecraft field_78455_a;
    @Shadow
    private ItemStack field_78453_b;
    public boolean cancelUpdate = false;
    public boolean cancelReset = false;
    private boolean renderItemInUse;

    @Shadow
    protected abstract void func_178101_a(float var1, float var2);

    @Shadow
    protected abstract void func_178109_a(AbstractClientPlayer var1);

    @Shadow
    protected abstract void func_178110_a(EntityPlayerSP var1, float var2);

    @Shadow
    protected abstract void func_178097_a(AbstractClientPlayer var1, float var2, float var3, float var4);

    @Shadow
    protected abstract void func_178096_b(float var1, float var2);

    @Shadow
    protected abstract void func_178104_a(AbstractClientPlayer var1, float var2);

    @Shadow
    protected abstract void func_178098_a(float var1, AbstractClientPlayer var2);

    @Shadow
    protected abstract void func_178105_d(float var1);

    @Shadow
    public abstract void func_178099_a(EntityLivingBase var1, ItemStack var2, TransformType var3);

    @Shadow
    protected abstract void func_178095_a(AbstractClientPlayer var1, float var2, float var3);

    @Override
    public void setCancelUpdate(boolean cancel) {
        this.cancelUpdate = cancel;
    }

    @Override
    public void setCancelReset(boolean reset) {
        this.cancelReset = reset;
    }

    @Override
    public boolean isRenderItemInUse() {
        return this.renderItemInUse;
    }

    @Override
    public void setRenderItemInUse(boolean renderItemInUse) {
        this.renderItemInUse = renderItemInUse;
    }

    @Redirect(
        method = "updateEquippedItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
        )
    )
    private ItemStack redirectGetCurrentItem(InventoryPlayer inventoryPlayer) {
        return inventoryPlayer.field_70462_a[inventoryPlayer.field_70461_c];
    }

    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    private void onUpdateEquippedItem(CallbackInfo ci) {
        if (this.cancelUpdate) {
            this.cancelUpdate = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Inject(method = "resetEquippedProgress", at = @At("HEAD"), cancellable = true)
    public void injectResetEquippedProgress(CallbackInfo ci) {
        if (this.cancelReset) {
            this.cancelReset = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Inject(method = "resetEquippedProgress2", at = @At("HEAD"), cancellable = true)
    public void injectResetEquippedProgress2(CallbackInfo ci) {
        if (this.cancelReset) {
            this.cancelReset = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Overwrite
    public void func_78440_a(float partialTicks) {
        float equipProgress = 1.0F - (this.field_78451_d + (this.field_78454_c - this.field_78451_d) * partialTicks);
        EntityPlayerSP player = this.field_78455_a.field_71439_g;
        float swingProgress = player.func_70678_g(partialTicks);
        float pitch = player.field_70127_C + (player.field_70125_A - player.field_70127_C) * partialTicks;
        float yaw = player.field_70126_B + (player.field_70177_z - player.field_70126_B) * partialTicks;
        this.func_178101_a(pitch, yaw);
        this.func_178109_a(player);
        this.func_178110_a(player, partialTicks);
        GlStateManager.func_179091_B();
        GlStateManager.func_179094_E();
        if (this.field_78453_b != null) {
            if (this.field_78453_b.func_77973_b() instanceof ItemMap) {
                this.func_178097_a(player, pitch, equipProgress, swingProgress);
            } else if (player.func_71052_bv() > 0) {
                EnumAction action = this.field_78453_b.func_77975_n();
                if (action == EnumAction.NONE) {
                    this.func_178096_b(equipProgress, 0.0F);
                } else if (action == EnumAction.EAT || action == EnumAction.DRINK) {
                    this.func_178104_a(player, partialTicks);
                    this.func_178096_b(equipProgress, swingProgress);
                } else if (action == EnumAction.BLOCK) {
                    if (!Animations.apply(swingProgress, equipProgress, player)) {
                        this.func_178096_b(equipProgress, swingProgress);
                        GlStateManager.func_179109_b(-0.5F, 0.2F, 0.0F);
                        GlStateManager.func_179114_b(30.0F, 0.0F, 1.0F, 0.0F);
                        GlStateManager.func_179114_b(-80.0F, 1.0F, 0.0F, 0.0F);
                        GlStateManager.func_179114_b(60.0F, 0.0F, 1.0F, 0.0F);
                    }
                } else if (action == EnumAction.BOW) {
                    this.func_178096_b(equipProgress, swingProgress);
                    this.func_178098_a(partialTicks, player);
                }
            } else {
                this.func_178105_d(swingProgress);
                this.func_178096_b(equipProgress, swingProgress);
            }

            this.func_178099_a(player, this.field_78453_b, TransformType.FIRST_PERSON);
        } else if (!player.func_82150_aj()) {
            this.func_178095_a(player, equipProgress, swingProgress);
        }

        GlStateManager.func_179121_F();
        GlStateManager.func_179101_C();
        RenderHelper.func_74518_a();
    }
}
