package com.viaversion.viaversion.api.data;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface FullMappings extends BiMappings {
    int id(String var1);

    int mappedId(String var1);

    String identifier(int var1);

    String mappedIdentifier(int var1);

    @Nullable String mappedIdentifier(String var1);

    FullMappings inverse();
}
