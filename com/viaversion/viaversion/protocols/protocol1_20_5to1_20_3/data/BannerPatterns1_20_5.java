package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BannerPatterns1_20_5 {
    private static final KeyMappings PATTERNS = new KeyMappings(
        "base",
        "square_bottom_left",
        "square_bottom_right",
        "square_top_left",
        "square_top_right",
        "stripe_bottom",
        "stripe_top",
        "stripe_left",
        "stripe_right",
        "stripe_center",
        "stripe_middle",
        "stripe_downright",
        "stripe_downleft",
        "small_stripes",
        "cross",
        "straight_cross",
        "triangle_bottom",
        "triangle_top",
        "triangles_bottom",
        "triangles_top",
        "diagonal_left",
        "diagonal_up_right",
        "diagonal_up_left",
        "diagonal_right",
        "circle",
        "rhombus",
        "half_vertical",
        "half_horizontal",
        "half_vertical_right",
        "half_horizontal_bottom",
        "border",
        "curly_border",
        "gradient",
        "gradient_up",
        "bricks",
        "globe",
        "creeper",
        "skull",
        "flower",
        "mojang",
        "piglin",
        "flow",
        "guster"
    );
    private static final Map<String, String> PATTERN_IDS = new HashMap<>();

    public static @Nullable String idToKey(int id) {
        return PATTERNS.idToKey(id);
    }

    public static int keyToId(String pattern) {
        return PATTERNS.keyToId(pattern);
    }

    public static @Nullable String compactToFullId(String compactId) {
        return PATTERN_IDS.get(compactId);
    }

    public static @Nullable String fullIdToCompact(String fullId) {
        for (Entry<String, String> entry : PATTERN_IDS.entrySet()) {
            if (entry.getValue().equals(fullId)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static String[] keys() {
        return PATTERNS.keys();
    }

    static {
        PATTERN_IDS.put("b", "base");
        PATTERN_IDS.put("bl", "square_bottom_left");
        PATTERN_IDS.put("br", "square_bottom_right");
        PATTERN_IDS.put("tl", "square_top_left");
        PATTERN_IDS.put("tr", "square_top_right");
        PATTERN_IDS.put("bs", "stripe_bottom");
        PATTERN_IDS.put("ts", "stripe_top");
        PATTERN_IDS.put("ls", "stripe_left");
        PATTERN_IDS.put("rs", "stripe_right");
        PATTERN_IDS.put("cs", "stripe_center");
        PATTERN_IDS.put("ms", "stripe_middle");
        PATTERN_IDS.put("drs", "stripe_downright");
        PATTERN_IDS.put("dls", "stripe_downleft");
        PATTERN_IDS.put("ss", "small_stripes");
        PATTERN_IDS.put("cr", "cross");
        PATTERN_IDS.put("sc", "straight_cross");
        PATTERN_IDS.put("bt", "triangle_bottom");
        PATTERN_IDS.put("tt", "triangle_top");
        PATTERN_IDS.put("bts", "triangles_bottom");
        PATTERN_IDS.put("tts", "triangles_top");
        PATTERN_IDS.put("ld", "diagonal_left");
        PATTERN_IDS.put("rd", "diagonal_up_right");
        PATTERN_IDS.put("lud", "diagonal_up_left");
        PATTERN_IDS.put("rud", "diagonal_right");
        PATTERN_IDS.put("mc", "circle");
        PATTERN_IDS.put("mr", "rhombus");
        PATTERN_IDS.put("vh", "half_vertical");
        PATTERN_IDS.put("hh", "half_horizontal");
        PATTERN_IDS.put("vhr", "half_vertical_right");
        PATTERN_IDS.put("hhb", "half_horizontal_bottom");
        PATTERN_IDS.put("bo", "border");
        PATTERN_IDS.put("cbo", "curly_border");
        PATTERN_IDS.put("gra", "gradient");
        PATTERN_IDS.put("gru", "gradient_up");
        PATTERN_IDS.put("bri", "bricks");
        PATTERN_IDS.put("glb", "globe");
        PATTERN_IDS.put("cre", "creeper");
        PATTERN_IDS.put("sku", "skull");
        PATTERN_IDS.put("flo", "flower");
        PATTERN_IDS.put("moj", "mojang");
        PATTERN_IDS.put("pig", "piglin");
    }
}
