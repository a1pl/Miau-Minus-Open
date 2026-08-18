package miau.util.player;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import miau.Miau;
import miau.util.network.ServerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static boolean isEntityLoaded(Entity entity) {
        return entity != null && mc.field_71441_e != null ? mc.field_71441_e.field_72996_f.contains(entity) : false;
    }

    public static List<Entity> getLoadedEntitiesSorted() {
        return mc.field_71441_e != null && mc.func_175598_ae() != null
            ? mc.field_71441_e
                .field_72996_f
                .stream()
                .sorted(
                    (entity1, entity2) -> {
                        double dist1 = mc.func_175598_ae()
                            .func_78714_a(entity1.field_70165_t, entity1.field_70163_u, entity1.field_70161_v);
                        double dist2 = mc.func_175598_ae()
                            .func_78714_a(entity2.field_70165_t, entity2.field_70163_u, entity2.field_70161_v);
                        if (dist1 < dist2) {
                            return 1;
                        } else {
                            return dist1 > dist2
                                ? -1
                                : entity1.func_110124_au().toString().compareTo(entity2.func_110124_au().toString());
                        }
                    }
                )
                .collect(Collectors.toList())
            : Collections.emptyList();
    }

    public static float getHealthScore(EntityLivingBase entityLivingBase) {
        if (entityLivingBase == null) {
            return 0.0F;
        }

        int armor = Math.max(1, entityLivingBase.func_70658_aO());
        return entityLivingBase.func_110143_aJ() * (20.0F / armor);
    }

    public static String stripName(Entity entity) {
        return entity != null && entity.func_145748_c_() != null
            ? entity.func_145748_c_().func_150254_d().replaceAll("§\\S$", "").replaceAll("(?i)§r", "§f").trim()
            : "";
    }

    public static Color getTeamColor(EntityPlayer player, float alpha) {
        if (player != null && mc.field_71466_p != null) {
            int colorCode = 16777215;
            ScorePlayerTeam playerTeam = (ScorePlayerTeam)player.func_96124_cp();
            if (playerTeam != null) {
                String colorPrefix = FontRenderer.func_78282_e(playerTeam.func_96668_e());
                if (colorPrefix.length() >= 2) {
                    colorCode = mc.field_71466_p.func_175064_b(colorPrefix.charAt(1));
                }
            }

            return new Color(colorCode & 16777215 | (int)(alpha * 255.0F) << 24, true);
        } else {
            return new Color(16777215 | (int)(alpha * 255.0F) << 24, true);
        }
    }

    public static boolean isBot(EntityPlayer player) {
        if (player == null || mc.field_71439_g == null || mc.func_147114_u() == null) {
            return false;
        } else if (player == mc.field_71439_g) {
            return false;
        } else if (!ServerUtil.isHypixel()) {
            return false;
        } else {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175104_a(player.func_70005_c_());
            if (playerInfo == null) {
                return true;
            } else if (player.func_70005_c_().startsWith("§k")) {
                return player.func_82150_aj();
            } else if (playerInfo.func_178853_c() < 1) {
                return true;
            } else {
                ScorePlayerTeam playerTeam = playerInfo.func_178850_i();
                if (playerTeam == null) {
                    return false;
                } else {
                    return !playerTeam.func_96669_c().isEmpty() ? false : playerTeam.func_96668_e().equals("§c");
                }
            }
        }
    }

    public static boolean isSameTeam(EntityPlayer player) {
        if (player == null || mc.field_71439_g == null || mc.func_147114_u() == null) {
            return false;
        }

        if (player == mc.field_71439_g) {
            return true;
        }

        NetworkPlayerInfo selfInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
        if (selfInfo == null) {
            return false;
        }

        ScorePlayerTeam selfTeam = selfInfo.func_178850_i();
        if (selfTeam == null) {
            return false;
        }

        NetworkPlayerInfo targetInfo = mc.func_147114_u().func_175102_a(player.func_110124_au());
        if (targetInfo == null) {
            return false;
        }

        ScorePlayerTeam targetTeam = targetInfo.func_178850_i();
        return targetTeam == null ? false : selfTeam.func_96668_e().equals(targetTeam.func_96668_e());
    }

    public static boolean hasTeamColor(EntityLivingBase entity) {
        if (entity == null || mc.field_71439_g == null || mc.field_71441_e == null || mc.func_147114_u() == null) {
            return false;
        }

        if (entity == mc.field_71439_g) {
            return true;
        }

        NetworkPlayerInfo selfInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
        if (selfInfo == null) {
            return false;
        }

        ScorePlayerTeam selfTeam = selfInfo.func_178850_i();
        if (selfTeam == null) {
            return false;
        }

        if (selfTeam.func_96668_e().length() < 2) {
            return false;
        }

        EntityLivingBase nearestArmorStand = (EntityLivingBase)mc.field_71441_e
            .func_72857_a(EntityArmorStand.class, entity.func_174813_aQ(), entity);
        return nearestArmorStand != null
            ? nearestArmorStand.func_70005_c_().contains(selfTeam.func_96668_e().substring(0, 2))
            : false;
    }

    public static boolean isShop(EntityLivingBase entity) {
        if (entity == null || mc.field_71439_g == null || mc.field_71441_e == null) {
            return false;
        } else if (entity == mc.field_71439_g) {
            return false;
        } else {
            EntityLivingBase armorStand = (EntityLivingBase)mc.field_71441_e
                .func_72857_a(EntityArmorStand.class, entity.func_174813_aQ(), entity);
            if (armorStand == null) {
                return false;
            } else {
                String displayName = armorStand.func_70005_c_();
                if (displayName.contains("RIGHT CLICK")) {
                    return true;
                } else if (displayName.contains("ITEM SHOP")) {
                    return true;
                } else if (displayName.contains("UPGRADES")) {
                    return true;
                } else {
                    return displayName.contains("BANKER") ? true : displayName.contains("STREAK POWERS");
                }
            }
        }
    }

    public static boolean isFriend(EntityPlayer player) {
        return player != null && Miau.friendManager != null && Miau.friendManager.isFriend(player.func_70005_c_());
    }

    public static boolean isTarget(EntityPlayer player) {
        return player != null && Miau.targetManager != null && Miau.targetManager.isFriend(player.func_70005_c_());
    }
}
