package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public class LevitationStorage implements StorableObject {
    private boolean active;
    private int amplifier;

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }
}
