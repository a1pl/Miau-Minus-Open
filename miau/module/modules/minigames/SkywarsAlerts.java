package miau.module.modules.minigames;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.module.Module;
import miau.notification.NotificationType;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumChatFormatting;

public class SkywarsAlerts extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty alertType = new ModeProperty("Alert", 0, new String[]{"Chat", "Notification", "All"});
    private final ModeProperty soundMode = new ModeProperty("Ping sound", 0, new String[]{"All", "Important", "None"});
    private final FloatProperty cooldown = new FloatProperty("Cooldown", 10.0F, 1.0F, 30.0F);
    private final BooleanProperty showDistance = new BooleanProperty("Show distance", true);
    private final BooleanProperty fireSword = new BooleanProperty("Fire Sword", true);
    private final BooleanProperty diamondSword = new BooleanProperty("Diamond Sword", true);
    private final BooleanProperty knockbackSword = new BooleanProperty("Knockback Sword", true);
    private final BooleanProperty knockbackRod = new BooleanProperty("Knockback Rod", true);
    private final BooleanProperty strengthPotion = new BooleanProperty("Strength Potion", true);
    private final BooleanProperty enderPearl = new BooleanProperty("Ender Pearl", true);
    private final BooleanProperty corruptPearl = new BooleanProperty("Corrupt Pearl", true);
    private final BooleanProperty warpPearl = new BooleanProperty("Time Warp Pearl", true);
    private static final Map<String, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Set<Item>> HELD_ITEM_CACHE = new HashMap<>();

    public SkywarsAlerts() {
        super("SkywarsAlerts", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != null
                    && player != mc.field_71439_g
                    && !player.field_70128_L
                    && (Miau.friendManager == null || !Miau.friendManager.isFriend(player.func_70005_c_()))
                    && !TeamUtil.isSameTeam(player)) {
                    ItemStack held = player.func_70694_bm();
                    UUID uuid = player.func_110124_au();
                    String itemName = null;
                    if (held != null) {
                        if (held.func_77973_b() == Items.field_151079_bi
                            && held.func_77962_s()
                            && this.getTooltipMatches(held, player, "Teleport back")
                            && this.warpPearl.getValue()) {
                            itemName = EnumChatFormatting.LIGHT_PURPLE + "Time Warp Pearl";
                        } else if (held.func_77973_b() == Items.field_151079_bi
                            && held.func_77962_s()
                            && this.corruptPearl.getValue()) {
                            itemName = EnumChatFormatting.DARK_AQUA + "Corrupt Pearl";
                        } else if (held.func_77973_b() == Items.field_151079_bi && this.enderPearl.getValue()) {
                            itemName = EnumChatFormatting.DARK_PURPLE + "Ender Pearl";
                            markHeld(uuid, Items.field_151079_bi);
                        } else if (held.func_77973_b() == Items.field_151048_u && this.diamondSword.getValue()) {
                            itemName = EnumChatFormatting.AQUA + "Diamond Sword";
                            markHeld(uuid, Items.field_151048_u);
                        } else if (held.func_77973_b() instanceof ItemSword
                            && held.func_77962_s()
                            && this.hasEnchantment(held, Enchantment.field_77334_n.field_77352_x)
                            && this.fireSword.getValue()) {
                            itemName = EnumChatFormatting.RED + "Fire Sword";
                            markHeld(uuid, held.func_77973_b());
                        } else if ((
                                held.func_77973_b() == Items.field_151112_aM
                                    || held.func_77973_b() == Items.field_151055_y
                                    || held.func_77973_b() == Items.field_151072_bj
                            )
                            && held.func_77962_s()
                            && this.hasEnchantment(held, Enchantment.field_180313_o.field_77352_x)
                            && this.knockbackRod.getValue()) {
                            itemName = EnumChatFormatting.GOLD + "Knockback Rod";
                            markHeld(uuid, held.func_77973_b());
                        } else if ((
                                held.func_77973_b() instanceof ItemSword
                                    || held.func_77973_b() == Items.field_151123_aH
                            )
                            && held.func_77962_s()
                            && this.hasEnchantment(held, Enchantment.field_180313_o.field_77352_x)
                            && this.knockbackSword.getValue()) {
                            itemName = EnumChatFormatting.YELLOW + "Knockback Sword";
                            markHeld(uuid, held.func_77973_b());
                        } else if (held.func_77973_b() instanceof ItemPotion
                            && this.getTooltipMatches(held, player, "Strength")
                            && this.strengthPotion.getValue()) {
                            itemName = EnumChatFormatting.DARK_RED + "Strength Potion";
                            markHeld(uuid, held.func_77973_b());
                        }

                        if (itemName != null && !this.hasCooldown(player.func_70005_c_(), itemName)) {
                            this.alert(player, itemName);
                            this.setCooldown(player.func_70005_c_(), itemName);
                        }
                    }
                }
            }
        }
    }

    private boolean getTooltipMatches(ItemStack stack, EntityPlayer player, String match) {
        try {
            for (String s : stack.func_82840_a(player, false)) {
                if (s.contains(match)) {
                    return true;
                }
            }
        } catch (Exception var6) {
        }

        return false;
    }

    private boolean hasEnchantment(ItemStack stack, int enchantId) {
        return EnchantmentHelper.func_77506_a(enchantId, stack) > 0;
    }

    private void alert(EntityPlayer player, String itemName) {
        int distanceToEntity = (int)player.func_70032_d(mc.field_71439_g);
        String rawItemName = EnumChatFormatting.func_110646_a(itemName).toLowerCase();
        String distanceText = this.showDistance.getValue()
            ? EnumChatFormatting.GRAY
                + " ("
                + EnumChatFormatting.AQUA
                + distanceToEntity
                + "m"
                + EnumChatFormatting.GRAY
                + ")"
            : "";
        String text = player.func_70005_c_() + EnumChatFormatting.GRAY + " has " + itemName;
        String mode = this.alertType.getModeString();
        if (mode.equals("Chat") || mode.equals("All")) {
            ChatUtil.display(text + distanceText);
        }

        if (mode.equals("Notification") || mode.equals("All")) {
            Miau.notificationManager.pop("SkywarsAlerts", text, NotificationType.WARN);
        }

        String sound = this.soundMode.getModeString();
        if (sound.equals("All")) {
            this.sound();
        } else if (sound.equals("Important")
            && (
                rawItemName.equalsIgnoreCase("ender pearl")
                    || rawItemName.equalsIgnoreCase("diamond sword")
                    || rawItemName.equalsIgnoreCase("knockback rod")
                    || rawItemName.contains("strength")
                    || rawItemName.contains("corrupt pearl")
                    || rawItemName.contains("time warp pearl")
            )) {
            this.sound();
        }
    }

    private boolean hasCooldown(String playerName, String itemName) {
        long alertCooldown = (long)(this.cooldown.getValue() * 1000.0F);
        Map<String, Long> playerCooldowns = COOLDOWNS.get(playerName);
        if (playerCooldowns == null) {
            return false;
        }

        Long lastTime = playerCooldowns.get(itemName);
        return lastTime != null && System.currentTimeMillis() - lastTime < alertCooldown;
    }

    private void setCooldown(String playerName, String itemName) {
        COOLDOWNS.computeIfAbsent(playerName, k -> new HashMap<>()).put(itemName, System.currentTimeMillis());
    }

    private void sound() {
        mc.field_71439_g.func_85030_a("random.orb", 1.0F, 1.0F);
    }

    private static void markHeld(UUID uuid, Item item) {
        if (item != null) {
            HELD_ITEM_CACHE.computeIfAbsent(uuid, k -> new HashSet<>()).add(item);
        }
    }

    @Override
    public void onDisabled() {
        COOLDOWNS.clear();
        HELD_ITEM_CACHE.clear();
    }
}
