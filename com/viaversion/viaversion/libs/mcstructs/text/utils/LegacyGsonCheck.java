package com.viaversion.viaversion.libs.mcstructs.text.utils;

import com.viaversion.viaversion.libs.gson.JsonSyntaxException;
import com.viaversion.viaversion.libs.gson.stream.MalformedJsonException;

public class LegacyGsonCheck {
    private static final String JSON_EXCEPTION = "Use JsonReader.setLenient(true) to accept malformed JSON";

    public static void check(String json, boolean lenient) {
        if (!lenient) {
            char c = nextNonWhitespace(json);
            if (c != ']' && c != ';' && c != ',' && c != '[' && c != '{') {
                throw new JsonSyntaxException(
                    new MalformedJsonException("Use JsonReader.setLenient(true) to accept malformed JSON")
                );
            }
        }
    }

    private static char nextNonWhitespace(String s) {
        char[] chars = s.toCharArray();
        int i = 0;

        while (i < chars.length) {
            char c = chars[i++];
            if (c != '\n' && c != ' ' && c != '\r' && c != '\t') {
                if (c != '/' && c != '#') {
                    return c;
                }

                throw new JsonSyntaxException(
                    new MalformedJsonException("Use JsonReader.setLenient(true) to accept malformed JSON")
                );
            }
        }

        return '{';
    }
}
