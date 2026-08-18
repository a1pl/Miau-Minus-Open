package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BackwardsItemRewriterBase<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends BackwardsProtocol<C, ?, ?, S>>
    extends ItemRewriter<C, S, T> {
    protected final boolean jsonNameFormat;

    protected BackwardsItemRewriterBase(
        T protocol,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType,
        boolean jsonFormat
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
        this.jsonNameFormat = jsonFormat;
    }

    protected BackwardsItemRewriterBase(
        T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, boolean jsonNameFormat
    ) {
        this(protocol, itemType, itemArrayType, itemType, itemArrayType, jsonNameFormat);
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        this.restoreDisplayTag(item);
        return item;
    }

    protected boolean hasBackupTag(CompoundTag tag, String tagName) {
        return tag.contains(this.nbtTagName(tagName));
    }

    protected void saveStringTag(CompoundTag tag, StringTag original, String name) {
        String backupName = this.nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.putString(backupName, original.getValue());
        }
    }

    protected void saveListTag(CompoundTag tag, ListTag<?> original, String name) {
        String backupName = this.nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.put(backupName, original.copy());
        }
    }

    protected void saveGenericTagList(CompoundTag tag, List<Tag> original, String name) {
        String backupName = this.nbtTagName(name);
        if (!tag.contains(backupName)) {
            CompoundTag output = new CompoundTag();

            for (int i = 0; i < original.size(); i++) {
                output.put(Integer.toString(i), original.get(i));
            }

            tag.put(backupName, output);
        }
    }

    protected List<Tag> removeGenericTagList(CompoundTag tag, String name) {
        String backupName = this.nbtTagName(name);
        CompoundTag data = tag.getCompoundTag(backupName);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return new ArrayList<>(data.values());
    }

    protected void restoreDisplayTag(Item item) {
        if (item.tag() != null) {
            CompoundTag display = item.tag().getCompoundTag("display");
            if (display != null) {
                if (display.remove(this.nbtTagName("customName")) != null) {
                    display.remove("Name");
                } else {
                    this.restoreStringTag(display, "Name");
                }

                this.restoreListTag(display, "Lore");
            }
        }
    }

    protected void restoreStringTag(CompoundTag tag, String tagName) {
        Tag original = tag.remove(this.nbtTagName(tagName));
        if (original instanceof StringTag) {
            tag.putString(tagName, ((StringTag)original).getValue());
        }
    }

    protected void restoreListTag(CompoundTag tag, String tagName) {
        Tag original = tag.remove(this.nbtTagName(tagName));
        if (original instanceof ListTag) {
            tag.put(tagName, ((ListTag)original).copy());
        }
    }

    public <T extends Tag> @Nullable ListTag<T> removeListTag(CompoundTag tag, String tagName, Class<T> tagType) {
        String backupName = this.nbtTagName(tagName);
        ListTag<T> data = tag.getListTag(backupName, tagType);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return data;
    }

    @Override
    public String nbtTagName() {
        return "VB|" + this.protocol.getClass().getSimpleName();
    }
}
