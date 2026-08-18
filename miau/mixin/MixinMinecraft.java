package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.GuiOpenEvent;
import miau.event.impl.HitBlockEvent;
import miau.event.impl.KeyEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.ResizeEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.SwapItemEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.init.Initializer;
import miau.module.modules.ghost.NoClickDelay;
import miau.module.modules.misc.BalanceFix;
import miau.ui.menu.MiauMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = Minecraft.class, priority = 9999)
public abstract class MixinMinecraft {
    @Shadow
    private int field_71429_W;
    @Shadow
    public PlayerControllerMP field_71442_b;
    @Shadow
    public WorldClient field_71441_e;
    @Shadow
    public EntityPlayerSP field_71439_g;
    @Shadow
    public GuiScreen field_71462_r;
    private GuiScreen modifiedGui = null;

    @Inject(method = "startGame", at = @At("HEAD"))
    private void startGame(CallbackInfo callbackInfo) {
        new Initializer();
    }

    @Inject(method = "startGame", at = @At("RETURN"))
    private void postStartGame(CallbackInfo callbackInfo) {
        new Miau();
    }

    @Inject(method = "runTick", at = @At("HEAD"), cancellable = true)
    private void runTick(CallbackInfo callbackInfo) {
        if (this.field_71441_e != null && this.field_71439_g != null) {
            TickEvent event = new TickEvent(EventType.PRE);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void postRunTick(CallbackInfo callbackInfo) {
        if (this.field_71441_e != null && this.field_71439_g != null) {
            EventManager.call(new TickEvent(EventType.POST));
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient worldClient, String string, CallbackInfo callbackInfo) {
        EventManager.call(new LoadWorldEvent());
    }

    @Inject(method = "updateFramebufferSize", at = @At("RETURN"))
    private void updateFramebufferSize(CallbackInfo callbackInfo) {
        EventManager.call(new ResizeEvent());
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void clickMouse(CallbackInfo callbackInfo) {
        if (Miau.moduleManager != null) {
            NoClickDelay noClickDelay = (NoClickDelay)Miau.moduleManager.modules.get(NoClickDelay.class);
            if (noClickDelay != null && noClickDelay.isEnabled()) {
                this.field_71429_W = 0;
            }
        }

        LeftClickMouseEvent event = new LeftClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void rightClickMouse(CallbackInfo callbackInfo) {
        RightClickMouseEvent event = new RightClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    private void sendClickBlockToController(CallbackInfo callbackInfo) {
        HitBlockEvent event = new HitBlockEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
            this.field_71442_b.func_78767_c();
        }
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V")
    )
    private void setKeyBindState(int integer, boolean boolean2) {
        KeyBinding.func_74510_a(integer, boolean2);
        if (boolean2 && this.field_71462_r == null) {
            EventManager.call(new KeyEvent(integer));
        }
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V")
    )
    private void changeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        SwapItemEvent event = new SwapItemEvent(-1, slot);
        EventManager.call(event);
        if (!event.isCancelled()) {
            inventoryPlayer.func_70453_c(slot);
        }
    }

    @Inject(method = "displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("HEAD"), cancellable = true)
    private void onDisplayGuiScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (Miau.moduleManager != null) {
            GuiScreen gui = guiScreenIn;
            if (gui instanceof GuiMainMenu || gui == null && this.field_71441_e == null) {
                gui = new MiauMainMenu();
            }

            if (gui != null) {
                GuiOpenEvent event = new GuiOpenEvent(gui);
                EventManager.call(event);
                if (event.isCancelled()) {
                    ci.cancel();
                    return;
                }

                if (event.getGui() != gui) {
                    this.modifiedGui = event.getGui();
                } else if (gui != guiScreenIn) {
                    this.modifiedGui = gui;
                }
            }
        }
    }

    @ModifyVariable(method = "displayGuiScreen", at = @At("HEAD"), argsOnly = true)
    private GuiScreen modifyGuiScreen(GuiScreen guiScreenIn) {
        if (this.modifiedGui != null) {
            GuiScreen ret = this.modifiedGui;
            this.modifiedGui = null;
            return ret;
        } else {
            return guiScreenIn;
        }
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.AFTER)
    )
    private void afterUpdateTimer(CallbackInfo ci) {
        if (Miau.moduleManager != null) {
            BalanceFix balanceFix = (BalanceFix)Miau.moduleManager.modules.get(BalanceFix.class);
            if (balanceFix != null && balanceFix.isEnabled()) {
                ((IAccessorMinecraft)this).getTimer().field_74280_b = Math.min(
                    ((IAccessorMinecraft)this).getTimer().field_74280_b, 10
                );
            }
        }
    }
}
