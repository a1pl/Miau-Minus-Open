package com.viaversion.viaversion.rewriter;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends Protocol<C, ?, ?, S>>
    extends ItemRewriter<C, S, T> {
    public StructuredItemRewriter(
        T protocol,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    public StructuredItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType, itemType, itemArrayType);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        MappingData mappingData = this.protocol.getMappingData();
        if (mappingData != null) {
            if (mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getNewItemId(item.identifier()));
            }

            if (mappingData.getDataComponentSerializerMappings() != null) {
                item.structuredData().setIdLookup(this.protocol, true);
            }
        }

        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        MappingData mappingData = this.protocol.getMappingData();
        if (mappingData != null) {
            if (mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getOldItemId(item.identifier()));
            }

            if (mappingData.getDataComponentSerializerMappings() != null) {
                item.structuredData().setIdLookup(this.protocol, false);
            }
        }

        return item;
    }
}
