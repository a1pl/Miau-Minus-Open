package miau.module.modules.movement;

import java.util.ArrayDeque;
import java.util.Queue;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorKeyBinding;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.network.PacketUtil;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import org.lwjgl.input.Mouse;

public class GUIMove extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty notInChests = new BooleanProperty("NotInChests", false);
    public final BooleanProperty aacAdditionPro = new BooleanProperty("AACAdditionPro", false);
    public final BooleanProperty intave = new BooleanProperty("Intave", false);
    public final BooleanProperty intaveSafe = new BooleanProperty("IntaveSafe", false, () -> this.intave.getValue());
    public final BooleanProperty saveC0E = new BooleanProperty("SaveC0E", false);
    public final BooleanProperty allowJump = new BooleanProperty("AllowJump", false);
    public final BooleanProperty allowSneak = new BooleanProperty("AllowSneak", false, () -> !this.intave.getValue());
    public final BooleanProperty noSprintWhenClosed = new BooleanProperty(
        "NoSprintWhenClosed", false, () -> this.saveC0E.getValue()
    );
    public final FloatProperty inventoryMotion = new FloatProperty("InventoryMotion", 1.0F, 0.0F, 2.0F);
    private final Queue<C0EPacketClickWindow> clickWindowList = new ArrayDeque<>();
    private final KeyBinding[] affectedBindings = new KeyBinding[]{
        mc.field_71474_y.field_74351_w,
        mc.field_71474_y.field_74368_y,
        mc.field_71474_y.field_74366_z,
        mc.field_71474_y.field_74370_x,
        mc.field_71474_y.field_74314_A,
        mc.field_71474_y.field_151444_V,
        mc.field_71474_y.field_74311_E
    };

    public GUIMove() {
        super("GUIMove", false);
    }

    private boolean isIntave() {
        return (mc.field_71462_r instanceof GuiInventory || mc.field_71462_r instanceof GuiChest)
            && this.intave.getValue();
    }

    private boolean shouldFreezeInputs(GuiScreen screen) {
        return this.notInChests.getValue() && screen instanceof GuiChest;
    }

    private boolean isButtonPressed(KeyBinding keyBinding) {
        return keyBinding.func_151463_i() < 0
            ? Mouse.isButtonDown(keyBinding.func_151463_i() + 100)
            : keyBinding.func_151470_d();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            GuiScreen screen = mc.field_71462_r;
            if (this.shouldFreezeInputs(screen)) {
                this.unPressKeys();
                return;
            }

            boolean intaveActive = this.isIntave();
            if (intaveActive && this.intaveSafe.getValue() && !mc.field_71439_g.field_70122_E) {
                this.unPressKeys();
                return;
            }

            if (screen instanceof GuiInventory || screen instanceof GuiChest) {
                mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w
                    * this.inventoryMotion.getValue().floatValue();
                mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y
                    * this.inventoryMotion.getValue().floatValue();
            }

            if (intaveActive && MoveUtil.isMoving()) {
                ((IAccessorKeyBinding)mc.field_71474_y.field_74311_E).setPressed(true);
            }

            for (KeyBinding affectedBinding : this.affectedBindings) {
                if (affectedBinding != mc.field_71474_y.field_74311_E || !intaveActive || !MoveUtil.isMoving()) {
                    if (affectedBinding != mc.field_71474_y.field_74314_A
                        || this.allowJump.getValue()
                        || !(screen instanceof GuiInventory) && !(screen instanceof GuiChest)) {
                        boolean pressed = this.isButtonPressed(affectedBinding)
                            || affectedBinding == mc.field_71474_y.field_74311_E
                                && this.allowSneak.getValue()
                                && this.isButtonPressed(mc.field_71474_y.field_74311_E)
                            || affectedBinding == mc.field_71474_y.field_151444_V
                                && Miau.moduleManager.modules.get(Sprint.class).isEnabled();
                        ((IAccessorKeyBinding)affectedBinding).setPressed(pressed);
                    } else {
                        ((IAccessorKeyBinding)affectedBinding).setPressed(false);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND) {
            if (!this.saveC0E.getValue()) {
                return;
            }

            if (this.noSprintWhenClosed.getValue()) {
                if (!this.clickWindowList.isEmpty() && mc.field_71462_r == null) {
                    mc.field_71439_g.func_70031_b(false);
                }

                if (event.getPacket() instanceof C0DPacketCloseWindow) {
                    event.setCancelled(true);
                    mc.field_71439_g.func_70031_b(false);
                    PacketUtil.sendPacketNoEvent(new C0DPacketCloseWindow());
                }
            }

            if (mc.field_71462_r != null) {
                if (event.getPacket() instanceof C0EPacketClickWindow) {
                    this.clickWindowList.add((C0EPacketClickWindow)event.getPacket());
                    event.setCancelled(true);
                }
            } else if (!this.clickWindowList.isEmpty()) {
                this.clickWindowList.forEach(PacketUtil::sendPacketNoEvent);
                this.clickWindowList.clear();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.restorePhysicalKeys();
    }

    private void restorePhysicalKeys() {
        for (KeyBinding affectedBinding : this.affectedBindings) {
            ((IAccessorKeyBinding)affectedBinding).setPressed(this.isButtonPressed(affectedBinding));
        }
    }

    private void unPressKeys() {
        for (KeyBinding affectedBinding : this.affectedBindings) {
            ((IAccessorKeyBinding)affectedBinding).setPressed(false);
        }
    }

    @Override
    public String[] getSuffix() {
        if (this.aacAdditionPro.getValue()) {
            return new String[]{"AACAdditionPro"};
        } else {
            return this.inventoryMotion.getValue() != 1.0F
                ? new String[]{String.valueOf(this.inventoryMotion.getValue())}
                : new String[0];
        }
    }
}
