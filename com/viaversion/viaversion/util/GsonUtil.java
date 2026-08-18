package com.viaversion.viaversion.util;

import com.viaversion.viaversion.libs.gson.Gson;
import com.viaversion.viaversion.libs.gson.GsonBuilder;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils;
import java.util.Comparator;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GsonUtil {
    private static final Gson GSON = new GsonBuilder().create();

    public static Gson getGson() {
        return GSON;
    }

    public static @Nullable JsonElement sort(@Nullable JsonElement object) {
        return JsonUtils.sort(object, Comparator.naturalOrder());
    }
}
