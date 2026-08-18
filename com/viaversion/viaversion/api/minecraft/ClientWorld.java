package com.viaversion.viaversion.api.minecraft;

import com.viaversion.viaversion.api.connection.StorableObject;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientWorld implements StorableObject {
    private Environment environment;

    public ClientWorld() {
    }

    public ClientWorld(Environment environment) {
        this.environment = environment;
    }

    public @Nullable Environment getEnvironment() {
        return this.environment;
    }

    public void setEnvironment(int environmentId) {
        this.environment = Environment.getEnvironmentById(environmentId);
    }
}
