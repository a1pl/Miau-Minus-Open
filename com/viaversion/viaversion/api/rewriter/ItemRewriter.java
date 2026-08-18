package com.viaversion.viaversion.api.rewriter;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ItemRewriter<T extends Protocol> extends Rewriter<T> {
    @Nullable Item handleItemToClient(UserConnection var1, @Nullable Item var2);

    @Nullable Item handleItemToServer(UserConnection var1, @Nullable Item var2);

    default @Nullable Type<Item> itemType() {
        return null;
    }

    default @Nullable Type<Item[]> itemArrayType() {
        return null;
    }

    default @Nullable Type<Item> mappedItemType() {
        return this.itemType();
    }

    default @Nullable Type<Item[]> mappedItemArrayType() {
        return this.itemArrayType();
    }

    default String nbtTagName() {
        return "VV|" + this.protocol().getClass().getSimpleName();
    }

    default String nbtTagName(String nbt) {
        return this.nbtTagName() + "|" + nbt;
    }
}
