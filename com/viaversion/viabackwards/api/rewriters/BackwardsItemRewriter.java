package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends BackwardsProtocol<C, ?, ?, S>>
    extends BackwardsItemRewriterBase<C, S, T> {
    public BackwardsItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType, true);
    }

    public BackwardsItemRewriter(
        T protocol,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, true);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag display = item.tag() != null ? item.tag().getCompoundTag("display") : null;
        if (this.protocol.getTranslatableRewriter() != null && display != null) {
            StringTag name = display.getStringTag("Name");
            if (name != null) {
                String newValue = this.protocol
                    .getTranslatableRewriter()
                    .processText(connection, name.getValue())
                    .toString();
                if (!newValue.equals(name.getValue())) {
                    this.saveStringTag(display, name, "Name");
                }

                name.setValue(newValue);
            }

            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore != null) {
                boolean changed = false;

                for (StringTag loreEntry : lore) {
                    String newValue = this.protocol
                        .getTranslatableRewriter()
                        .processText(connection, loreEntry.getValue())
                        .toString();
                    if (!changed && !newValue.equals(loreEntry.getValue())) {
                        changed = true;
                        this.saveListTag(display, lore, "Lore");
                    }

                    loreEntry.setValue(newValue);
                }
            }
        }

        MappedItem data = this.protocol.getMappingData() != null
            ? this.protocol.getMappingData().getMappedItem(item.identifier())
            : null;
        if (data == null) {
            return super.handleItemToClient(connection, item);
        }

        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }

        item.tag().putInt(this.nbtTagName("id"), item.identifier());
        item.setIdentifier(data.id());
        if (data.customModelData() != null && !item.tag().contains("CustomModelData")) {
            item.tag().putInt("CustomModelData", data.customModelData());
        }

        if (display == null) {
            item.tag().put("display", display = new CompoundTag());
        }

        if (!display.contains("Name")) {
            display.put("Name", new StringTag(data.jsonName()));
            display.put(this.nbtTagName("customName"), new ByteTag());
        }

        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        if (item.tag() != null) {
            Tag originalId = item.tag().remove(this.nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag)originalId).asInt());
            }
        }

        return item;
    }

    @Override
    public void registerAdvancements(C packetType) {
        this.protocol
            .registerClientbound(
                packetType,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.handler(
                            wrapper -> {
                                wrapper.passthrough(Type.BOOLEAN);
                                int size = wrapper.passthrough(Type.VAR_INT);

                                for (int i = 0; i < size; i++) {
                                    wrapper.passthrough(Type.STRING);
                                    wrapper.passthrough(Type.OPTIONAL_STRING);
                                    if (wrapper.passthrough(Type.BOOLEAN)) {
                                        JsonElement title = wrapper.passthrough(Type.COMPONENT);
                                        JsonElement description = wrapper.passthrough(Type.COMPONENT);
                                        TranslatableRewriter<C> translatableRewriter = BackwardsItemRewriter.this.protocol
                                            .getTranslatableRewriter();
                                        if (translatableRewriter != null) {
                                            translatableRewriter.processText(wrapper.user(), title);
                                            translatableRewriter.processText(wrapper.user(), description);
                                        }

                                        Item icon = BackwardsItemRewriter.this.handleItemToClient(
                                            wrapper.user(), wrapper.read(BackwardsItemRewriter.this.itemType())
                                        );
                                        wrapper.write(BackwardsItemRewriter.this.mappedItemType(), icon);
                                        wrapper.passthrough(Type.VAR_INT);
                                        int flags = wrapper.passthrough(Type.INT);
                                        if ((flags & 1) != 0) {
                                            wrapper.passthrough(Type.STRING);
                                        }

                                        wrapper.passthrough(Type.FLOAT);
                                        wrapper.passthrough(Type.FLOAT);
                                    }

                                    wrapper.passthrough(Type.STRING_ARRAY);
                                    int arrayLength = wrapper.passthrough(Type.VAR_INT);

                                    for (int array = 0; array < arrayLength; array++) {
                                        wrapper.passthrough(Type.STRING_ARRAY);
                                    }
                                }
                            }
                        );
                    }
                }
            );
    }

    @Override
    public void registerAdvancements1_20_3(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    Tag title = wrapper.passthrough(Type.TAG);
                    Tag description = wrapper.passthrough(Type.TAG);
                    TranslatableRewriter<C> translatableRewriter = this.protocol.getTranslatableRewriter();
                    if (translatableRewriter != null) {
                        translatableRewriter.processTag(wrapper.user(), title);
                        translatableRewriter.processTag(wrapper.user(), description);
                    }

                    Item icon = this.handleItemToClient(wrapper.user(), wrapper.read(this.itemType()));
                    wrapper.write(this.mappedItemType(), icon);
                    wrapper.passthrough(Type.VAR_INT);
                    int flags = wrapper.passthrough(Type.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING);
                    }

                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                }

                int requirements = wrapper.passthrough(Type.VAR_INT);

                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY);
                }

                wrapper.passthrough(Type.BOOLEAN);
            }
        });
    }
}
