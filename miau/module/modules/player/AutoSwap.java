package miau.module.modules.player;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.TextProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class AutoSwap extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty useBlockWhitelist = new BooleanProperty("use-block-whitelist", false);
    public final TextProperty blockWhitelist = new TextProperty("whitelisted-blocks", "");
    private ItemStack trackedStack;
    private int lastPlaceSlot = -1;
    private int lastSwapSlot = -1;
    private long lastSwapTime;

    public AutoSwap() {
        super("AutoSwap", false);
    }

    @Override
    public void onEnabled() {
        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
    }

    @EventTarget(3)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.SEND) {
                if (mc.field_71441_e != null && mc.field_71439_g != null) {
                    if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                        C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement)event.getPacket();
                        if (packet.func_149568_f() != 255) {
                            ItemStack stack = packet.func_149574_g();
                            if (stack != null && stack.func_77973_b() instanceof ItemBlock) {
                                this.trackedStack = stack.func_77946_l();
                                this.trackedStack.field_77994_a = 1;
                                this.lastPlaceSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget(3)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71441_e == null || mc.field_71439_g == null) {
                    this.resetState();
                } else if (mc.field_71415_G
                    && mc.field_71462_r == null
                    && mc.field_71474_y.field_74313_G.func_151470_d()) {
                    if (this.trackedStack != null
                        && this.lastPlaceSlot != -1
                        && mc.field_71439_g.field_71071_by.field_70461_c == this.lastPlaceSlot) {
                        ItemStack held = mc.field_71439_g.func_70694_bm();
                        if (held == null || held.field_77994_a <= 0) {
                            if (this.isWhitelistedBlock(this.trackedStack)) {
                                long now = System.currentTimeMillis();

                                for (int slot = 8; slot >= 0; slot--) {
                                    if (slot != this.lastSwapSlot || now - this.lastSwapTime >= 300L) {
                                        ItemStack candidate = mc.field_71439_g.field_71071_by.func_70301_a(slot);
                                        if (this.matchesTrackedStack(candidate)) {
                                            this.swapToSlot(slot);
                                            this.lastSwapSlot = slot;
                                            this.lastSwapTime = now;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isWhitelistedBlock(ItemStack stack) {
        if (!this.useBlockWhitelist.getValue()) {
            return true;
        }

        if (stack != null && stack.func_77973_b() instanceof ItemBlock) {
            String whitelist = this.blockWhitelist.getValue().trim();
            if (whitelist.isEmpty()) {
                return true;
            }

            Block block = ((ItemBlock)stack.func_77973_b()).func_179223_d();
            Object registryName = Block.field_149771_c.func_177774_c(block);
            if (block != null && registryName != null) {
                String registryId = registryName.toString();
                int meta = stack.func_77960_j();
                String storageId = meta != 0 ? registryId + ":" + meta : registryId;

                for (String entry : whitelist.split(",")) {
                    entry = entry.trim();
                    if (!entry.isEmpty() && (entry.equals(registryId) || entry.equals(storageId))) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean matchesTrackedStack(ItemStack stack) {
        if (this.trackedStack == null || stack == null || stack.func_77973_b() != this.trackedStack.func_77973_b()) {
            return false;
        } else {
            return stack.func_77981_g() && stack.func_77960_j() != this.trackedStack.func_77960_j()
                ? false
                : ItemStack.func_77970_a(stack, this.trackedStack);
        }
    }

    private void swapToSlot(int slot) {
        if (slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
            mc.field_71439_g.field_71071_by.field_70461_c = slot;
            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }

    private void resetState() {
        this.trackedStack = null;
        this.lastPlaceSlot = -1;
        this.lastSwapSlot = -1;
        this.lastSwapTime = 0L;
    }
}
