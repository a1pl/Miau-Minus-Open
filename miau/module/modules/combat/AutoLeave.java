package miau.module.modules.combat;

import java.util.Random;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;

public class AutoLeave extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty health = new FloatProperty("Health", 8.0F, 0.0F, 20.0F);
    public final ModeProperty mode = new ModeProperty(
        "Mode", 0, new String[]{"Quit", "InvalidPacket", "SelfHurt", "IllegalChat"}
    );
    private final Random random = new Random();

    public AutoLeave() {
        super("AutoLeave", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (!(mc.field_71439_g.func_110143_aJ() > this.health.getValue())) {
                if (!mc.field_71439_g.field_71075_bZ.field_75098_d && !mc.func_71387_A()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            mc.field_71441_e.func_72882_A();
                            break;
                        case 1:
                            PacketUtil.sendPacket(
                                new C04PacketPlayerPosition(
                                    Double.NaN,
                                    Double.NEGATIVE_INFINITY,
                                    Double.POSITIVE_INFINITY,
                                    !mc.field_71439_g.field_70122_E
                                )
                            );
                            break;
                        case 2:
                            PacketUtil.sendPacket(new C02PacketUseEntity(mc.field_71439_g, Action.ATTACK));
                            break;
                        default:
                            mc.field_71439_g
                                .func_71165_d(
                                    Integer.toString(this.random.nextInt())
                                        + "§§§"
                                        + Integer.toString(this.random.nextInt())
                                );
                    }

                    this.setEnabled(false);
                }
            }
        }
    }
}
