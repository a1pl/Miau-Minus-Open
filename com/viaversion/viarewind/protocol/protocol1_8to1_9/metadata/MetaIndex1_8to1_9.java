package com.viaversion.viarewind.protocol.protocol1_8to1_9.metadata;

import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.metadata.MetaIndex;
import com.viaversion.viaversion.util.Pair;
import java.util.HashMap;
import java.util.Optional;

public class MetaIndex1_8to1_9 {
    private static final HashMap<Pair<EntityTypes1_10.EntityType, Integer>, MetaIndex> metadataRewrites = new HashMap<>();

    private static Optional<MetaIndex> getIndex(EntityType type, int index) {
        Pair<EntityType, Integer> pair = new Pair<>(type, index);
        return metadataRewrites.containsKey(pair) ? Optional.of(metadataRewrites.get(pair)) : Optional.empty();
    }

    public static MetaIndex searchIndex(EntityType type, int index) {
        EntityType currentType = type;

        do {
            Optional<MetaIndex> optMeta = getIndex(currentType, index);
            if (optMeta.isPresent()) {
                return optMeta.get();
            }

            currentType = currentType.getParent();
        } while (currentType != null);

        return null;
    }

    static {
        for (MetaIndex index : MetaIndex.values()) {
            metadataRewrites.put(new Pair<>(index.getClazz(), index.getNewIndex()), index);
        }
    }
}
