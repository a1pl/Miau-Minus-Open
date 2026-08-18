package miau.module.modules.player;

import java.util.HashSet;
import java.util.Set;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoChest extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty delay = new IntProperty("Delay", 100, 0, 500);
    public final IntProperty waitDelay = new IntProperty("Wait Delay", 100, 0, 500);
    public final IntProperty dumpKey = new IntProperty("Dump Key", 0, 0, 255);
    public final IntProperty takeKey = new IntProperty("Take Key", 0, 0, 255);
    public final BooleanProperty renderClicked = new BooleanProperty("Render Clicked", true);
    public final BooleanProperty enderChests = new BooleanProperty("Enderchest", true);
    public final BooleanProperty normalChests = new BooleanProperty("Normal Chest", false);
    public final BooleanProperty iron = new BooleanProperty("Iron", true);
    public final BooleanProperty gold = new BooleanProperty("Gold", true);
    public final BooleanProperty diamonds = new BooleanProperty("Diamonds", true);
    public final BooleanProperty emeralds = new BooleanProperty("Emeralds", true);
    private static final String LOCAL_CHEST = I18n.func_135052_a("container.chest", new Object[0]);
    private static final String LOCAL_ENDER_CHEST = I18n.func_135052_a("container.enderchest", new Object[0]);
    public static final Set<Integer> CLICKED_SLOTS = new HashSet<>();
    private static long nextClickTime = 0L;
    private static boolean shouldDeposit = false;
    private static boolean shouldTake = false;
    private static boolean shouldDump = false;
    private static int lastCheckedSlot = 0;
    private boolean chestWasOpen = false;
    private int openDelayTicks = 0;

    public AutoChest() {
        super("AutoChest", false);
    }

    @Override
    public void onEnabled() {
        this.chestWasOpen = false;
        this.openDelayTicks = 0;
        resetState();
        CLICKED_SLOTS.clear();
    }

    @Override
    public void onDisabled() {
        CLICKED_SLOTS.clear();
        resetState();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (!(mc.field_71462_r instanceof GuiChest)) {
                    if (this.chestWasOpen) {
                        this.chestWasOpen = false;
                        resetState();
                    }
                } else {
                    GuiChest chest = (GuiChest)mc.field_71462_r;
                    if (!(chest.field_147002_h instanceof ContainerChest)) {
                        if (this.chestWasOpen) {
                            this.chestWasOpen = false;
                            resetState();
                        }
                    } else {
                        ContainerChest container = (ContainerChest)chest.field_147002_h;
                        IInventory inventory = container.func_85151_d();
                        if (!this.chestWasOpen) {
                            this.chestWasOpen = true;
                            if (this.allowedChest(inventory)) {
                                shouldDeposit = true;
                                lastCheckedSlot = 0;
                                this.openDelayTicks = Math.max(1, this.waitDelay.getValue() / 50);
                            }
                        } else if (this.openDelayTicks > 0) {
                            this.openDelayTicks--;
                        } else if (!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !Mouse.isButtonDown(2)
                            || !shouldDump && !shouldTake && !shouldDeposit) {
                            if (this.dumpKey.getValue() != 0 && Keyboard.isKeyDown(this.dumpKey.getValue())) {
                                shouldDump = true;
                                lastCheckedSlot = 0;
                            }

                            if (this.takeKey.getValue() != 0 && Keyboard.isKeyDown(this.takeKey.getValue())) {
                                shouldTake = true;
                                lastCheckedSlot = 0;
                            }

                            if (this.renderClicked.getValue()) {
                                CLICKED_SLOTS.clear();
                            }

                            if (shouldDeposit || shouldTake || shouldDump) {
                                long now = System.currentTimeMillis();
                                if (now >= nextClickTime) {
                                    nextClickTime = now + this.delay.getValue().intValue();
                                    if (!this.activate(container)) {
                                        resetState();
                                    }
                                }
                            }
                        } else {
                            ChatUtil.display("&cYou can't click while AutoChest is currently active, disabling.");
                            resetState();
                        }
                    }
                }
            }
        }
    }

    private boolean activate(ContainerChest container) {
        int size = container.field_75151_b.size();

        for (int i = lastCheckedSlot; i < size; i++) {
            Slot slot = container.func_75139_a(i);
            if (slot != null
                && slot.func_75216_d()
                && (
                    !shouldDeposit && !shouldDump
                        ? slot.field_75224_c != mc.field_71439_g.field_71071_by
                        : slot.field_75224_c == mc.field_71439_g.field_71071_by
                )) {
                ItemStack stack = slot.func_75211_c();
                if (stack != null && (shouldDump || this.allowedItem(stack.func_77973_b()))) {
                    this.click(container, slot);
                    lastCheckedSlot = i + 1;
                    return true;
                }
            }
        }

        return false;
    }

    private void click(ContainerChest container, Slot slot) {
        mc.field_71442_b.func_78753_a(container.field_75152_c, slot.field_75222_d, 0, 1, mc.field_71439_g);
        if (this.renderClicked.getValue()) {
            CLICKED_SLOTS.add(slot.field_75222_d);
        }
    }

    private boolean allowedItem(Item item) {
        return this.iron.getValue() && item == Items.field_151042_j
            || this.gold.getValue() && item == Items.field_151043_k
            || this.diamonds.getValue() && item == Items.field_151045_i
            || this.emeralds.getValue() && item == Items.field_151166_bC;
    }

    private boolean allowedChest(IInventory inventory) {
        String containerName = inventory.func_145748_c_().func_150260_c();
        if (this.enderChests.getValue() && inventory instanceof InventoryEnderChest) {
            if (containerName.isEmpty()) {
                return true;
            }

            if (LOCAL_ENDER_CHEST.equals(containerName)) {
                return true;
            }
        }

        if (this.normalChests.getValue() && !(inventory instanceof InventoryEnderChest)) {
            if (containerName.isEmpty()) {
                return true;
            }

            if (LOCAL_CHEST.equals(containerName)) {
                return true;
            }
        }

        return false;
    }

    private static void resetState() {
        shouldDeposit = false;
        shouldTake = false;
        shouldDump = false;
    }
}
