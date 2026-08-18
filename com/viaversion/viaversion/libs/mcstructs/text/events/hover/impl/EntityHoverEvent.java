package com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class EntityHoverEvent extends AHoverEvent {
    private Identifier entityType;
    private UUID uuid;
    @Nullable
    private ATextComponent name;

    public EntityHoverEvent(HoverEventAction action, Identifier entityType, UUID uuid, @Nullable ATextComponent name) {
        super(action);
        this.entityType = entityType;
        this.uuid = uuid;
        this.name = name;
    }

    public Identifier getEntityType() {
        return this.entityType;
    }

    public EntityHoverEvent setEntityType(Identifier entityType) {
        this.entityType = entityType;
        return this;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public EntityHoverEvent setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    @Nullable
    public ATextComponent getName() {
        return this.name;
    }

    public EntityHoverEvent setName(@Nullable ATextComponent name) {
        this.name = name;
        return this;
    }

    @Override
    public TextHoverEvent toLegacy(TextComponentSerializer textComponentSerializer, SNbtSerializer<?> sNbtSerializer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", this.entityType.getValue());
        tag.putString("id", this.uuid.toString());
        tag.putString(
            "name", textComponentSerializer.serialize(this.name == null ? new StringComponent("") : this.name)
        );

        try {
            return new TextHoverEvent(this.getAction(), new StringComponent(sNbtSerializer.serialize(tag)));
        } catch (SNbtSerializeException e) {
            throw new RuntimeException("This should never happen! Please report to the developer immediately!", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            EntityHoverEvent that = (EntityHoverEvent)o;
            return Objects.equals(this.entityType, that.entityType)
                && Objects.equals(this.uuid, that.uuid)
                && Objects.equals(this.name, that.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityType, this.uuid, this.name);
    }

    @Override
    public String toString() {
        return ToString.of(this)
            .add("action", this.action)
            .add("entityType", this.entityType)
            .add("uuid", this.uuid)
            .add("name", this.name, Objects::nonNull)
            .toString();
    }
}
