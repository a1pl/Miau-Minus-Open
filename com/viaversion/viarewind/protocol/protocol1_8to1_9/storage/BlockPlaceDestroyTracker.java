package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public class BlockPlaceDestroyTracker implements StorableObject {
    private long lastMining;

    public boolean isMining() {
        long time = System.currentTimeMillis() - this.lastMining;
        return time < 75L;
    }

    public void setMining() {
        this.lastMining = System.currentTimeMillis();
    }

    public void updateMining() {
        if (this.isMining()) {
            this.lastMining = System.currentTimeMillis();
        }
    }

    public void setLastMining(long lastMining) {
        this.lastMining = lastMining;
    }
}
