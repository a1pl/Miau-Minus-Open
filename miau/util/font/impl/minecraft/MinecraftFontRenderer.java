package miau.util.font.impl.minecraft;

import java.awt.Color;
import miau.util.font.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class MinecraftFontRenderer extends Font {
    private final FontRenderer fr = Minecraft.func_71410_x().field_71466_p;

    @Override
    public int draw(String text, double x, double y, int color, boolean dropShadow) {
        return this.fr.func_175065_a(text, (float)x, (float)y, color, dropShadow);
    }

    @Override
    public int draw(String text, double x, double y, int color) {
        return this.fr.func_175065_a(text, (float)x, (float)y, color, true);
    }

    @Override
    public int drawWithShadow(String text, double x, double y, int color) {
        return this.fr.func_175063_a(text, (float)x, (float)y, color);
    }

    @Override
    public int width(String text) {
        return this.fr.func_78256_a(text);
    }

    @Override
    public int drawCentered(String text, double x, double y, int color) {
        return this.fr.func_175065_a(text, (float)(x - this.fr.func_78256_a(text) / 2.0), (float)y, color, false);
    }

    @Override
    public int drawRight(String text, double x, double y, int color) {
        return this.fr.func_175065_a(text, (float)(x - this.fr.func_78256_a(text)), (float)y, color, false);
    }

    @Override
    public float height() {
        return this.fr.field_78288_b;
    }

    @Override
    public void drawCharacter(char character, int x, int y, Color color) {
        this.fr.func_175063_a(String.valueOf(character), x, y, color.getRGB());
    }
}
