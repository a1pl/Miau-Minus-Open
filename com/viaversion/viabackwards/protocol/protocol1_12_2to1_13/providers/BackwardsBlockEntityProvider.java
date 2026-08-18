package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.BannerHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.BedHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.FlowerPotHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.PistonHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.SkullHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.SpawnerHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.platform.providers.Provider;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import java.util.HashMap;
import java.util.Map;

public class BackwardsBlockEntityProvider implements Provider {
    private final Map<String, BackwardsBlockEntityProvider.BackwardsBlockEntityHandler> handlers = new HashMap<>();

    public BackwardsBlockEntityProvider() {
        this.handlers.put("minecraft:flower_pot", new FlowerPotHandler());
        this.handlers.put("minecraft:bed", new BedHandler());
        this.handlers.put("minecraft:banner", new BannerHandler());
        this.handlers.put("minecraft:skull", new SkullHandler());
        this.handlers.put("minecraft:mob_spawner", new SpawnerHandler());
        this.handlers.put("minecraft:piston", new PistonHandler());
    }

    public boolean isHandled(String key) {
        return this.handlers.containsKey(key);
    }

    public CompoundTag transform(UserConnection user, Position position, CompoundTag tag) throws Exception {
        StringTag idTag = tag.getStringTag("id");
        if (idTag == null) {
            return tag;
        }

        String id = idTag.getValue();
        BackwardsBlockEntityProvider.BackwardsBlockEntityHandler handler = this.handlers.get(id);
        if (handler == null) {
            return tag;
        }

        BackwardsBlockStorage storage = user.get(BackwardsBlockStorage.class);
        Integer blockId = storage.get(position);
        return blockId == null ? tag : handler.transform(blockId, tag);
    }

    public CompoundTag transform(UserConnection user, Position position, String id) throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("x", Math.toIntExact(position.x()));
        tag.putInt("y", Math.toIntExact(position.y()));
        tag.putInt("z", Math.toIntExact(position.z()));
        return this.transform(user, position, tag);
    }

    @FunctionalInterface
    public interface BackwardsBlockEntityHandler {
        CompoundTag transform(int var1, CompoundTag var2);
    }
}
