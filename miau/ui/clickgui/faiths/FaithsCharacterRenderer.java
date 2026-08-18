package miau.ui.clickgui.faiths;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import miau.Miau;
import miau.module.modules.render.ClickGUI;
import miau.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class FaithsCharacterRenderer {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static ResourceLocation characterTexture = null;
    private static ResourceLocation glowMaskTexture = null;
    private static Color hairGlowColor = null;
    private static String loadedCharName = null;
    private static float charAspect = 0.7007007F;
    private static int charRawHeight = 0;
    private static float slideProgress = 0.0F;
    private static final Map<String, ResourceLocation> characterTextureMap = new HashMap<>();
    private static final Map<String, ResourceLocation> glowMaskTextureMap = new HashMap<>();
    private static final Map<String, Color> characterGlowMap = new HashMap<>();
    private static final Map<String, Float> characterAspectMap = new HashMap<>();
    private static final Map<String, Integer> characterRawHeightMap = new HashMap<>();

    public static void resetAnimation() {
        slideProgress = 0.0F;
    }

    private static void scanAssetCharacters(List<String> list) {
        try {
            URL url = Miau.class.getResource("/assets/keystrokesmod/textures/gui/faiths/");
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    File folder = new File(url.toURI());
                    File[] files = folder.listFiles(
                        (d, name) -> name.toLowerCase().endsWith(".png")
                            || name.toLowerCase().endsWith(".jpg")
                            || name.toLowerCase().endsWith(".jpeg")
                    );
                    if (files != null) {
                        for (File f : files) {
                            String name = f.getName();
                            int dot = name.lastIndexOf(46);
                            if (dot > 0) {
                                String charName = name.substring(0, dot).toLowerCase();
                                if (!list.contains(charName)) {
                                    list.add(charName);
                                }
                            }
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    String path = url.getPath();
                    int bang = path.indexOf("!");
                    if (bang != -1) {
                        String jarPath = path.substring(5, bang);
                        JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

                        try {
                            Enumeration<JarEntry> entries = jar.entries();
                            String prefix = "assets/keystrokesmod/textures/gui/faiths/";

                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String entryName = entry.getName();
                                if (entryName.startsWith(prefix) && !entry.isDirectory()) {
                                    String sub = entryName.substring(prefix.length());
                                    int dot = sub.lastIndexOf(46);
                                    if (dot > 0) {
                                        String charName = sub.substring(0, dot).toLowerCase();
                                        if (!list.contains(charName)) {
                                            list.add(charName);
                                        }
                                    }
                                }
                            }
                        } catch (Throwable var14) {
                            try {
                                jar.close();
                            } catch (Throwable var13) {
                                var14.addSuppressed(var13);
                            }

                            throw var14;
                        }

                        jar.close();
                    }
                }
            }
        } catch (Exception var15) {
        }
    }

    public static List<String> getAvailableCharacters() {
        List<String> list = new ArrayList<>();
        scanAssetCharacters(list);
        File dir = new File("./config/Miau/characters/");
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            File[] files = dir.listFiles(
                (d, namex) -> namex.toLowerCase().endsWith(".png")
                    || namex.toLowerCase().endsWith(".jpg")
                    || namex.toLowerCase().endsWith(".jpeg")
            );
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    int dot = name.lastIndexOf(46);
                    if (dot > 0) {
                        String charName = name.substring(0, dot).toLowerCase();
                        if (!list.contains(charName)) {
                            list.add(charName);
                        }
                    }
                }
            }
        }

        if (list.isEmpty()) {
            list.add("character");
        }

        return list;
    }

    public static String[] getCharacterArray() {
        List<String> list = getAvailableCharacters();
        return list.toArray(new String[0]);
    }

    private static Color extractHairGlowColor(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int endY = (int)(height * 0.35);
        long totalR = 0L;
        long totalG = 0L;
        long totalB = 0L;
        int count = 0;
        float maxVibrancy = -1.0F;
        Color vibrantColor = null;

        for (int y = 0; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);
                int alpha = argb >>> 24 & 0xFF;
                if (alpha >= 180) {
                    int r = argb >> 16 & 0xFF;
                    int g = argb >> 8 & 0xFF;
                    int b = argb & 0xFF;
                    float[] hsb = Color.RGBtoHSB(r, g, b, null);
                    float saturation = hsb[1];
                    float brightness = hsb[2];
                    if (brightness > 0.2F) {
                        totalR += r;
                        totalG += g;
                        totalB += b;
                        count++;
                        float vibrancy = saturation * brightness;
                        if (vibrancy > maxVibrancy) {
                            maxVibrancy = vibrancy;
                            vibrantColor = new Color(r, g, b);
                        }
                    }
                }
            }
        }

        if (vibrantColor != null && maxVibrancy > 0.2F) {
            return vibrantColor;
        } else {
            return count > 0
                ? new Color((int)(totalR / count), (int)(totalG / count), (int)(totalB / count))
                : new Color(180, 140, 255);
        }
    }

    private static void loadTexture(String charName) {
        String key = charName != null ? charName.toLowerCase() : "character";
        if (!key.equalsIgnoreCase(loadedCharName) || characterTexture == null || glowMaskTexture == null) {
            loadedCharName = key;
            if (characterTextureMap.containsKey(key)) {
                characterTexture = characterTextureMap.get(key);
                glowMaskTexture = glowMaskTextureMap.get(key);
                hairGlowColor = characterGlowMap.get(key);
                Float aspect = characterAspectMap.get(key);
                if (aspect != null) {
                    charAspect = aspect;
                }

                Integer rh = characterRawHeightMap.get(key);
                if (rh != null) {
                    charRawHeight = rh;
                }
            } else {
                BufferedImage src = null;
                File dir = new File("./config/Miau/characters/");
                if (dir.exists() && dir.isDirectory()) {
                    File[] matches = dir.listFiles(
                        (d, name) -> {
                            String n = name.toLowerCase();
                            return n.startsWith(loadedCharName + ".")
                                && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg"));
                        }
                    );
                    if (matches != null && matches.length > 0) {
                        try {
                            src = ImageIO.read(matches[0]);
                        } catch (Exception var15) {
                        }
                    }
                }

                if (src == null) {
                    String[] extensions = new String[]{".png", ".jpg", ".jpeg"};

                    for (String ext : extensions) {
                        String assetPath = "/assets/keystrokesmod/textures/gui/faiths/" + loadedCharName + ext;

                        try {
                            InputStream stream = Miau.class.getResourceAsStream(assetPath);

                            label154: {
                                try {
                                    if (stream != null) {
                                        src = ImageIO.read(stream);
                                        if (src != null) {
                                            break label154;
                                        }
                                    }
                                } catch (Throwable var19) {
                                    if (stream != null) {
                                        try {
                                            stream.close();
                                        } catch (Throwable var14) {
                                            var19.addSuppressed(var14);
                                        }
                                    }

                                    throw var19;
                                }

                                if (stream != null) {
                                    stream.close();
                                }
                                continue;
                            }

                            if (stream != null) {
                                stream.close();
                            }
                            break;
                        } catch (Exception var20) {
                        }
                    }
                }

                if (src == null && !loadedCharName.equals("character")) {
                    try {
                        InputStream stream = Miau.class
                            .getResourceAsStream("/assets/keystrokesmod/textures/gui/faiths/character.png");

                        try {
                            if (stream != null) {
                                src = ImageIO.read(stream);
                            }
                        } catch (Throwable var17) {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Throwable var13) {
                                    var17.addSuppressed(var13);
                                }
                            }

                            throw var17;
                        }

                        if (stream != null) {
                            stream.close();
                        }
                    } catch (Exception var18) {
                    }
                }

                if (src != null) {
                    try {
                        hairGlowColor = extractHairGlowColor(src);
                        long time = System.currentTimeMillis();
                        characterTexture = mc.func_110434_K()
                            .func_110578_a("faiths_char_" + time, new DynamicTexture(src));
                        int w = src.getWidth();
                        int h = src.getHeight();
                        charRawHeight = h;
                        if (h > 0) {
                            charAspect = (float)w / h;
                        }

                        BufferedImage mask = new BufferedImage(w, h, 2);

                        for (int py = 0; py < h; py++) {
                            for (int px = 0; px < w; px++) {
                                int alpha = src.getRGB(px, py) >>> 24 & 0xFF;
                                if (alpha > 0) {
                                    mask.setRGB(px, py, alpha << 24 | 16777215);
                                }
                            }
                        }

                        glowMaskTexture = mc.func_110434_K()
                            .func_110578_a("faiths_char_glow_" + time, new DynamicTexture(mask));
                        characterTextureMap.put(key, characterTexture);
                        glowMaskTextureMap.put(key, glowMaskTexture);
                        characterGlowMap.put(key, hairGlowColor);
                        characterAspectMap.put(key, charAspect);
                        characterRawHeightMap.put(key, charRawHeight);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void drawColoredTexture(
        float x, float y, float width, float height, float r, float g, float b, float a
    ) {
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181709_i);
        int red = (int)(r * 255.0F);
        int green = (int)(g * 255.0F);
        int blue = (int)(b * 255.0F);
        int alpha = (int)(a * 255.0F);
        worldrenderer.func_181662_b(x, y + height, 0.0)
            .func_181673_a(0.0, 1.0)
            .func_181669_b(red, green, blue, alpha)
            .func_181675_d();
        worldrenderer.func_181662_b(x + width, y + height, 0.0)
            .func_181673_a(1.0, 1.0)
            .func_181669_b(red, green, blue, alpha)
            .func_181675_d();
        worldrenderer.func_181662_b(x + width, y, 0.0)
            .func_181673_a(1.0, 0.0)
            .func_181669_b(red, green, blue, alpha)
            .func_181675_d();
        worldrenderer.func_181662_b(x, y, 0.0)
            .func_181673_a(0.0, 0.0)
            .func_181669_b(red, green, blue, alpha)
            .func_181675_d();
        tessellator.func_78381_a();
    }

    public static void renderCharacter(float delta) {
        try {
            ClickGUI clickGuiMod = (ClickGUI)Miau.moduleManager.getModule(ClickGUI.class);
            if (clickGuiMod != null && !clickGuiMod.showCharacter.getValue()) {
                return;
            }
        } catch (Exception var28) {
        }

        String charName = "character";

        try {
            ClickGUI clickGuiMod = (ClickGUI)Miau.moduleManager.getModule(ClickGUI.class);
            if (clickGuiMod != null && clickGuiMod.character != null) {
                charName = clickGuiMod.character.getModeString();
            }
        } catch (Exception var27) {
        }

        loadTexture(charName);
        if (characterTexture != null && glowMaskTexture != null) {
            slideProgress = MathUtil.lerp(slideProgress, 1.0F, 0.03F * delta);
            if (slideProgress > 0.999F) {
                slideProgress = 1.0F;
            }

            ScaledResolution sr = new ScaledResolution(mc);
            float charHeight = charAspect >= 0.78F
                ? Math.min(240.0F, sr.func_78328_b() * 0.7F)
                : Math.min(320.0F, sr.func_78328_b() * 0.85F);
            float charWidth = charHeight * charAspect;
            float targetX = sr.func_78326_a() - charWidth - 10.0F;
            float startX = sr.func_78326_a() + 50.0F;
            float charX = startX + (targetX - startX) * slideProgress;
            float charY = sr.func_78328_b() - charHeight;
            Color glowColor = hairGlowColor;
            if (glowColor == null) {
                glowColor = new Color(180, 140, 255);
            }

            float r = glowColor.getRed() / 255.0F;
            float g = glowColor.getGreen() / 255.0F;
            float b = glowColor.getBlue() / 255.0F;
            GlStateManager.func_179147_l();
            GlStateManager.func_179098_w();
            GL11.glDisable(3089);
            GL11.glDisable(2929);
            mc.func_110434_K().func_110577_a(glowMaskTexture);
            GlStateManager.func_179112_b(1, 1);
            int totalSamples = 81;
            float intensity = 0.65F * slideProgress / totalSamples;
            drawColoredTexture(charX, charY, charWidth, charHeight, r * intensity, g * intensity, b * intensity, 1.0F);
            float[][] rings = new float[][]{{1.5F, 8.0F}, {3.5F, 12.0F}, {6.0F, 16.0F}, {9.0F, 20.0F}, {12.5F, 24.0F}};

            for (float[] ring : rings) {
                float radius = ring[0];
                int count = (int)ring[1];

                for (int j = 0; j < count; j++) {
                    double angle = (Math.PI * 2) * j / count;
                    float offX = (float)(Math.cos(angle) * radius);
                    float offY = (float)(Math.sin(angle) * radius);
                    drawColoredTexture(
                        charX + offX,
                        charY + offY,
                        charWidth,
                        charHeight,
                        r * intensity,
                        g * intensity,
                        b * intensity,
                        1.0F
                    );
                }
            }

            GlStateManager.func_179112_b(770, 771);
            mc.func_110434_K().func_110577_a(characterTexture);
            drawColoredTexture(charX, charY, charWidth, charHeight, 1.0F, 1.0F, 1.0F, slideProgress);
            GlStateManager.func_179117_G();
        }
    }
}
