package miau.module.modules.player;

import miau.Miau;
import miau.component.BadPacketsComponent;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.MathUtil;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class AutoHead extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final TimerUtil timer = new TimerUtil();
    private long nextUse;
    public final IntProperty health = new IntProperty("health", 15, 1, 20);
    public final IntProperty minDelay = new IntProperty("min-delay", 500, 0, 5000);
    public final IntProperty maxDelay = new IntProperty("max-delay", 1000, 0, 5000);
    public final ModeProperty slot = new ModeProperty(
        "slot", 0, new String[]{"Auto", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty insane = new BooleanProperty("insane", false);

    public AutoHead() {
        super("AutoHead", false);
    }

    public boolean isHealing() {
        if (!this.isEnabled()) {
            return false;
        }

        if (!this.insane.getValue() && !this.timer.hasTimeElapsed(this.nextUse)) {
            return true;
        }

        if (mc.field_71439_g.func_110139_bj() > 0.0F) {
            return false;
        }

        if (mc.field_71439_g.func_110143_aJ() > this.health.getValue().floatValue()) {
            return false;
        }

        if (Miau.moduleManager.modules.get(Scaffold.class).isEnabled()) {
            return false;
        }

        if (this.slot.getValue() > 0) {
            return true;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.func_77973_b() instanceof ItemSkull) {
                return true;
            }
        }

        return false;
    }

    @EventTarget(1)
    public void onPreUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if ((this.insane.getValue() || this.timer.hasTimeElapsed(this.nextUse))
                && !(mc.field_71439_g.func_110139_bj() > 0.0F)
                && !((Scaffold)Miau.moduleManager.modules.get(Scaffold.class)).isEnabled()) {
                if (!(mc.field_71439_g.func_110143_aJ() > this.health.getValue().floatValue())) {
                    if (this.slot.getValue() > 0) {
                        int targetSlot = this.slot.getValue() - 1;
                        Miau.slotComponent.setSlot(targetSlot);
                        if (!BadPacketsComponent.bad()) {
                            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(Miau.slotComponent.getItemStack()));
                            if (!this.insane.getValue()) {
                                this.nextUse = Math.round(
                                    MathUtil.getRandom(
                                        this.minDelay.getValue().intValue(), this.maxDelay.getValue().intValue()
                                    )
                                );
                                this.timer.reset();
                            }
                        }
                    } else {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                            if (stack != null && stack.func_77973_b() instanceof ItemSkull) {
                                Miau.slotComponent.setSlot(i);
                                if (!BadPacketsComponent.bad()) {
                                    ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                                    PacketUtil.sendPacket(
                                        new C08PacketPlayerBlockPlacement(Miau.slotComponent.getItemStack())
                                    );
                                    if (!this.insane.getValue()) {
                                        this.nextUse = Math.round(
                                            MathUtil.getRandom(
                                                this.minDelay.getValue().intValue(),
                                                this.maxDelay.getValue().intValue()
                                            )
                                        );
                                        this.timer.reset();
                                    }
                                }

                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
