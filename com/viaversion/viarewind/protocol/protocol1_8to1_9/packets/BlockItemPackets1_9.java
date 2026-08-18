package com.viaversion.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viarewind.api.rewriter.VRBlockItemRewriter;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.data.PotionMappings;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ItemRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.HashSet;
import java.util.Set;

public class BlockItemPackets1_9
    extends VRBlockItemRewriter<ClientboundPackets1_9, ServerboundPackets1_8, Protocol1_8To1_9> {
    public final Set<String> VALID_ATTRIBUTES = new HashSet<>();
    private LegacyEnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_9(Protocol1_8To1_9 protocol) {
        super(protocol, "1.9");
        this.VALID_ATTRIBUTES.add("generic.maxHealth");
        this.VALID_ATTRIBUTES.add("generic.followRange");
        this.VALID_ATTRIBUTES.add("generic.knockbackResistance");
        this.VALID_ATTRIBUTES.add("generic.movementSpeed");
        this.VALID_ATTRIBUTES.add("generic.attackDamage");
        this.VALID_ATTRIBUTES.add("horse.jumpStrength");
        this.VALID_ATTRIBUTES.add("zombie.spawnReinforcements");
    }

    @Override
    protected void registerPackets() {
        this.registerBlockChange(ClientboundPackets1_9.BLOCK_CHANGE);
        this.registerMultiBlockChange(ClientboundPackets1_9.MULTI_BLOCK_CHANGE);
        this.registerCreativeInvAction(ServerboundPackets1_8.CREATIVE_INVENTORY_ACTION);
        this.protocol.registerClientbound(ClientboundPackets1_9.CLOSE_WINDOW, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            WindowTracker tracker = wrapper.user().get(WindowTracker.class);
            String windowType = tracker.get(windowId);
            if (windowType != null && windowType.equalsIgnoreCase("minecraft:enchanting_table")) {
                tracker.clearEnchantmentProperties();
            }

            tracker.remove(windowId);
        });
        this.protocol.registerClientbound(ClientboundPackets1_9.OPEN_WINDOW, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            String windowType = wrapper.passthrough(Type.STRING);
            JsonElement windowTitle = wrapper.passthrough(Type.COMPONENT);
            wrapper.user().get(WindowTracker.class).put(windowId, windowType);
            if (windowType.equalsIgnoreCase("minecraft:shulker_box")) {
                wrapper.set(Type.STRING, 0, "minecraft:container");
            }

            if (windowTitle.toString().equalsIgnoreCase("{\"translate\":\"container.shulkerBox\"}")) {
                wrapper.set(Type.COMPONENT, 0, JsonParser.parseString("{\"text\":\"Shulker Box\"}"));
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_9.WINDOW_ITEMS, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            Item[] items = wrapper.read(Type.ITEM1_8_SHORT_ARRAY);

            for (int i = 0; i < items.length; i++) {
                items[i] = this.handleItemToClient(wrapper.user(), items[i]);
            }

            if (windowId == 0 && items.length == 46) {
                Item[] old = items;
                items = new Item[45];
                System.arraycopy(old, 0, items, 0, 45);
            } else {
                String type = wrapper.user().get(WindowTracker.class).get(windowId);
                if (type != null && type.equalsIgnoreCase("minecraft:brewing_stand")) {
                    System.arraycopy(items, 0, wrapper.user().get(WindowTracker.class).getBrewingItems(windowId), 0, 4);
                    WindowTracker.updateBrewingStand(wrapper.user(), items[4], windowId);
                    Item[] old = items;
                    items = new Item[old.length - 1];
                    System.arraycopy(old, 0, items, 0, 4);
                    System.arraycopy(old, 5, items, 4, old.length - 5);
                }
            }

            wrapper.write(Type.ITEM1_8_SHORT_ARRAY, items);
        });
        this.protocol.registerClientbound(ClientboundPackets1_9.SET_SLOT, wrapper -> {
            byte windowId = wrapper.passthrough(Type.UNSIGNED_BYTE).byteValue();
            short slot = wrapper.passthrough(Type.SHORT);
            Item item = wrapper.passthrough(Type.ITEM1_8);
            this.handleItemToClient(wrapper.user(), item);
            if (windowId == 0 && slot == 45) {
                wrapper.cancel();
            } else {
                WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                String windowType = windowTracker.get(windowId);
                if (windowType != null && windowType.equalsIgnoreCase("minecraft:brewing_stand")) {
                    if (slot > 4) {
                        wrapper.set(Type.SHORT, 0, (short)(slot - 1));
                    } else if (slot == 4) {
                        wrapper.cancel();
                        WindowTracker.updateBrewingStand(wrapper.user(), wrapper.get(Type.ITEM1_8, 0), windowId);
                    } else {
                        windowTracker.getBrewingItems(windowId)[slot] = wrapper.get(Type.ITEM1_8, 0);
                    }
                }
            }
        });
        this.protocol.registerServerbound(ServerboundPackets1_8.CLOSE_WINDOW, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            wrapper.user().get(WindowTracker.class).remove(windowId);
        });
        this.protocol
            .registerServerbound(
                ServerboundPackets1_8.CLICK_WINDOW,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.BYTE, Type.VAR_INT);
                        this.map(Type.ITEM1_8);
                        this.handler(
                            wrapper -> BlockItemPackets1_9.this.handleItemToServer(
                                wrapper.user(), wrapper.get(Type.ITEM1_8, 0)
                            )
                        );
                        this.handler(wrapper -> {
                            short windowId = wrapper.get(Type.UNSIGNED_BYTE, 0);
                            String windowType = wrapper.user().get(WindowTracker.class).get(windowId);
                            if (windowType != null && windowType.equalsIgnoreCase("minecraft:brewing_stand")) {
                                short slot = wrapper.get(Type.SHORT, 0);
                                if (slot > 3) {
                                    wrapper.set(Type.SHORT, 0, (short)(slot + 1));
                                }
                            }
                        });
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_9.WINDOW_PROPERTY, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            short key = wrapper.read(Type.SHORT);
            short value = wrapper.read(Type.SHORT);
            WindowTracker tracker = wrapper.user().get(WindowTracker.class);
            String windowType = tracker.get(windowId);
            if (windowType != null && windowType.equalsIgnoreCase("minecraft:enchanting_table")) {
                if (key >= 4 && key <= 6) {
                    tracker.putEnchantmentProperty(key, value);
                    wrapper.cancel();
                } else if (key >= 7 && key <= 9) {
                    key = (short)(key - 3);
                    short property = tracker.getEnchantmentValue(key);
                    value = (short)(property | value << 8);
                }
            }

            wrapper.write(Type.SHORT, key);
            wrapper.write(Type.SHORT, value);
        });
    }

    @Override
    protected void registerRewrites() {
        this.enchantmentRewriter = new LegacyEnchantmentRewriter(this.nbtTagName());
        this.enchantmentRewriter.registerEnchantment(9, "§7Frost Walker");
        this.enchantmentRewriter.registerEnchantment(70, "§7Mending");
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToClient(connection, item);
        CompoundTag tag = item.tag();
        if (tag == null) {
            item.setTag(tag = new CompoundTag());
        }

        this.enchantmentRewriter.handleToClient(item);
        CompoundTag displayTag = tag.get("display");
        if (item.data() != 0 && tag.contains("Unbreakable")) {
            ByteTag unbreakableTag = tag.get("Unbreakable");
            if (unbreakableTag != null && unbreakableTag.asByte() != 0) {
                tag.put(this.nbtTagName() + "|Unbreakable", new ByteTag(unbreakableTag.asByte()));
                tag.remove("Unbreakable");
                if (displayTag == null) {
                    tag.put("display", displayTag = new CompoundTag());
                    tag.put(this.nbtTagName() + "|noDisplay", new ByteTag());
                }

                ListTag<StringTag> loreTag = displayTag.getListTag("Lore", StringTag.class);
                if (loreTag == null) {
                    displayTag.put("Lore", loreTag = new ListTag<>(StringTag.class));
                }

                loreTag.add(new StringTag("§9Unbreakable"));
            }
        }

        if (item.identifier() == 383 && item.data() == 0) {
            int data = 0;
            CompoundTag entityTag = tag.getCompoundTag("EntityTag");
            if (entityTag != null) {
                StringTag idTag = entityTag.getStringTag("id");
                if (idTag != null) {
                    String id = idTag.getValue();
                    if (ItemRewriter.ENTITY_NAME_TO_ID.containsKey(id)) {
                        data = ItemRewriter.ENTITY_NAME_TO_ID.get(id);
                    } else if (displayTag == null) {
                        tag.put("display", displayTag = new CompoundTag());
                        tag.put(this.nbtTagName() + "|noDisplay", new ByteTag());
                        displayTag.put("Name", new StringTag("§rSpawn " + id));
                    }
                }
            }

            item.setData((short)data);
        }

        boolean potion = item.identifier() == 373;
        boolean splashPotion = item.identifier() == 438;
        boolean lingeringPotion = item.identifier() == 441;
        if (potion || splashPotion || lingeringPotion) {
            int data = 0;
            StringTag potionTag = tag.getStringTag("Potion");
            if (potionTag != null) {
                String potionName = Key.stripMinecraftNamespace(potionTag.getValue());
                if (PotionMappings.POTION_NAME_TO_ID.containsKey(potionName)) {
                    data = PotionMappings.POTION_NAME_TO_ID.get(potionName);
                }

                if (splashPotion) {
                    potionName = potionName + "_splash";
                } else if (lingeringPotion) {
                    potionName = potionName + "_lingering";
                }

                if ((displayTag == null || !displayTag.contains("Name"))
                    && PotionMappings.POTION_NAME_INDEX.containsKey(potionName)) {
                    tag.put("display", displayTag = new CompoundTag());
                    tag.put(this.nbtTagName() + "|noDisplay", new ByteTag());
                    displayTag.put("Name", new StringTag(PotionMappings.POTION_NAME_INDEX.get(potionName)));
                }
            }

            if (splashPotion || lingeringPotion) {
                item.setIdentifier(373);
                data += 8192;
            }

            item.setData((short)data);
        }

        ListTag<CompoundTag> attributeModifiers = tag.getListTag("AttributeModifiers", CompoundTag.class);
        if (attributeModifiers != null) {
            tag.put(this.nbtTagName() + "|AttributeModifiers", attributeModifiers.copy());
            attributeModifiers.getValue().removeIf(entries -> {
                StringTag nameTag = entries.getStringTag("AttributeName");
                return nameTag != null && !this.VALID_ATTRIBUTES.contains(nameTag.getValue());
            });
        }

        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        CompoundTag tag = item.tag();
        if (tag == null) {
            item.setTag(tag = new CompoundTag());
        }

        this.enchantmentRewriter.handleToServer(item);
        if (item.identifier() == 383 && item.data() != 0) {
            if (!tag.contains("EntityTag") && ItemRewriter.ENTITY_ID_TO_NAME.containsKey(Integer.valueOf(item.data()))) {
                CompoundTag entityTag = new CompoundTag();
                entityTag.put("id", new StringTag(ItemRewriter.ENTITY_ID_TO_NAME.get(Integer.valueOf(item.data()))));
                tag.put("EntityTag", entityTag);
            }

            item.setData((short)0);
        }

        if (item.identifier() == 373 && !tag.contains("Potion")) {
            if (item.data() >= 16384) {
                item.setIdentifier(438);
                item.setData((short)(item.data() - 8192));
            }

            String name = item.data() == 8192 ? "water" : ItemRewriter.potionNameFromDamage(item.data());
            tag.put("Potion", new StringTag("minecraft:" + name));
            item.setData((short)0);
        }

        Tag noDisplayTag = tag.remove(this.nbtTagName() + "|noDisplay");
        if (noDisplayTag != null) {
            tag.remove("display");
        }

        Tag unbreakableTag = tag.remove(this.nbtTagName() + "|Unbreakable");
        if (unbreakableTag != null) {
            tag.put("Unbreakable", unbreakableTag);
        }

        Tag attributeModifiersTag = tag.remove(this.nbtTagName() + "|AttributeModifiers");
        if (attributeModifiersTag != null) {
            tag.put("AttributeModifiers", attributeModifiersTag);
        }

        return item;
    }
}
