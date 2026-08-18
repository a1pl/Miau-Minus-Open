package miau.util.font.impl.rise;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class FontCharacter {
    private int texture;
    private final int width;
    private final int height;
    private final BufferedImage image;

    public FontCharacter(int texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.image = null;
    }

    public FontCharacter(BufferedImage image, int width, int height) {
        this.texture = -1;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public void upload() {
        if (this.texture == -1 && this.image != null) {
            this.texture = GL11.glGenTextures();
            int width = this.width;
            int height = this.height;
            int[] pixels = this.image.getRGB(0, 0, width, height, new int[width * height], 0, width);
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
            GlStateManager.func_179144_i(this.texture);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, byteBuffer);
        }
    }

    public void render(float x, float y) {
        if (this.texture == -1) {
            this.upload();
        }

        GL11.glBindTexture(3553, this.texture);
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(x, y + this.height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(x + this.width, y + this.height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(x + this.width, y);
        GL11.glEnd();
    }

    public int getTexture() {
        return this.texture;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
