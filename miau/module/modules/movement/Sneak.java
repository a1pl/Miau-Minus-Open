package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class Sneak extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "Mode", 3, new String[]{"Legit", "Vanilla", "Switch", "MineSecure"}
    );
    public final BooleanProperty stopMove = new BooleanProperty("StopMove", false);
    private boolean sneaking = false;

    public Sneak() {
        super("Sneak", false);
    }

    @EventTarget
    public void onMotion(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.stopMove.getValue() && MoveUtil.isMoving()) {
                if (this.sneaking) {
                    this.onDisabled();
                }
            } else {
                String mode = this.mode.getModeString().toLowerCase();
                if (mode.equals("legit")) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), true);
                } else if (mode.equals("vanilla")) {
                    if (this.sneaking) {
                        return;
                    }

                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                } else if (mode.equals("switch")) {
                    if (event.getType() == EventType.PRE) {
                        PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                        PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                    } else if (event.getType() == EventType.POST) {
                        PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                        PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                    }
                } else if (mode.equals("minesecure")) {
                    if (event.getType() == EventType.PRE) {
                        return;
                    }

                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                }
            }
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.sneaking = false;
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            EntityPlayerSP player = mc.field_71439_g;
            String mode = this.mode.getModeString().toLowerCase();
            if (mode.equals("legit")) {
                if (!GameSettings.func_100015_a(mc.field_71474_y.field_74311_E)) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), false);
                }
            } else {
                PacketUtil.sendPacket(new C0BPacketEntityAction(player, Action.STOP_SNEAKING));
            }

            this.sneaking = false;
        }
    }
}
