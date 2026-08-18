package miau.module.modules.combat.velocity;

import miau.Miau;
import miau.event.impl.TickEvent;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;

public class LegitClickVelocity extends VelocityMode {
    public final BooleanProperty ignoreBlocking = new BooleanProperty("ignore-blocking", true);
    public final IntProperty durationHurtTime = new IntProperty("duration-hurt-time", 4, 1, 10);
    public final BooleanProperty whenFacingEnemyOnly = new BooleanProperty("facing-enemy-only", false);
    public final IntProperty clickRange = new IntProperty("click-range", 3, 1, 6);
    public final ModeProperty swingMode = new ModeProperty("swing-mode", 0, new String[]{"Normal", "Packet"});
    public final IntProperty clicksMin = new IntProperty("clicks-min", 1, 1, 5);
    public final IntProperty clicksMax = new IntProperty("clicks-max", 3, 1, 10);
    public final IntProperty clicksInterval = new IntProperty("clicks-interval", 1, 1, 100);
    private int attackStartHurtTime = 0;
    private int clicksTick = 0;

    public LegitClickVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.attackStartHurtTime = 0;
        this.clicksTick = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null && Velocity.mc.field_71441_e != null) {
            if (player.field_70737_aN == 0) {
                this.attackStartHurtTime = 0;
                this.clicksTick = 0;
            } else {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (!this.ignoreBlocking.getValue()
                    || !player.func_70632_aY() && (killAura == null || !killAura.shouldAutoBlock())) {
                    if (this.attackStartHurtTime == 0 && player.field_70737_aN > 0) {
                        this.attackStartHurtTime = player.field_70737_aN;
                    }

                    if (this.attackStartHurtTime - player.field_70737_aN < this.durationHurtTime.getValue()) {
                        Entity entity = Velocity.mc.field_71476_x != null
                            ? Velocity.mc.field_71476_x.field_72308_g
                            : null;
                        if (entity == null) {
                            if (this.whenFacingEnemyOnly.getValue()) {
                                return;
                            }

                            entity = VelocityUtil.getNearestEntityInRange(this.clickRange.getValue().intValue());
                        }

                        if (entity != null) {
                            this.clicksTick++;
                            if (this.clicksTick % this.clicksInterval.getValue() == 0) {
                                int totalClicks = VelocityUtil.randomInt(
                                    this.clicksMin.getValue(), this.clicksMax.getValue() + 1
                                );

                                for (int i = 0; i < totalClicks; i++) {
                                    if (this.swingMode.getValue() == 0) {
                                        player.func_71038_i();
                                    } else {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }

                                    player.func_71059_n(entity);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.attackStartHurtTime = 0;
        this.clicksTick = 0;
    }
}
