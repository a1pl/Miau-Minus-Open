package miau.module.modules.minigames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoBuy extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty purchaseDelay = new IntProperty("Purchase Delay", 100, 100, 400);
    public final IntProperty woolKeybind = new IntProperty("Wool Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty woolQuickslot = new ModeProperty(
        "Wool Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty woolTurbo = new BooleanProperty("Wool Turbo", false);
    public final IntProperty stoneSwordKeybind = new IntProperty("Stone Sword Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty stoneSwordQuickslot = new ModeProperty(
        "Stone Sword Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty stoneSwordTurbo = new BooleanProperty("Stone Sword Turbo", false);
    public final IntProperty ironSwordKeybind = new IntProperty("Iron Sword Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty ironSwordQuickslot = new ModeProperty(
        "Iron Sword Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty ironSwordTurbo = new BooleanProperty("Iron Sword Turbo", false);
    public final IntProperty goldenAppleKeybind = new IntProperty("Golden Apple Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty goldenAppleQuickslot = new ModeProperty(
        "Golden Apple Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty goldenAppleTurbo = new BooleanProperty("Golden Apple Turbo", false);
    public final IntProperty fireballKeybind = new IntProperty("Fireball Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty fireballQuickslot = new ModeProperty(
        "Fireball Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty fireballTurbo = new BooleanProperty("Fireball Turbo", false);
    public final IntProperty tntKeybind = new IntProperty("TNT Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty tntQuickslot = new ModeProperty(
        "TNT Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty tntTurbo = new BooleanProperty("TNT Turbo", false);
    public final IntProperty enderPearlKeybind = new IntProperty("Ender Pearl Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty enderPearlQuickslot = new ModeProperty(
        "Ender Pearl Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty enderPearlTurbo = new BooleanProperty("Ender Pearl Turbo", false);
    public final IntProperty pickaxeKeybind = new IntProperty("Pickaxe Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty pickaxeQuickslot = new ModeProperty(
        "Pickaxe Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty pickaxeTurbo = new BooleanProperty("Pickaxe Turbo", false);
    public final IntProperty axeKeybind = new IntProperty("Axe Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty axeQuickslot = new ModeProperty(
        "Axe Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty axeTurbo = new BooleanProperty("Axe Turbo", false);
    public final IntProperty shearsKeybind = new IntProperty("Shears Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty shearsQuickslot = new ModeProperty(
        "Shears Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty shearsTurbo = new BooleanProperty("Shears Turbo", false);
    public final IntProperty chainmailArmorKeybind = new IntProperty("Chainmail Armor Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty chainmailArmorQuickslot = new ModeProperty(
        "Chainmail Armor Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty chainmailArmorTurbo = new BooleanProperty("Chainmail Armor Turbo", false);
    public final IntProperty ironArmorKeybind = new IntProperty("Iron Armor Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty ironArmorQuickslot = new ModeProperty(
        "Iron Armor Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty ironArmorTurbo = new BooleanProperty("Iron Armor Turbo", false);
    public final IntProperty diamondSwordKeybind = new IntProperty("Diamond Sword Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty diamondSwordQuickslot = new ModeProperty(
        "Diamond Sword Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty diamondSwordTurbo = new BooleanProperty("Diamond Sword Turbo", false);
    public final IntProperty stickKeybind = new IntProperty("Knockback Stick Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty stickQuickslot = new ModeProperty(
        "Knockback Stick Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty stickTurbo = new BooleanProperty("Knockback Stick Turbo", false);
    public final IntProperty arrowsKeybind = new IntProperty("Arrows Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty arrowsQuickslot = new ModeProperty(
        "Arrows Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty arrowsTurbo = new BooleanProperty("Arrows Turbo", false);
    public final IntProperty diamondArmorKeybind = new IntProperty("Diamond Armor Keybind", 0, 0, Integer.MAX_VALUE);
    public final ModeProperty diamondArmorQuickslot = new ModeProperty(
        "Diamond Armor Quickslot", 0, new String[]{"Disabled", "1", "2", "3", "4", "5", "6", "7", "8", "9"}
    );
    public final BooleanProperty diamondArmorTurbo = new BooleanProperty("Diamond Armor Turbo", false);
    public final IntProperty sharpnessKeybind = new IntProperty("Sharpness Keybind", 0, 0, Integer.MAX_VALUE);
    public final IntProperty protectionKeybind = new IntProperty("Protection Keybind", 0, 0, Integer.MAX_VALUE);
    public final IntProperty miningFatigueKeybind = new IntProperty("Mining Fatigue Keybind", 0, 0, Integer.MAX_VALUE);
    public final IntProperty hasteKeybind = new IntProperty("Haste Keybind", 0, 0, Integer.MAX_VALUE);
    public final IntProperty featherFallingKeybind = new IntProperty("Feather Falling Keybind", 0, 0, Integer.MAX_VALUE);
    private final Map<String, String> itemDisplayNames = new LinkedHashMap<>();
    private final List<String> items = new ArrayList<>();
    private final Map<String, Integer> locations = new HashMap<>();
    private final Map<String, Long> purchases = new HashMap<>();
    private final Map<String, Boolean> keyStates = new HashMap<>();
    private final List<int[]> clickList = new ArrayList<>();
    private final Map<String, IntProperty> keybindMap = new HashMap<>();
    private final Map<String, ModeProperty> quickslotMap = new HashMap<>();
    private final Map<String, BooleanProperty> turboMap = new HashMap<>();
    private static final Set<String> PICKAXE_TYPES = new HashSet<>(
        Arrays.asList("wooden_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe")
    );
    private static final Set<String> AXE_TYPES = new HashSet<>(
        Arrays.asList("wooden_axe", "stone_axe", "iron_axe", "diamond_axe")
    );

    public AutoBuy() {
        super("AutoBuy", false);
        this.setupItems();
    }

    private void setupItems() {
        this.registerItem("wool", "Wool", this.woolKeybind, this.woolQuickslot, this.woolTurbo);
        this.registerItem(
            "stone_sword", "Stone Sword", this.stoneSwordKeybind, this.stoneSwordQuickslot, this.stoneSwordTurbo
        );
        this.registerItem(
            "iron_sword", "Iron Sword", this.ironSwordKeybind, this.ironSwordQuickslot, this.ironSwordTurbo
        );
        this.registerItem(
            "golden_apple", "Golden Apple", this.goldenAppleKeybind, this.goldenAppleQuickslot, this.goldenAppleTurbo
        );
        this.registerItem("fire_charge", "Fireball", this.fireballKeybind, this.fireballQuickslot, this.fireballTurbo);
        this.registerItem("tnt", "TNT", this.tntKeybind, this.tntQuickslot, this.tntTurbo);
        this.registerItem(
            "ender_pearl", "Ender Pearl", this.enderPearlKeybind, this.enderPearlQuickslot, this.enderPearlTurbo
        );
        this.registerItem("pickaxe", "Pickaxe", this.pickaxeKeybind, this.pickaxeQuickslot, this.pickaxeTurbo);
        this.registerItem("axe", "Axe", this.axeKeybind, this.axeQuickslot, this.axeTurbo);
        this.registerItem("shears", "Shears", this.shearsKeybind, this.shearsQuickslot, this.shearsTurbo);
        this.registerItem(
            "chainmail_boots",
            "Chainmail Armor",
            this.chainmailArmorKeybind,
            this.chainmailArmorQuickslot,
            this.chainmailArmorTurbo
        );
        this.registerItem(
            "iron_boots", "Iron Armor", this.ironArmorKeybind, this.ironArmorQuickslot, this.ironArmorTurbo
        );
        this.registerItem(
            "diamond_sword",
            "Diamond Sword",
            this.diamondSwordKeybind,
            this.diamondSwordQuickslot,
            this.diamondSwordTurbo
        );
        this.registerItem("stick", "Knockback Stick", this.stickKeybind, this.stickQuickslot, this.stickTurbo);
        this.registerItem("arrow", "Arrows", this.arrowsKeybind, this.arrowsQuickslot, this.arrowsTurbo);
        this.registerItem(
            "diamond_boots",
            "Diamond Armor",
            this.diamondArmorKeybind,
            this.diamondArmorQuickslot,
            this.diamondArmorTurbo
        );
        this.registerUpgradeItem("upg iron_sword", "Sharpness", this.sharpnessKeybind);
        this.registerUpgradeItem("upg iron_chestplate", "Protection", this.protectionKeybind);
        this.registerUpgradeItem("upg iron_pickaxe", "Mining Fatigue", this.miningFatigueKeybind);
        this.registerUpgradeItem("upg golden_pickaxe", "Haste", this.hasteKeybind);
        this.registerUpgradeItem("upg diamond_boots", "Feather Falling", this.featherFallingKeybind);
    }

    private void registerItem(
        String item, String displayName, IntProperty keybind, ModeProperty quickslot, BooleanProperty turbo
    ) {
        this.itemDisplayNames.put(item, displayName);
        this.items.add(item);
        this.keybindMap.put(item, keybind);
        this.quickslotMap.put(item, quickslot);
        this.turboMap.put(item, turbo);
    }

    private void registerUpgradeItem(String item, String displayName, IntProperty keybind) {
        this.itemDisplayNames.put(item, displayName);
        this.items.add(item);
        this.keybindMap.put(item, keybind);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        this.locations.clear();
        this.clickList.clear();
        this.purchases.clear();
        this.keyStates.clear();
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (!(mc.field_71462_r instanceof GuiChest)) {
                    this.locations.clear();
                    this.clickList.clear();
                } else {
                    GuiChest chest = (GuiChest)mc.field_71462_r;
                    if (!(chest.field_147002_h instanceof ContainerChest)) {
                        this.locations.clear();
                        this.clickList.clear();
                    } else {
                        ContainerChest container = (ContainerChest)chest.field_147002_h;
                        String chestName = container.func_85151_d().func_70005_c_();
                        int chestSize = container.func_85151_d().func_70302_i_();
                        boolean isQuickBuy = chestName.equals("Quick Buy");
                        boolean isUpgrades = chestName.equals("Upgrades & Traps");
                        if (isQuickBuy || isUpgrades) {
                            long now = System.currentTimeMillis();
                            Iterator delayTicks = this.items.iterator();

                            while (true) {
                                String item;
                                String searchItem;
                                while (true) {
                                    if (!delayTicks.hasNext()) {
                                        for (String itemx : this.items) {
                                            if ((!isQuickBuy || !itemx.startsWith("upg "))
                                                && (!isUpgrades || itemx.startsWith("upg "))) {
                                                IntProperty keybindProp = this.keybindMap.get(itemx);
                                                if (keybindProp != null) {
                                                    int keyCode = keybindProp.getValue();
                                                    if (keyCode != 0) {
                                                        boolean keyDown = this.isKeyDown(keyCode);
                                                        boolean lastKeyState = this.keyStates
                                                            .getOrDefault(itemx, false);
                                                        if (!keyDown) {
                                                            this.keyStates.put(itemx, false);
                                                        } else {
                                                            int hotbarSlot = -1;
                                                            boolean turbo = false;
                                                            if (!itemx.startsWith("upg ")) {
                                                                ModeProperty slotProp = this.quickslotMap.get(itemx);
                                                                if (slotProp != null && slotProp.getValue() > 0) {
                                                                    hotbarSlot = slotProp.getValue() - 1;
                                                                }

                                                                BooleanProperty turboProp = this.turboMap.get(itemx);
                                                                if (turboProp != null) {
                                                                    turbo = turboProp.getValue();
                                                                }
                                                            }

                                                            long cooldown = itemx.startsWith("upg ") ? 300L : 90L;
                                                            long lastTime = this.purchases.getOrDefault(itemx, 0L);
                                                            if ((turbo || !lastKeyState) && now - lastTime >= cooldown) {
                                                                this.purchases.put(itemx, now);
                                                                this.keyStates.put(itemx, true);
                                                                Integer slot = this.locations.get(itemx);
                                                                if (slot != null) {
                                                                    this.clickList.add(new int[]{slot, hotbarSlot});
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        int delayTicksx = Math.max(1, this.purchaseDelay.getValue() / 50);
                                        if (mc.field_71439_g.field_70173_aa % delayTicksx == 0
                                            && !this.clickList.isEmpty()) {
                                            int[] click = this.clickList.remove(0);
                                            int slot = click[0];
                                            int hotbarSlot = click[1];
                                            if (hotbarSlot >= 0) {
                                                mc.field_71442_b
                                                    .func_78753_a(
                                                        container.field_75152_c, slot, hotbarSlot, 2, mc.field_71439_g
                                                    );
                                            } else {
                                                mc.field_71442_b
                                                    .func_78753_a(container.field_75152_c, slot, 0, 0, mc.field_71439_g);
                                            }
                                        }

                                        return;
                                    }

                                    item = (String)delayTicks.next();
                                    searchItem = item;
                                    if (!isQuickBuy || !item.startsWith("upg ")) {
                                        if (!isUpgrades) {
                                            break;
                                        }

                                        if (item.startsWith("upg ")) {
                                            searchItem = item.substring(4);
                                            break;
                                        }
                                    }
                                }

                                Set<String> itemTypes = null;
                                if (isQuickBuy) {
                                    if (searchItem.equals("pickaxe")) {
                                        itemTypes = PICKAXE_TYPES;
                                    } else if (searchItem.equals("axe")) {
                                        itemTypes = AXE_TYPES;
                                    }
                                }

                                int start = isQuickBuy ? 18 : 9;
                                int end = isQuickBuy ? chestSize - 9 : 27;

                                for (int i = start; i < end; i++) {
                                    ItemStack stack = container.func_75139_a(i).func_75211_c();
                                    if (stack != null && stack.func_77973_b() != null) {
                                        String unlocalizedName = stack.func_77973_b().func_77658_a();
                                        String cleanName = unlocalizedName.replace("tile.", "").replace("item.", "");
                                        if ((itemTypes == null || itemTypes.contains(cleanName))
                                            && (itemTypes != null || cleanName.equals(searchItem))) {
                                            this.locations.put(item, i);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isKeyDown(int keyCode) {
        return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
    }
}
