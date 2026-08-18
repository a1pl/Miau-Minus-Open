package miau.util.font.impl.rise;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import miau.util.font.Font;
import miau.util.render.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class FontRenderer extends Font {
    private static final String ALPHABET = "ABCDEFGHOKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String COLOR_CODE_CHARACTERS = "0123456789abcdefklmnor";
    private static final Color TRANSPARENT_COLOR = new Color(255, 255, 255, 0);
    private static final float SCALE = 0.5F;
    private static final float SCALE_INVERSE = 2.0F;
    private static final char COLOR_INVOKER = '§';
    private static final int[] COLOR_CODES = new int[32];
    private static final int LATIN_MAX_AMOUNT = 256;
    private static final int MARGIN_WIDTH = 4;
    private static final int MASK = 255;
    private final java.awt.Font font;
    private final boolean fractionalMetrics;
    private final float fontHeight;
    private final Map<Character, FontCharacter> regularCharacters = new HashMap<>();
    private final Map<Character, FontCharacter> boldCharacters = new HashMap<>();
    private boolean antialiasing = true;
    private boolean international = false;
    private java.awt.Font plainFont;
    private java.awt.Font boldFont;
    private FontMetrics plainFontMetrics;
    private FontMetrics boldFontMetrics;
    private Graphics2D plainFontGraphics;
    private Graphics2D boldFontGraphics;

    public FontRenderer(java.awt.Font font, boolean fractionalMetrics, boolean antialiasing, boolean international) {
        calculateColorCodes();
        this.antialiasing = antialiasing;
        this.font = font;
        this.fractionalMetrics = fractionalMetrics;
        this.fontHeight = (float)(
            font.getStringBounds(
                        "ABCDEFGHOKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
                        new FontRenderContext(new AffineTransform(), antialiasing, fractionalMetrics)
                    )
                    .getHeight()
                / 2.0
        );
        this.setupFonts();
        this.fillCharacters(this.regularCharacters, 0);
        this.fillCharacters(this.boldCharacters, 1);
        this.international = international;
    }

    public FontRenderer(java.awt.Font font, boolean fractionalMetrics, boolean antialiasing) {
        this(font, fractionalMetrics, antialiasing, false);
    }

    public FontRenderer(java.awt.Font font, boolean fractionalMetrics) {
        this(font, fractionalMetrics, true, false);
    }

    private void setupFonts() {
        this.plainFont = this.font.deriveFont(0);
        this.boldFont = this.font.deriveFont(1);
        BufferedImage plainFontImage = new BufferedImage(1, 1, 2);
        this.plainFontGraphics = (Graphics2D)plainFontImage.getGraphics();
        this.plainFontMetrics = this.plainFontGraphics.getFontMetrics(this.plainFont);
        BufferedImage boldFontImage = new BufferedImage(1, 1, 2);
        this.boldFontGraphics = (Graphics2D)boldFontImage.getGraphics();
        this.boldFontMetrics = this.boldFontGraphics.getFontMetrics(this.boldFont);
    }

    public static void calculateColorCodes() {
        for (int i = 0; i < 32; i++) {
            int amplifier = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + amplifier;
            int green = (i >> 1 & 1) * 170 + amplifier;
            int blue = (i & 1) * 170 + amplifier;
            if (i == 6) {
                red += 85;
            }

            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            COLOR_CODES[i] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }
    }

    public void fillCharacters(Map<Character, FontCharacter> characters, int style) {
        for (int i = 0; i < 256; i++) {
            char character = (char)i;
            characters.put(character, this.createCharacter(character, style));
        }
    }

    private FontCharacter createCharacter(char character, int style) {
        java.awt.Font font = style == 1 ? this.boldFont : this.plainFont;
        Graphics2D fontGraphics = style == 1 ? this.boldFontGraphics : this.plainFontGraphics;
        FontMetrics fontMetrics = style == 1 ? this.boldFontMetrics : this.plainFontMetrics;
        Rectangle2D charRectangle = fontMetrics.getStringBounds(character + "", fontGraphics);
        int width = Math.max(1, MathHelper.func_76123_f((float)charRectangle.getWidth()) + 8);
        int maxAscent = fontMetrics.getMaxAscent();
        int maxDescent = fontMetrics.getMaxDescent();
        int baseline = Math.max(font.getSize(), maxAscent);
        int height = Math.max(1, baseline + maxDescent);
        BufferedImage charImage = new BufferedImage(width, height, 2);
        Graphics2D charGraphics = (Graphics2D)charImage.getGraphics();
        charGraphics.setFont(font);
        charGraphics.setColor(TRANSPARENT_COLOR);
        charGraphics.fillRect(0, 0, width, height);
        this.setRenderHints(charGraphics);
        charGraphics.drawString(character + "", 4, baseline);
        if (Minecraft.func_71410_x().func_152345_ab()) {
            int charTexture = GL11.glGenTextures();
            this.uploadTexture(charTexture, charImage, width, height);
            return new FontCharacter(charTexture, width, height);
        } else {
            return new FontCharacter(charImage, width, height);
        }
    }

    private synchronized FontCharacter getCharacter(char character, int style) {
        Map<Character, FontCharacter> characters = style == 1 ? this.boldCharacters : this.regularCharacters;
        FontCharacter fontCharacter = characters.get(character);
        if (fontCharacter == null) {
            fontCharacter = this.createCharacter(character, style);
            characters.put(character, fontCharacter);
        }

        if (fontCharacter.getTexture() == -1 && Minecraft.func_71410_x().func_152345_ab()) {
            fontCharacter.upload();
        }

        return fontCharacter;
    }

    public void setRenderHints(Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        if (this.antialiasing) {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            this.fractionalMetrics
                ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                : RenderingHints.VALUE_FRACTIONALMETRICS_OFF
        );
    }

    public void uploadTexture(int texture, BufferedImage image, int width, int height) {
        int[] pixels = image.getRGB(0, 0, width, height, new int[width * height], 0, width);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[x + y * width];
                byteBuffer.put((byte)(pixel >> 16 & 0xFF));
                byteBuffer.put((byte)(pixel >> 8 & 0xFF));
                byteBuffer.put((byte)(pixel & 0xFF));
                byteBuffer.put((byte)(pixel >> 24 & 0xFF));
            }
        }

        ((Buffer)byteBuffer).flip();
        GlStateManager.func_179144_i(texture);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, byteBuffer);
    }

    @Override
    public int draw(String text, double x, double y, int color) {
        return this.draw(text, x, y, color, false);
    }

    @Override
    public int drawCentered(String text, double x, double y, int color) {
        return this.draw(text, x - (this.width(text) >> 1), y, color, false);
    }

    @Override
    public int drawRight(String text, double x, double y, int color) {
        return this.draw(text, x - this.width(text), y, color, false);
    }

    @Override
    public int drawWithShadow(String text, double x, double y, int color) {
        return this.draw(text, x, y, color, false);
    }

    public void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        this.draw(text, x - (this.width(text) >> 1), y, color, false);
    }

    @Override
    public int draw(String text, double x, double y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }

        double givenX = x;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        GL11.glEnable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        x -= 2.0;
        y -= 2.0;
        x *= 2.0;
        y *= 2.0;
        y -= this.fontHeight / 5.0F;
        double startX = x;
        ColorUtil.glColor(shadow ? Color.white.getRGB() : color);
        text = text.replaceAll("§l", "");

        try {
            char[] characters = text.toCharArray();
            int textLength = characters.length;
            int lineHeightTimes2 = (int)(this.height() * 2.0F);
            int marginWidthTimes2 = 8;

            for (int i = 0; i < textLength; i++) {
                char character = characters[i];
                if (character == '\n') {
                    x = startX;
                    y += lineHeightTimes2;
                } else if (character == 167) {
                    if (++i < characters.length) {
                        int index = "0123456789abcdefklmnor".indexOf(characters[i]);
                        if (index != -1 && index < COLOR_CODES.length) {
                            ColorUtil.glColor(COLOR_CODES[index]);
                        }
                    }
                } else {
                    FontCharacter fontCharacter = this.getCharacter(character, 0);
                    if (fontCharacter != null) {
                        float characterWidth = fontCharacter.getWidth();
                        fontCharacter.render((float)x, (float)y);
                        x += characterWidth - 8.0F;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        GL11.glDisable(3042);
        GL11.glDisable(3553);
        GlStateManager.func_179144_i(0);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        return (int)(x - givenX);
    }

    @Override
    public void drawCharacter(char character, int x, int y, Color color) {
        FontCharacter fontCharacter = this.getCharacter(character, 0);
        if (fontCharacter != null) {
            GlStateManager.func_179131_c(
                color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
            );
            fontCharacter.render(x, y);
        }
    }

    @Override
    public int width(String text) {
        if (text == null) {
            return 0;
        }

        text = text.replaceAll("§l", "");
        int length = text.length();
        int width = 0;

        for (int i = 0; i < length; i++) {
            char character = text.charAt(i);
            if (character == 167) {
                i++;
            } else {
                FontCharacter fontCharacter = this.getCharacter(character, 0);
                if (fontCharacter != null) {
                    width += fontCharacter.getWidth() - 8;
                }
            }
        }

        return width / 2;
    }

    @Override
    public float height() {
        return this.fontHeight;
    }

    private boolean requiresInternationalFont(String text) {
        int highest = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > highest) {
                highest = text.charAt(i);
            }
        }

        return highest >= 256;
    }
}
