package miau.mixin;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.AttackEvent;
import miau.event.impl.BlockBreakEvent;
import miau.event.impl.BlockDamageEvent;
import miau.event.impl.CancelUseEvent;
import miau.event.impl.WindowClickEvent;
import miau.util.misc.SilentAttackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = PlayerControllerMP.class, priority = 9999)
public abstract class MixinPlayerControllerMP {
    @Redirect(
        method = "syncCurrentPlayItem",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I")
    )
    private int miau$getCurrentItemRedirect(InventoryPlayer inventoryPlayer) {
        return Miau.slotComponent.getItemIndex();
    }

    @Inject(
        method = "attackEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"
        )
    )
    private void attackEntity(EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo callbackInfo) {
        if (!SilentAttackManager.isSilent()) {
            AttackEvent event = new AttackEvent(targetEntity);
            EventManager.call(event);
        }
    }

    @Inject(method = "windowClick", at = @At("HEAD"), cancellable = true)
    private void windowClick(
        int windowId,
        int slotId,
        int mouseButtonClicked,
        int mode,
        EntityPlayer entityPlayer,
        CallbackInfoReturnable<ItemStack> callbackInfoReturnable
    ) {
        WindowClickEvent event = new WindowClickEvent(windowId, slotId, mouseButtonClicked, mode);
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = "onStoppedUsingItem", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsingItem(CallbackInfo callbackInfo) {
        CancelUseEvent event = new CancelUseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "clickBlock", at = @At("HEAD"))
    private void onClickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        EventManager.call(new BlockDamageEvent(Minecraft.func_71410_x().field_71439_g, loc));
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At("HEAD"))
    private void onDestroyBlock(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        EventManager.call(new BlockBreakEvent());
    }
}
