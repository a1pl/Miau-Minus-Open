package com.viaversion.viarewind.protocol.protocol1_8to1_9.data;

import com.viaversion.viarewind.api.data.VRMappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectArrayList;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectList;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public final class RewindMappings extends com.viaversion.viarewind.api.data.RewindMappings {
    private final ObjectList<String> sounds = new ObjectArrayList<>();

    public RewindMappings() {
        super("1.9.4", "1.8");
    }

    @Override
    protected void loadExtras(CompoundTag data) {
        super.loadExtras(data);

        for (JsonElement sound : VRMappingDataLoader.INSTANCE.loadData("sounds-1.9.json").getAsJsonArray("sounds")) {
            this.sounds.add(sound.getAsString());
        }
    }

    public String soundName(int soundId) {
        return this.sounds.get(soundId);
    }
}
