package miau.mixin;

import miau.util.client.ChatUtil;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame_ChatOffset {
    @Unique
    private static float openMiau$cachedAnim = 0.0F;

    @Unique
    private void openMiau$pushAnimMatrix() {
        if (openMiau$cachedAnim > 0.001F) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b(0.0F, -15.0F * openMiau$cachedAnim, 0.0F);
        }
    }

    @Unique
    private void openMiau$popAnimMatrix() {
        if (openMiau$cachedAnim > 0.001F) {
            GlStateManager.func_179121_F();
        }
    }

    @Inject(method = "renderPlayerStats", at = @At("HEAD"))
    private void onRenderPlayerStatsPre(ScaledResolution scaledRes, CallbackInfo ci) {
        openMiau$cachedAnim = ChatUtil.openingAnimation.getOutput().floatValue();
        this.openMiau$pushAnimMatrix();
    }

    @Inject(method = "renderPlayerStats", at = @At("RETURN"))
    private void onRenderPlayerStatsPost(ScaledResolution scaledRes, CallbackInfo ci) {
        this.openMiau$popAnimMatrix();
    }

    @Inject(method = "renderExpBar", at = @At("HEAD"))
    private void onRenderExpBarPre(ScaledResolution scaledRes, int x, CallbackInfo ci) {
        this.openMiau$pushAnimMatrix();
    }

    @Inject(method = "renderExpBar", at = @At("RETURN"))
    private void onRenderExpBarPost(ScaledResolution scaledRes, int x, CallbackInfo ci) {
        this.openMiau$popAnimMatrix();
    }

    @Inject(method = "renderHorseJumpBar", at = @At("HEAD"))
    private void onRenderHorseJumpBarPre(ScaledResolution scaledRes, int x, CallbackInfo ci) {
        this.openMiau$pushAnimMatrix();
    }

    @Inject(method = "renderHorseJumpBar", at = @At("RETURN"))
    private void onRenderHorseJumpBarPost(ScaledResolution scaledRes, int x, CallbackInfo ci) {
        this.openMiau$popAnimMatrix();
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void onRenderTooltipPre(ScaledResolution scaledRes, float partialTicks, CallbackInfo ci) {
        this.openMiau$pushAnimMatrix();
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void onRenderTooltipPost(ScaledResolution scaledRes, float partialTicks, CallbackInfo ci) {
        this.openMiau$popAnimMatrix();
    }

    @Inject(method = "renderSelectedItem", at = @At("HEAD"))
    private void onRenderSelectedItemPre(ScaledResolution scaledRes, CallbackInfo ci) {
        this.openMiau$pushAnimMatrix();
    }

    @Inject(method = "renderSelectedItem", at = @At("RETURN"))
    private void onRenderSelectedItemPost(ScaledResolution scaledRes, CallbackInfo ci) {
        this.openMiau$popAnimMatrix();
    }
}
