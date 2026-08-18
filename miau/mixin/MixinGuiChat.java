package miau.mixin;

import miau.util.animation.Direction;
import miau.util.animation.impl.DecelerateAnimation;
import miau.util.client.ChatUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {
    @Shadow
    protected GuiTextField field_146415_a;
    @Shadow
    private String field_146410_g;
    @Shadow
    private int field_146416_h;
    @Unique
    private static float openMiau$cachedAnimOut;

    @Shadow
    public abstract void func_146404_p_();

    @Shadow
    public abstract void func_146402_a(int var1);

    @Inject(method = "initGui", at = @At("HEAD"))
    private void onInitGui(CallbackInfo ci) {
        ChatUtil.openingAnimation = new DecelerateAnimation(175, 1.0);
        ChatUtil.openingAnimation.reset();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onGuiClosedInject(CallbackInfo ci) {
        ChatUtil.openingAnimation.setDirection(Direction.BACKWARDS);
    }
}
