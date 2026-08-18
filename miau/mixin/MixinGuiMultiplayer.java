package miau.mixin;

import de.florianmichael.viamcp.gui.AsyncVersionSlider;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer extends GuiScreen {
    @Inject(method = "createButtons", at = @At("TAIL"))
    private void addViaVersionSlider(CallbackInfo ci) {
        this.field_146292_n.add(new AsyncVersionSlider(-1, this.field_146294_l - 110, 30, 100, 20));
    }
}
