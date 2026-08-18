package miau.util.render;

import java.awt.Color;
import miau.Miau;
import miau.module.modules.render.HUD;
import miau.util.font.Font;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class ColorUtil {
    public static final Color RED = new Color(255, 0, 0);
    public static final Color GOLD = new Color(255, 165, 0);
    public static final Color YELLOW = new Color(255, 255, 0);
    public static final Color GREEN = new Color(0, 255, 0);

    private ColorUtil() {
    }

    public static Color fromHSB(float hue, float saturation, float brightness) {
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public static Color interpolate(float progress, Color startColor, Color endColor) {
        progress = Math.min(Math.max(progress, 0.0F), 1.0F);
        return new Color(
            (int)(startColor.getRed() + progress * (endColor.getRed() - startColor.getRed())),
            (int)(startColor.getGreen() + progress * (endColor.getGreen() - startColor.getGreen())),
            (int)(startColor.getBlue() + progress * (endColor.getBlue() - startColor.getBlue()))
        );
    }

    public static Color getHealthBlend(float percent) {
        if (percent >= 0.9F) {
            return GREEN;
        } else if (percent >= 0.55F) {
            return interpolate((percent - 0.55F) / 0.35F, YELLOW, GREEN);
        } else if (percent >= 0.45F) {
            return YELLOW;
        } else {
            return percent >= 0.1F ? interpolate((percent - 0.1F) / 0.35F, RED, YELLOW) : RED;
        }
    }

    public static Color scale(Color color, float scaleFactor, int alpha) {
        return new Color(
            Math.min(Math.max((int)(color.getRed() * scaleFactor), 0), 255),
            Math.min(Math.max((int)(color.getGreen() * scaleFactor), 0), 255),
            Math.min(Math.max((int)(color.getBlue() * scaleFactor), 0), 255),
            alpha
        );
    }

    public static void glColor(int hex) {
        float a = (hex >> 24 & 0xFF) / 255.0F;
        float r = (hex >> 16 & 0xFF) / 255.0F;
        float g = (hex >> 8 & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;
        if (a == 0.0F) {
            a = 1.0F;
        }

        GL11.glColor4f(r, g, b, a);
        GlStateManager.func_179131_c(r, g, b, a);
    }

    public static void glColor(Color color) {
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;
        GL11.glColor4f(r, g, b, a);
        GlStateManager.func_179131_c(r, g, b, a);
    }

    public static Color darker(Color color, float factor) {
        return scale(color, factor, color.getAlpha());
    }

    public static Color brighter(Color color, float factor) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();
        int i = (int)(1.0F / (1.0F - factor));
        if (red == 0 && green == 0 && blue == 0) {
            return new Color(i, i, i, alpha);
        }

        if (red > 0 && red < i) {
            red = i;
        }

        if (green > 0 && green < i) {
            green = i;
        }

        if (blue > 0 && blue < i) {
            blue = i;
        }

        return new Color(
            Math.min((int)(red / factor), 255),
            Math.min((int)(green / factor), 255),
            Math.min((int)(blue / factor), 255),
            alpha
        );
    }

    public static Color withRed(Color color, int red) {
        return new Color(red, color.getGreen(), color.getBlue());
    }

    public static Color withGreen(Color color, int green) {
        return new Color(color.getRed(), green, color.getBlue());
    }

    public static Color withBlue(Color color, int blue) {
        return new Color(color.getRed(), color.getGreen(), blue);
    }

    public static Color withAlpha(Color color, int alpha) {
        if (alpha == color.getAlpha()) {
            return color;
        }

        int clampedAlpha = Math.min(255, Math.max(0, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
    }

    public static Color mixColors(Color color1, Color color2, double percent) {
        double inverse_percent = 1.0 - percent;
        int redPart = (int)(color1.getRed() * percent + color2.getRed() * inverse_percent);
        int greenPart = (int)(color1.getGreen() * percent + color2.getGreen() * inverse_percent);
        int bluePart = (int)(color1.getBlue() * percent + color2.getBlue() * inverse_percent);
        return new Color(redPart, greenPart, bluePart);
    }

    public static double getBlendFactor(Vector2d screenCoordinates) {
        return Math.sin(
                    System.currentTimeMillis() / 600.0
                        + screenCoordinates.getX() * 0.005
                        + screenCoordinates.getY() * 0.06
                )
                * 0.5
            + 0.5;
    }

    public static Color rainbow(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 10.0);
        rainbowState %= 360.0;
        return Color.getHSBColor((float)(rainbowState / 360.0), 0.6F, 1.0F);
    }

    public static void drawInterpolatedText(Font font, String text, double x, double y, boolean shadow) {
        float w = 0.0F;
        Themes theme = Themes.getCurrentTheme();

        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            Color color = mixColors(theme.getFirstColor(), theme.getSecondColor(), Math.sin(i * 0.095) * 0.5 + 0.5);
            if (shadow) {
                font.drawWithShadow(character, x + w, y, color.getRGB());
            } else {
                font.draw(character, x + w, y, color.getRGB());
            }

            w += font.width(character) + 0.5F;
        }
    }

    public static void drawInterpolatedText(Font font, String text, double x, double y) {
        drawInterpolatedText(font, text, x, y, true);
    }

    public static int getChroma(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float)(time % (15000L / speed)) / (15000.0F / (float)speed), 1.0F, 1.0F).getRGB();
    }

    public static float drawThemeString(String text, float x, float y, boolean shadow) {
        HUD hud = null;
        if (Miau.moduleManager != null) {
            hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
        }

        Minecraft mc = Minecraft.func_71410_x();
        float currentX = x;

        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            int color;
            if (hud != null && hud.isEnabled()) {
                color = hud.getColor(System.currentTimeMillis(), i).getRGB();
            } else {
                Themes theme = Themes.getCurrentTheme();
                color = theme.getAccentColor(new Vector2d(0.0, i * 15)).getRGB();
            }

            if (shadow) {
                mc.field_71466_p.func_175063_a(character, currentX, y, color);
            } else {
                mc.field_71466_p.func_175065_a(character, currentX, y, color, false);
            }

            currentX += mc.field_71466_p.func_78256_a(character);
        }

        return currentX;
    }

    public static String getClosestVanillaColor(Color color) {
        int[] colors = new int[]{
            0,
            170,
            43520,
            43690,
            11141120,
            11141290,
            16755200,
            11184810,
            5592405,
            5592575,
            5635925,
            5636095,
            16733525,
            16733695,
            16777045,
            16777215
        };
        String[] codes = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        int closest = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < colors.length; i++) {
            int r = colors[i] >> 16 & 0xFF;
            int g = colors[i] >> 8 & 0xFF;
            int b = colors[i] & 0xFF;
            double distance = Math.pow(color.getRed() - r, 2.0)
                + Math.pow(color.getGreen() - g, 2.0)
                + Math.pow(color.getBlue() - b, 2.0);
            if (distance < minDistance) {
                minDistance = distance;
                closest = i;
            }
        }

        return "§" + codes[closest];
    }

    public static String getThemedName(String name) {
        StringBuilder sb = new StringBuilder();
        HUD hud = null;
        if (Miau.moduleManager != null) {
            hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
        }

        for (int i = 0; i < name.length(); i++) {
            Color c;
            if (hud != null && hud.isEnabled()) {
                c = hud.getColor(System.currentTimeMillis(), i);
            } else {
                Themes theme = Themes.getCurrentTheme();
                c = theme.getAccentColor(new Vector2d(0.0, i * 15));
            }

            sb.append(getClosestVanillaColor(c)).append(name.charAt(i));
        }

        return sb.toString();
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int[] getRGBA(int color) {
        return new int[]{color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF};
    }
}
