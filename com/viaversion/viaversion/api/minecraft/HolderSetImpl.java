package com.viaversion.viaversion.api.minecraft;

import com.viaversion.viaversion.util.EitherImpl;

final class HolderSetImpl extends EitherImpl<String, int[]> implements HolderSet {
    HolderSetImpl(String tagKey) {
        super(tagKey, null);
    }

    HolderSetImpl(int[] ids) {
        super(null, ids);
    }

    @Override
    public String tagKey() {
        return this.left();
    }

    @Override
    public boolean hasTagKey() {
        return this.isLeft();
    }

    @Override
    public int[] ids() {
        return this.right();
    }

    @Override
    public boolean hasIds() {
        return this.isRight();
    }
}
