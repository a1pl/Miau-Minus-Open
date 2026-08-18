package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.misc.HolderType;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import io.netty.buffer.ByteBuf;

public final class ArmorTrimMaterial {
    public static final HolderType<ArmorTrimMaterial> TYPE = new HolderType<ArmorTrimMaterial>() {
        public ArmorTrimMaterial readDirect(ByteBuf buffer) throws Exception {
            String assetName = Type.STRING.read(buffer);
            int item = Type.VAR_INT.readPrimitive(buffer);
            float itemModelIndex = buffer.readFloat();
            int overrideArmorMaterialsSize = Type.VAR_INT.readPrimitive(buffer);
            Int2ObjectMap<String> overrideArmorMaterials = new Int2ObjectOpenHashMap<>(overrideArmorMaterialsSize);

            for (int i = 0; i < overrideArmorMaterialsSize; i++) {
                int key = Type.VAR_INT.readPrimitive(buffer);
                String value = Type.STRING.read(buffer);
                overrideArmorMaterials.put(key, value);
            }

            Tag description = Type.TAG.read(buffer);
            return new ArmorTrimMaterial(assetName, item, itemModelIndex, overrideArmorMaterials, description);
        }

        public void writeDirect(ByteBuf buffer, ArmorTrimMaterial value) throws Exception {
            Type.STRING.write(buffer, value.assetName());
            Type.VAR_INT.writePrimitive(buffer, value.itemId());
            buffer.writeFloat(value.itemModelIndex());
            Type.VAR_INT.writePrimitive(buffer, value.overrideArmorMaterials().size());

            for (Int2ObjectMap.Entry<String> entry : value.overrideArmorMaterials().int2ObjectEntrySet()) {
                Type.VAR_INT.writePrimitive(buffer, entry.getIntKey());
                Type.STRING.write(buffer, entry.getValue());
            }

            Type.TAG.write(buffer, value.description());
        }
    };
    private final String assetName;
    private final int itemId;
    private final float itemModelIndex;
    private final Int2ObjectMap<String> overrideArmorMaterials;
    private final Tag description;

    public ArmorTrimMaterial(
        String assetName,
        int itemId,
        float itemModelIndex,
        Int2ObjectMap<String> overrideArmorMaterials,
        Tag description
    ) {
        this.assetName = assetName;
        this.itemId = itemId;
        this.itemModelIndex = itemModelIndex;
        this.overrideArmorMaterials = overrideArmorMaterials;
        this.description = description;
    }

    public String assetName() {
        return this.assetName;
    }

    public int itemId() {
        return this.itemId;
    }

    public float itemModelIndex() {
        return this.itemModelIndex;
    }

    public Int2ObjectMap<String> overrideArmorMaterials() {
        return this.overrideArmorMaterials;
    }

    public Tag description() {
        return this.description;
    }
}
