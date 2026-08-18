package miau.util.font;

import java.awt.Color;

public abstract class Font {
    public abstract int draw(String var1, double var2, double var4, int var6, boolean var7);

    public abstract int draw(String var1, double var2, double var4, int var6);

    public abstract int drawWithShadow(String var1, double var2, double var4, int var6);

    public abstract int width(String var1);

    public abstract int drawCentered(String var1, double var2, double var4, int var6);

    public abstract int drawRight(String var1, double var2, double var4, int var6);

    public abstract float height();

    public abstract void drawCharacter(char var1, int var2, int var3, Color var4);

    public int getStringWidth(String text) {
        return this.width(text);
    }

    public int getFontHeight() {
        return (int)this.height();
    }

    public int getTextTopOffset() {
        return 0;
    }

    public int getTextBottomOffset() {
        return (int)this.height();
    }
}
