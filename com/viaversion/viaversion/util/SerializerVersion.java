package com.viaversion.viaversion.util;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtDeserializeException;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentCodec;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

public enum SerializerVersion {
    V1_6(TextComponentSerializer.V1_6, null),
    V1_7(TextComponentSerializer.V1_7, SNbtSerializer.V1_7),
    V1_8(TextComponentSerializer.V1_8, SNbtSerializer.V1_8),
    V1_9(TextComponentSerializer.V1_9, SNbtSerializer.V1_8),
    V1_12(TextComponentSerializer.V1_12, SNbtSerializer.V1_12),
    V1_13(TextComponentSerializer.V1_12, SNbtSerializer.V1_13),
    V1_14(TextComponentSerializer.V1_14, SNbtSerializer.V1_14),
    V1_15(TextComponentSerializer.V1_15, SNbtSerializer.V1_14),
    V1_16(TextComponentSerializer.V1_16, SNbtSerializer.V1_14),
    V1_17(TextComponentSerializer.V1_17, SNbtSerializer.V1_14),
    V1_18(TextComponentSerializer.V1_18, SNbtSerializer.V1_14),
    V1_19_4(TextComponentSerializer.V1_19_4, SNbtSerializer.V1_14),
    V1_20_3(TextComponentCodec.V1_20_3, SNbtSerializer.V1_14),
    V1_20_5(TextComponentCodec.V1_20_5, SNbtSerializer.V1_14);

    final TextComponentSerializer jsonSerializer;
    final SNbtSerializer<? extends Tag> snbtSerializer;
    final TextComponentCodec codec;

    SerializerVersion(TextComponentSerializer jsonSerializer, SNbtSerializer<? extends Tag> snbtSerializer) {
        this.jsonSerializer = jsonSerializer;
        this.snbtSerializer = snbtSerializer;
        this.codec = null;
    }

    SerializerVersion(TextComponentCodec codec, SNbtSerializer<? extends Tag> snbtSerializer) {
        this.codec = codec;
        this.jsonSerializer = codec.asSerializer();
        this.snbtSerializer = snbtSerializer;
    }

    public String toString(ATextComponent component) {
        return this.jsonSerializer.serialize(component);
    }

    public JsonElement toJson(ATextComponent component) {
        return this.jsonSerializer.serializeJson(component);
    }

    public Tag toTag(ATextComponent component) {
        if (this.codec == null) {
            throw new IllegalStateException("Cannot convert component to NBT with this version");
        } else {
            return this.codec.serializeNbt(component);
        }
    }

    public ATextComponent toComponent(JsonElement json) {
        return this.jsonSerializer.deserialize(json);
    }

    public ATextComponent toComponent(String json) {
        if (this.ordinal() >= V1_20_3.ordinal()) {
            return this.jsonSerializer.deserializeParser(json);
        } else {
            return this.ordinal() >= V1_9.ordinal()
                ? this.jsonSerializer.deserializeReader(json)
                : this.jsonSerializer.deserialize(json);
        }
    }

    public ATextComponent toComponent(Tag tag) {
        if (this.codec == null) {
            throw new IllegalStateException("Cannot convert NBT to component with this version");
        } else {
            return this.codec.deserializeNbtTree(tag);
        }
    }

    public Tag toTag(String snbt) {
        if (this.snbtSerializer == null) {
            throw new IllegalStateException("Cannot convert SNBT to NBT with this version");
        }

        try {
            return this.snbtSerializer.deserialize(snbt);
        } catch (SNbtDeserializeException e) {
            throw new RuntimeException(e);
        }
    }

    public String toSNBT(Tag tag) {
        if (this.snbtSerializer == null) {
            throw new IllegalStateException("Cannot convert SNBT to NBT with this version");
        }

        try {
            return this.snbtSerializer.serialize(tag);
        } catch (SNbtSerializeException e) {
            throw new RuntimeException(e);
        }
    }
}
