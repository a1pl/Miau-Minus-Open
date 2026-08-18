package com.viaversion.viarewind.api.type.item;

import com.viaversion.viarewind.api.type.Types1_7_6_10;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public class ItemType extends Type<Item> {
    public ItemType() {
        super(Item.class);
    }

    public Item read(ByteBuf buffer) throws Exception {
        short id = buffer.readShort();
        if (id < 0) {
            return null;
        }

        Item item = new DataItem();
        item.setIdentifier(id);
        item.setAmount(buffer.readByte());
        item.setData(buffer.readShort());
        item.setTag(Types1_7_6_10.COMPRESSED_NBT.read(buffer));
        return item;
    }

    public void write(ByteBuf buffer, Item item) throws Exception {
        if (item == null) {
            buffer.writeShort(-1);
        } else {
            buffer.writeShort(item.identifier());
            buffer.writeByte(item.amount());
            buffer.writeShort(item.data());
            Types1_7_6_10.COMPRESSED_NBT.write(buffer, item.tag());
        }
    }
}
