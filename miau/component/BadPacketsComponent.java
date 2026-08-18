package miau.component;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;

public final class BadPacketsComponent {
    private static boolean slot;
    private static boolean attack;
    private static boolean swing;
    private static boolean block;
    private static boolean inventory;
    private static boolean savedSlot;
    private static boolean savedAttack;
    private static boolean savedSwing;
    private static boolean savedBlock;
    private static boolean savedInventory;

    public static boolean bad() {
        return bad(true, true, true, true, true);
    }

    public static boolean bad(
        boolean slotCheck, boolean attackCheck, boolean swingCheck, boolean blockCheck, boolean inventoryCheck
    ) {
        return savedSlot && slotCheck
            || savedAttack && attackCheck
            || savedSwing && swingCheck
            || savedBlock && blockCheck
            || savedInventory && inventoryCheck;
    }

    @EventTarget(0)
    public final void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof C09PacketHeldItemChange) {
                slot = true;
            } else if (packet instanceof C0APacketAnimation) {
                swing = true;
            } else if (packet instanceof C02PacketUseEntity) {
                attack = true;
            } else if (!(packet instanceof C08PacketPlayerBlockPlacement)
                && !(packet instanceof C07PacketPlayerDigging)) {
                if (packet instanceof C0EPacketClickWindow
                    || packet instanceof C16PacketClientStatus
                        && ((C16PacketClientStatus)packet).func_149435_c() == EnumState.OPEN_INVENTORY_ACHIEVEMENT
                    || packet instanceof C0DPacketCloseWindow) {
                    inventory = true;
                }
            } else {
                block = true;
            }
        }
    }

    @EventTarget(0)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            savedSlot = slot;
            savedAttack = attack;
            savedSwing = swing;
            savedBlock = block;
            savedInventory = inventory;
        } else if (event.getType() == EventType.POST) {
            reset();
        }
    }

    public static void reset() {
        slot = false;
        swing = false;
        attack = false;
        block = false;
        inventory = false;
        savedSlot = false;
        savedAttack = false;
        savedSwing = false;
        savedBlock = false;
        savedInventory = false;
    }
}
