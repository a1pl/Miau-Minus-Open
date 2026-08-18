package miau.module.modules.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.TickEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AntiBot extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty basic = new BooleanProperty("Basic", true);
    public final BooleanProperty matrixBot = new BooleanProperty("MatrixBot", false);
    private final Map<EntityPlayer, double[]> matrixSamples = new HashMap<>();
    private final Set<EntityPlayer> matrixNotAlwaysInRadius = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean matrixCollectSample;

    public AntiBot() {
        super("AntiBot", true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            this.handleMatrixBot();
        }
    }

    private void handleMatrixBot() {
        if (this.matrixBot.getValue()) {
            if (this.matrixNotAlwaysInRadius.size() > 1000) {
                this.matrixNotAlwaysInRadius.clear();
            }

            this.matrixSamples
                .keySet()
                .removeIf(
                    playerx -> !mc.field_71441_e.field_72996_f.contains(playerx)
                        || this.matrixNotAlwaysInRadius.contains(playerx)
                );

            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityPlayer && entity != mc.field_71439_g) {
                    EntityPlayer player = (EntityPlayer)entity;
                    if (!this.isInMatrixCheckArea(player, 10.0F) && !this.matrixNotAlwaysInRadius.contains(player)) {
                        this.matrixNotAlwaysInRadius.add(player);
                        this.matrixSamples.remove(player);
                    }
                }
            }

            if (this.matrixCollectSample) {
                this.matrixSamples.clear();

                for (Entity entity : mc.field_71441_e.field_72996_f) {
                    if (entity instanceof EntityPlayer && entity != mc.field_71439_g) {
                        EntityPlayer player = (EntityPlayer)entity;
                        if (!this.matrixNotAlwaysInRadius.contains(player)) {
                            this.matrixSamples.put(player, new double[]{player.field_70165_t, player.field_70161_v});
                        }
                    }
                }
            } else {
                List<EntityPlayer> bots = new ArrayList<>();

                for (Entry<EntityPlayer, double[]> entry : this.matrixSamples.entrySet()) {
                    EntityPlayer player = entry.getKey();
                    double[] sample = entry.getValue();
                    if (player != null
                        && !this.matrixNotAlwaysInRadius.contains(player)
                        && mc.field_71441_e.field_72996_f.contains(player)) {
                        double xDiff = sample[0] - player.field_70165_t;
                        double zDiff = sample[1] - player.field_70161_v;
                        double speed = Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 10.0;
                        if (this.isMatrixBot(player, speed)) {
                            bots.add(player);
                        }
                    }
                }

                for (EntityPlayer bot : bots) {
                    mc.field_71441_e.func_72900_e(bot);
                    this.matrixSamples.remove(bot);
                }
            }

            this.matrixCollectSample = !this.matrixCollectSample;
        }
    }

    private boolean isMatrixBot(EntityPlayer player, double speed) {
        return player != mc.field_71439_g
            && !this.matrixNotAlwaysInRadius.contains(player)
            && speed > 8.0
            && this.isInMatrixCheckArea(player, 5.0F);
    }

    private boolean isInMatrixCheckArea(EntityPlayer player, float radius) {
        return mc.field_71439_g.func_70032_d(player) <= radius
            && this.within(
                player.field_70163_u, mc.field_71439_g.field_70163_u - 1.5, mc.field_71439_g.field_70163_u + 1.5
            );
    }

    private double getHorizontalSpeed(EntityPlayer player) {
        double xDiff = player.field_70165_t - player.field_70169_q;
        double zDiff = player.field_70161_v - player.field_70166_s;
        return Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 10.0;
    }

    private boolean within(double value, double min, double max) {
        return value >= min && value <= max;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clearAll();
    }

    private void clearAll() {
        this.matrixSamples.clear();
        this.matrixNotAlwaysInRadius.clear();
        this.matrixCollectSample = true;
    }

    @Override
    public void onDisabled() {
        this.clearAll();
    }

    public boolean isBotPlayer(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity == mc.field_71439_g) {
            return false;
        } else if (!this.isEnabled()) {
            return false;
        } else {
            EntityPlayer player = (EntityPlayer)entity;
            int id = player.func_145782_y();
            if (this.matrixBot.getValue() && this.isInvalidMatrixBotArmor(player)) {
                return true;
            } else if (!this.basic.getValue()) {
                return false;
            } else if (player.func_110124_au().version() == 2) {
                return true;
            } else {
                return player.func_145748_c_().func_150260_c().contains("[NPC]")
                    ? true
                    : player.func_70005_c_().isEmpty()
                        || player.func_70005_c_().equals(mc.field_71439_g.func_70005_c_());
            }
        }
    }

    private boolean isInvalidMatrixBotArmor(EntityPlayer player) {
        ItemStack helmet = player.field_71071_by.field_70460_b[3];
        ItemStack chestplate = player.field_71071_by.field_70460_b[2];
        if (helmet == null || chestplate == null) {
            return true;
        } else if (helmet.func_77973_b() instanceof ItemArmor && chestplate.func_77973_b() instanceof ItemArmor) {
            int helmetColor = ((ItemArmor)helmet.func_77973_b()).func_82814_b(helmet);
            int chestplateColor = ((ItemArmor)chestplate.func_77973_b()).func_82814_b(chestplate);
            return chestplateColor <= 0 || helmetColor <= 0 || chestplateColor != helmetColor;
        } else {
            return true;
        }
    }

    public static boolean isBot(EntityLivingBase entity) {
        AntiBot antiBot = (AntiBot)Miau.moduleManager.getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && antiBot.isBotPlayer(entity);
    }

    public static boolean isBasicEnabled() {
        AntiBot antiBot = (AntiBot)Miau.moduleManager.getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && antiBot.basic.getValue();
    }
}
