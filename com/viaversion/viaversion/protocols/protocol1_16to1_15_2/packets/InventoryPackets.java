package com.viaversion.viaversion.protocols.protocol1_16to1_15_2.packets;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ServerboundPackets1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.data.AttributeMappings;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.storage.InventoryTracker1_16;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.UUIDUtil;
import java.util.UUID;

public class InventoryPackets extends ItemRewriter<ClientboundPackets1_15, ServerboundPackets1_16, Protocol1_16To1_15_2> {
    public InventoryPackets(Protocol1_16To1_15_2 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    public void registerPackets() {
        final PacketHandler cursorRemapper = wrapper -> {
            PacketWrapper clearPacket = wrapper.create(ClientboundPackets1_16.SET_SLOT);
            clearPacket.write(Type.UNSIGNED_BYTE, (short)-1);
            clearPacket.write(Type.SHORT, (short)-1);
            clearPacket.write(Type.ITEM1_13_2, null);
            clearPacket.send(Protocol1_16To1_15_2.class);
        };
        this.protocol.registerClientbound(ClientboundPackets1_15.OPEN_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.map(Type.COMPONENT);
                this.handler(cursorRemapper);
                this.handler(wrapper -> {
                    InventoryTracker1_16 inventoryTracker = wrapper.user().get(InventoryTracker1_16.class);
                    int windowType = wrapper.get(Type.VAR_INT, 1);
                    if (windowType >= 20) {
                        wrapper.set(Type.VAR_INT, 1, ++windowType);
                    }

                    inventoryTracker.setInventoryOpen(true);
                });
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_15.CLOSE_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                this.handler(cursorRemapper);
                this.handler(wrapper -> {
                    InventoryTracker1_16 inventoryTracker = wrapper.user().get(InventoryTracker1_16.class);
                    inventoryTracker.setInventoryOpen(false);
                });
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_15.WINDOW_PROPERTY, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.SHORT);
                this.map(Type.SHORT);
                this.handler(wrapper -> {
                    short property = wrapper.get(Type.SHORT, 0);
                    if (property >= 4 && property <= 6) {
                        short enchantmentId = wrapper.get(Type.SHORT, 1);
                        if (enchantmentId >= 11) {
                            wrapper.set(Type.SHORT, 1, ++enchantmentId);
                        }
                    }
                });
            }
        });
        this.registerSetCooldown(ClientboundPackets1_15.COOLDOWN);
        this.registerWindowItems(ClientboundPackets1_15.WINDOW_ITEMS);
        this.registerTradeList(ClientboundPackets1_15.TRADE_LIST);
        this.registerSetSlot(ClientboundPackets1_15.SET_SLOT);
        this.registerAdvancements(ClientboundPackets1_15.ADVANCEMENTS);
        this.protocol.registerClientbound(ClientboundPackets1_15.ENTITY_EQUIPMENT, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int slot = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.BYTE, (byte)slot);
                    InventoryPackets.this.handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2));
                });
            }
        });
        new RecipeRewriter<>(this.protocol).register(ClientboundPackets1_15.DECLARE_RECIPES);
        this.registerClickWindow(ServerboundPackets1_16.CLICK_WINDOW);
        this.registerCreativeInvAction(ServerboundPackets1_16.CREATIVE_INVENTORY_ACTION);
        this.protocol.registerServerbound(ServerboundPackets1_16.CLOSE_WINDOW, wrapper -> {
            InventoryTracker1_16 inventoryTracker = wrapper.user().get(InventoryTracker1_16.class);
            inventoryTracker.setInventoryOpen(false);
        });
        this.protocol
            .registerServerbound(
                ServerboundPackets1_16.EDIT_BOOK,
                wrapper -> this.handleItemToServer(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2))
            );
        this.registerSpawnParticle(ClientboundPackets1_15.SPAWN_PARTICLE, Type.DOUBLE);
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag tag = item.tag();
        if (item.identifier() == 771 && tag != null) {
            CompoundTag ownerTag = tag.getCompoundTag("SkullOwner");
            if (ownerTag != null) {
                StringTag idTag = ownerTag.getStringTag("Id");
                if (idTag != null) {
                    UUID id = UUID.fromString(idTag.getValue());
                    ownerTag.put("Id", new IntArrayTag(UUIDUtil.toIntArray(id)));
                }
            }
        } else if (item.identifier() == 759 && tag != null) {
            ListTag<StringTag> pages = tag.getListTag("pages", StringTag.class);
            if (pages != null) {
                for (StringTag pageTag : pages) {
                    pageTag.setValue(
                        this.protocol.getComponentRewriter().processText(connection, pageTag.getValue()).toString()
                    );
                }
            }
        }

        oldToNewAttributes(item);
        item.setIdentifier(Protocol1_16To1_15_2.MAPPINGS.getNewItemId(item.identifier()));
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        item.setIdentifier(Protocol1_16To1_15_2.MAPPINGS.getOldItemId(item.identifier()));
        if (item.identifier() == 771 && item.tag() != null) {
            CompoundTag tag = item.tag();
            CompoundTag ownerTag = tag.getCompoundTag("SkullOwner");
            if (ownerTag != null) {
                IntArrayTag idTag = ownerTag.getIntArrayTag("Id");
                if (idTag != null) {
                    UUID id = UUIDUtil.fromIntArray(idTag.getValue());
                    ownerTag.putString("Id", id.toString());
                }
            }
        }

        newToOldAttributes(item);
        return item;
    }

    public static void oldToNewAttributes(Item item) {
        if (item.tag() != null) {
            ListTag<CompoundTag> attributes = item.tag().getListTag("AttributeModifiers", CompoundTag.class);
            if (attributes != null) {
                for (CompoundTag attribute : attributes) {
                    rewriteAttributeName(attribute, "AttributeName", false);
                    rewriteAttributeName(attribute, "Name", false);
                    NumberTag leastTag = attribute.getNumberTag("UUIDLeast");
                    NumberTag mostTag = attribute.getNumberTag("UUIDMost");
                    if (leastTag != null && mostTag != null) {
                        int[] uuidIntArray = UUIDUtil.toIntArray(mostTag.asLong(), leastTag.asLong());
                        attribute.put("UUID", new IntArrayTag(uuidIntArray));
                        attribute.remove("UUIDLeast");
                        attribute.remove("UUIDMost");
                    }
                }
            }
        }
    }

    public static void newToOldAttributes(Item item) {
        if (item.tag() != null) {
            ListTag<CompoundTag> attributes = item.tag().getListTag("AttributeModifiers", CompoundTag.class);
            if (attributes != null) {
                for (CompoundTag attribute : attributes) {
                    rewriteAttributeName(attribute, "AttributeName", true);
                    rewriteAttributeName(attribute, "Name", true);
                    IntArrayTag uuidTag = attribute.getIntArrayTag("UUID");
                    if (uuidTag != null && uuidTag.getValue().length == 4) {
                        UUID uuid = UUIDUtil.fromIntArray(uuidTag.getValue());
                        attribute.putLong("UUIDLeast", uuid.getLeastSignificantBits());
                        attribute.putLong("UUIDMost", uuid.getMostSignificantBits());
                        attribute.remove("UUID");
                    }
                }
            }
        }
    }

    public static void rewriteAttributeName(CompoundTag compoundTag, String entryName, boolean inverse) {
        StringTag attributeNameTag = compoundTag.getStringTag(entryName);
        if (attributeNameTag != null) {
            String attributeName = attributeNameTag.getValue();
            if (inverse) {
                attributeName = Key.namespaced(attributeName);
            }

            String mappedAttribute = (String)(inverse
                    ? AttributeMappings.attributeIdentifierMappings().inverse()
                    : AttributeMappings.attributeIdentifierMappings())
                .get(attributeName);
            if (mappedAttribute != null) {
                attributeNameTag.setValue(mappedAttribute);
            }
        }
    }
}
