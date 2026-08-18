package com.viaversion.viaversion.api.minecraft;

public interface HolderSet {
    static HolderSet of(String tagKey) {
        return new HolderSetImpl(tagKey);
    }

    static HolderSet of(int[] ids) {
        return new HolderSetImpl(ids);
    }

    String tagKey();

    boolean hasTagKey();

    int[] ids();

    boolean hasIds();
}
