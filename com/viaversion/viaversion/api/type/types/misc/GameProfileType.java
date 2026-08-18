package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import java.util.UUID;

public final class GameProfileType extends Type<GameProfile> {
    public GameProfileType() {
        super(GameProfile.class);
    }

    public GameProfile read(ByteBuf buffer) throws Exception {
        String name = Type.OPTIONAL_STRING.read(buffer);
        UUID id = Type.OPTIONAL_UUID.read(buffer);
        int propertyCount = Type.VAR_INT.readPrimitive(buffer);
        GameProfile.Property[] properties = new GameProfile.Property[propertyCount];

        for (int i = 0; i < propertyCount; i++) {
            String propertyName = Type.STRING.read(buffer);
            String propertyValue = Type.STRING.read(buffer);
            String propertySignature = Type.OPTIONAL_STRING.read(buffer);
            properties[i] = new GameProfile.Property(propertyName, propertyValue, propertySignature);
        }

        return new GameProfile(name, id, properties);
    }

    public void write(ByteBuf buffer, GameProfile value) throws Exception {
        Type.OPTIONAL_STRING.write(buffer, value.name());
        Type.OPTIONAL_UUID.write(buffer, value.id());
        Type.VAR_INT.writePrimitive(buffer, value.properties().length);

        for (GameProfile.Property property : value.properties()) {
            Type.STRING.write(buffer, property.name());
            Type.STRING.write(buffer, property.value());
            Type.OPTIONAL_STRING.write(buffer, property.signature());
        }
    }
}
