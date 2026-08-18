package miau.module.modules.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.module.Module;
import miau.module.modules.misc.cheatdetector.CheatDetectorData;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class CheatDetector extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty alertMode = new ModeProperty("alert-mode", 0, new String[]{"Notification", "Chat"});
    public final BooleanProperty verbose = new BooleanProperty("verbose", false);
    public final BooleanProperty checkAutoBlock = new BooleanProperty("auto-block", true);
    public final BooleanProperty checkNoSlow = new BooleanProperty("no-slow", true);
    public final BooleanProperty checkLegitScaffold = new BooleanProperty("legit-scaffold", true);
    public final BooleanProperty checkKillaura = new BooleanProperty("killaura", true);
    public final BooleanProperty selfCheck = new BooleanProperty("check-self", false);
    public final FloatProperty alertCoolDown = new FloatProperty("alert-cooldown", 1000.0F, 0.0F, 2000.0F);
    private final Set<EntityPlayer> cheaters = new HashSet<>();
    private final Map<UUID, CheatDetectorData> dataMap = new HashMap<>();

    public CheatDetector() {
        super("CheatDetector", false);
    }

    public boolean isCheckEnabled(String name) {
        if ("AutoBlock".equals(name)) {
            return this.checkAutoBlock.getValue();
        } else if ("No slow".equals(name)) {
            return this.checkNoSlow.getValue();
        } else if ("Legit scaffold".equals(name)) {
            return this.checkLegitScaffold.getValue();
        } else {
            return "Killaura".equals(name) ? this.checkKillaura.getValue() : false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (this.isEnabled()) {
            if (mc.field_71441_e != null) {
                for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                    if (!(player.func_70032_d(mc.field_71439_g) > 16 * mc.field_71474_y.field_151451_c)
                        && (this.selfCheck.getValue() || player != mc.field_71439_g)
                        && !player.field_70128_L
                        && (Miau.friendManager == null || !Miau.friendManager.isFriend(player.func_70005_c_()))
                        && !AntiBot.isBot(player)) {
                        CheatDetectorData data = this.dataMap
                            .computeIfAbsent(player.func_110124_au(), k -> new CheatDetectorData());
                        data.onUpdate(player);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (this.isEnabled()) {
            if (mc.field_71441_e != null) {
                for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                    if (!(player.func_70032_d(mc.field_71439_g) > 16 * mc.field_71474_y.field_151451_c)
                        && (this.selfCheck.getValue() || player != mc.field_71439_g)
                        && !player.field_70128_L
                        && (Miau.friendManager == null || !Miau.friendManager.isFriend(player.func_70005_c_()))
                        && !AntiBot.isBot(player)) {
                        CheatDetectorData data = this.dataMap
                            .computeIfAbsent(player.func_110124_au(), k -> new CheatDetectorData());
                        data.onPacket(e, player);
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        this.cheaters.clear();
        this.dataMap.clear();
    }

    public void mark(EntityPlayer ent) {
        this.cheaters.add(ent);
    }

    public boolean isCheater(EntityPlayer ent) {
        for (EntityPlayer player : this.cheaters) {
            if (ent.func_70005_c_().equals(player.func_70005_c_())) {
                return true;
            }
        }

        return false;
    }

    public void cleanup() {
        Set<UUID> onlineUUIDs = mc.field_71441_e
            .field_73010_i
            .stream()
            .<UUID>map(Entity::func_110124_au)
            .collect(Collectors.toSet());
        this.dataMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}
