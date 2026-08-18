package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class ToolProperties {
    public static final Type<ToolProperties> TYPE = new Type<ToolProperties>(ToolProperties.class) {
        public ToolProperties read(ByteBuf buffer) throws Exception {
            ToolRule[] rules = ToolRule.ARRAY_TYPE.read(buffer);
            float defaultMiningSpeed = buffer.readFloat();
            int damagePerBlock = Type.VAR_INT.readPrimitive(buffer);
            return new ToolProperties(rules, defaultMiningSpeed, damagePerBlock);
        }

        public void write(ByteBuf buffer, ToolProperties value) throws Exception {
            ToolRule.ARRAY_TYPE.write(buffer, value.rules());
            buffer.writeFloat(value.defaultMiningSpeed());
            Type.VAR_INT.writePrimitive(buffer, value.damagePerBlock());
        }
    };
    private final ToolRule[] rules;
    private final float defaultMiningSpeed;
    private final int damagePerBlock;

    public ToolProperties(ToolRule[] rules, float defaultMiningSpeed, int damagePerBlock) {
        this.rules = rules;
        this.defaultMiningSpeed = defaultMiningSpeed;
        this.damagePerBlock = damagePerBlock;
    }

    public ToolRule[] rules() {
        return this.rules;
    }

    public float defaultMiningSpeed() {
        return this.defaultMiningSpeed;
    }

    public int damagePerBlock() {
        return this.damagePerBlock;
    }
}
