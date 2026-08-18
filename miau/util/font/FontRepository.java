package miau.util.font;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import miau.util.font.impl.minecraft.MinecraftFontRenderer;
import miau.util.font.impl.rise.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class FontRepository {
    private static final Map<String, Font> RENDERER_CACHE = new HashMap<>();
    private static final String FONT_PATH = "assets/keystrokesmod/fonts/";
    private static final String RESOURCE_PREFIX = "keystrokesmod:fonts/";
    public static final String[] FONT_NAMES = new String[]{
        "Minecraft",
        "brcobane-regular",
        "brcobane-medium",
        "brcobane-bold",
        "brcobane-semibold",
        "comfortaa-regular",
        "comfortaa-medium",
        "comfortaa-bold",
        "comfortaa-semibold",
        "geistsans-regular",
        "geistsans-medium",
        "geistsans-bold",
        "geistsans-semibold",
        "greycliffcf-regular",
        "greycliffcf-medium",
        "greycliffcf-bold",
        "greycliffcf-semibold",
        "inter-regular",
        "inter-medium",
        "inter-bold",
        "inter-semibold",
        "manrope-regular",
        "manrope-medium",
        "manrope-bold",
        "manrope-semibold",
        "materialicons-regular",
        "materialicons-outlined",
        "materialsymbolsoutlined",
        "productsans-regular",
        "productsans-medium",
        "productsans-bold",
        "productsans-semibold",
        "rubik-regular",
        "rubik-bold",
        "sfuidisplay-regular",
        "sfuidisplay-medium",
        "sfuidisplay-bold",
        "sfuidisplay-semibold",
        "sourcesans3-regular",
        "sourcesans3-medium",
        "sourcesans3-bold",
        "sourcesans3-semibold",
        "tahoma-regular",
        "tahoma-bold",
        "ubuntusans-regular",
        "ubuntusans-medium",
        "ubuntusans-bold",
        "ubuntusans-semibold",
        "augustus"
    };
    private static int currentHudFace = 0;
    private static int currentGuiFace = 0;

    public static Font getFont(String name) {
        return getFont(name, 18.0F);
    }

    public static Font getFont(String name, float size) {
        String key = name + "@" + (int)size;
        Font cached = RENDERER_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            java.awt.Font awt = loadAwtFont(name, size);
            if (awt != null) {
                boolean smooth = name.toLowerCase().startsWith("material");
                Font renderer = new FontRenderer(awt, true, true, smooth);
                RENDERER_CACHE.put(key, renderer);
                return renderer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new MinecraftFontRenderer();
    }

    public static Font getHudFont(int size) {
        if (isMinecraftSelected()) {
            return new MinecraftFontRenderer();
        }

        String name = getHudFontName();
        return name.isEmpty() ? new MinecraftFontRenderer() : getFont(name, size);
    }

    public static Font getMinecraftFont() {
        return new MinecraftFontRenderer();
    }

    public static Font getClickGuiFont(float size) {
        if (currentGuiFace <= 0) {
            return new MinecraftFontRenderer();
        }

        String name = getGuiFaceName();
        return name.isEmpty() ? new MinecraftFontRenderer() : getFont(name, size);
    }

    public static Font getClickGuiFont() {
        return getClickGuiFont(18.0F);
    }

    public static void setGuiFace(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < FONT_NAMES.length) {
            currentGuiFace = faceIndex;
            RENDERER_CACHE.clear();
        }
    }

    public static int getGuiFace() {
        return currentGuiFace;
    }

    public static String getGuiFaceName() {
        return currentGuiFace == 0 ? "" : FONT_NAMES[currentGuiFace];
    }

    public static boolean isGuiMinecraftSelected() {
        return currentGuiFace == 0;
    }

    public static void setHudFace(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < FONT_NAMES.length) {
            currentHudFace = faceIndex;
            RENDERER_CACHE.clear();
        }
    }

    public static int getHudFace() {
        return currentHudFace;
    }

    public static String getHudFontName() {
        return currentHudFace == 0 ? "" : FONT_NAMES[currentHudFace];
    }

    public static boolean isMinecraftSelected() {
        return currentHudFace == 0;
    }

    public static void clearCache() {
        RENDERER_CACHE.clear();
    }

    private static java.awt.Font loadAwtFont(String name, float size) throws Exception {
        String fileName = name.endsWith(".ttf") ? name : name + ".ttf";
        InputStream is = null;

        try {
            ResourceLocation loc = new ResourceLocation("keystrokesmod:fonts/" + fileName);
            is = Minecraft.func_71410_x().func_110442_L().func_110536_a(loc).func_110527_b();
        } catch (Exception var5) {
        }

        if (is == null) {
            is = FontRepository.class.getClassLoader().getResourceAsStream("assets/keystrokesmod/fonts/" + fileName);
        }

        return is == null ? null : java.awt.Font.createFont(0, is).deriveFont(size);
    }

    private FontRepository() {
    }
}
