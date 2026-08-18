package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import java.util.HashMap;
import java.util.Map;

public final class CookieStorage implements StorableObject {
    private final Map<String, byte[]> cookies = new HashMap<>();

    public Map<String, byte[]> cookies() {
        return this.cookies;
    }

    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }
}
