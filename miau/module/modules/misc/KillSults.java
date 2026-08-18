package miau.module.modules.misc;

import java.util.Random;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.misc.killsults.CSGOMode;
import miau.module.modules.misc.killsults.DefaultMode;
import miau.module.modules.misc.killsults.EnglishMode;
import miau.module.modules.misc.killsults.KillSultMode;
import miau.module.modules.misc.killsults.LiberationDayMode;
import miau.module.modules.misc.killsults.MiauTeamMode;
import miau.module.modules.misc.killsults.VietnameseMode;
import miau.module.modules.misc.killsults.WatchdogMode;
import miau.module.modules.misc.killsults.WhatsAppMode;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class KillSults extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "Mode",
        0,
        new String[]{"English", "Vietnamese", "30-04-1975", "Default", "Watchdog", "WhatsApp", "CSGO", "MiauTeam"}
    );
    public final BooleanProperty shout = new BooleanProperty("Shout", false);
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 50);
    public final BooleanProperty randomizer = new BooleanProperty("Randomizer", false);
    private EntityPlayer target;
    private long lastAttackTime;
    private int ticks;
    private final KillSultMode englishMode = new EnglishMode();
    private final KillSultMode vietnameseMode = new VietnameseMode();
    private final KillSultMode liberationDayMode = new LiberationDayMode();
    private final KillSultMode defaultMode = new DefaultMode();
    private final KillSultMode watchdogMode = new WatchdogMode();
    private final KillSultMode whatsAppMode = new WhatsAppMode();
    private final KillSultMode csgoMode = new CSGOMode();
    private final KillSultMode miauTeamMode = new MiauTeamMode();

    public KillSults() {
        super("KillSults", false, false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            if (event.getTarget() instanceof EntityPlayer) {
                this.target = (EntityPlayer)event.getTarget();
                this.lastAttackTime = System.currentTimeMillis();
                this.ticks = 0;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (this.target != null) {
                    if (!this.target.field_70128_L && !(this.target.func_110143_aJ() <= 0.0F)) {
                        if (System.currentTimeMillis() - this.lastAttackTime > 10000L) {
                            this.target = null;
                        }
                    } else {
                        if (this.ticks >= this.delay.getValue()) {
                            this.sendKillSult(this.target.func_70005_c_());
                        }

                        this.target = null;
                    }
                }

                if (this.target != null) {
                    this.ticks++;
                }
            }
        }
    }

    private void sendKillSult(String targetName) {
        if (mc.field_71439_g != null) {
            KillSultMode activeMode = this.englishMode;
            switch (this.mode.getModeString()) {
                case "English":
                    activeMode = this.englishMode;
                    break;
                case "Vietnamese":
                    activeMode = this.vietnameseMode;
                    break;
                case "30-04-1975":
                    activeMode = this.liberationDayMode;
                    break;
                case "Default":
                    activeMode = this.defaultMode;
                    break;
                case "Watchdog":
                    activeMode = this.watchdogMode;
                    break;
                case "WhatsApp":
                    activeMode = this.whatsAppMode;
                    break;
                case "CSGO":
                    activeMode = this.csgoMode;
                    break;
                case "MiauTeam":
                    activeMode = this.miauTeamMode;
            }

            String message = activeMode.getMessage(targetName);
            if (this.randomizer.getValue()) {
                Random rng = new Random();
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < 10; i++) {
                    sb.append((char)(97 + rng.nextInt(26)));
                }

                message = message + " " + sb.toString();
            }

            if (this.shout.getValue()) {
                message = "/shout " + message;
            }

            mc.field_71439_g.func_71165_d(message);
        }
    }
}
