package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import java.util.UUID;

public final class ModifierData {
    public static final Type<ModifierData> TYPE = new Type<ModifierData>(ModifierData.class) {
        public ModifierData read(ByteBuf buffer) throws Exception {
            UUID uuid = Type.UUID.read(buffer);
            String name = Type.STRING.read(buffer);
            double amount = buffer.readDouble();
            int operation = Type.VAR_INT.readPrimitive(buffer);
            return new ModifierData(uuid, name, amount, operation);
        }

        public void write(ByteBuf buffer, ModifierData value) throws Exception {
            Type.UUID.write(buffer, value.uuid);
            Type.STRING.write(buffer, value.name);
            buffer.writeDouble(value.amount);
            Type.VAR_INT.writePrimitive(buffer, value.operation);
        }
    };
    private final UUID uuid;
    private final String name;
    private final double amount;
    private final int operation;

    public ModifierData(UUID uuid, String name, double amount, int operation) {
        this.uuid = uuid;
        this.name = name;
        this.amount = amount;
        this.operation = operation;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public double amount() {
        return this.amount;
    }

    public int operation() {
        return this.operation;
    }
}
