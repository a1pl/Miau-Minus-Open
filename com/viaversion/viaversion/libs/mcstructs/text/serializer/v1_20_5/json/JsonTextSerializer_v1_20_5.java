package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.json;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.json.JsonTextSerializer_v1_20_3;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class JsonTextSerializer_v1_20_5 extends JsonTextSerializer_v1_20_3 {
    public JsonTextSerializer_v1_20_5(
        Function<JsonTextSerializer_v1_20_3, ITypedSerializer<JsonElement, Style>> styleSerializer
    ) {
        super(styleSerializer);
    }

    @Override
    public boolean isNumber(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    @Override
    public boolean requiredBoolean(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing boolean for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("Expected boolean for '" + name + "' tag");
            } else {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    return primitive.getAsBoolean();
                } else {
                    throw new IllegalArgumentException("Expected boolean for '" + name + "' tag");
                }
            }
        }
    }

    @Override
    public int requiredInt(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing int for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("Expected int for '" + name + "' tag");
            } else {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                } else {
                    throw new IllegalArgumentException("Expected int for '" + name + "' tag");
                }
            }
        }
    }
}
