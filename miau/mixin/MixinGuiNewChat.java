package miau.mixin;

import java.util.List;
import miau.Miau;
import miau.module.modules.render.HUD;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiNewChat.class, priority = 9999)
public abstract class MixinGuiNewChat {
    private static final int OPENMIAU_DUPLICATE_CHAT_ID = 873420;
    private String openMiauLastMessage = null;
    private int openMiauDuplicateCount = 1;
    private boolean openMiauReplacingDuplicate = false;
    @Shadow
    @Final
    private Minecraft field_146247_f;
    @Shadow
    @Final
    private List<ChatLine> field_146253_i;
    @Shadow
    private int field_146250_j;
    @Shadow
    private boolean field_146251_k;
    private HUD openMiauCachedHud;

    @Shadow
    public abstract int func_146232_i();

    @Shadow
    public abstract boolean func_146241_e();

    @Shadow
    public abstract float func_146244_h();

    @Shadow
    public abstract int func_146228_f();

    @Shadow
    public abstract void func_146242_c(int var1);

    @Shadow
    public abstract void func_146234_a(IChatComponent var1, int var2);

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), cancellable = true)
    private void openMiau$compactDuplicateChat(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        if (!this.openMiauReplacingDuplicate && chatComponent != null && chatLineId == 0) {
            String message = chatComponent.func_150260_c();
            if (message != null && !message.isEmpty()) {
                if (message.equals(this.openMiauLastMessage)) {
                    this.openMiauDuplicateCount++;
                    IChatComponent compacted = chatComponent.func_150259_f();
                    compacted.func_150258_a(" §7[x" + this.openMiauDuplicateCount + "]");
                    ci.cancel();
                    this.openMiauReplacingDuplicate = true;
                    this.func_146242_c(873420);
                    this.func_146234_a(compacted, 873420);
                    this.openMiauReplacingDuplicate = false;
                } else {
                    if (this.openMiauDuplicateCount > 1) {
                        this.func_146242_c(873420);
                    }

                    this.openMiauLastMessage = message;
                    this.openMiauDuplicateCount = 1;
                }
            } else {
                this.openMiauLastMessage = message;
                this.openMiauDuplicateCount = 1;
            }
        }
    }

    @Redirect(
        method = "drawChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"
        )
    )
    private int openMiau$renderCustomChatPrefix(FontRenderer fontRenderer, String text, float x, float y, int color) {
        if (text != null && text.contains("Miau Minus » ")) {
            if (this.openMiauCachedHud == null) {
                this.openMiauCachedHud = (HUD)Miau.moduleManager.getModule(HUD.class);
            }

            if (this.openMiauCachedHud != null && this.openMiauCachedHud.isEnabled()) {
                int prefixIndex = text.indexOf("Miau Minus » ");
                String before = text.substring(0, prefixIndex);
                float currentX = x;
                if (!before.isEmpty()) {
                    currentX = fontRenderer.func_175063_a(before, currentX, y, color);
                }

                long time = System.currentTimeMillis();
                int alpha = color >> 24 & 0xFF;

                for (int i = 0; i < "Miau Minus » ".length(); i++) {
                    char c = "Miau Minus » ".charAt(i);
                    int charColor = this.openMiauCachedHud.getColor(time, i * 15).getRGB();
                    int finalColor = alpha << 24 | charColor & 16777215;
                    currentX = fontRenderer.func_175063_a(String.valueOf(c), currentX, y, finalColor);
                }

                String after = text.substring(prefixIndex + "Miau Minus » ".length());
                if (!after.isEmpty()) {
                    currentX = fontRenderer.func_175063_a(after, currentX, y, color);
                }

                return (int)currentX;
            }
        }

        return fontRenderer.func_175063_a(text, x, y, color);
    }

    @Redirect(
        method = "drawChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V",
            ordinal = 0
        )
    )
    private void redirectChatTranslate(float x, float y, float z) {
        float animY = 17.0F;

        try {
            if (ChatUtil.openingAnimation != null) {
                animY = 17.0F - 16.0F * ChatUtil.openingAnimation.getOutput().floatValue();
            }
        } catch (Exception e) {
            animY = 17.0F;
        }

        GlStateManager.func_179109_b(x, animY, z);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void onDrawChatPre(int updateCounter, CallbackInfo ci) {
        try {
            if (this.field_146247_f.field_71441_e == null || this.field_146247_f.field_71439_g == null) {
                return;
            }

            if (this.openMiauCachedHud == null) {
                this.openMiauCachedHud = (HUD)Miau.moduleManager.getModule(HUD.class);
            }
        } catch (Exception var4) {
        }
    }
}
