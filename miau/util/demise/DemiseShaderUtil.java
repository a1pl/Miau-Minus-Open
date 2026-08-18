package miau.util.demise;

import org.lwjgl.opengl.GL11;

public class DemiseShaderUtil {
    private final ShaderUtils shader;

    public DemiseShaderUtil(String fragmentShader) {
        this.shader = new ShaderUtils(fragmentShader);
    }

    public void init() {
        this.shader.init();
    }

    public void unload() {
        this.shader.unload();
    }

    public void setUniformf(String name, float... values) {
        this.shader.setUniformf(name, values);
    }

    public void setUniformi(String name, int... values) {
        this.shader.setUniformi(name, values);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }
}
