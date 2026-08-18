package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.packets;

import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.BlockIdData;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.MappingData;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.SoundSource;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.SpawnEggRewriter;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.IdAndData;
import com.viaversion.viaversion.util.Key;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class InventoryPackets
    extends ItemRewriter<ClientboundPackets1_12_1, ServerboundPackets1_13, Protocol1_13To1_12_2> {
    public InventoryPackets(Protocol1_13To1_12_2 protocol) {
        super(protocol, null, null);
    }

    @Override
    public void registerPackets() {
        this.protocol
            .registerClientbound(
                ClientboundPackets1_12_1.SET_SLOT,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.ITEM1_8, Type.ITEM1_13);
                        this.handler(
                            wrapper -> InventoryPackets.this.handleItemToClient(
                                wrapper.user(), wrapper.get(Type.ITEM1_13, 0)
                            )
                        );
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_12_1.WINDOW_ITEMS, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.ITEM1_8_SHORT_ARRAY, Type.ITEM1_13_SHORT_ARRAY);
                this.handler(wrapper -> {
                    Item[] items = wrapper.get(Type.ITEM1_13_SHORT_ARRAY, 0);

                    for (Item item : items) {
                        InventoryPackets.this.handleItemToClient(wrapper.user(), item);
                    }
                });
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_12_1.WINDOW_PROPERTY,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.SHORT);
                        this.handler(
                            wrapper -> {
                                short property = wrapper.get(Type.SHORT, 0);
                                if (property >= 4 && property <= 6) {
                                    wrapper.set(
                                        Type.SHORT,
                                        1,
                                        (short)InventoryPackets.this.protocol
                                            .getMappingData()
                                            .getEnchantmentMappings()
                                            .getNewId(wrapper.get(Type.SHORT, 1))
                                    );
                                }
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_12_1.PLUGIN_MESSAGE,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.STRING);
                        this.handlerSoftFail(
                            wrapper -> {
                                String channel = wrapper.get(Type.STRING, 0);
                                if (channel.equals("MC|StopSound")) {
                                    String originalSource = wrapper.read(Type.STRING);
                                    String originalSound = wrapper.read(Type.STRING);
                                    wrapper.clearPacket();
                                    wrapper.setPacketType(ClientboundPackets1_13.STOP_SOUND);
                                    byte flags = 0;
                                    wrapper.write(Type.BYTE, flags);
                                    if (!originalSource.isEmpty()) {
                                        flags = (byte)(flags | 1);
                                        Optional<SoundSource> finalSource = SoundSource.findBySource(originalSource);
                                        if (!finalSource.isPresent()) {
                                            if (!Via.getConfig().isSuppressConversionWarnings()
                                                || Via.getManager().isDebug()) {
                                                Via.getPlatform()
                                                    .getLogger()
                                                    .info(
                                                        "Could not handle unknown sound source "
                                                            + originalSource
                                                            + " falling back to default: master"
                                                    );
                                            }

                                            finalSource = Optional.of(SoundSource.MASTER);
                                        }

                                        wrapper.write(Type.VAR_INT, finalSource.get().getId());
                                    }

                                    if (!originalSound.isEmpty()) {
                                        flags = (byte)(flags | 2);
                                        wrapper.write(Type.STRING, originalSound);
                                    }

                                    wrapper.set(Type.BYTE, 0, flags);
                                } else {
                                    if (channel.equals("MC|TrList")) {
                                        channel = "minecraft:trader_list";
                                        wrapper.passthrough(Type.INT);
                                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);

                                        for (int i = 0; i < size; i++) {
                                            Item input = wrapper.read(Type.ITEM1_8);
                                            InventoryPackets.this.handleItemToClient(wrapper.user(), input);
                                            wrapper.write(Type.ITEM1_13, input);
                                            Item output = wrapper.read(Type.ITEM1_8);
                                            InventoryPackets.this.handleItemToClient(wrapper.user(), output);
                                            wrapper.write(Type.ITEM1_13, output);
                                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN);
                                            if (secondItem) {
                                                Item second = wrapper.read(Type.ITEM1_8);
                                                InventoryPackets.this.handleItemToClient(wrapper.user(), second);
                                                wrapper.write(Type.ITEM1_13, second);
                                            }

                                            wrapper.passthrough(Type.BOOLEAN);
                                            wrapper.passthrough(Type.INT);
                                            wrapper.passthrough(Type.INT);
                                        }
                                    } else {
                                        String old = channel;
                                        channel = InventoryPackets.getNewPluginChannelId(channel);
                                        if (channel == null) {
                                            if (!Via.getConfig().isSuppressConversionWarnings()
                                                || Via.getManager().isDebug()) {
                                                Via.getPlatform()
                                                    .getLogger()
                                                    .warning("Ignoring clientbound plugin message with channel: " + old);
                                            }

                                            wrapper.cancel();
                                            return;
                                        }

                                        if (channel.equals("minecraft:register")
                                            || channel.equals("minecraft:unregister")) {
                                            String[] channels = new String(
                                                    wrapper.read(Type.REMAINING_BYTES), StandardCharsets.UTF_8
                                                )
                                                .split("\u0000");
                                            List<String> rewrittenChannels = new ArrayList<>();

                                            for (String s : channels) {
                                                String rewritten = InventoryPackets.getNewPluginChannelId(s);
                                                if (rewritten != null) {
                                                    rewrittenChannels.add(rewritten);
                                                } else if (!Via.getConfig().isSuppressConversionWarnings()
                                                    || Via.getManager().isDebug()) {
                                                    Via.getPlatform()
                                                        .getLogger()
                                                        .warning(
                                                            "Ignoring plugin channel in clientbound "
                                                                + Key.stripMinecraftNamespace(channel)
                                                                    .toUpperCase(Locale.ROOT)
                                                                + ": "
                                                                + s
                                                        );
                                                }
                                            }

                                            if (rewrittenChannels.isEmpty()) {
                                                wrapper.cancel();
                                                return;
                                            }

                                            wrapper.write(
                                                Type.REMAINING_BYTES,
                                                Joiner.on('\u0000')
                                                    .join(rewrittenChannels)
                                                    .getBytes(StandardCharsets.UTF_8)
                                            );
                                        }
                                    }

                                    wrapper.set(Type.STRING, 0, channel);
                                }
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_12_1.ENTITY_EQUIPMENT,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.VAR_INT);
                        this.map(Type.ITEM1_8, Type.ITEM1_13);
                        this.handler(
                            wrapper -> InventoryPackets.this.handleItemToClient(
                                wrapper.user(), wrapper.get(Type.ITEM1_13, 0)
                            )
                        );
                    }
                }
            );
        this.protocol
            .registerServerbound(
                ServerboundPackets1_13.CLICK_WINDOW,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.VAR_INT);
                        this.map(Type.ITEM1_13, Type.ITEM1_8);
                        this.handler(
                            wrapper -> InventoryPackets.this.handleItemToServer(
                                wrapper.user(), wrapper.get(Type.ITEM1_8, 0)
                            )
                        );
                    }
                }
            );
        this.protocol
            .registerServerbound(
                ServerboundPackets1_13.PLUGIN_MESSAGE,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.STRING);
                        this.handlerSoftFail(
                            wrapper -> {
                                String channel = wrapper.get(Type.STRING, 0);
                                String old = channel;
                                channel = InventoryPackets.getOldPluginChannelId(channel);
                                if (channel == null) {
                                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                                        Via.getPlatform()
                                            .getLogger()
                                            .warning("Ignoring serverbound plugin message with channel: " + old);
                                    }

                                    wrapper.cancel();
                                } else {
                                    if (channel.equals("REGISTER") || channel.equals("UNREGISTER")) {
                                        String[] channels = new String(
                                                wrapper.read(Type.REMAINING_BYTES), StandardCharsets.UTF_8
                                            )
                                            .split("\u0000");
                                        List<String> rewrittenChannels = new ArrayList<>();

                                        for (String s : channels) {
                                            String rewritten = InventoryPackets.getOldPluginChannelId(s);
                                            if (rewritten != null) {
                                                rewrittenChannels.add(rewritten);
                                            } else if (!Via.getConfig().isSuppressConversionWarnings()
                                                || Via.getManager().isDebug()) {
                                                Via.getPlatform()
                                                    .getLogger()
                                                    .warning(
                                                        "Ignoring plugin channel in serverbound " + channel + ": " + s
                                                    );
                                            }
                                        }

                                        wrapper.write(
                                            Type.REMAINING_BYTES,
                                            Joiner.on('\u0000')
                                                .join(rewrittenChannels)
                                                .getBytes(StandardCharsets.UTF_8)
                                        );
                                    }

                                    wrapper.set(Type.STRING, 0, channel);
                                }
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerServerbound(
                ServerboundPackets1_13.CREATIVE_INVENTORY_ACTION,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.SHORT);
                        this.map(Type.ITEM1_13, Type.ITEM1_8);
                        this.handler(
                            wrapper -> InventoryPackets.this.handleItemToServer(
                                wrapper.user(), wrapper.get(Type.ITEM1_8, 0)
                            )
                        );
                    }
                }
            );
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag tag = item.tag();
        int originalId = item.identifier() << 16 | item.data() & '\uffff';
        int rawId = IdAndData.toRawData(item.identifier(), item.data());
        if (isDamageable(item.identifier())) {
            if (tag == null) {
                item.setTag(tag = new CompoundTag());
            }

            tag.put("Damage", new IntTag(item.data()));
        }

        if (item.identifier() == 358) {
            if (tag == null) {
                item.setTag(tag = new CompoundTag());
            }

            tag.put("map", new IntTag(item.data()));
        }

        if (tag != null) {
            boolean banner = item.identifier() == 425;
            if (banner || item.identifier() == 442) {
                CompoundTag blockEntityTag = tag.getCompoundTag("BlockEntityTag");
                if (blockEntityTag != null) {
                    NumberTag baseTag = blockEntityTag.getNumberTag("Base");
                    if (baseTag != null) {
                        if (banner) {
                            rawId = 6800 + baseTag.asInt();
                        }

                        blockEntityTag.putInt("Base", 15 - baseTag.asInt());
                    }

                    ListTag<CompoundTag> patternsTag = blockEntityTag.getListTag("Patterns", CompoundTag.class);
                    if (patternsTag != null) {
                        for (CompoundTag pattern : patternsTag) {
                            NumberTag colorTag = pattern.getNumberTag("Color");
                            if (colorTag != null) {
                                pattern.putInt("Color", 15 - colorTag.asInt());
                            }
                        }
                    }
                }
            }

            CompoundTag display = tag.getCompoundTag("display");
            if (display != null) {
                StringTag name = display.getStringTag("Name");
                if (name != null) {
                    display.putString(this.nbtTagName("Name"), name.getValue());
                    name.setValue(ComponentUtil.legacyToJsonString(name.getValue(), true));
                }
            }

            ListTag<CompoundTag> ench = tag.getListTag("ench", CompoundTag.class);
            if (ench != null) {
                ListTag<CompoundTag> enchantments = new ListTag<>(CompoundTag.class);

                for (CompoundTag enchEntry : ench) {
                    NumberTag idTag = enchEntry.getNumberTag("id");
                    if (idTag != null) {
                        CompoundTag enchantmentEntry = new CompoundTag();
                        short oldId = idTag.asShort();
                        String newId = (String)Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().get(oldId);
                        if (newId == null) {
                            newId = "viaversion:legacy/" + oldId;
                        }

                        enchantmentEntry.putString("id", newId);
                        NumberTag levelTag = enchEntry.getNumberTag("lvl");
                        if (levelTag != null) {
                            enchantmentEntry.putShort("lvl", levelTag.asShort());
                        }

                        enchantments.add(enchantmentEntry);
                    }
                }

                tag.remove("ench");
                tag.put("Enchantments", enchantments);
            }

            ListTag<CompoundTag> storedEnch = tag.getListTag("StoredEnchantments", CompoundTag.class);
            if (storedEnch != null) {
                ListTag<CompoundTag> newStoredEnch = new ListTag<>(CompoundTag.class);

                for (CompoundTag enchEntry : storedEnch) {
                    NumberTag idTag = enchEntry.getNumberTag("id");
                    if (idTag != null) {
                        CompoundTag enchantmentEntry = new CompoundTag();
                        short oldId = idTag.asShort();
                        String newId = (String)Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().get(oldId);
                        if (newId == null) {
                            newId = "viaversion:legacy/" + oldId;
                        }

                        enchantmentEntry.putString("id", newId);
                        NumberTag levelTag = enchEntry.getNumberTag("lvl");
                        if (levelTag != null) {
                            enchantmentEntry.putShort("lvl", levelTag.asShort());
                        }

                        newStoredEnch.add(enchantmentEntry);
                    }
                }

                tag.put("StoredEnchantments", newStoredEnch);
            }

            ListTag<?> canPlaceOnTag = tag.getListTag("CanPlaceOn");
            if (canPlaceOnTag != null) {
                ListTag<StringTag> newCanPlaceOn = new ListTag<>(StringTag.class);
                tag.put(this.nbtTagName("CanPlaceOn"), canPlaceOnTag.copy());

                for (Tag oldTag : canPlaceOnTag) {
                    Object value = oldTag.getValue();
                    String oldId = Key.stripMinecraftNamespace(value.toString());
                    String numberConverted = BlockIdData.numberIdToString.get(Ints.tryParse(oldId));
                    if (numberConverted != null) {
                        oldId = numberConverted;
                    }

                    String[] newValues = BlockIdData.blockIdMapping.get(oldId.toLowerCase(Locale.ROOT));
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanPlaceOn.add(new StringTag(newValue));
                        }
                    } else {
                        newCanPlaceOn.add(new StringTag(oldId.toLowerCase(Locale.ROOT)));
                    }
                }

                tag.put("CanPlaceOn", newCanPlaceOn);
            }

            ListTag<?> canDestroyTag = tag.getListTag("CanDestroy");
            if (canDestroyTag != null) {
                ListTag<StringTag> newCanDestroy = new ListTag<>(StringTag.class);
                tag.put(this.nbtTagName("CanDestroy"), canDestroyTag.copy());

                for (Tag oldTag : canDestroyTag) {
                    Object value = oldTag.getValue();
                    String oldId = Key.stripMinecraftNamespace(value.toString());
                    String numberConverted = BlockIdData.numberIdToString.get(Ints.tryParse(oldId));
                    if (numberConverted != null) {
                        oldId = numberConverted;
                    }

                    String[] newValues = BlockIdData.blockIdMapping.get(oldId.toLowerCase(Locale.ROOT));
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanDestroy.add(new StringTag(newValue));
                        }
                    } else {
                        newCanDestroy.add(new StringTag(oldId.toLowerCase(Locale.ROOT)));
                    }
                }

                tag.put("CanDestroy", newCanDestroy);
            }

            if (item.identifier() == 383) {
                CompoundTag entityTag = tag.getCompoundTag("EntityTag");
                if (entityTag != null) {
                    StringTag idTag = entityTag.getStringTag("id");
                    if (idTag != null) {
                        rawId = SpawnEggRewriter.getSpawnEggId(idTag.getValue());
                        if (rawId == -1) {
                            rawId = 25100288;
                        } else {
                            entityTag.remove("id");
                            if (entityTag.isEmpty()) {
                                tag.remove("EntityTag");
                            }
                        }
                    } else {
                        rawId = 25100288;
                    }
                } else {
                    rawId = 25100288;
                }
            }

            if (tag.isEmpty()) {
                tag = null;
                item.setTag(null);
            }
        }

        if (Protocol1_13To1_12_2.MAPPINGS.getItemMappings().getNewId(rawId) == -1) {
            if (!isDamageable(item.identifier()) && item.identifier() != 358) {
                if (tag == null) {
                    item.setTag(tag = new CompoundTag());
                }

                tag.put(this.nbtTagName(), new IntTag(originalId));
            }

            if (item.identifier() == 31 && item.data() == 0) {
                rawId = IdAndData.toRawData(32);
            } else if (Protocol1_13To1_12_2.MAPPINGS.getItemMappings().getNewId(IdAndData.removeData(rawId)) != -1) {
                rawId = IdAndData.removeData(rawId);
            } else {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    Via.getPlatform().getLogger().warning("Failed to get 1.13 item for " + item.identifier());
                }

                rawId = 16;
            }
        }

        item.setIdentifier(Protocol1_13To1_12_2.MAPPINGS.getItemMappings().getNewId(rawId));
        item.setData((short)0);
        return item;
    }

    public static String getNewPluginChannelId(String old) {
        switch (old) {
            case "MC|TrList":
                return "minecraft:trader_list";
            case "MC|Brand":
                return "minecraft:brand";
            case "MC|BOpen":
                return "minecraft:book_open";
            case "MC|DebugPath":
                return "minecraft:debug/paths";
            case "MC|DebugNeighborsUpdate":
                return "minecraft:debug/neighbors_update";
            case "REGISTER":
                return "minecraft:register";
            case "UNREGISTER":
                return "minecraft:unregister";
            case "BungeeCord":
                return "bungeecord:main";
            case "bungeecord:main":
                return null;
            default:
                String mappedChannel = (String)Protocol1_13To1_12_2.MAPPINGS.getChannelMappings().get(old);
                return mappedChannel != null ? mappedChannel : MappingData.validateNewChannel(old);
        }
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        Integer rawId = null;
        boolean gotRawIdFromTag = false;
        CompoundTag tag = item.tag();
        if (tag != null) {
            NumberTag viaTag = tag.getNumberTag(this.nbtTagName());
            if (viaTag != null) {
                rawId = viaTag.asInt();
                tag.remove(this.nbtTagName());
                gotRawIdFromTag = true;
            }
        }

        if (rawId == null) {
            int oldId = Protocol1_13To1_12_2.MAPPINGS.getItemMappings().inverse().getNewId(item.identifier());
            if (oldId != -1) {
                Optional<String> eggEntityId = SpawnEggRewriter.getEntityId(oldId);
                if (eggEntityId.isPresent()) {
                    rawId = 25100288;
                    if (tag == null) {
                        item.setTag(tag = new CompoundTag());
                    }

                    if (!tag.contains("EntityTag")) {
                        CompoundTag entityTag = new CompoundTag();
                        entityTag.put("id", new StringTag(eggEntityId.get()));
                        tag.put("EntityTag", entityTag);
                    }
                } else {
                    rawId = IdAndData.getId(oldId) << 16 | oldId & 15;
                }
            }
        }

        if (rawId == null) {
            if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                Via.getPlatform().getLogger().warning("Failed to get 1.12 item for " + item.identifier());
            }

            rawId = 65536;
        }

        item.setIdentifier((short)(rawId >> 16));
        item.setData((short)(rawId & 65535));
        if (tag != null) {
            if (isDamageable(item.identifier())) {
                NumberTag damageTag = tag.getNumberTag("Damage");
                if (damageTag != null) {
                    if (!gotRawIdFromTag) {
                        item.setData(damageTag.asShort());
                    }

                    tag.remove("Damage");
                }
            }

            if (item.identifier() == 358) {
                NumberTag mapTag = tag.getNumberTag("map");
                if (mapTag != null) {
                    if (!gotRawIdFromTag) {
                        item.setData(mapTag.asShort());
                    }

                    tag.remove("map");
                }
            }

            if (item.identifier() == 442 || item.identifier() == 425) {
                CompoundTag blockEntityTag = tag.getCompoundTag("BlockEntityTag");
                if (blockEntityTag != null) {
                    NumberTag baseTag = blockEntityTag.getNumberTag("Base");
                    if (baseTag != null) {
                        blockEntityTag.putInt("Base", 15 - baseTag.asInt());
                    }

                    ListTag<CompoundTag> patternsTag = blockEntityTag.getListTag("Patterns", CompoundTag.class);
                    if (patternsTag != null) {
                        for (CompoundTag pattern : patternsTag) {
                            NumberTag colorTag = pattern.getNumberTag("Color");
                            pattern.putInt("Color", 15 - colorTag.asInt());
                        }
                    }
                }
            }

            CompoundTag display = tag.getCompoundTag("display");
            if (display != null) {
                StringTag name = display.getStringTag("Name");
                if (name != null) {
                    Tag via = display.remove(this.nbtTagName("Name"));
                    name.setValue(
                        via instanceof StringTag ? (String)via.getValue() : ComponentUtil.jsonToLegacy(name.getValue())
                    );
                }
            }

            ListTag<CompoundTag> enchantments = tag.getListTag("Enchantments", CompoundTag.class);
            if (enchantments != null) {
                ListTag<CompoundTag> ench = new ListTag<>(CompoundTag.class);

                for (CompoundTag enchantmentEntry : enchantments) {
                    StringTag idTag = enchantmentEntry.getStringTag("id");
                    if (idTag != null) {
                        CompoundTag enchEntry = new CompoundTag();
                        String newId = idTag.getValue();
                        Short oldId = (Short)Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().inverse().get(newId);
                        if (oldId == null && newId.startsWith("viaversion:legacy/")) {
                            oldId = Short.valueOf(newId.substring(18));
                        }

                        if (oldId != null) {
                            enchEntry.putShort("id", oldId);
                            NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
                            if (levelTag != null) {
                                enchEntry.putShort("lvl", levelTag.asShort());
                            }

                            ench.add(enchEntry);
                        }
                    }
                }

                tag.remove("Enchantments");
                tag.put("ench", ench);
            }

            ListTag<CompoundTag> storedEnch = tag.getListTag("StoredEnchantments", CompoundTag.class);
            if (storedEnch != null) {
                ListTag<CompoundTag> newStoredEnch = new ListTag<>(CompoundTag.class);

                for (CompoundTag enchantmentEntry : storedEnch) {
                    StringTag idTag = enchantmentEntry.getStringTag("id");
                    if (idTag != null) {
                        CompoundTag enchEntry = new CompoundTag();
                        String newId = idTag.getValue();
                        Short oldId = (Short)Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().inverse().get(newId);
                        if (oldId == null && newId.startsWith("viaversion:legacy/")) {
                            oldId = Short.valueOf(newId.substring(18));
                        }

                        if (oldId != null) {
                            enchEntry.putShort("id", oldId);
                            NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
                            if (levelTag != null) {
                                enchEntry.putShort("lvl", levelTag.asShort());
                            }

                            newStoredEnch.add(enchEntry);
                        }
                    }
                }

                tag.put("StoredEnchantments", newStoredEnch);
            }

            if (tag.getListTag(this.nbtTagName("CanPlaceOn")) != null) {
                tag.put("CanPlaceOn", tag.remove(this.nbtTagName("CanPlaceOn")));
            } else if (tag.getListTag("CanPlaceOn") != null) {
                ListTag<?> old = tag.getListTag("CanPlaceOn");
                ListTag<StringTag> newCanPlaceOn = new ListTag<>(StringTag.class);

                for (Tag oldTag : old) {
                    Object value = oldTag.getValue();
                    String[] newValues = BlockIdData.fallbackReverseMapping
                        .get(value instanceof String ? Key.stripMinecraftNamespace((String)value) : null);
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanPlaceOn.add(new StringTag(newValue));
                        }
                    } else {
                        newCanPlaceOn.add(new StringTag(value.toString()));
                    }
                }

                tag.put("CanPlaceOn", newCanPlaceOn);
            }

            if (tag.getListTag(this.nbtTagName("CanDestroy")) != null) {
                tag.put("CanDestroy", tag.remove(this.nbtTagName("CanDestroy")));
            } else if (tag.getListTag("CanDestroy") != null) {
                ListTag<?> old = tag.getListTag("CanDestroy");
                ListTag<StringTag> newCanDestroy = new ListTag<>(StringTag.class);

                for (Tag oldTag : old) {
                    Object value = oldTag.getValue();
                    String[] newValues = BlockIdData.fallbackReverseMapping
                        .get(value instanceof String ? Key.stripMinecraftNamespace((String)value) : null);
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanDestroy.add(new StringTag(newValue));
                        }
                    } else {
                        newCanDestroy.add(new StringTag(oldTag.getValue().toString()));
                    }
                }

                tag.put("CanDestroy", newCanDestroy);
            }
        }

        return item;
    }

    public static String getOldPluginChannelId(String newId) {
        newId = MappingData.validateNewChannel(newId);
        if (newId == null) {
            return null;
        }

        switch (newId) {
            case "minecraft:trader_list":
                return "MC|TrList";
            case "minecraft:book_open":
                return "MC|BOpen";
            case "minecraft:debug/paths":
                return "MC|DebugPath";
            case "minecraft:debug/neighbors_update":
                return "MC|DebugNeighborsUpdate";
            case "minecraft:register":
                return "REGISTER";
            case "minecraft:unregister":
                return "UNREGISTER";
            case "minecraft:brand":
                return "MC|Brand";
            case "bungeecord:main":
                return "BungeeCord";
            default:
                String mappedChannel = (String)Protocol1_13To1_12_2.MAPPINGS.getChannelMappings().inverse().get(newId);
                if (mappedChannel != null) {
                    return mappedChannel;
                } else {
                    return newId.length() > 20 ? newId.substring(0, 20) : newId;
                }
        }
    }

    public static boolean isDamageable(int id) {
        return id >= 256 && id <= 259
            || id == 261
            || id >= 267 && id <= 279
            || id >= 283 && id <= 286
            || id >= 290 && id <= 294
            || id >= 298 && id <= 317
            || id == 346
            || id == 359
            || id == 398
            || id == 442
            || id == 443;
    }
}
