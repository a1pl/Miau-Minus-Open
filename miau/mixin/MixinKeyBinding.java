package miau.mixin;

import miau.event.EventManager;
import miau.event.impl.SwapItemEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = KeyBinding.class, priority = 9999)
public abstract class MixinKeyBinding {
    @Shadow
    private String field_74515_c;

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (callbackInfoReturnable.getReturnValue()) {
            Minecraft mc = Minecraft.func_71410_x();

            for (int i = 0; i < 9; i++) {
                if (mc.field_71474_y.field_151456_ac[i].func_151464_g().equals(this.field_74515_c)) {
                    SwapItemEvent event = new SwapItemEvent(i, 0);
                    EventManager.call(event);
                    if (event.isCancelled()) {
                        callbackInfoReturnable.setReturnValue(false);
                    }
                }
            }
        }
    }
}
